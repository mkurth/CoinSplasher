package com.mkurth.coinsplasher.console

import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import com.mkurth.coinsplasher.domain.RebalancePortfolio.SourcePortfolio
import com.mkurth.coinsplasher.domain._
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.refineV

object BinanceSourcePortfolio {

  def get[A <: Currency](client: BinanceClient[IO, String], geckoClient: CoinGeckoClient[IO], a: A): IO[SourcePortfolio[A]] =
    (for {
      balance    <- EitherT(client.currentBalance)
      marketData <- EitherT(geckoClient.markets[A](a))
      entries    <- EitherT.fromOption[IO].apply(NonEmptyList.fromList(matchMarketWithBalance(marketData, balance)), "no positive balances")
    } yield Portfolio(entries)).value.map(_.toOption.get)

  private def matchMarketWithBalance[A <: Currency](marketData: NonEmptyList[Coin[A]], accountBalance: AccountBalance) =
    accountBalance.balances.flatMap(balance => {
      marketData.find(_.currency.name.value.toLowerCase.startsWith(balance.asset.value.toLowerCase.replace("LD", ""))).flatMap { market =>
        for {
          positiveBalance <- refineV[Positive](balance.free.value).toOption
        } yield
          PortfolioEntry(
            coin = market,
            Share(positiveBalance)
          )
      }
    })
}
