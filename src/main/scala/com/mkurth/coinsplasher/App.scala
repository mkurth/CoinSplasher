package com.mkurth.coinsplasher

import com.mkurth.coinsplasher.domain.repo._
import com.mkurth.coinsplasher.domain.{CoinService, CoinServiceConfig}
import org.scalajs.dom.raw.HTMLInputElement
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
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
  private val css = AppCSS
  implicit val ec: ExecutionContext = ExecutionContext.global
  type Props = Unit
  case class State(key: String, secret: String, tradeRepo: TradeRepo)

  override def initialState: State = State("", "", new BinanceTradeRepo("", "")(ExecutionContext.global))

  val marketRepo: MarketRepo = new CoinMarketCap
  def tradeRepo: TradeRepo = new BinanceTradeRepo(state.key, state.secret)
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
      div("API Key"),
      input(
        `type` := "text",
        name := "apiKey",
        id:= "apiKey",
        value := state.key,
        onChange := (event => setState(
          state.copy(key = event.target.asInstanceOf[HTMLInputElement].value, tradeRepo = tradeRepo)
        ))
      ),
      div("API Secret"),
      input(
        `type` := "text",
        name := "apiSecret",
        id:= "apiSecret",
        value := state.secret,
        onChange := (event => setState(
          state.copy(secret = event.target.asInstanceOf[HTMLInputElement].value, tradeRepo = tradeRepo)
        ))
      ),
      div(className := "market")(MarketData(repo = marketRepo)),
      div(className := "target")(CurrentBalance(repo = state.tradeRepo))
    )
  }
}
