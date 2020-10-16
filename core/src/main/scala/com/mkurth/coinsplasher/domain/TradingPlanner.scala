package com.mkurth.coinsplasher.domain

import com.mkurth.coinsplasher.domain.RebalancePortfolio.{TargetPortfolio, TradePlanner}
import com.mkurth.coinsplasher.domain.RefinedOps._

object TradingPlanner {

  sealed trait Order {
    val currency: CryptoCurrency
    val share: Share
  }
  final case class BuyOrder(currency: CryptoCurrency, share: Share) extends Order
  final case class SellOrder(currency: CryptoCurrency, share: Share) extends Order

  final case class TradingPlan(sellOrders: List[SellOrder], buyOrders: List[BuyOrder])

  def planTrade[A <: Currency]: TradePlanner[A] =
    source =>
      target => {
        val sellOrder = source.entries.toList.flatMap(sourceEntry => {
          val sourceCurrency = sourceEntry.coin.currency
          findInPortfolio(target, sourceCurrency) match {
            case Some(targetEntry) if targetEntry.share.value < sourceEntry.share.value =>
              val sellAmount = sourceEntry.share.value.minusE(targetEntry.share.value)
              sellAmount.map(s => SellOrder(sourceCurrency, Share(s))).toOption
            case None => Some(SellOrder(sourceCurrency, sourceEntry.share))
            case _    => None
          }
        })
        val buyOrder = target.entries.toList.flatMap(targetEntry => {
          val targetCurrency = targetEntry.coin.currency
          findInPortfolio(source, targetCurrency) match {
            case Some(sourceEntry) if sourceEntry.share.value < targetEntry.share.value =>
              val buyAmount = targetEntry.share.value.minusE(sourceEntry.share.value)
              buyAmount.map(s => BuyOrder(targetCurrency, Share(s))).toOption
            case None => Some(BuyOrder(targetCurrency, targetEntry.share))
            case _    => None
          }
        })
        TradingPlan(sellOrders = sellOrder, buyOrders = buyOrder)
    }

  private def findInPortfolio[A <: Currency](portfolio: TargetPortfolio[A], cryptoCurrency: CryptoCurrency): Option[PortfolioEntry[A]] =
    portfolio.entries.find(targetEntry => targetEntry.coin.currency == cryptoCurrency)

}
