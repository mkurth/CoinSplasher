package com.mkurth.coinsplasher.domain.repo

import com.mkurth.coinsplasher.domain.Types.CoinSymbol
import com.mkurth.coinsplasher.domain.model.CoinBalance
import com.mkurth.coinsplasher.domain.{BuyOrder, SellOrder}

import scala.concurrent.Future

trait TradeRepo {

  def currentBalance(ignoreCoins: List[CoinSymbol]): Future[List[CoinBalance]]

  def sell(order: SellOrder): Future[Any]

  def buy(order: BuyOrder): Future[Any]

}
