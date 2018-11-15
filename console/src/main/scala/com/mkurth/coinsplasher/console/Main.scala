package com.mkurth.coinsplasher.console

import java.io.File

import com.mkurth.coinsplasher.domain.repo.{Binance, CoinMarketCap, MarketRepo, TradeRepo}
import com.mkurth.coinsplasher.domain.{BuyOrder, CoinService, CoinServiceConfig, SellOrder}
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.io.Source

object Main extends App with ConsoleIO {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val marketRepo: MarketRepo = new CoinMarketCap
  val tradeRepo: TradeRepo = new Binance
  val config = ConfigFactory.parseFile(new File(args.find(_.endsWith(".conf")).getOrElse("application.conf")))
  val serviceConfig = new CoinServiceConfig {
    override val blacklistedCoins: Seq[String] = config.getStringList("blacklisted.coins").asScala
    override val ignoreBalanceForCoins: Seq[String] = config.getStringList("ignore.balance.coins").asScala
    override val ignoreTradesBelowWorth: BigDecimal = config.getDouble("ignore.trades.below")
    override val threshold: BigDecimal = config.getDouble("max.percent.of.share")
    override val limitToCoins: Int = config.getInt("splash.portfolio.to.number.of.coins")
  }
  val service = new CoinService(marketRepo, tradeRepo, serviceConfig)

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
