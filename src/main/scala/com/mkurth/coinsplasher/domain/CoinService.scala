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

class CoinService(marketRepo: MarketRepo, tradeRepo: TradeRepo)(implicit ec: ExecutionContext) extends NormalisedCoinSymbols {

  val blacklistedCoins = Seq("USDT")

  def calculateOrders: Future[Seq[Order]] = {
    val threshold = 0.10
    val marketData = marketRepo.loadMarketData(blacklistedCoins).map(normaliseCoinSymbols)
    val actualShares = tradeRepo.currentBalance
    for {
      balance <- actualShares
      market <- marketData
      shares = ShareCalculator.shares(by = _.marketCap)(market.take(20).map(_.coin), threshold)
    } yield TradeSolver.solveTrades(balance, shares, market)
  }

  def executeOrders(orders: Seq[Order]): Future[Seq[Any]] = {
    val (sellOrders, buyOrders) = orders.partition(_.isInstanceOf[SellOrder])
    for {
      soldOrders <- Future.sequence(sellOrders.map { case order: SellOrder => tradeRepo.sell(order) })
      boughtOrders <- Future.sequence(buyOrders.map { case order: BuyOrder => tradeRepo.buy(order) })
    } yield soldOrders ++ boughtOrders
  }

}
