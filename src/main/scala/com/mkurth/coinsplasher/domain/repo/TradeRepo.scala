package com.mkurth.coinsplasher.domain.repo

import com.mkurth.coinsplasher.domain.model.CoinBalance

import scala.concurrent.Future

trait TradeRepo {

  def currentBalance: Future[Seq[CoinBalance]]

  def sell(coin: String, amount: BigDecimal): Future[Any]

  def buy(coin: String, amount: BigDecimal): Future[Any]

}
