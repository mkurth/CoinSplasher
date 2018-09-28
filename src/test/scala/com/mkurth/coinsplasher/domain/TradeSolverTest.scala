package com.mkurth.coinsplasher.domain

import com.mkurth.coinsplasher.domain.repo.MarketCoin
import com.mkurth.coinsplasher.{Coin, CoinBalance, Share}
import org.scalatest.{FlatSpec, Matchers}

class TradeSolverTest extends FlatSpec with Matchers {

  val btc = Coin("BTC", 1, 1)
  val ltc = Coin("LTC", 1, 1)
  val `1000€ in LTC` = Seq(CoinBalance(ltc.coinSymbol, 10))
  val `1000€ in BTC` = Seq(CoinBalance(btc.coinSymbol, 1))
  val `1000€ in LTC and 1000€ in BTC` = Seq(CoinBalance(ltc.coinSymbol, 10), CoinBalance(btc.coinSymbol, 1))
  val `only BTC target` = Seq(Share(btc, 1))
  val `50:50 BTC/LTC target` = Seq(Share(btc, 0.5), Share(ltc, 0.5))
  val marketCoins = Seq(
    MarketCoin(btc, 1000),
    MarketCoin(ltc, 100)
  )

  s"TradeSolver with 1000€ in LTC and only BTC target" should "place BuyOrder for BTC" in {
    TradeSolver.solveTrades(`1000€ in LTC`, `only BTC target`, marketCoins) should contain(BuyOrder(btc.coinSymbol, 1))
  }

  it should "place SellOrder for all LTC" in {
    TradeSolver.solveTrades(`1000€ in LTC`, `only BTC target`, marketCoins) should contain(SellOrder(ltc.coinSymbol, 10))
  }

  s"TradeSolver with 1000€ in LTC and 1000€ in BTC and only BTC target" should "place BuyOrder for 1 BTC" in {
    TradeSolver.solveTrades(`1000€ in LTC`, `only BTC target`, marketCoins) should contain(BuyOrder(btc.coinSymbol, 1))
  }

  s"TradeSolver with 1000€ in BTC and 50:50 BTC/LTC target" should "place SellOrder for 0.5 BTC" in {
    TradeSolver.solveTrades(`1000€ in BTC`, `50:50 BTC/LTC target`, marketCoins) should contain(SellOrder(btc.coinSymbol, 0.5))
  }

  it should "place BuyOrder for 5 LTC" in {
    TradeSolver.solveTrades(`1000€ in BTC`, `50:50 BTC/LTC target`, marketCoins) should contain(BuyOrder(ltc.coinSymbol, 5))
  }

}
