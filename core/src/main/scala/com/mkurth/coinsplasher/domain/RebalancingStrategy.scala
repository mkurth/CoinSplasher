package com.mkurth.coinsplasher.domain

import cats.data.NonEmptyList
import com.mkurth.coinsplasher.domain.RebalancePortfolio.{SourcePortfolio, TargetPortfolio}
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.{refineMV, refineV}

sealed trait RebalancingStrategy[A <: Currency] {
  def rebalance(sourcePortfolio: SourcePortfolio[A]): Option[TargetPortfolio[A]]
}

final case class NoopStrategy[A <: Currency]() extends RebalancingStrategy[A] {
  override def rebalance(sourcePortfolio: SourcePortfolio[A]): Option[TargetPortfolio[A]] = Some(sourcePortfolio)
}

final case class EquallySplashTo[A <: Currency](coins: NonEmptyList[Coin[A]]) extends RebalancingStrategy[A] {
  override def rebalance(sourcePortfolio: SourcePortfolio[A]): Option[TargetPortfolio[A]] = {
    val totalValueOfPortfolio = sourcePortfolio.totalValue.getOrElse(Price(refineMV(BigDecimal(1))))
    val valueForEachCoin      = totalValueOfPortfolio.value / coins.length
    val target = coins.toList.flatMap(coin => {
      val share = valueForEachCoin / coin.price.value
      refineV[Positive](share).map(s => PortfolioEntry(share = Share(s), coin = coin)).toOption
    })
    NonEmptyList.fromList(target).map(x => sourcePortfolio.copy(entries = x))
  }
}
