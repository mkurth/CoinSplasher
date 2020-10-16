package com.mkurth.coinsplasher.domain

import cats.data.NonEmptyList
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineMV
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EquallyDistributeToTest extends AnyFlatSpec with Matchers {
  behavior of "SplashTopN"
  val BTC: Coin[Fiat] = Coin[Fiat](
    marketCapitalisation = MarketCapitalisation(refineMV(BigDecimal(1))),
    price                = Price(refineMV(BigDecimal(1))),
    currency             = CryptoCurrency(refineMV[NonEmpty]("BTC"), '“')
  )
  val LTC: Coin[Fiat] = Coin[Fiat](
    marketCapitalisation = MarketCapitalisation(refineMV(BigDecimal(1))),
    price                = Price(refineMV(BigDecimal(0.1))),
    currency             = CryptoCurrency(refineMV[NonEmpty]("LTC"), '“')
  )

  it should "rebalance" in {
    val source = Portfolio[Fiat](
      entries = NonEmptyList.one(
        PortfolioEntry(
          coin  = BTC,
          share = Share(refineMV(BigDecimal(1)))
        )
      )
    )

    val expectedTarget = Portfolio[Fiat](
      entries = NonEmptyList.of(
        PortfolioEntry(
          coin  = BTC,
          share = Share(refineMV(BigDecimal(0.5)))
        ),
        PortfolioEntry(
          coin  = LTC,
          share = Share(refineMV(BigDecimal(5)))
        )
      )
    )

    val result = EquallyDistributeTo[Fiat](NonEmptyList.of(BTC, LTC)).rebalance(source)
    result should not be source
    result shouldBe expectedTarget
  }
}
