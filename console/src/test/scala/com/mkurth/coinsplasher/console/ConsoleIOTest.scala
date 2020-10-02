package com.mkurth.coinsplasher.console

import com.mkurth.coinsplasher.domain.{BuyOrder, SellOrder}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConsoleIOTest extends AnyFlatSpec with Matchers {

  val consoleIo = new ConsoleIO {}
  "ConsoleIO" should "write BuyOrder" in {
    consoleIo.orderToString(BuyOrder("BTC", 12, 0)) should (include("buy") and include("BTC") and include("12"))
  }

  it should "read BuyOrder" in {
    consoleIo.stringToOrder("buy LTC 1.11") should be(Some(BuyOrder("LTC", 1.11, 0)))
  }

  it should "write SellOrder" in {
    consoleIo.orderToString(SellOrder("XLM", 0.0012, 0)) should (include("sell") and include("XLM") and include("0.0012"))
  }

  it should "read SellOrder" in {
    consoleIo.stringToOrder("sell \t OMG\t 0.00001 for 10 €") should be(Some(SellOrder("OMG", 0.00001, 0)))
  }

  it should "not read invalid command" in {
    consoleIo.stringToOrder("something something") should be(None)
  }

}
