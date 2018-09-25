package org.poki.coinsplasher.domain

import org.poki.coinsplasher.Coin

import scala.concurrent.Future


trait MarketRepo {

  def loadMarketData: Future[Seq[Coin]]

}
