package com.mkurth.coinsplasher.portadapter.repo.trade

import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.domain._
import com.binance.api.client.domain.account.NewOrder.{marketBuy, marketSell}
import com.binance.api.client.impl.{BinanceApiAsyncRestClientImpl, BinanceApiWebSocketClientImpl}
import com.mkurth.coinsplasher.domain.model.CoinBalance
import com.mkurth.coinsplasher.domain.repo.TradeRepo

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

class Binance extends TradeRepo {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val factory = new BinanceApiClientFactory(
    sys.env.getOrElse("BINANCE_API_KEY", StdIn.readLine("binance api key: ")),
    sys.env.getOrElse("BINANCE_SECRET",  StdIn.readLine("binance secret : ")))
  val client: BinanceApiAsyncRestClientImpl = factory.newAsyncRestClient
  val webSocketClient: BinanceApiWebSocketClientImpl = factory.newWebSocketClient

  override def currentBalance: Future[Seq[CoinBalance]] = {
    client
      .getAccount()
      .map(_.balances.map(ab => CoinBalance(ab.asset.value, ab.free.value)))
      .map(_.filter(_.amount > 0))
  }

  override def buy(coin: String, amount: BigDecimal): Future[Any] = {
    client.newOrderTest(marketBuy(Symbol(coin), Quantity(amount)))
  }

  override def sell(coin: String, amount: BigDecimal): Future[Any] = {
    client.newOrderTest(marketSell(Symbol(coin), Quantity(amount)))
  }
}
