package com.mkurth.coinsplasher

import com.mkurth.coinsplasher.domain.Types.{CoinShare, CoinSymbol, Percent}
import com.mkurth.coinsplasher.domain.repo.{MarketCoin, MarketRepo, TradeRepo}
import com.mkurth.coinsplasher.domain.{BuyOrder, SellOrder, ShareCalculator, TradeSolver}
import com.mkurth.coinsplasher.portadapter.repo.market.CoinMarketCap
import com.mkurth.coinsplasher.portadapter.repo.trade.Binance

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

case class Coin(coinSymbol: CoinSymbol, marketCap: BigDecimal, tradeCap: BigDecimal)

case class CoinBalance(coinSymbol: CoinSymbol, amount: CoinShare)

case class Share(coin: Coin, share: Percent)

object Main extends App {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val marketRepo: MarketRepo = new CoinMarketCap
  val tradeRepo: TradeRepo = new Binance
  val threshold = 0.10
  val blacklistedCoins = Seq("USDT")
  val normalieCoinSymbols = Map(
    "BCH" -> "BCC",
    "MIOTA" -> "IOTA"
  )

  private val marketData = marketRepo.loadMarketData(blacklistedCoins).map(normaliseCoinSymbols)
  val targetShares = marketData
    .map(coins => ShareCalculator.shares(by = _.marketCap)(coins.take(20).map(_.coin), threshold))
  val actualShares = tradeRepo.currentBalance
  val trades = for {
    balance <- actualShares
    shares <- targetShares
    market <- marketData
  } yield TradeSolver.solveTrades(balance, shares, market)

  trades.foreach(trade => {
    println(trade.sortBy({
      case BuyOrder(coinSymbol, amount) => amount
      case SellOrder(coinSymbol, amount) => amount * -1
    }).mkString("\n"))
    sys.exit()
  })

  def normaliseCoinSymbols: Seq[MarketCoin] => Seq[MarketCoin] = {
    marketCoins =>
      marketCoins.map(
        mCoin => mCoin.copy(coin = mCoin.coin.copy(coinSymbol = normalieCoinSymbols.getOrElse(mCoin.coin.coinSymbol, mCoin.coin.coinSymbol)))
      )
  }

}
