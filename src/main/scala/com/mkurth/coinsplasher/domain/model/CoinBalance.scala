package com.mkurth.coinsplasher.domain.model

import com.mkurth.coinsplasher.domain.Types.{CoinShare, CoinSymbol}

case class CoinBalance(coinSymbol: CoinSymbol, amount: CoinShare)

