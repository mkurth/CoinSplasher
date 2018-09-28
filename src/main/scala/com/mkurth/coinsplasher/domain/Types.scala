package com.mkurth.coinsplasher.domain

object Types {

  /**
    * value between 0 and 1, e.g. 0.6 for 60%
    */
  type Percent = BigDecimal
  /**
    * representation of coin amount, e.g. 1.7002131 BTC
    */
  type CoinShare = BigDecimal

  /**
    * balance on trade exchange in currency, e.g. 1300 (€)
    */
  type UserBalance = BigDecimal

  /**
    * e.g.: BTC, LTC
    */
  type CoinSymbol = String
}
