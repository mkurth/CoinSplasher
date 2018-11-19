package com.mkurth.coinsplasher

import com.mkurth.coinsplasher.domain.{BuyOrder, CoinService, CoinServiceConfig, SellOrder}
import com.mkurth.coinsplasher.domain.Types.CoinSymbol
import com.mkurth.coinsplasher.domain.model.CoinBalance
import com.mkurth.coinsplasher.domain.repo.{CoinMarketCap, MarketCoin, MarketRepo, TradeRepo}
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, ScalaJSDefined}

@JSImport("resources/App.css", JSImport.Default)
@js.native
object AppCSS extends js.Object

@JSImport("resources/logo.svg", JSImport.Default)
@js.native
object ReactLogo extends js.Object

@react class App extends StatelessComponent {
  type Props = Unit
  private val css = AppCSS

  implicit val ec: ExecutionContext = ExecutionContext.global

  val marketRepo: MarketRepo = new CoinMarketCap
  val tradeRepo: TradeRepo = new TradeRepo {
    override def currentBalance(ignoreCoins: Seq[CoinSymbol]): Future[Seq[CoinBalance]] = ???

    override def sell(order: SellOrder): Future[Any] = ???

    override def buy(order: BuyOrder): Future[Any] = ???
  }
  val config: CoinServiceConfig = new CoinServiceConfig {
    override val blacklistedCoins: Seq[String] = Seq()
    override val ignoreBalanceForCoins: Seq[String] = Seq()
    override val ignoreTradesBelowWorth: BigDecimal = 1
    override val threshold: BigDecimal = 10
    override val limitToCoins: Int = 20
  }

  val service = new CoinService(marketRepo, tradeRepo, config)

  def render(): ReactElement = {
    div(className := "App")(
      header(className := "App-header")(
        img(src := ReactLogo.asInstanceOf[String], className := "App-logo", alt := "logo"),
        h1(className := "App-title")("Welcome to CoinSplasher")
      ),
      div(className := "market")(MarketData(repo = marketRepo)),
      div(className := "target")()
    )
  }
}
