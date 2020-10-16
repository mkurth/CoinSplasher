package com.mkurth.coinsplasher.domain

import com.mkurth.coinsplasher.domain.RefinedOps.NonEmptyString

sealed trait Currency {
  val name: NonEmptyString
  val symbol: Char
}
final case class CryptoCurrency(name: NonEmptyString, symbol: Char) extends Currency
final case class Fiat(name: NonEmptyString, symbol: Char) extends Currency
