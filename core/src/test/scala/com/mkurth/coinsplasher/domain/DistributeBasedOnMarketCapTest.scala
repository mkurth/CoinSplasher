package com.mkurth.coinsplasher.domain

import cats.data.NonEmptyList
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.predicates.all.NonEmpty
import eu.timepit.refined.refineMV
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DistributeBasedOnMarketCapTest extends AnyFlatSpec with Matchers {

  private val BTC = Coin[Fiat](
    marketCapitalisation = MarketCapitalisation(refineMV[Positive](BigDecimal(100))),
    price                = Price(refineMV[Positive](BigDecimal(10))),
    currency             = CryptoCurrency(refineMV[NonEmpty]("BTC"), 'B')
  )
  private val LTC = Coin[Fiat](
    marketCapitalisation = MarketCapitalisation(refineMV[Positive](BigDecimal(50))),
    price                = Price(refineMV[Positive](BigDecimal(1))),
    currency             = CryptoCurrency(refineMV[NonEmpty]("LTC"), 'L')
  )

  behavior of "DistributeBasedOnMarketCap"

  it should "create a target portfolio based on the current market cap" in {
    val marketCap = List(
      BTC,
      LTC
    )

    val source = Portfolio[Fiat](NonEmptyList.one(PortfolioEntry(BTC, Share(refineMV[Positive](BigDecimal(15))))))
    DistributeBasedOnMarketCap(marketCap).rebalance(source) shouldBe Some(
      Portfolio[Fiat](
        NonEmptyList.of(
          PortfolioEntry(BTC, Share(refineMV[Positive](BigDecimal(10.0)))),
          PortfolioEntry(LTC, Share(refineMV[Positive](BigDecimal(50.0))))
        )))
  }

}
