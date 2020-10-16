package com.mkurth.coinsplasher.domain

import cats.effect.Async
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.mkurth.coinsplasher.domain.TradingPlanner.{Order, TradingPlan}

object RebalancePortfolio {

  sealed trait TradingResult
  case object SuccessfulTrade extends TradingResult
  case object FailedTrade extends TradingResult
  case class PartialTrade(finished: List[Order], failed: List[Order], notTried: List[Order]) extends TradingResult

  type SourcePortfolio[A <: Currency] = Portfolio[A]
  type TargetPortfolio[A <: Currency] = Portfolio[A]
  type Rebalance[A <: Currency]       = SourcePortfolio[A] => RebalancingStrategy[A] => TargetPortfolio[A]
  type TradePlanner[A <: Currency]    = SourcePortfolio[A] => TargetPortfolio[A] => TradingPlan
  type TradeExecutor[F[_]]            = TradingPlan => F[TradingResult]

  def rebalance[F[_]: Async, A <: Currency](sourcePortfolio: F[SourcePortfolio[A]],
                                            strategy: F[RebalancingStrategy[A]],
                                            tradePlanner: F[TradePlanner[A]],
                                            tradeExecutor: F[TradeExecutor[F]]): F[TradingResult] =
    for {
      source   <- sourcePortfolio
      str      <- strategy
      planner  <- tradePlanner
      executor <- tradeExecutor
      target = str.rebalance(source)
      plan   = planner(source)(target)
      tradingResult <- executor(plan)
    } yield tradingResult
}
