package org.poki.coinsplasher

import org.poki.coinsplasher.domain.Types.{CoinShare, CoinSymbol, Percent}
import org.poki.coinsplasher.domain.{MarketRepo, ShareCalculator, TradeRepo}
import org.poki.coinsplasher.market.repo.CoinMarketCap
import org.poki.coinsplasher.trade.repo.Binance

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
