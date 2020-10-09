package com.mkurth.coinsplasher.console

import cats.effect.{ContextShift, IO}
import com.mkurth.coinsplasher.domain.{Coin, CryptoCurrency, Euro, MarketCapitalisation, PortfolioEntry, Price, Share}
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.refineV
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.circe.asJson
import io.circe.generic.auto._
import io.circe.generic.extras._
import io.circe.generic.semiauto._
import sttp.client.{SttpBackend, basicRequest, _}

import scala.<:<.refl

@ConfiguredJsonCodec
final case class MarketData(symbol: String, marketCap: Long, marketCapRank: Int, currentPrice: BigDecimal)

object MarketData {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
}

trait CoinGeckoClient[F[_]] {

  def markets: F[Either[String, List[Coin[Euro]]]]

}

object CoinGeckoClient {
  def apply(): CoinGeckoClient[IO] = new CoinGeckoClient[IO] {
    implicit val cs: ContextShift[IO]                             = IO.contextShift(scala.concurrent.ExecutionContext.global)
    val ioBackend: IO[SttpBackend[IO, Nothing, WebSocketHandler]] = AsyncHttpClientCatsBackend[IO]()

    override def markets: IO[Either[String, List[Coin[Euro]]]] =
      for {
        backend <- ioBackend
        response <- basicRequest
          .get(uri"https://api.coingecko.com/api/v3/coins/markets?vs_currency=eur&order=market_cap_desc&per_page=100&page=1&sparkline=false")
          .response(asJson[List[MarketData]])
          .send()(backend, refl)
      } yield {
        println(response.body)
        response.body.left
          .map(_.toString)
          .map(
            _.flatMap(
              md =>
                for {
                  positiveMarketCap <- refineV[Positive](BigDecimal(md.marketCap)).toOption
                  positivePrice     <- refineV[Positive](md.currentPrice).toOption
                  symbol            <- refineV[NonEmpty](md.symbol).toOption
                } yield
                  Coin(
                    marketCapitalisation = MarketCapitalisation(positiveMarketCap),
                    price                = Price(positivePrice),
                    CryptoCurrency(symbol, '$')
                )))
      }
  }
}
