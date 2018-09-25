package org.poki.coinsplasher.domain

import org.poki.coinsplasher.Coin

import scala.concurrent.Future

case class MarketCoin(coin: Coin, price: BigDecimal)

trait MarketRepo {

  def loadMarketData: Future[Seq[MarketCoin]]

}
