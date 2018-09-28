package com.mkurth.coinsplasher.domain.repo

import com.mkurth.coinsplasher.domain.{BuyOrder, SellOrder}
import com.mkurth.coinsplasher.domain.model.CoinBalance

import scala.concurrent.Future

trait TradeRepo {

  def currentBalance: Future[Seq[CoinBalance]]

  def sell(order: SellOrder): Future[Any]

  def buy(order: BuyOrder): Future[Any]

}
