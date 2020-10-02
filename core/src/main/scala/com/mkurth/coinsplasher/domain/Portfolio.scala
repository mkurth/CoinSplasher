package com.mkurth.coinsplasher.domain
import cats.data.NonEmptyList
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.refineV

final case class Share(value: BigDecimal Refined Positive) {
  def <(other: Share): Boolean               = value.value < other.value.value
  def -(other: Share): Either[String, Share] = refineV[Positive](value.value - other.value.value).map(Share)
}
final case class Price[A <: Currency](value: BigDecimal Refined Positive)
final case class MarketCapitalisation(value: BigDecimal Refined Positive)
final case class Coin[A <: Currency](marketCapitalisation: MarketCapitalisation, price: Price[A], currency: CryptoCurrency)
final case class PortfolioEntry[A <: Currency](coin: Coin[A], share: Share)
final case class Portfolio[A <: Currency](entries: NonEmptyList[PortfolioEntry[A]]) {
  val totalValue: Either[String, Price[A]] = refineV[Positive](entries.map(entry => entry.share.value * entry.coin.price.value).toList.sum).map(Price[A])
}
