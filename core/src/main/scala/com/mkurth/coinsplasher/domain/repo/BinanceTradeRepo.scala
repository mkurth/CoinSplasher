package com.mkurth.coinsplasher.domain.repo

import com.mkurth.binance.client
import com.mkurth.binance.client.{BinanceAuth, BinanceClient, MarketOrder}
import com.mkurth.coinsplasher.domain.Types.CoinSymbol
import com.mkurth.coinsplasher.domain.model.CoinBalance
import com.mkurth.coinsplasher.domain.{BuyOrder, SellOrder}

import scala.concurrent.{ExecutionContext, Future}

class BinanceTradeRepo(apiKey: () => String, apiSecret: () => String)(implicit val ex: ExecutionContext) extends TradeRepo {

  def auth = BinanceAuth(apiKey(), apiSecret())
  def client = new BinanceClient(auth, None)

  override def currentBalance(ignoreCoins: List[CoinSymbol]): Future[List[CoinBalance]] = {
    client.account.map({
      case Right(accountInfo) =>
        accountInfo.balances.map(balance =>
          CoinBalance(balance.asset, BigDecimal(balance.free))
        ).toList
    })
  }

  override def sell(order: SellOrder): Future[Any] = {
    MarketOrder(
      order.coinSymbol,
      client.SellOrder,
      order.amount,
      0,
      0
    )
    client.order(order)
  }

  override def buy(order: BuyOrder): Future[Any] = ???
}
