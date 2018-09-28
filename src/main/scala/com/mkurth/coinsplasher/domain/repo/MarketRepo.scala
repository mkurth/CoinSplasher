package com.mkurth.coinsplasher.domain.repo

import com.mkurth.coinsplasher.Coin

import scala.concurrent.Future

case class MarketCoin(coin: Coin, price: BigDecimal)

trait MarketRepo {

  def loadMarketData: Future[Seq[MarketCoin]]

}
