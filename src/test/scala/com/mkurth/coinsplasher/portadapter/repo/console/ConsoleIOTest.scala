package com.mkurth.coinsplasher.portadapter.repo.console

import com.mkurth.coinsplasher.domain.{BuyOrder, SellOrder}
import org.scalatest._

class ConsoleIOTest extends FlatSpec with Matchers {


  val consoleIo = new ConsoleIO {}
  "ConsoleIO" should "write BuyOrder" in {
    consoleIo.orderToString(BuyOrder("BTC", 12)) should be("buy BTC 12")
  }

  it should "read BuyOrder" in {
    consoleIo.stringToOrder("buy LTC 1.11") should be(Some(BuyOrder("LTC", 1.11)))
  }

  it should "write SellOrder" in {
    consoleIo.orderToString(SellOrder("XLM", 0.0012)) should be("sell XLM 0.0012")
  }

  it should "read SellOrder" in {
    consoleIo.stringToOrder("sell OMG 0.00001") should be(Some(SellOrder("OMG", 0.00001)))
  }

  it should "not read invalid command" in {
    consoleIo.stringToOrder("something something") should be(None)
  }

}
