package com.mkurth.coinsplasher.domain

import com.mkurth.coinsplasher.domain.repo.{MarketCoin, MarketRepo, TradeRepo}

import scala.concurrent.{ExecutionContext, Future}

class CoinService(marketRepo: MarketRepo, tradeRepo: TradeRepo) {

  val blacklistedCoins = Seq("USDT")
  val normalieCoinSymbols = Map(
    "BCH" -> "BCC",
    "MIOTA" -> "IOTA"
  )
  def calculateOrders(implicit ec: ExecutionContext): Future[Seq[Order]] = {
    val marketData = marketRepo.loadMarketData(blacklistedCoins).map(normaliseCoinSymbols)
    val threshold = 0.10
    val targetShares = marketData
      .map(coins => ShareCalculator.shares(by = _.marketCap)(coins.take(20).map(_.coin), threshold))
    val actualShares = tradeRepo.currentBalance
    for {
      balance <- actualShares
      shares <- targetShares
      market <- marketData
    } yield TradeSolver.solveTrades(balance, shares, market)
  }

  def normaliseCoinSymbols: Seq[MarketCoin] => Seq[MarketCoin] = {
    marketCoins =>
      marketCoins.map(
        mCoin => mCoin.copy(coin = mCoin.coin.copy(coinSymbol = normalieCoinSymbols.getOrElse(mCoin.coin.coinSymbol, mCoin.coin.coinSymbol)))
      )
  }
}
