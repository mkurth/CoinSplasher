package com.mkurth.coinsplasher.domain

import cats.FlatMap
import cats.syntax.flatMap._
import cats.syntax.functor._

object RebalancePortfolio {

  type SourcePortfolio[A <: Currency] = Portfolio[A]
  type TargetPortfolio[A <: Currency] = Portfolio[A]
  type Rebalance[A <: Currency]       = SourcePortfolio[A] => RebalancingStrategy[A] => TargetPortfolio[A]

  def rebalance[F[_]: FlatMap, A <: Currency](sourcePortfolio: F[SourcePortfolio[A]], strategy: F[RebalancingStrategy[A]]): F[Option[TargetPortfolio[A]]] =
    for {
      source <- sourcePortfolio
      str    <- strategy
    } yield str.rebalance(source)
}
