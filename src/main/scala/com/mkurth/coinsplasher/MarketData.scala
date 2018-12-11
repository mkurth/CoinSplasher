package com.mkurth.coinsplasher

import com.mkurth.coinsplasher.domain.ShareCalculator
import com.mkurth.coinsplasher.domain.Types.CoinSymbol
import com.mkurth.coinsplasher.domain.model.Share
import com.mkurth.coinsplasher.domain.repo.{MarketCoin, MarketRepo}
import org.scalajs.dom.Element
import org.scalajs.dom.raw.{Event, HTMLInputElement}
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.{React, ReactElement, ReactRef}
import slinky.web.html._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@JSImport("resources/MarketData.css", JSImport.Default)
@js.native
object MarketDataCSS extends js.Object

@react class MarketData extends Component {

  val limitCoinRef: ReactRef[Element] = React.createRef[Element]
  val maxShareRef: ReactRef[Element] = React.createRef[Element]
  val blacklistedCoinsRef: ReactRef[Element] = React.createRef[Element]
  val marketDataRef: ReactRef[Element] = React.createRef[Element]

  case class Props(repo: MarketRepo)

  case class State(coins: Seq[MarketCoin],
                   blacklistedCoins: Seq[CoinSymbol] = Seq(),
                   limit: Int = 20,
                   maxShareInPercent: Int = 20)

  props.repo.loadMarketData(state.blacklistedCoins, 150).map(market => setState(s = state.copy(coins = market)))

  override def initialState: State = State(Seq())

  override def render(): ReactElement = {
    div(className := "market-data")(h1("Set up target Portfolio"),
      div(className := "slidecontainer")(
        div("Take n coins"),
        input(
          `type` := "range",
          ref := limitCoinRef,
          min := "2", max := "100", value := state.limit.toString,
          className := "slider",
          onChange := updateLimit()
        ),
        input(
          `type` := "number",
          min := "2", max := "100", value := state.limit.toString,
          readOnly := true,
          onChange := updateLimit()
        ),
        div("limit to max % per share"),
        input(
          `type` := "range",
          ref := maxShareRef,
          min := (100 / state.limit).toString, max := "100", value := state.maxShareInPercent.toString,
          className := "slider",
          onChange := updateShare()
        ),
        input(
          `type` := "number",
          min := (100 / state.limit).toString, max := "100", value := state.maxShareInPercent.toString,
          readOnly := true,
          onChange := updateShare()
        )
      ),
      div(className := "blacklistedCoins")(
        div("blacklist coins"),
        input(id := "blacklisted", onChange := updateBlacklistedCoins(), ref := blacklistedCoinsRef),
        div(state.blacklistedCoins.mkString(","))
      ),
      div(ref := marketDataRef)(
        PieChartWithLegend(getShares)
      )
    )
  }

  private def getShares: Seq[Share] = {
    ShareCalculator.shares(_.marketCap)(
      state.coins.filterNot(c => state.blacklistedCoins.contains(c.coin.coinSymbol))
        .take(state.limit)
        .map(_.coin), BigDecimal(state.maxShareInPercent) / 100
    )
  }

  private def refreshCoins(): Event => Unit = {
    _ => props.repo.loadMarketData(state.blacklistedCoins, 200).map(x => setState(s = state.copy(coins = x)))
  }

  private def updateLimit(): Event => Unit = {
    _ => {
      setState(_.copy(limit = limitCoinRef.current.asInstanceOf[HTMLInputElement].value.toInt))
    }
  }

  private def updateShare(): Event => Unit = {
    _ => {
      setState(_.copy(maxShareInPercent = maxShareRef.current.asInstanceOf[HTMLInputElement].value.toInt))
    }
  }

  private def updateBlacklistedCoins(): Event => Unit = {
    _ => {
      val coinToBlacklist = blacklistedCoinsRef.current.asInstanceOf[HTMLInputElement].value.split("[, .;\t\n]").map(_.toUpperCase)
      setState(_.copy(blacklistedCoins = coinToBlacklist))
    }
  }
}
