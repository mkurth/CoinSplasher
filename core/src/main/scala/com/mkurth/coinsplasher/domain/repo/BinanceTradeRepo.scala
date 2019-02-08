package com.mkurth.coinsplasher.domain.repo

import com.mkurth.binance.client.{BinanceAuth, BinanceClient}
import com.mkurth.coinsplasher.domain.Types.CoinSymbol
import com.mkurth.coinsplasher.domain.model.CoinBalance
import com.mkurth.coinsplasher.domain.{BuyOrder, SellOrder}

import scala.concurrent.{ExecutionContext, Future}

class BinanceTradeRepo(apiKey: () => String, apiSecret: () => String)(implicit val ex: ExecutionContext) extends TradeRepo {

  def auth = BinanceAuth(apiKey(), apiSecret())
  def client = new BinanceClient(auth, Some("http://localhost:9090"))

  override def currentBalance(ignoreCoins: Seq[CoinSymbol]): Future[Seq[CoinBalance]] = {
    client.account.map({
      case Right(accountInfo) =>
        accountInfo.balances.map(balance =>
          CoinBalance(balance.asset, BigDecimal(balance.free))
        )
    })
  }

  override def sell(order: SellOrder): Future[Any] = ???

  override def buy(order: BuyOrder): Future[Any] = ???
}
