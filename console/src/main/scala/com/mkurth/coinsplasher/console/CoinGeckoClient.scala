package com.mkurth.coinsplasher.console

import cats.data.NonEmptyList
import cats.effect.{ContextShift, IO}
import com.mkurth.coinsplasher.domain._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.refineV
import io.circe.Decoder.decodeNonEmptyList
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.circe.asJson
import sttp.client.{SttpBackend, basicRequest, _}

import scala.<:<.refl

@ConfiguredJsonCodec
final case class MarketData(symbol: String, marketCap: Long, marketCapRank: Int, currentPrice: BigDecimal)

object MarketData {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
}

trait CoinGeckoClient[F[_]] {

  def markets[A <: Currency](currency: A): F[Either[String, NonEmptyList[Coin[A]]]]

}

object CoinGeckoClient {
  def apply(): CoinGeckoClient[IO] = new CoinGeckoClient[IO] {
    implicit val cs: ContextShift[IO]                             = IO.contextShift(scala.concurrent.ExecutionContext.global)
    val ioBackend: IO[SttpBackend[IO, Nothing, WebSocketHandler]] = AsyncHttpClientCatsBackend[IO]()

    override def markets[A <: Currency](currency: A): IO[Either[String, NonEmptyList[Coin[A]]]] =
      for {
        backend <- ioBackend
        response <- basicRequest
          .get(uri"https://api.coingecko.com/api/v3/coins/markets?vs_currency=${currency.name.value}&order=market_cap_desc&per_page=100&page=1&sparkline=false")
          .response(asJson[NonEmptyList[MarketData]])
          .send()(backend, refl)
      } yield {
        response.body.left
          .map(_.toString)
          .map(
            _.toList.flatMap(
              md =>
                for {
                  positiveMarketCap <- refineV[Positive](BigDecimal(md.marketCap)).toOption
                  positivePrice     <- refineV[Positive](md.currentPrice).toOption
                  symbol            <- refineV[NonEmpty](md.symbol).toOption
                } yield
                  Coin[A](
                    marketCapitalisation = MarketCapitalisation(positiveMarketCap),
                    price                = Price(positivePrice),
                    CryptoCurrency(symbol, '$')
                )
            )
          )
          .map(NonEmptyList.fromList)
          .flatMap {
            case Some(value) => Right(value)
            case None        => Left("All entries from coingecko return invalid data")
          }
      }
  }
}
