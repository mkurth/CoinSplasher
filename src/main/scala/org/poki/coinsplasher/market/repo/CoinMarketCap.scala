package org.poki.coinsplasher.market.repo

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.poki.coinsplasher.Coin
import org.poki.coinsplasher.domain.MarketRepo
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.{ExecutionContext, Future}

case class QuotaWithCurrency(currency: String, price: BigDecimal, market_cap: BigDecimal, volume_24h: BigDecimal)
case class Quota(price: BigDecimal, market_cap: BigDecimal, volume_24h: BigDecimal) {
  def withCurrency(currency: String) = QuotaWithCurrency(
    currency = currency,
    price = price,
    market_cap = market_cap,
    volume_24h = volume_24h)
}
case class CoinInfo(id: Int, name: String, symbol: String, rank: Int, quotes: JsObject)
case class CoinInfoWithQuota(id: Int, name: String, symbol: String, rank: Int, quotes: Seq[QuotaWithCurrency]) {
  def toCoin = Coin(
    symbol,
    quotes.find(_.currency == "EUR").map(_.market_cap).getOrElse(0),
    quotes.find(_.currency == "EUR").map(_.volume_24h).getOrElse(0)
  )
}

object CoinInfoWithQuota {
  def apply(coinInfo: CoinInfo, quotes: Seq[QuotaWithCurrency]): CoinInfoWithQuota =
    new CoinInfoWithQuota(
      id = coinInfo.id,
      name = coinInfo.name,
      symbol = coinInfo.symbol,
      rank = coinInfo.rank,
      quotes = quotes)
}

class CoinMarketCap extends MarketRepo {

  final val listingURL = "https://api.coinmarketcap.com/v2/listings/"
  final val tickerURL = "https://api.coinmarketcap.com/v2/ticker/?convert=EUR&limit=30"

  implicit val system: ActorSystem = ActorSystem()
  system.registerOnTermination {
    System.exit(0)
  }
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val coinFormat = Json.format[CoinInfo]
  implicit val quotaFormat = Json.format[Quota]

  val wsClient = StandaloneAhcWSClient()

  override def loadMarketData: Future[Seq[Coin]] = {
    wsClient.url(tickerURL)
      .get()
      .map(_.body[JsValue])
      .map(json => (json \ "data").validate[JsObject].get)
      .map(data => data.fields.map({ case (key, value) => value.validate[CoinInfo].get }))
      .map(coinInfos => coinInfos.map(coinInfo => CoinInfoWithQuota(coinInfo, coinInfo.quotes.fields.map({ case (currency, value) => value.validate[Quota].get.withCurrency(currency) }))))
      .map(_.sortBy(_.rank).map(_.toCoin))
  }
}
