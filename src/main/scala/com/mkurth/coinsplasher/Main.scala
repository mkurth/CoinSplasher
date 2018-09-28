package com.mkurth.coinsplasher

import com.mkurth.coinsplasher.domain._
import com.mkurth.coinsplasher.domain.repo.{MarketRepo, TradeRepo}
import com.mkurth.coinsplasher.portadapter.repo.market.CoinMarketCap
import com.mkurth.coinsplasher.portadapter.repo.trade.Binance

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn
import scala.language.postfixOps
import scala.util.control.NonFatal

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
  })

  val x: Future[Object] = if(readBoolean("execute?")){
    trades.map(service.executeOrders)
  } else {
    Future.successful("nothing to do")
  }.recover({case NonFatal(e) =>
    e.printStackTrace()
    e.getMessage
  })
  x.onComplete(sys.exit())

  def readBoolean(message: String): Boolean = {
    StdIn.readLine(message).toLowerCase match {
      case "y" => true
      case "yes" => true
      case "true" => true
      case _ => false
    }
  }
}
