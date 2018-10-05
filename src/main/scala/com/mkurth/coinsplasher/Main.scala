package com.mkurth.coinsplasher

import java.io.File

import com.mkurth.coinsplasher.domain._
import com.mkurth.coinsplasher.domain.repo.{MarketRepo, TradeRepo}
import com.mkurth.coinsplasher.portadapter.repo.console.ConsoleIO
import com.mkurth.coinsplasher.portadapter.repo.market.CoinMarketCap
import com.mkurth.coinsplasher.portadapter.repo.trade.Binance
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import scala.io.Source
import scala.language.postfixOps

object Main extends App with ConsoleIO {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val marketRepo: MarketRepo = new CoinMarketCap
  val tradeRepo: TradeRepo = new Binance
  val config = ConfigFactory.parseFile(new File(args.find(_.endsWith(".conf")).getOrElse("application.conf")))
  val service = new CoinService(marketRepo, tradeRepo, config)

  if (args.contains("-o")) {
    val trades = service.calculateOrders
    trades.foreach(trade => {
      println(trade.sortBy({
        case BuyOrder(_, amount) => amount
        case SellOrder(_, amount) => amount * -1
      }).map(orderToString).mkString("\n"))
      sys.exit()
    })
    trades.onComplete(sys.exit(0))
  } else if (args.contains("-i")) {
    val ordersFromStdin = Source.stdin
      .getLines()
      .map(stringToOrder)
      .collect({
        case Some(order) => order
      })
      .toSeq
    service.executeOrders(ordersFromStdin)
      .foreach(orders => {
        println(orders.mkString("\n"))
        sys.exit()
      })
  } else if (args.contains("-a")) {
    service.calculateOrders
      .flatMap(service.executeOrders)
      .foreach(orders => {
        println(orders.mkString("\n"))
        sys.exit()
      })
  }

}
