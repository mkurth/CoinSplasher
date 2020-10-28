package com.mkurth.coinsplasher.console

import java.math.{BigInteger, MathContext, RoundingMode}

import cats.data.NonEmptyList
import cats.effect.{ContextShift, IO}
import cats.implicits.toTraverseOps
import cats.syntax.functor._
import com.mkurth.coinsplasher.domain.RefinedOps.{NonEmptyString, PositiveBigDecimal}
import com.mkurth.coinsplasher.domain.TradingPlanner.{BuyOrder, SellOrder}
import com.mkurth.coinsplasher.domain.{Currency, Share}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.{NonNegative, Positive}
import eu.timepit.refined.refineV
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.refined._
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import sttp.client._
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.circe._

import scala.<:<.refl

final case class ServerTimeResponse(serverTime: Long)
final case class Balance(asset: String Refined NonEmpty, free: BigDecimal Refined NonNegative, locked: BigDecimal Refined NonNegative)
final case class AccountBalance(balances: List[Balance])
final case class AvgPrice(price: String)

trait BinanceClient[F[_], E] {

  def currentBalance: IO[Either[E, AccountBalance]]
  def avgPrice(symbol: NonEmptyString): IO[Either[E, BigDecimal Refined Positive]]
  def putSellOrders(sellOrders: NonEmptyList[SellOrder]): F[Unit]
  def putBuyOrders(buyOrders: NonEmptyList[BuyOrder]): F[Unit]

}

object BinanceClient {
  implicit val decodeSymbolFilter: Decoder[SymbolFilter] =
    List[Decoder[SymbolFilter]](
      Decoder[PriceSymbolFilter].widen,
      Decoder[PercentSymbolFilter].widen,
      Decoder[LotSize].widen,
      Decoder[MinNotional].widen,
      Decoder[IcebergParts].widen,
      Decoder[MaxNumOrders].widen,
      Decoder[MaxNumAlgoOrders].widen,
      Decoder[MaxNumIcebergOrders].widen,
      Decoder[MaxPositionSymbolFilter].widen
    ).reduceLeft(_ or _)
  sealed trait SymbolFilter
  final case class PriceSymbolFilter(minPrice: String, maxPrice: String, tickSize: String) extends SymbolFilter
  final case class PercentSymbolFilter(multiplierUp: String, multiplierDown: String, avgPriceMins: Int) extends SymbolFilter
  final case class LotSize(minQty: String, maxQty: String, stepSize: String, filterType: String) extends SymbolFilter
  final case class MinNotional(minNotional: String, applyToMarket: Boolean, avgPriceMins: Int) extends SymbolFilter
  final case class IcebergParts(limit: Int) extends SymbolFilter
  final case class MaxNumOrders(maxNumOrders: Int) extends SymbolFilter
  final case class MaxNumAlgoOrders(maxNumAlgoOrders: Int) extends SymbolFilter
  final case class MaxNumIcebergOrders(maxNumIcebergOrders: Int) extends SymbolFilter
  final case class MaxPositionSymbolFilter(maxPosition: String) extends SymbolFilter
  final case class ExchangeSymbol(symbol: String,
                                  status: String,
                                  baseAsset: String,
                                  baseAssetPrecision: Int,
                                  quoteAsset: String,
                                  quotePrecision: Int,
                                  orderTypes: List[String],
                                  filters: List[SymbolFilter])
  final case class ExchangeInfo(timezone: String, serverTime: Long, symbols: List[ExchangeSymbol])

  type SecretKey = NonEmptyString
  type Data      = NonEmptyString
  type ApiKey    = NonEmptyString

  implicit private val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  def apply(secretKey: SecretKey, apiKey: ApiKey): IO[BinanceClient[IO, String]] =
    for {
      backend      <- AsyncHttpClientCatsBackend[IO]()
      exchangeInfo <- basicRequest.get(uri"https://api.binance.com/api/v3/exchangeInfo").response(asJson[ExchangeInfo]).send()(backend, refl).map(_.body.toOption.get)
      serverTimeOffset <- basicRequest
        .get(uri"https://api.binance.com/api/v3/time")
        .response(asJson[ServerTimeResponse])
        .send()(backend, refl)
        .map(_.body.map(_.serverTime).getOrElse(0L))
        .map(serverTime => System.currentTimeMillis() - serverTime)
    } yield {
      new BinanceClient[IO, String] {

        override def currentBalance: IO[Either[String, AccountBalance]] =
          for {
            timestamp <- getServerTime
            queryParams = refineV[NonEmpty](s"recvWindow=60000&timestamp=$timestamp").toOption.get
            signature <- signIO(secretKey, queryParams)
            response <- basicRequest
              .get(uri"https://api.binance.com/api/v3/account?recvWindow=60000&timestamp=$timestamp&signature=$signature")
              .header("X-MBX-APIKEY", apiKey.value)
              .response(asJson[AccountBalance])
              .send()(backend, refl)
          } yield {
            response.body.left.map(_.toString).map(ab => ab.copy(balances = ab.balances.filter(_.free.value > 0)))
          }

        override def putSellOrders(sellOrders: NonEmptyList[SellOrder]): IO[Unit] =
          sellOrders.filter(_.currency.name.value.toLowerCase != "btc").traverse(so => sendOrder("SELL", so.currency, so.share)).as(())
        override def putBuyOrders(buyOrders: NonEmptyList[BuyOrder]): IO[Unit] =
          buyOrders.filter(_.currency.name.value.toLowerCase != "btc").traverse(bo => sendOrder("BUY", bo.currency, bo.share)).as(())
        override def avgPrice(symbol: Refined[String, NonEmpty]): IO[Either[String, Refined[BigDecimal, Positive]]] =
          basicRequest
            .get(uri"https://api.binance.com/api/v3/avgPrice?symbol=$symbol")
            .response(asJson[AvgPrice])
            .send()(backend, refl)
            .map(_.body.flatMap(avg => refineV[Positive](BigDecimal(avg.price))).left.map(_.toString))

        private def sendOrder(side: String, currency: Currency, share: Share): IO[Unit] =
          exchangeInfo.symbols.find(assetOnExchange(_, currency)) match {
            case Some(exchangeSymbol) if shareIsInLotSize(exchangeSymbol, share.value) =>
              for {
                timestamp <- getServerTime
                symbolName     = exchangeSymbol.baseAsset + exchangeSymbol.quoteAsset
                shareValue     = roundAndAdjustShare(exchangeSymbol, share.value)
                quoteOrBaseQty = if (exchangeSymbol.baseAsset == "BTC") "quoteOrderQty" else "quantity"
                queryParams    = refineV[NonEmpty](s"symbol=$symbolName&side=$side&type=MARKET&$quoteOrBaseQty=$shareValue&timestamp=$timestamp").toOption.get
                signature <- signIO(secretKey, queryParams)
                response <- basicRequest
                  .post(uri"https://api.binance.com/api/v3/order/test")
                  .body(queryParams + s"&signature=$signature")
                  .header("X-MBX-APIKEY", apiKey.value)
                  .send()(backend, refl)
                res <- if (response.code.isSuccess) IO.unit else IO(println(s"Error while trying to send $queryParams with signature $signature: ${response.body}"))
              } yield res
            case Some(_) => IO(println("Target share is too low or too high"))
            case None    => IO(println(s"Asset ${currency.name} not found on exchange"))
          }

        private def getServerTime: IO[Long] = IO(System.currentTimeMillis() + serverTimeOffset)
      }
    }

  private def assetOnExchange(es: ExchangeSymbol, currency: Currency): Boolean =
    (es.baseAsset == currency.name.value.toUpperCase && es.quoteAsset == "BTC") || (es.quoteAsset == currency.name.value.toUpperCase && es.baseAsset == "BTC")

  private def roundAndAdjustShare(exchangeSymbol: ExchangeSymbol, share: PositiveBigDecimal) = {
    val adjustedToStepSize = exchangeSymbol.filters
      .collectFirst {
        case LotSize(_, _, stepSize, "LOT_SIZE") if BigDecimal(stepSize) > 0 => (share.value / BigDecimal(stepSize)).toInt * BigDecimal(stepSize)
      }
      .getOrElse(share.value)
    adjustedToStepSize.round(new MathContext(exchangeSymbol.quotePrecision, RoundingMode.DOWN))
  }

  private def shareIsInLotSize(exchangeSymbol: ExchangeSymbol, share: PositiveBigDecimal): Boolean =
    exchangeSymbol.filters.forall {
      case LotSize(minQty, maxQty, _, "LOT_SIZE") => share.value >= BigDecimal(minQty) && share.value <= BigDecimal(maxQty)
      case _                                      => true
    }

  private def signIO(secretKey: SecretKey, queryParams: Refined[String, NonEmpty]) =
    sign(queryParams, secretKey) match {
      case Some(value) => IO.pure(value)
      case None        => IO.raiseError(new IllegalArgumentException("signing didn't work. wrong key?"))
    }

  def sign(data: Data, secretKey: SecretKey): Option[NonEmptyString] = {
    val mac           = Mac.getInstance("HmacSHA256")
    val secretKeySpec = new SecretKeySpec(secretKey.value.getBytes(), "HmacSHA256")
    mac.init(secretKeySpec)
    refineV[NonEmpty](String.format("%032x", new BigInteger(1, mac.doFinal(data.value.getBytes())))).toOption
  }
}
