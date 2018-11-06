package com.mkurth.coinsplasher

import java.io.File

import com.mkurth.coinsplasher.domain._
import com.mkurth.coinsplasher.domain.repo.{MarketRepo, TradeRepo}
import com.mkurth.coinsplasher.portadapter.repo.console.ConsoleIO
import com.mkurth.coinsplasher.portadapter.repo.market.CoinMarketCap
import com.mkurth.coinsplasher.portadapter.repo.trade.Binance
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.io.Source
import scala.language.postfixOps

object Main extends App with ConsoleIO {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val marketRepo: MarketRepo = new CoinMarketCap
  val tradeRepo: TradeRepo = new Binance
  val config = ConfigFactory.parseFile(new File(args.find(_.endsWith(".conf")).getOrElse("application.conf")))
  val service = new CoinService(marketRepo, tradeRepo, config)

  if (args.contains("-o")) {
    val trades = Await.result(service.calculateOrders, 10 seconds)
    println(trades.sortBy({
      case BuyOrder(coinSymbol, amount, worth) => worth
      case SellOrder(coinSymbol, amount, worth) => worth * -1
    }).map(orderToString).mkString("\n"))
    sys.exit()
  } else if (args.contains("-i")) {
    val ordersFromStdin = Source.stdin
      .getLines()
      .map(stringToOrder)
      .collect({
        case Some(order) => order
      })
      .toSeq
    val executedOrders = Await.result(service.executeOrders(ordersFromStdin), 10 seconds)
    println(executedOrders.mkString("\n"))
    sys.exit()
  } else if (args.contains("-a")) {
    val executedOrders = Await.result(service.calculateOrders.flatMap(service.executeOrders), 1 minute)
    println(executedOrders.mkString("\n"))
    sys.exit()
  }

}
