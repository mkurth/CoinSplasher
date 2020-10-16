package com.mkurth.coinsplasher.domain
import cats.data.NonEmptyList
import com.mkurth.coinsplasher.domain.RefinedOps._

final case class Share(value: PositiveBigDecimal)
final case class Price[A <: Currency](value: PositiveBigDecimal)
final case class MarketCapitalisation(value: PositiveBigDecimal)
final case class Coin[A <: Currency](marketCapitalisation: MarketCapitalisation, price: Price[A], currency: CryptoCurrency)
final case class PortfolioEntry[A <: Currency](coin: Coin[A], share: Share)
final case class Portfolio[A <: Currency](entries: NonEmptyList[PortfolioEntry[A]]) {
  val totalValue: Price[A] = Price[A](entries.map(entry => entry.share.value * entry.coin.price.value).sum)
}
