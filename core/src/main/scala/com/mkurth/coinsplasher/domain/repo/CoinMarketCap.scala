package com.mkurth.coinsplasher.domain.repo

import com.mkurth.coinsplasher.domain.Types._
import com.mkurth.coinsplasher.domain.model.Coin
import com.softwaremill.sttp.quick._
import play.api.libs.json.{JsObject, JsValue, Json, OFormat}

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

class CoinMarketCap(implicit val ex: ExecutionContext) extends MarketRepo {
  implicit val coinFormat: OFormat[CoinInfo] = Json.format[CoinInfo]
  implicit val quotaFormat: OFormat[Quota] = Json.format[Quota]

  override def loadMarketData(blacklisted: Seq[CoinSymbol] = Seq(), limitToCoins: Int = 20): Future[Seq[MarketCoin]] = {
    val tickerURL = s"https://api.coinmarketcap.com/v2/ticker/?convert=EUR&limit=${limitToCoins + 10}"
    sttp.get(uri"$tickerURL")
        .send().map(_.body
            .map(Json.parse)
          .map(jsonToMarketCoins(blacklisted)) match {
          case Left(value: String) => throw new IllegalArgumentException(value)
          case Right(value: Seq[MarketCoin]) => value
        }
      )
  }

  def jsonToMarketCoins(blacklisted: Seq[CoinSymbol]): JsValue => Seq[MarketCoin] = {
    json => {
      (json \ "data").validate[JsObject].get
      .fields.map({ case (key, value) => value.validate[CoinInfo].get })
      .filter(coinInfo => !blacklisted.contains(coinInfo.symbol))
      .map(toCoinInfoWithQuota)
      .sortBy(_.rank).map(cwq => {
      val coin = cwq.toCoin
        MarketCoin(coin, cwq.quotes.find(_.currency == "EUR").get.price)
      })
    }
  }

  private def toCoinInfoWithQuota: CoinInfo => CoinInfoWithQuota = {
    coinInfo =>
      CoinInfoWithQuota(coinInfo, coinInfo.quotes.fields.map(
        { case (currency, value) => value.validate[Quota].get.withCurrency(currency) }
      ))
  }
}
