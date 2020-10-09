package com.mkurth.coinsplasher.console

import cats.data.EitherT
import cats.effect.{ExitCode, IO, IOApp}
import com.mkurth.coinsplasher.domain.RebalancePortfolio._
import com.mkurth.coinsplasher.domain._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.predicates.all.NonEmpty
import eu.timepit.refined.refineV

import scala.io.StdIn.{readInt, readLine}
import scala.language.implicitConversions

object Console extends IOApp {

  val tradePlanner: IO[TradePlanner[Euro]] = IO(TradingPlanner.planTrade)
  val tradeExecutor: IO[TradeExecutor[IO]] = IO(plan => {
    println(plan)
    IO(SuccessfulTrade)
  })

  type WithError[T] = EitherT[IO, String, T]

  override def run(args: List[String]): IO[ExitCode] =
    for {
      secretKey <- readNonEmptyLine("Binance Secret Key")
      apiKey    <- readNonEmptyLine("Binance API Key")
      splashTo  <- readPositiveIntLine("amount of coins to distribute to")
      client          = BinanceClient(secretKey, apiKey)
      gecko           = CoinGeckoClient()
      sourcePortfolio = BinanceSourcePortfolio.get(client, gecko)
      strategy: IO[RebalancingStrategy[Euro]] = gecko.markets.map {
        case Right(market) => EquallySplashTo[Euro](market.take(splashTo.value))
        case Left(_)       => NoopStrategy[Euro]()
      }
      result <- RebalancePortfolio.rebalance[IO, Euro](sourcePortfolio, strategy, tradePlanner, tradeExecutor)
    } yield
      result match {
        case Some(SuccessfulTrade) => ExitCode.Success
        case _                     => ExitCode.Error
      }

  private def readPositiveIntLine(prompt: String): IO[Int Refined Positive] =
    IO {
      println(prompt + ":")
      readInt()
    }.map(i =>
      refineV[Positive](i) match {
        case Left(value)  => throw new IllegalArgumentException(s"$value is not positive")
        case Right(value) => value
    })

  private def readNonEmptyLine(prompt: String): IO[String Refined NonEmpty] =
    IO(readLine(prompt + ":\n")).map(v =>
      refineV[NonEmpty](v.trim) match {
        case Left(value)  => throw new IllegalArgumentException(s"$value is empty")
        case Right(value) => value
    })
}
