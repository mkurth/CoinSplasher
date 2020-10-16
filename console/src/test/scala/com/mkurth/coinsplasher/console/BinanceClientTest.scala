package com.mkurth.coinsplasher.console

import com.mkurth.coinsplasher.console.BinanceClient._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineMV
import io.circe
import io.circe.Decoder.Result
import io.circe.Json
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe.generic.auto._
import io.circe.parser.parse

import scala.io.Source

class BinanceClientTest extends AnyFlatSpec with Matchers {
  behavior of "Binance Client"

  it should "sign correctly with the example secret key" in {
    val signature = BinanceClient.sign(
      data      = refineMV("symbol=LTCBTC&side=BUY&type=LIMIT&timeInForce=GTC&quantity=1&price=0.1&recvWindow=5000&timestamp=1499827319559"),
      secretKey = refineMV("NhqPtmdSJYdKjVHjA7PZj4Mge3R5YNiP1e3UZjInClVN65XAbvqqM6A7H5fATj0j")
    )
    signature shouldBe Some(refineMV[NonEmpty]("c8db56825ae71d6d79447849e617115f4a920fa2acdcab2b053c4b2838bd6b71"))
  }

  it should "decode filter" in {
    parse("""{
            |  "filterType": "PRICE_FILTER",
            |  "minPrice": "0.00000100",
            |  "maxPrice": "100000.00000000",
            |  "tickSize": "0.00000100"
            |}""".stripMargin).flatMap(_.as[SymbolFilter]) shouldBe Right(PriceSymbolFilter("0.00000100", "100000.00000000", "0.00000100"))
    parse("""{
            |  "filterType":"MAX_POSITION",
            |  "maxPosition":"10.00000000"
            |}""".stripMargin).flatMap(_.as[SymbolFilter]) shouldBe Right(MaxPositionSymbolFilter("10.00000000"))

    parse("""{
            |  "filterType": "MARKET_LOT_SIZE",
            |  "minQty": "0.00100000",
            |  "maxQty": "100000.00000000",
            |  "stepSize": "0.00100000"
            |}""".stripMargin).flatMap(_.as[SymbolFilter]) shouldBe Right(LotSize("0.00100000", "100000.00000000", "0.00100000", "MARKET_LOT_SIZE"))
  }
}
