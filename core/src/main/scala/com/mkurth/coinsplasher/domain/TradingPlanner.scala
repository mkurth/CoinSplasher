package com.mkurth.coinsplasher.domain

import com.mkurth.coinsplasher.domain.RebalancePortfolio.{TargetPortfolio, TradePlanner}

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
            case Some(targetEntry) if targetEntry.share < sourceEntry.share =>
              val sellAmount = sourceEntry.share - targetEntry.share
              sellAmount.map(s => SellOrder(sourceCurrency, s)).toOption
            case None => Some(SellOrder(sourceCurrency, sourceEntry.share))
            case _    => None
          }
        })
        val buyOrder = target.entries.toList.flatMap(targetEntry => {
          val targetCurrency = targetEntry.coin.currency
          findInPortfolio(source, targetCurrency) match {
            case Some(sourceEntry) if sourceEntry.share < targetEntry.share =>
              val buyAmount = targetEntry.share - sourceEntry.share
              buyAmount.map(s => BuyOrder(targetCurrency, s)).toOption
            case None => Some(BuyOrder(targetCurrency, targetEntry.share))
            case _    => None
          }
        })
        TradingPlan(sellOrders = sellOrder, buyOrders = buyOrder)
    }

  private def findInPortfolio[A <: Currency](portfolio: TargetPortfolio[A], cryptoCurrency: CryptoCurrency): Option[PortfolioEntry[A]] =
    portfolio.entries.find(targetEntry => targetEntry.coin.currency == cryptoCurrency)

}
