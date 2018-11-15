package com.mkurth.coinsplasher.domain.repo

import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.domain._
import com.binance.api.client.domain.account.NewOrder.{marketBuy, marketSell}
import com.binance.api.client.domain.general.{ExchangeInfo, FilterType}
import com.binance.api.client.impl.{BinanceApiAsyncRestClientImpl, BinanceApiWebSocketClientImpl}
import com.mkurth.coinsplasher.domain.Types.{CoinShare, CoinSymbol}
import com.mkurth.coinsplasher.domain.model.CoinBalance
import com.mkurth.coinsplasher.domain.{BuyOrder, SellOrder}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.StdIn
import scala.language.postfixOps
import scala.math.BigDecimal.RoundingMode

class Binance extends TradeRepo {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val factory = new BinanceApiClientFactory(
    sys.env.getOrElse("BINANCE_API_KEY", StdIn.readLine("binance api key: ")),
    sys.env.getOrElse("BINANCE_SECRET",  StdIn.readLine("binance secret : ")))
  val client: BinanceApiAsyncRestClientImpl = factory.newAsyncRestClient
  val webSocketClient: BinanceApiWebSocketClientImpl = factory.newWebSocketClient
  val exchangeInfo: ExchangeInfo = Await.result(client.getExchangeInfo, 10 seconds)

  override def currentBalance(ignoreCoins: Seq[CoinSymbol]): Future[Seq[CoinBalance]] = {
    client
      .getAccount()
      .map(_.balances.map(ab => CoinBalance(ab.asset.value, ab.free.value)))
      .map(_.filter(balance => balance.amount > 0 && !ignoreCoins.contains(balance.coinSymbol)))
  }

  override def buy(order: BuyOrder): Future[Any] = {
    val marketBuyOrder = marketBuy(Symbol(order.coinSymbol + "BTC"), roundAmount(order.coinSymbol, order.amount).get)
      .copy(timestamp = Some(Instant(System.currentTimeMillis())))
    client.newOrder(marketBuyOrder)
  }

  override def sell(order: SellOrder): Future[Any] = {
    val marketSellOrder = marketSell(Symbol(order.coinSymbol + "BTC"), roundAmount(order.coinSymbol, order.amount).get)
      .copy(timestamp = Some(Instant(System.currentTimeMillis())))
    client.newOrder(marketSellOrder)
  }

  def roundAmount(coinSymbol: CoinSymbol, coinShare: CoinShare): Option[Quantity] = exchangeInfo.symbols.find(_.symbol == Symbol(coinSymbol + "BTC")).map(info => {
    val stepSize = BigDecimal(info.filters.find(_.filterType == FilterType.LOT_SIZE).get.stepSize.get)
    Quantity((coinShare / stepSize).setScale(0, RoundingMode.DOWN) * stepSize)
  })
}
