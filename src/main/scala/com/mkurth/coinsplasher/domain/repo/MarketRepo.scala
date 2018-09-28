package com.mkurth.coinsplasher.domain.repo

import com.mkurth.coinsplasher.Coin
import com.mkurth.coinsplasher.domain.Types.CoinSymbol

import scala.concurrent.Future

case class MarketCoin(coin: Coin, price: BigDecimal)

trait MarketRepo {

  def loadMarketData(blacklisted: Seq[CoinSymbol] = Seq()): Future[Seq[MarketCoin]]

}
