package com.mkurth.coinsplasher.console

import cats.data.EitherT
import cats.effect.{ExitCode, IO, IOApp}
import com.mkurth.coinsplasher.domain.RebalancePortfolio._
import com.mkurth.coinsplasher.domain.{RebalancingStrategy, _}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.predicates.all.NonEmpty
import eu.timepit.refined.refineV

import scala.io.StdIn.{readInt, readLine}
import scala.language.implicitConversions

object Console extends IOApp {

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
      currency <- readNonEmptyLine("Choose a FIAT currency (EURO/DOLLAR)")
        .map(
          _.value.toLowerCase() match {
            case "eur" | "euro" => Euro
            case "$" | "dollar" => Dollar
            case _              => Euro
          }
        )
      exitCode <- main(secretKey, apiKey, splashTo, currency)
    } yield exitCode

  private def main[A <: Currency](secretKey: String Refined NonEmpty, apiKey: String Refined NonEmpty, splashTo: Int Refined Positive, a: A) =
    for {
      _ <- IO(println(s"using $a as FIAT currency"))
      client          = BinanceClient(secretKey, apiKey)
      gecko           = CoinGeckoClient()
      sourcePortfolio = BinanceSourcePortfolio.get[A](client, gecko, a)
      strategy = gecko.markets[A](a).map {
        case Right(market) => EquallySplashTo[A](market.take(splashTo.value))
        case Left(_)       => NoopStrategy[A]()
      }
      tradePlanner = IO(TradingPlanner.planTrade[A])
      result <- RebalancePortfolio.rebalance[IO, A](sourcePortfolio, strategy, tradePlanner, tradeExecutor)
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
