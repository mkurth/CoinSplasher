package com.mkurth.coinsplasher.console

import cats.Show
import cats.data.EitherT
import cats.effect.ExitCase.{Canceled, Error}
import cats.effect.{ExitCode, IO, IOApp}
import com.mkurth.coinsplasher.domain.RebalancePortfolio._
import com.mkurth.coinsplasher.domain.RefinedOps.{NELOps, NonEmptyString}
import com.mkurth.coinsplasher.domain._
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.predicates.all.NonEmpty
import eu.timepit.refined.{refineMV, refineV}
import eu.timepit.refined.types.numeric.PosInt

import scala.io.StdIn.{readInt, readLine}
import scala.language.implicitConversions

object Console extends IOApp {

  type WithError[T] = EitherT[IO, String, T]

  override def run(args: List[String]): IO[ExitCode] =
    for {
      secretKey <- readNonEmptyLine("Binance Secret Key")
      apiKey    <- readNonEmptyLine("Binance API Key")
      splashTo  <- readPositiveIntLine("amount of coins to distribute to")
      currency <- readNonEmptyLine("Choose a FIAT currency ([EURO]/DOLLAR)")
        .map(
          _.value.toLowerCase() match {
            case "$" | "dollar" | "usd" => Fiat(refineMV[NonEmpty]("usd"), '$')
            case _                      => Fiat(refineMV[NonEmpty]("eur"), '€')
          }
        )
      strategy <- choose[String]("Choose a strategy", List(DistributeBasedOnMarketCap, NoopStrategy, EquallyDistributeTo).map(_.toString), DistributeBasedOnMarketCap.toString).map(i =>
        refineV[NonEmpty](i) match {
          case Left(value)  => throw new IllegalArgumentException(s"$value is not positive")
          case Right(value) => value
      })
      exitCode <- main(secretKey, apiKey, splashTo, currency, strategy).guaranteeCase {
        case Canceled => IO(println("Interrupted: releasing and exiting!"))
        case Error(e) => IO(println(s"Error occurred: $e"))
        case _        => IO(println("Normal exit!"))
      }
    } yield exitCode

  private def main[A <: Currency](secretKey: NonEmptyString, apiKey: NonEmptyString, splashTo: PosInt, a: A, strategyChoice: NonEmptyString): IO[ExitCode] =
    for {
      _      <- IO(println(s"using $a as FIAT currency"))
      client <- BinanceClient(secretKey, apiKey)
      gecko           = CoinGeckoClient()
      sourcePortfolio = BinanceSourcePortfolio.get[A](client, gecko, a)
      strategy = gecko.markets[A](a).map {
        case Right(market) =>
          strategyChoice.value match {
            case "EquallyDistributeTo"        => EquallyDistributeTo[A](market.take(splashTo))
            case "DistributeBasedOnMarketCap" => DistributeBasedOnMarketCap[A](market.take(splashTo))
            case _                            => NoopStrategy[A]()
          }
        case Left(_) => NoopStrategy[A]()
      }
      tradePlanner  = IO(TradingPlanner.planTrade[A])
      tradeExecutor = IO(BinanceTradeExecutor.executor(client))
      result <- RebalancePortfolio.rebalance[IO, A](sourcePortfolio, strategy, tradePlanner, tradeExecutor)
    } yield
      result match {
        case SuccessfulTrade => ExitCode.Success
        case _               => ExitCode.Error
      }

  private def readPositiveIntLine(prompt: String): IO[PosInt] =
    IO {
      println(prompt + ":")
      readInt()
    }.map(i =>
      refineV[Positive](i) match {
        case Left(value)  => throw new IllegalArgumentException(s"$value is not positive")
        case Right(value) => value
    })

  private def readNonEmptyLine(prompt: String): IO[NonEmptyString] =
    IO(readLine(prompt + ":\n")).map(v =>
      refineV[NonEmpty](v.trim) match {
        case Left(value)  => throw new IllegalArgumentException(s"$value is empty")
        case Right(value) => value
    })

  private def choose[A: Show](prompt: String, options: List[A], default: A): IO[A] =
    IO(
      readLine(
        prompt + options
          .map(option => {
            if (default == option) "[" + Show[A].show(option) + "]" else Show[A].show(option)
          })
          .mkString(" (", ", ", ")") + ":\n")).map(v => options.find(option => Show[A].show(option).toLowerCase == v.trim.toLowerCase).getOrElse(default))
}
