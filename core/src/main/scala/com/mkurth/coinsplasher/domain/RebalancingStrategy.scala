package com.mkurth.coinsplasher.domain

import cats.data.NonEmptyList
import com.mkurth.coinsplasher.domain.RebalancePortfolio.{SourcePortfolio, TargetPortfolio}
import com.mkurth.coinsplasher.domain.RefinedOps._

sealed trait RebalancingStrategy[A <: Currency] {
  def rebalance(sourcePortfolio: SourcePortfolio[A]): TargetPortfolio[A]
}

final case class NoopStrategy[A <: Currency]() extends RebalancingStrategy[A] {
  override def rebalance(sourcePortfolio: SourcePortfolio[A]): TargetPortfolio[A] = sourcePortfolio
}

final case class EquallyDistributeTo[A <: Currency](coins: NonEmptyList[Coin[A]]) extends RebalancingStrategy[A] {
  override def rebalance(sourcePortfolio: SourcePortfolio[A]): TargetPortfolio[A] = {
    val totalValueOfPortfolio = sourcePortfolio.totalValue
    val target                = coins.map(coin => PortfolioEntry(share = Share(totalValueOfPortfolio.value / coins.count / coin.price.value), coin = coin))
    sourcePortfolio.copy(entries = target)
  }
}

final case class DistributeBasedOnMarketCap[A <: Currency](coins: NonEmptyList[Coin[A]]) extends RebalancingStrategy[A] {
  override def rebalance(sourcePortfolio: SourcePortfolio[A]): TargetPortfolio[A] = {
    val totalValueOfPortfolio = sourcePortfolio.totalValue
    val marketCap             = coins.map(_.marketCapitalisation.value).sum
    val entries = coins.map(coin => {
      val shareOfMarket      = coin.marketCapitalisation.value / marketCap
      val targetValueOfShare = totalValueOfPortfolio.value * shareOfMarket
      PortfolioEntry[A](coin, Share(targetValueOfShare / coin.price.value))
    })
    Portfolio(entries)
  }
}
