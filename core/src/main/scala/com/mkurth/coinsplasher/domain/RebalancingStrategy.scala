package com.mkurth.coinsplasher.domain

import cats.data.NonEmptyList
import com.mkurth.coinsplasher.domain.RebalancePortfolio.{SourcePortfolio, TargetPortfolio}
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.refineV

sealed trait RebalancingStrategy[A <: Currency] {
  def rebalance(sourcePortfolio: SourcePortfolio[A]): Option[TargetPortfolio[A]]
}

final case class NoopStrategy[A <: Currency]() extends RebalancingStrategy[A] {
  override def rebalance(sourcePortfolio: SourcePortfolio[A]): Option[TargetPortfolio[A]] = Some(sourcePortfolio)
}

final case class EquallyDistributeTo[A <: Currency](coins: List[Coin[A]]) extends RebalancingStrategy[A] {
  override def rebalance(sourcePortfolio: SourcePortfolio[A]): Option[TargetPortfolio[A]] =
    for {
      totalValueOfPortfolio <- sourcePortfolio.totalValue.toOption
      valueForEachCoin = totalValueOfPortfolio.value / coins.length
      target           = coins.flatMap(coin => refineV[Positive](valueForEachCoin / coin.price.value).map(s => PortfolioEntry(share = Share(s), coin = coin)).toOption)
      result <- NonEmptyList.fromList(target).map(x => sourcePortfolio.copy(entries = x))
    } yield result
}

final case class DistributeBasedOnMarketCap[A <: Currency](coins: List[Coin[A]]) extends RebalancingStrategy[A] {
  override def rebalance(sourcePortfolio: SourcePortfolio[A]): Option[TargetPortfolio[A]] =
    for {
      marketCap             <- refineV[Positive](coins.map(_.marketCapitalisation.value.value).sum).toOption
      totalValueOfPortfolio <- sourcePortfolio.totalValue.toOption
      entries <- NonEmptyList.fromList(coins.flatMap(coin => {
        val shareOfMarket      = coin.marketCapitalisation.value.value / marketCap
        val targetValueOfShare = totalValueOfPortfolio.value.value * shareOfMarket
        refineV[Positive](targetValueOfShare / coin.price.value.value).toOption.map(targetShare => PortfolioEntry[A](coin, Share(targetShare)))
      }))
    } yield Portfolio(entries)
}
