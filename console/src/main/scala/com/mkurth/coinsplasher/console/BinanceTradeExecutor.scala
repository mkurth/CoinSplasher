package com.mkurth.coinsplasher.console

import cats.data.NonEmptyList
import cats.effect.IO
import com.mkurth.coinsplasher.domain.RebalancePortfolio.{SuccessfulTrade, TradeExecutor}

object BinanceTradeExecutor {

  def executor(binanceClient: BinanceClient[IO, String]): TradeExecutor[IO] =
    plan =>
      for {
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
