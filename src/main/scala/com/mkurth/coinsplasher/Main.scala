package com.mkurth.coinsplasher

import com.mkurth.coinsplasher.domain._
import com.mkurth.coinsplasher.domain.repo.{MarketRepo, TradeRepo}
import com.mkurth.coinsplasher.portadapter.repo.market.CoinMarketCap
import com.mkurth.coinsplasher.portadapter.repo.trade.Binance

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

object Main extends App {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val marketRepo: MarketRepo = new CoinMarketCap
  val tradeRepo: TradeRepo = new Binance
  val service = new CoinService(marketRepo, tradeRepo)
  val trades = service.calculateOrders
  trades.foreach(trade => {
    println(trade.sortBy({
      case BuyOrder(coinSymbol, amount) => amount
      case SellOrder(coinSymbol, amount) => amount * -1
    }).mkString("\n"))
    sys.exit()
  })

}
