package com.mkurth.coinsplasher.console

import java.math.BigInteger

import cats.data.NonEmptyList
import cats.effect.{ContextShift, IO}
import com.mkurth.coinsplasher.domain.TradingPlanner.{BuyOrder, SellOrder}
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.{refineMV, refineV}
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
trait BinanceClient[F[_]] {

  def getServerTime: F[Long]
  def currentBalance: IO[Either[ResponseError[io.circe.Error], AccountBalance]]
  def putSellOrders(sellOrders: NonEmptyList[SellOrder]): F[Unit]
  def putBuyOrders(buyOrders: NonEmptyList[BuyOrder]): F[Unit]

}

object BinanceClient {
  type SecretKey = String Refined NonEmpty
  type Data      = String Refined NonEmpty
  type ApiKey    = String Refined NonEmpty

  def apply(secretKey: SecretKey, apiKey: ApiKey): BinanceClient[IO] = new BinanceClient[IO] {
    implicit val cs: ContextShift[IO]                             = IO.contextShift(scala.concurrent.ExecutionContext.global)
    val ioBackend: IO[SttpBackend[IO, Nothing, WebSocketHandler]] = AsyncHttpClientCatsBackend[IO]()

    override def getServerTime: IO[Long] =
      for {
        backend  <- ioBackend
        response <- basicRequest.get(uri"https://api.binance.com/api/v3/time").response(asJson[ServerTimeResponse]).send()(backend, refl)
      } yield {
        response.body.map(_.serverTime).getOrElse(0L)
      }

    override def currentBalance: IO[Either[ResponseError[io.circe.Error], AccountBalance]] =
      for {
        backend   <- ioBackend
        timestamp <- getServerTime
        queryParams: Data = refineV[NonEmpty](s"recvWindow=60000&timestamp=$timestamp").toOption.get
        signature <- sign(queryParams, secretKey) match {
          case Some(value) => IO(value)
          case None        => IO.raiseError(new IllegalArgumentException("signing didn't work. wrong key?"))
        }
        response <- basicRequest
          .get(uri"https://api.binance.com/api/v3/account?$queryParams&signature=$signature")
          .header("X-MBX-APIKEY", apiKey.value)
          .response(asJson[AccountBalance])
          .send()(backend, refl)
      } yield {
        response.body
      }

    override def putSellOrders(sellOrders: NonEmptyList[SellOrder]): IO[Unit] = ???

    override def putBuyOrders(buyOrders: NonEmptyList[BuyOrder]): IO[Unit] = ???
  }

  def sign(data: Data, secretKey: SecretKey): Option[String Refined NonEmpty] = {
    val mac           = Mac.getInstance("HmacSHA256")
    val secretKeySpec = new SecretKeySpec(secretKey.value.getBytes(), "HmacSHA256")
    mac.init(secretKeySpec)
    refineV[NonEmpty](String.format("%032x", new BigInteger(1, mac.doFinal(data.value.getBytes())))).toOption
  }
}
