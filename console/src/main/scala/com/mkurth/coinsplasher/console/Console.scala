package com.mkurth.coinsplasher.console

import cats.effect.{ExitCode, IO, IOApp}
import com.mkurth.coinsplasher.domain.RebalancePortfolio._
import com.mkurth.coinsplasher.domain._

object Console extends IOApp {

  val sourcePortfolio: IO[SourcePortfolio[Euro]] = IO(???)
  val strategy: IO[RebalancingStrategy[Euro]]    = IO(NoopStrategy())
  val tradePlanner: IO[TradePlanner[Euro]]       = IO(???)
  val tradeExecutor: IO[TradeExecutor[IO]]       = IO(???)

  override def run(args: List[String]): IO[ExitCode] =
    for {
      result <- RebalancePortfolio.rebalance[IO, Euro](sourcePortfolio, strategy, tradePlanner, tradeExecutor)
    } yield
      result match {
        case Some(SuccessfulTrade) => ExitCode.Success
        case _                     => ExitCode.Error
      }
}
