package org.poki.coinsplasher.domain

import org.poki.coinsplasher.{CoinBalance, Share}

sealed trait Order
case class SellOrder(coinSymbol: String, amount: BigDecimal) extends Order
case class BuyOrder(coinSymbol: String, amount: BigDecimal) extends Order

object TradeSolver {

  def solveTrades(currentBalance: Seq[CoinBalance], targetShares: Seq[Share], marketData: Seq[MarketCoin]): Seq[Order] = {
    ???
  }

}
