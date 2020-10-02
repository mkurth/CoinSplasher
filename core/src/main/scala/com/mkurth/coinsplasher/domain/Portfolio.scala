package com.mkurth.coinsplasher.domain
import cats.data.NonEmptyList
import com.mkurth.coinsplasher.domain.Portfolio.PositiveBigDecimal
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.refineV

final case class Share(value: PositiveBigDecimal)
final case class Price[A <: Currency](value: PositiveBigDecimal)
final case class MarketCapitalisation(value: PositiveBigDecimal)
final case class Coin[A <: Currency](marketCapitalisation: MarketCapitalisation, price: Price[A], currency: CryptoCurrency)
final case class PortfolioEntry[A <: Currency](coin: Coin[A], share: Share)
final case class Portfolio[A <: Currency](entries: NonEmptyList[PortfolioEntry[A]]) {
  val totalValue: Either[String, Price[A]] = refineV[Positive](entries.map(entry => entry.share.value * entry.coin.price.value).toList.sum).map(Price[A])
}
object Portfolio {
  type PositiveBigDecimal = BigDecimal Refined Positive
  implicit class Ops[T: Numeric](value: T Refined Positive)(implicit validate: Validate[T, Positive]) {
    def <(other: T Refined Positive): Boolean            = Numeric[T].lt(value, other)
    def >(other: T Refined Positive): Boolean            = Numeric[T].gt(value, other)
    def -(other: T Refined Positive): T                  = Numeric[T].minus(value, other)
    def +(other: T Refined Positive): T Refined Positive = refineV[Positive](Numeric[T].plus(value, other)).toOption.get
    def *(other: T Refined Positive): T Refined Positive = refineV[Positive](Numeric[T].times(value, other)).toOption.get
  }
}
