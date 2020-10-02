package com.mkurth.coinsplasher.console

import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineMV
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BinanceClientTest extends AnyFlatSpec with Matchers {
  behavior of "Binance Client"

  it should "sign correctly with the example secret key" in {
    val signature = BinanceClient.sign(
      data      = refineMV("symbol=LTCBTC&side=BUY&type=LIMIT&timeInForce=GTC&quantity=1&price=0.1&recvWindow=5000&timestamp=1499827319559"),
      secretKey = refineMV("NhqPtmdSJYdKjVHjA7PZj4Mge3R5YNiP1e3UZjInClVN65XAbvqqM6A7H5fATj0j")
    )
    signature shouldBe Some(refineMV[NonEmpty]("c8db56825ae71d6d79447849e617115f4a920fa2acdcab2b053c4b2838bd6b71"))
  }
}
