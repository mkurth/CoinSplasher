package com.mkurth.coinsplasher

import com.mkurth.coinsplasher.domain.model.{CoinBalance, Share}
import com.mkurth.coinsplasher.domain.repo._
import org.scalajs.dom.raw.{Event, HTMLInputElement}
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.{React, ReactElement, ReactRef}
import slinky.web.html._

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@JSImport("resources/App.css", JSImport.Default)
@js.native
object AppCSS extends js.Object

@JSImport("resources/logo.svg", JSImport.Default)
@js.native
object ReactLogo extends js.Object

@react class App extends Component {
  implicit val ec: ExecutionContext = ExecutionContext.global
  type Props = Unit

  case class State(key: String, secret: String,
                   targetShare: Seq[Share] = Seq(),
                   currentBalance: Seq[Balance] = Seq(),
                   marketCoins: Seq[MarketCoin] = Seq()
                  )

  override def initialState: State = State("", "")

  val marketRepo: MarketRepo = new CoinMarketCap
  val marketDataRef: ReactRef[TargetShare] = React.createRef[TargetShare]
  val currentBalanceRef: ReactRef[CurrentBalance] = React.createRef[CurrentBalance]
  val tradeRepo = new BinanceTradeRepo(() => state.key, () => state.secret)(ExecutionContext.global)

  private val css = AppCSS

  def updatedTargetShares(targetShares: Seq[Share]): Unit = {
    setState(state.copy(targetShare = targetShares))
  }

  def updatedCurrentBalance(balance: Seq[Balance]): Unit = {
    setState(state.copy(currentBalance = balance))
  }

  def updatedMarketData(marketCoins: Seq[MarketCoin]): Unit = {
    setState(state.copy(marketCoins = marketCoins))
  }

  def render(): ReactElement = {
    div(className := "App")(
      header(className := "App-header")(
        h1(className := "App-title")("Welcome to CoinSplasher")
      ),
      div(className := "API-Keys")(
        div("API Key"),
        input(
          `type` := "text",
          className := "api-input",
          value := state.key,
          onChange := updateKey
        ),
        div("API Secret"),
        input(
          `type` := "text",
          className := "api-input",
          value := state.secret,
          onChange := updateSecret
        )
      ),
      div(className := "market")(TargetShare(repo = marketRepo, updatedMarketCoins = updatedMarketData, updatedShare = updatedTargetShares).withRef(marketDataRef)),
      div(className := "target")(CurrentBalance(tradeRepo, marketDataRef, updateCallback = updatedCurrentBalance).withRef(currentBalanceRef)),
      div(className := "trade")(
        TradeExecutor(tradeRepo, state.targetShare, state.currentBalance, marketDataRef)
      )
    )
  }

  private def updateSecret: Event => Unit = {
    event =>
      val secret = event.target.asInstanceOf[HTMLInputElement].value
      setState(_.copy(secret = secret)
    )
  }

  private def updateKey: Event => Unit = {
    event =>
      val key = event.target.asInstanceOf[HTMLInputElement].value
      setState(_.copy(key = key)
    )
  }
}
