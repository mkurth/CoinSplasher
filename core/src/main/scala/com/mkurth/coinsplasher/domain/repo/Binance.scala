package com.mkurth.coinsplasher.domain.repo

import com.mkurth.coinsplasher.domain.Types.CoinSymbol
import com.mkurth.coinsplasher.domain.model.CoinBalance
import com.mkurth.coinsplasher.domain.{BuyOrder, SellOrder}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class Binance extends TradeRepo {

  implicit val ec: ExecutionContext = ExecutionContext.global

  override def currentBalance(ignoreCoins: Seq[CoinSymbol]): Future[Seq[CoinBalance]] = ???

  override def buy(order: BuyOrder): Future[Any] = ???

  override def sell(order: SellOrder): Future[Any] = ???

}
