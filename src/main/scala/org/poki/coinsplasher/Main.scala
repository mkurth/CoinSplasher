package org.poki.coinsplasher

import org.poki.coinsplasher.domain.{MarketRepo, Rebalancer, TradeRepo}
import org.poki.coinsplasher.market.repo.CoinMarketCap
import org.poki.coinsplasher.trade.repo.Binance

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

case class Coin(short: String, marketCap: BigDecimal, tradeCap: BigDecimal)

case class CoinBalance(symbol: String, amount: BigDecimal)

case class Share(coin: Coin, share: BigDecimal)

object Main extends App {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val marketRepo: MarketRepo = new CoinMarketCap
  val tradeRepo: TradeRepo = new Binance
  val threshold = 0.10
  val blacklistedCoins = Seq("USDT")

  private val marketData = marketRepo.loadMarketData
  val targetShares = marketData
    .map(coins => Rebalancer.rebalance(by = _.marketCap)(coins.filter(mc => !blacklistedCoins.contains(mc.coin.short)).take(20).map(_.coin), threshold))
  val actualShares = tradeRepo.currentBalance

}
