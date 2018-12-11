package com.mkurth.coinsplasher

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

  case class State(key: String, secret: String, tradeRepo: TradeRepo)

  override def initialState: State = State("", "", new BinanceTradeRepo("", "")(ExecutionContext.global))

  val marketRepo: MarketRepo = new CoinMarketCap
  val marketDataRef: ReactRef[MarketData] = React.createRef[MarketData]

  private val css = AppCSS

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
      div(className := "market")(MarketData(repo = marketRepo).withRef(marketDataRef)),
      div(className := "target")(CurrentBalance(state.tradeRepo, marketDataRef))
    )
  }

  private def updateSecret: Event => Unit = {
    event =>
      val secret = event.target.asInstanceOf[HTMLInputElement].value
      setState(_.copy(secret = secret, tradeRepo = new BinanceTradeRepo(state.key, secret)(ExecutionContext.global))
    )
  }

  private def updateKey: Event => Unit = {
    event =>
      val key = event.target.asInstanceOf[HTMLInputElement].value
      setState(_.copy(key = key, tradeRepo = new BinanceTradeRepo(key, state.secret)(ExecutionContext.global))
    )
  }
}
