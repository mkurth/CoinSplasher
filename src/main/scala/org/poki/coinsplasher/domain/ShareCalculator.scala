package org.poki.coinsplasher.domain

import org.poki.coinsplasher.domain.Types.Percent
import org.poki.coinsplasher.{Coin, Share}

object ShareCalculator {

  /**
    * calculates weighted shares for all given coins
    *
    * @param by function to determine weight of each coin
    * @param data actual coin data
    * @param maxShareInPercent percentage cap for a single coin
    * @return percentage of each coin
    */
  def shares(by: Coin => BigDecimal)(data: Seq[Coin], maxShareInPercent: Percent): Seq[Share] = {
    val totalCap = data.map(by).sum
    val shares = data.map(coin => Share(coin, by(coin) / totalCap))
    val (sharesOverThreshold, sharesBeneathThreshold) = shares.partition(_.share >= maxShareInPercent)
    val restPercent = 1 - sharesOverThreshold.length * maxShareInPercent
    val sum = sharesBeneathThreshold.map(_.share).sum
    val scaledBeneath = sharesBeneathThreshold.map(s => s.copy(share = restPercent / sum * s.share))
    sharesOverThreshold.map(s => s.copy(share = maxShareInPercent)) ++ capRemainingShares(by)(scaledBeneath, maxShareInPercent, restPercent)
  }

  /**
    * recursive function to cap remaining coins
    */
  private def capRemainingShares(by: Coin => BigDecimal = coin => coin.marketCap)(shares: Seq[Share], maxShareInPercent: Percent, remaining: Percent): Seq[Share] = {
    val (sharesOverThreshold, sharesBeneathThreshold) = shares.partition(_.share >= maxShareInPercent)
    val sum = sharesBeneathThreshold.map(_.share).sum
    val cappedOver = sharesOverThreshold.map(s => s.copy(share = maxShareInPercent))
    val restPercent = remaining - sharesOverThreshold.length * maxShareInPercent
    if(sum != 0 && restPercent / sum > 1) {
      val scaledBeneath = sharesBeneathThreshold.map(s => s.copy(share = restPercent / sum * s.share))
      cappedOver.map(s => s.copy(share = maxShareInPercent)) ++ capRemainingShares(by)(scaledBeneath, maxShareInPercent, restPercent)
    } else {
      sharesOverThreshold ++ sharesBeneathThreshold
    }

  }
}
