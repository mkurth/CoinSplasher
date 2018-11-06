package com.mkurth.coinsplasher.portadapter.repo.console

import com.mkurth.coinsplasher.domain.{BuyOrder, Order, SellOrder}

import scala.math.BigDecimal
import scala.math.BigDecimal.RoundingMode
import scala.util.Try

trait ConsoleIO {

  def orderToString(order: Order): String = {
    order match {
      case BuyOrder(coinSymbol, amount, worth) =>
        s"buy  $coinSymbol\t ${amount.setScale(10, RoundingMode.CEILING)}\t for ${worth.setScale(2, RoundingMode.CEILING)} €"
      case SellOrder(coinSymbol, amount, worth) =>
        s"sell $coinSymbol\t ${amount.setScale(10, RoundingMode.CEILING)}\t for ${worth.setScale(2, RoundingMode.CEILING)} €"
    }
  }

  def stringToOrder(in: String): Option[Order] = {
    Try {
      val Array(orderType: String, coinSymbol: String, amount: String) = in.split("[ \t]+").take(3)
      orderType match {
        case "buy" => BuyOrder(coinSymbol, BigDecimal(amount), 0)
        case "sell" => SellOrder(coinSymbol, BigDecimal(amount), 0)
      }
    }.toOption
  }

}
