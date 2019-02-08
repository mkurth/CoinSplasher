package com.mkurth.coinsplasher.order

import com.mkurth.coinsplasher.domain.{BuyOrder, Order, SellOrder}
import slinky.core.StatelessComponent
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html.{className, div}

import scala.math.BigDecimal.RoundingMode
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@JSImport("resources/order/OrderComponent.css", JSImport.Default)
@js.native
object OrderComponentCSS extends js.Object

@react
class OrderComponent extends StatelessComponent {

  case class Props(order: Order)

  private val css = OrderComponentCSS

  override def render(): ReactElement = {
     Seq(div(props.order match {
        case SellOrder(coinSymbol, amount, worth) => "sell"
        case BuyOrder(coinSymbol, amount, worth) => "buy"
      }),
      div(props.order.coinSymbol),
      div(props.order.amount.setScale(3, RoundingMode.HALF_UP).toString()),
      div(props.order.worth.setScale(2, RoundingMode.HALF_UP).toString() + " €")
    )
  }
}
