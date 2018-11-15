package com.mkurth.coinsplasher.web

import com.mkurth.coinsplasher.domain.Types.CoinSymbol
import com.mkurth.coinsplasher.domain.model.CoinBalance
import com.mkurth.coinsplasher.domain.repo.{CoinMarketCap, MarketRepo, TradeRepo}
import com.mkurth.coinsplasher.domain.{BuyOrder, CoinService, CoinServiceConfig, SellOrder}

import scala.concurrent.{ExecutionContext, Future}

object Main extends App {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val marketRepo: MarketRepo = new CoinMarketCap
  val tradeRepo: TradeRepo = new TradeRepo {
    override def currentBalance(ignoreCoins: Seq[CoinSymbol]): Future[Seq[CoinBalance]] = ???

    override def sell(order: SellOrder): Future[Any] = ???

    override def buy(order: BuyOrder): Future[Any] = ???
  }
  val config: CoinServiceConfig = new CoinServiceConfig {
    override val blacklistedCoins: Seq[String] = Seq()
    override val ignoreBalanceForCoins: Seq[String] = Seq()
    override val ignoreTradesBelowWorth: BigDecimal = 1
    override val threshold: BigDecimal = 10
    override val limitToCoins: Int = 20
  }

  val service = new CoinService(marketRepo, tradeRepo, config)

  marketRepo.loadMarketData().map(println)
}
