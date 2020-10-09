package com.mkurth.coinsplasher.console

import java.math.BigInteger

import cats.data.NonEmptyList
import cats.effect.{ContextShift, IO}
import com.mkurth.coinsplasher.domain.TradingPlanner.{BuyOrder, SellOrder}
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.{NonNegative, Positive}
import eu.timepit.refined.{refineMV, refineV}
import io.circe
import io.circe.DecodingFailure
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client._
import sttp.client.circe._
import io.circe.generic.auto._
import io.circe.refined._
import sttp.client.SttpBackend
import sttp.client.asynchttpclient.WebSocketHandler

import scala.<:<.refl

final case class ServerTimeResponse(serverTime: Long)
final case class Balance(asset: String Refined NonEmpty, free: BigDecimal Refined NonNegative, locked: BigDecimal Refined NonNegative)
final case class AccountBalance(balances: List[Balance])
final case class AvgPrice(price: String)
trait BinanceClient[F[_], E] {

  def getServerTime: F[Long]
  def currentBalance: IO[Either[E, AccountBalance]]
  def avgPrice(symbol: String Refined NonEmpty): IO[Either[E, BigDecimal Refined Positive]]
  def putSellOrders(sellOrders: NonEmptyList[SellOrder]): F[Unit]
  def putBuyOrders(buyOrders: NonEmptyList[BuyOrder]): F[Unit]

}

object BinanceClient {
  type SecretKey = String Refined NonEmpty
  type Data      = String Refined NonEmpty
  type ApiKey    = String Refined NonEmpty

  def apply(secretKey: SecretKey, apiKey: ApiKey): BinanceClient[IO, String] = new BinanceClient[IO, String] {
    implicit val cs: ContextShift[IO]                             = IO.contextShift(scala.concurrent.ExecutionContext.global)
    val ioBackend: IO[SttpBackend[IO, Nothing, WebSocketHandler]] = AsyncHttpClientCatsBackend[IO]()

    override def getServerTime: IO[Long] =
      for {
        backend  <- ioBackend
        response <- basicRequest.get(uri"https://api.binance.com/api/v3/time").response(asJson[ServerTimeResponse]).send()(backend, refl)
      } yield {
        response.body.map(_.serverTime).getOrElse(0L)
      }

    override def currentBalance: IO[Either[String, AccountBalance]] =
      for {
        backend   <- ioBackend
        timestamp <- getServerTime
        queryParams: Data = refineV[NonEmpty](s"recvWindow=60000&timestamp=$timestamp").toOption.get
        signature <- sign(queryParams, secretKey) match {
          case Some(value) => IO(value)
          case None        => IO.raiseError(new IllegalArgumentException("signing didn't work. wrong key?"))
        }
        response <- basicRequest
          .get(uri"https://api.binance.com/api/v3/account?recvWindow=60000&timestamp=$timestamp&signature=$signature")
          .header("X-MBX-APIKEY", apiKey.value)
          .response(asJson[AccountBalance])
          .send()(backend, refl)
      } yield {
        response.body.left.map(_.toString).map(ab => ab.copy(balances = ab.balances.filter(_.free.value > 0)))
      }

    override def putSellOrders(sellOrders: NonEmptyList[SellOrder]): IO[Unit] = ???

    override def putBuyOrders(buyOrders: NonEmptyList[BuyOrder]): IO[Unit] = ???

    override def avgPrice(symbol: Refined[String, NonEmpty]): IO[Either[String, Refined[BigDecimal, Positive]]] =
      for {
        backend <- ioBackend
        _ = println(s"looking up $symbol")
        response <- basicRequest.get(uri"https://api.binance.com/api/v3/avgPrice?symbol=$symbol").response(asJson[AvgPrice]).send()(backend, refl)
      } yield {
        response.body.flatMap(avg => refineV[Positive](BigDecimal(avg.price))).left.map(_.toString)
      }
  }

  def sign(data: Data, secretKey: SecretKey): Option[String Refined NonEmpty] = {
    val mac           = Mac.getInstance("HmacSHA256")
    val secretKeySpec = new SecretKeySpec(secretKey.value.getBytes(), "HmacSHA256")
    mac.init(secretKeySpec)
    refineV[NonEmpty](String.format("%032x", new BigInteger(1, mac.doFinal(data.value.getBytes())))).toOption
  }
}
