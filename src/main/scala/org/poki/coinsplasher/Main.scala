package org.poki.coinsplasher

import org.poki.coinsplasher.domain.{MarketRepo, Rebalancer}
import org.poki.coinsplasher.market.repo.CoinMarketCap

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

case class Coin(short: String, marketCap: BigDecimal, tradeCap: BigDecimal)

case class Share(coin: Coin, share: BigDecimal)

object Main extends App {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val marketRepo: MarketRepo = new CoinMarketCap
  val threshold = 0.10
  val blacklistedCoins = Seq("USDT")

  marketRepo.loadMarketData
    .map(coins => Rebalancer.rebalanceByMarketCap(coins.filter(coin => !blacklistedCoins.contains(coin.short)).take(20), threshold))
    .foreach(coins => {
      println("marke " + coins.mkString("\n"))
      sys.exit(0)
    })
}
