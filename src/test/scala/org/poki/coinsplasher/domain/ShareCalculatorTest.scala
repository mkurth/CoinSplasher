package org.poki.coinsplasher.domain

import java.math.MathContext

import org.poki.coinsplasher.{Coin, Share}
import org.scalatest._

class ShareCalculatorTest extends FlatSpec with Matchers {

  "ShareCalculator with two coins having a market share of 70:30" should "return 50:50 shares, when capped at 50%" in {
    val btc = Coin("BTC", 70, 1)
    val ltc = Coin("LTC", 30, 1)
    val coins = Seq(btc, ltc)

    ShareCalculator.shares(_.marketCap)(coins, 0.50).map(rounded) should contain(Share(btc, 0.50))
    ShareCalculator.shares(_.marketCap)(coins, 0.50).map(rounded) should contain(Share(ltc, 0.50))
  }

  "ShareCalculator with one coin" should "return one Share with 50%, even when capped at 50%" in {
    val ltc = Coin("LTC", 30, 1)
    val coins = Seq(ltc)

    ShareCalculator.shares(_.marketCap)(coins, 0.50).map(rounded) should contain(Share(ltc, 0.50))
  }

  private def rounded(share: Share): Share = share.copy(share = share.share.round(new MathContext(2)))

}
