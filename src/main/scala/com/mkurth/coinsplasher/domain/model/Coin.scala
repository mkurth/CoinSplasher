package com.mkurth.coinsplasher.domain.model

import com.mkurth.coinsplasher.domain.Types.CoinSymbol

case class Coin(coinSymbol: CoinSymbol, marketCap: BigDecimal, tradeCap: BigDecimal)
