package com.mkurth.coinsplasher.domain

import cats.data.NonEmptyList
import com.mkurth.coinsplasher.domain.TradingPlanner.{BuyOrder, SellOrder}
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineMV
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TradingPlannerTest extends AnyFlatSpec with Matchers {

  behavior of "TradingPlanner"

  it should "sell 0.5 BTC and buy 5 LTC" in {
    val source = Portfolio[Fiat](NonEmptyList.one(`1 BTC`))
    val target = Portfolio[Fiat](NonEmptyList.of(`0.5 BTC`, `5 LTC`))

    val result = TradingPlanner.planTrade[Fiat](source)(target)
    result.sellOrders shouldBe List(SellOrder(BTC.currency, Share(refineMV(BigDecimal(0.5)))))
    result.buyOrders shouldBe List(BuyOrder(LTC.currency, Share(refineMV(BigDecimal(5)))))
  }

  private val BTC: Coin[Fiat] = Coin[Fiat](
    marketCapitalisation = MarketCapitalisation(refineMV(BigDecimal(1))),
    price                = Price(refineMV(BigDecimal(1))),
    currency             = CryptoCurrency(refineMV[NonEmpty]("BTC"), '“')
  )
  private val LTC: Coin[Fiat] = Coin[Fiat](
    marketCapitalisation = MarketCapitalisation(refineMV(BigDecimal(1))),
    price                = Price(refineMV(BigDecimal(0.1))),
    currency             = CryptoCurrency(refineMV[NonEmpty]("LTC"), '“')
  )

  private val `1 BTC` = PortfolioEntry(
    coin  = BTC,
    share = Share(refineMV(BigDecimal(1)))
  )
  private val `5 LTC` = PortfolioEntry(
    coin  = LTC,
    share = Share(refineMV(BigDecimal(5)))
  )
  private val `0.5 BTC` = PortfolioEntry(
    coin  = BTC,
    share = Share(refineMV(BigDecimal(0.5)))
  )
}
