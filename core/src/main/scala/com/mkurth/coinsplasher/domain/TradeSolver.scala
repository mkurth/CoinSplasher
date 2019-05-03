package com.mkurth.coinsplasher.domain

import com.mkurth.coinsplasher.domain.Types.{CoinShare, CoinSymbol}
import com.mkurth.coinsplasher.domain.model.{CoinBalance, Share}
import com.mkurth.coinsplasher.domain.repo.MarketCoin

import scala.language.implicitConversions

sealed trait Order {
  val coinSymbol: CoinSymbol
  val amount: CoinShare
  val worth: BigDecimal
}

case class SellOrder(coinSymbol: CoinSymbol, amount: CoinShare, worth: BigDecimal) extends Order

case class BuyOrder(coinSymbol: CoinSymbol, amount: CoinShare, worth: BigDecimal) extends Order

object TradeSolver {

  implicit def toTuple(share: Share): (CoinSymbol, Share) = share.coin.coinSymbol -> share

  implicit def toTuple(share: CoinBalance): (CoinSymbol, CoinBalance) = share.coinSymbol -> share

  implicit def toTuple(share: MarketCoin): (CoinSymbol, MarketCoin) = share.coin.coinSymbol -> share

  implicit def toTupleList[A](a: List[A])(implicit transformer: A => (CoinSymbol, A)): List[(CoinSymbol, A)] = a.map(transformer)

  implicit class TargetShareWorth(share: Share) {
    /**
      * 60% von 1000€
      * 0.6 * 1000 = 600
      */
    def worth(implicit balance: CoinShare): BigDecimal = share.share * balance
  }

  implicit class MarketWorth(market: MarketCoin) {
    /**
      * BTC@6000€ * 0.1 BTC im Besitz
      */
    def worth(implicit balance: CoinBalance): CoinShare = market.price * balance.amount
  }

  def solveTrades(currentBalance: List[CoinBalance], targetShares: List[Share], marketData: List[MarketCoin]): List[Order] = {
    val targetShareMap: Map[CoinSymbol, Share] = targetShares.map(toTuple).toMap
    val coinBalanceMap: Map[CoinSymbol, CoinBalance] = currentBalance.map(toTuple).toMap(implicitly)
    val marketDataMap: Map[CoinSymbol, MarketCoin] = marketData.map(toTuple).toMap(implicitly)

    implicit val userBalance: CoinShare = (coinBalanceMap.keys ++ marketDataMap.keys).toSet
      .map((coinSymbol: CoinSymbol) => coinSymbol -> (coinBalanceMap.get(coinSymbol), marketDataMap.get(coinSymbol)))
      .collect { case (_, (Some(balance), Some(market: MarketCoin))) => market.worth(balance) }
      .sum

    (targetShareMap.keys ++ coinBalanceMap.keys ++ marketDataMap.keys).toSet
      .map({key: CoinSymbol => (targetShareMap.get(key), coinBalanceMap.get(key), marketDataMap.get(key))})
      .collect({
        case (Some(targetShare: Share), Some(balance), Some(market: MarketCoin)) if targetShare.worth > market.worth(balance) =>
          val orderVolume = targetShare.worth / market.price - balance.amount
          BuyOrder(balance.coinSymbol, orderVolume, targetShare.worth - market.worth(balance) )
        case (Some(targetShare: Share), Some(balance), Some(market: MarketCoin)) if targetShare.worth < market.worth(balance) =>
          val sellVolume = balance.amount - targetShare.worth / market.price
          SellOrder(balance.coinSymbol, sellVolume, market.worth(balance) - targetShare.worth)
        case (None, Some(balance), Some(market: MarketCoin)) =>
          SellOrder(balance.coinSymbol, balance.amount, market.worth(balance))
        case (Some(targetShare: Share), None, Some(market: MarketCoin)) =>
          val orderVolume = targetShare.worth / market.price
          BuyOrder(targetShare.coin.coinSymbol, orderVolume, targetShare.worth)
      }).toList
  }

}
