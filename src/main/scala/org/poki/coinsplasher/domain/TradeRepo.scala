package org.poki.coinsplasher.domain

import org.poki.coinsplasher.CoinBalance

import scala.concurrent.Future

trait TradeRepo {

  def currentBalance: Future[Seq[CoinBalance]]

  def sell(coin: String, amount: BigDecimal): Future[Any]

  def buy(coin: String, amount: BigDecimal): Future[Any]

}
