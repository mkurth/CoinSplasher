package com.mkurth.coinsplasher.portadapter.repo.console

import com.mkurth.coinsplasher.domain.{BuyOrder, Order, SellOrder}

import scala.util.Try

trait ConsoleIO {

  def orderToString(order: Order): String = {
    order match {
      case BuyOrder(coinSymbol, amount) => s"buy $coinSymbol $amount"
      case SellOrder(coinSymbol, amount) => s"sell $coinSymbol $amount"
    }
  }

  def stringToOrder(in: String): Option[Order] = {
    Try {
      val Array(orderType: String, coinSymbol: String, amount: String) = in.split(" ")
      orderType match {
        case "buy" => BuyOrder(coinSymbol, BigDecimal(amount))
        case "sell" => SellOrder(coinSymbol, BigDecimal(amount))
      }
    }.toOption
  }

}
