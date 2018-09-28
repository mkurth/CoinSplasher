package com.mkurth.coinsplasher

import com.mkurth.coinsplasher.domain.ShareCalculator
import com.mkurth.coinsplasher.domain.Types.{CoinShare, CoinSymbol, Percent}
import com.mkurth.coinsplasher.domain.repo.{MarketRepo, TradeRepo}
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

  private val marketData = marketRepo.loadMarketData
  val targetShares = marketData
    .map(coins => ShareCalculator.shares(by = _.marketCap)(coins.filter(mc => !blacklistedCoins.contains(mc.coin.coinSymbol)).take(20).map(_.coin), threshold))
  val actualShares = tradeRepo.currentBalance

}
