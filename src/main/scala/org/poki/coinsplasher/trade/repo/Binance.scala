package org.poki.coinsplasher.trade.repo

import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.domain._
import com.binance.api.client.impl.{BinanceApiAsyncRestClientImpl, BinanceApiWebSocketClientImpl}
import org.poki.coinsplasher.CoinBalance
import org.poki.coinsplasher.domain.TradeRepo
import com.binance.api.client.domain.account.NewOrder.{limitBuy, marketBuy, marketSell}

import scala.concurrent.{ExecutionContext, Future}

class Binance extends TradeRepo {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val factory = new BinanceApiClientFactory(sys.env("BINANCE_API_KEY"), sys.env("BINANCE_SECRET"))
  val client: BinanceApiAsyncRestClientImpl = factory.newAsyncRestClient
  val webSocketClient: BinanceApiWebSocketClientImpl = factory.newWebSocketClient

  override def currentBalance: Future[Seq[CoinBalance]] = {
    client
      .getAccount()
      .map(_.balances.map(ab => CoinBalance(ab.asset.value, ab.free.value)))
  }

  override def buy(coin: String, amount: BigDecimal): Future[Any] = {
    client.newOrderTest(marketBuy(Symbol(coin), Quantity(amount)))
  }

  override def sell(coin: String, amount: BigDecimal): Future[Any] = {
    client.newOrderTest(marketSell(Symbol(coin), Quantity(amount)))
  }
}
