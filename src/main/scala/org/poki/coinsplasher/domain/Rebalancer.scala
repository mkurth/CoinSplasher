package org.poki.coinsplasher.domain

import org.poki.coinsplasher.{Coin, Share}

object Rebalancer {

  def rebalanceByMarketCap(data: Seq[Coin], maxShareInPercent: BigDecimal): Seq[Share] = {
    val totalCap = data.map(_.marketCap).sum
    val shares = data.map(coin => Share(coin, coin.marketCap / totalCap))
    val (sharesOverThreshold, sharesBeneathThreshold) = shares.partition(_.share >= maxShareInPercent)
    val restPercent = 1 - sharesOverThreshold.length * maxShareInPercent
    val sum = sharesBeneathThreshold.map(_.share).sum
    val scaledBeneath = sharesBeneathThreshold.map(s => s.copy(share = restPercent / sum * s.share))
    sharesOverThreshold.map(s => s.copy(share = maxShareInPercent)) ++ rebalanceByMarketCap(scaledBeneath, maxShareInPercent, restPercent)
  }

  private def rebalanceByMarketCap(shares: Seq[Share], maxShareInPercent: BigDecimal, remaining: BigDecimal): Seq[Share] = {
    val (sharesOverThreshold, sharesBeneathThreshold) = shares.partition(_.share >= maxShareInPercent)
    val sum = sharesBeneathThreshold.map(_.share).sum
    val cappedOver = sharesOverThreshold.map(s => s.copy(share = maxShareInPercent))
    val restPercent = remaining - sharesOverThreshold.length * maxShareInPercent
    if(restPercent / sum > 1) {
      val scaledBeneath = sharesBeneathThreshold.map(s => s.copy(share = restPercent / sum * s.share))
      cappedOver.map(s => s.copy(share = maxShareInPercent)) ++ rebalanceByMarketCap(scaledBeneath, maxShareInPercent, restPercent)
    } else {
      sharesOverThreshold ++ sharesBeneathThreshold
    }

  }
}
