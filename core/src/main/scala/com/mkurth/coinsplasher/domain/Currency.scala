package com.mkurth.coinsplasher.domain

import eu.timepit.refined.api.Refined
import eu.timepit.refined.predicates.all.NonEmpty
import eu.timepit.refined.{refineMV, refineV}

sealed trait Currency {
  val name: String Refined NonEmpty
  val symbol: Char
}
final case class CryptoCurrency(name: String Refined NonEmpty, symbol: Char) extends Currency
object CryptoCurrency {
  def apply(name: String, symbol: Char): Either[String, CryptoCurrency] = refineV[NonEmpty](name).map(n => CryptoCurrency(n, symbol))
}

sealed trait Fiat extends Currency
case object Dollar extends Fiat {
  val name: String Refined NonEmpty = refineMV("usd")
  val symbol: Char                  = '$'
}
case object Euro extends Fiat {
  val name: String Refined NonEmpty = refineMV("eur")
  val symbol: Char                  = '€'
}
