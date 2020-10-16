package com.mkurth.coinsplasher.domain

import cats.data.NonEmptyList
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.numeric.{NonPositive, Positive}
import eu.timepit.refined.predicates.all.NonEmpty
import eu.timepit.refined.refineV
import eu.timepit.refined.types.all._

object RefinedOps {

  type PositiveBigDecimal = PosBigDecimal
  type NonEmptyString     = String Refined NonEmpty

  implicit class BigDecimalOps(value: PositiveBigDecimal)(implicit validate: Validate[BigDecimal, Positive], vNeg: Validate[BigDecimal, NonPositive]) {
    def <(other: PositiveBigDecimal): Boolean    = Numeric[BigDecimal].lt(value.value, other.value)
    def >(other: PositiveBigDecimal): Boolean    = Numeric[BigDecimal].gt(value.value, other.value)
    def -(other: PositiveBigDecimal): BigDecimal = Numeric[BigDecimal].minus(value.value, other.value)
    def minusE(other: PositiveBigDecimal): Either[NonPosBigDecimal, PositiveBigDecimal] = Numeric[BigDecimal].minus(value.value, other.value) match {
      case i if i > 0 => Right(refineV[Positive](i).toOption.get)
      case i          => Left(refineV[NonPositive](i).toOption.get)
    }
    def +(other: PositiveBigDecimal): PositiveBigDecimal = refineV[Positive](Numeric[BigDecimal].plus(value.value, other.value)).toOption.get
    def *(other: PositiveBigDecimal): PositiveBigDecimal = refineV[Positive](Numeric[BigDecimal].times(value.value, other.value)).toOption.get
    def /(other: PositiveBigDecimal): PositiveBigDecimal = refineV[Positive](value.value / other.value).toOption.get
  }
  implicit class NELPositiveOps(nel: NonEmptyList[PositiveBigDecimal]) {
    val sum: PositiveBigDecimal = refineV[Positive](nel.foldLeft(BigDecimal(0))(_ + _.value)).toOption.get
  }
  implicit class NELOps[A](nel: NonEmptyList[A]) {
    val count: PositiveBigDecimal                      = refineV[Positive](BigDecimal(nel.length)).toOption.get
    def take(i: Int Refined Positive): NonEmptyList[A] = NonEmptyList.fromListUnsafe(nel.toList.take(i.value))
  }

}
