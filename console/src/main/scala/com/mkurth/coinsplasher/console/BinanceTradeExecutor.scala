package com.mkurth.coinsplasher.console

import java.math.{MathContext, RoundingMode}

import cats.Show
import cats.data.NonEmptyList
import cats.effect.IO
import com.mkurth.coinsplasher.domain.RebalancePortfolio.{SuccessfulTrade, TradeExecutor}
import com.mkurth.coinsplasher.domain.TradingPlanner.{BuyOrder, SellOrder}

import scala.io.StdIn.readLine

object BinanceTradeExecutor {

  implicit val showBuy: Show[BuyOrder]   = (t: BuyOrder) => s"BUY ${t.currency.name} - ${t.share.value.value.round(new MathContext(4, RoundingMode.HALF_UP))}"
  implicit val showSell: Show[SellOrder] = (t: SellOrder) => s"SELL ${t.currency.name} - ${t.share.value.value.round(new MathContext(4, RoundingMode.HALF_UP))}"

  def executor(binanceClient: BinanceClient[IO, String]): TradeExecutor[IO] =
    plan =>
      for {
        _            <- IO(println(plan.buyOrders.map(Show[BuyOrder].show).mkString("Buy Orders\n", "\n", "")))
        _            <- IO(println(plan.sellOrders.map(Show[SellOrder].show).mkString("Sell Orders\n", "\n", "")))
        confirmation <- IO(readLine("Is this okay? [Y/n]"))
        _            <- if (confirmation == "Y" || confirmation.isEmpty) IO.unit else IO.raiseError(new InterruptedException)
        _ <- NonEmptyList.fromList(plan.sellOrders) match {
          case Some(value) => binanceClient.putSellOrders(value)
          case None        => IO.unit
        }
        _ <- NonEmptyList.fromList(plan.buyOrders) match {
          case Some(value) => binanceClient.putBuyOrders(value)
          case None        => IO.unit
        }
      } yield SuccessfulTrade
}
