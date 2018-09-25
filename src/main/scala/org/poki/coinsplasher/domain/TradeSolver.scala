package org.poki.coinsplasher.domain

import org.poki.coinsplasher.{CoinBalance, Share}

sealed trait Order
case class SellOrder(coinSymbol: String, amount: BigDecimal) extends Order
case class BuyOrder(coinSymbol: String, amount: BigDecimal) extends Order

object TradeSolver {

  def solveTrades(currentBalance: Seq[CoinBalance], targetShares: Seq[Share], marketData: Seq[MarketCoin]): Seq[Order] = {
    val ts: Map[String, Share] = targetShares.groupBy(_.coin.coinSymbol).map(ele => ele.copy(_2 = ele._2.head))
    val cb: Map[String, CoinBalance] = currentBalance.groupBy(_.coinSymbol).map(ele => ele.copy(_2 = ele._2.head))
    val md: Map[String, MarketCoin] = marketData.groupBy(_.coin.coinSymbol).map(ele => ele.copy(_2 = ele._2.head))

    val x: Map[String, (Option[Share], Option[CoinBalance], MarketCoin)] = (ts.keys ++ cb.keys ++ md.keys).toSet.map(key => {
      key -> (ts.get(key), cb.get(key), md.get(key))
    }).collect({ case (k, (mts, mcb, Some(mmd))) => k -> (mts, mcb, mmd)}).toMap

    Seq()
  }

}
