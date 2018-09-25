package org.poki.coinsplasher.domain

import scala.concurrent.Future

trait TradeRepo {

  def currentBalance: Future[Any]

  def trade(coin: String, amount: BigDecimal)

}
