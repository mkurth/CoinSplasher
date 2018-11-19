package com.mkurth.coinsplasher.domain

import com.mkurth.coinsplasher.domain.repo.{MarketCoin, MarketRepo, TradeRepo}

import scala.concurrent.{ExecutionContext, Future}

trait NormalisedCoinSymbols {

  /**
    * maps from MarketRepo-Symbol to TradeRepo-Symbol
    */
  val normalisedCoinSymbols = Map(
    "BCH" -> "BCC",
    "MIOTA" -> "IOTA"
  )

  def normaliseCoinSymbols: Seq[MarketCoin] => Seq[MarketCoin] = {
    marketCoins =>
      marketCoins.map(
        mCoin => mCoin.copy(coin = mCoin.coin.copy(coinSymbol = normalisedCoinSymbols.getOrElse(mCoin.coin.coinSymbol, mCoin.coin.coinSymbol)))
      )
  }
}

trait CoinServiceConfig {
  val blacklistedCoins: Seq[String]
  val ignoreBalanceForCoins: Seq[String]
  val ignoreTradesBelowWorth: BigDecimal
  val threshold: BigDecimal
  val limitToCoins: Int
}

class CoinService(marketRepo: MarketRepo, tradeRepo: TradeRepo, config: CoinServiceConfig)(implicit ec: ExecutionContext) extends NormalisedCoinSymbols {

  def calculateOrders: Future[Seq[Order]] = {
    val marketData = marketRepo.loadMarketData(config.blacklistedCoins, config.limitToCoins).map(normaliseCoinSymbols)
    val actualShares = tradeRepo.currentBalance(config.ignoreBalanceForCoins)
    for {
      balance <- actualShares
      market <- marketData
      shares = ShareCalculator.shares(by = _.marketCap)(market.take(config.limitToCoins).map(_.coin), config.threshold)
    } yield TradeSolver.solveTrades(balance, shares, market)
      .filter(_.coinSymbol != "BTC")
      .filter(order => order.worth > config.ignoreTradesBelowWorth)
  }

  def executeOrders(orders: Seq[Order]): Future[Seq[Any]] = {
    val (sellOrders, buyOrders) = orders.partition(_.isInstanceOf[SellOrder])
    for {
      soldOrders <- Future.sequence(sellOrders.collect { case order: SellOrder => tradeRepo.sell(order) })
      boughtOrders <- Future.sequence(buyOrders.collect { case order: BuyOrder => tradeRepo.buy(order) })
    } yield soldOrders ++ boughtOrders
  }

}
