package com.mkurth.coinsplasher.domain

import cats.data.NonEmptyList
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineMV
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NoopStrategyTest extends AnyFlatSpec with Matchers {
  behavior of "NoopStrategy"

  it should "do absolutely nothing" in {
    val source = Portfolio[Euro](
      entries = NonEmptyList.one(
        PortfolioEntry(
          coin = Coin(
            marketCapitalisation = MarketCapitalisation(refineMV(BigDecimal(1))),
            price                = Price(refineMV(BigDecimal(1))),
            currency             = CryptoCurrency(refineMV[NonEmpty]("BTC"), '“')
          ),
          share = Share(refineMV(BigDecimal(1)))
        )
      )
    )

    NoopStrategy[Euro]().rebalance(source) shouldBe Some(source)
  }
}
