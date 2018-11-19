package com.mkurth.coinsplasher

import com.mkurth.coinsplasher.domain.ShareCalculator
import com.mkurth.coinsplasher.domain.Types.CoinSymbol
import com.mkurth.coinsplasher.domain.model.Share
import com.mkurth.coinsplasher.domain.repo.{MarketCoin, MarketRepo}
import org.scalajs.dom.raw.{Event, HTMLInputElement}
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._
import slinky.web.svg.{fill, height, rect, svg, width}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.math.BigDecimal.RoundingMode
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@JSImport("resources/MarketData.css", JSImport.Default)
@js.native
object MarketDataCSS extends js.Object

@react class MarketData extends Component {

  private val css = MarketDataCSS

  case class Props(repo: MarketRepo)

  case class State(coins: Seq[MarketCoin],
                   blacklistedCoins: Seq[CoinSymbol] = Seq(),
                   limit: Int = 20,
                   maxShareInPercent: Int = 20)

  props.repo.loadMarketData(state.blacklistedCoins, state.limit).map(market => setState(s = state.copy(coins = market)))

  override def initialState: State = State(Seq())

  override def render(): ReactElement = {
    div(h1("Set up target Portfolio"),
      div(className := "slidecontainer")(
        div("Take n coins"),
        input(
          `type` := "range",
          min := "1", max := "100", value := state.limit.toString,
          id := "limitCoins", className := "slider",
          name := "limitCoins",
          onChange := updateLimit()
        ),
        div("limit to max % per share"),
        input(
          `type` := "range",
          min := (100 / state.limit).toString, max := "100", value := state.maxShareInPercent.toString,
          id := "maxShare", className := "slider",
          name := "maxShare",
          onChange := updateShare()
        )
      ),
      div(className := "blacklistedCoins")(
        div("blacklist coins"),
        div( state.blacklistedCoins.mkString(",")),
        input(id := "blacklisted", onKeyUp := updateBlacklistedCoins())
      ),
      ul(id := "market-legend", className := "market-legend")(
        getSharesWithIndex.map({ case (share, idx) =>
          li(key := share.coin.coinSymbol, className := "legend-item")(
            svg(width := "20", height := "20")(
              rect(width := "20", height := "20", fill := MarketData.colors(idx))
            ),
            " " + share.coin.coinSymbol,
            s" ${(share.share * 100).setScale(2, RoundingMode.HALF_DOWN)}%"
          )
        })
      ),
      div(className := "market-pie-chart")(
        PieChart(slices = getSharesWithIndex.map({ case (share, idx) =>
          Slice(share.coin.coinSymbol, MarketData.colors(idx), share.share)
        }))
      )
    )
  }

  private def getSharesWithIndex: Seq[(Share, Int)] = {
    ShareCalculator.shares(_.marketCap)(state.coins.map(_.coin), BigDecimal(state.maxShareInPercent) / 100).zipWithIndex
  }

  private def refreshCoins(): Event => Unit = {
    _ => props.repo.loadMarketData(state.blacklistedCoins, state.limit).map(x => setState(s = state.copy(coins = x)))
  }

  private def updateLimit(): Event => Unit = {
    event => {
      setState(state.copy(limit = event.target.asInstanceOf[HTMLInputElement].value.toInt))
      refreshCoins()(event)
    }
  }

  private def updateShare(): Event => Unit = {
    event => {
      setState(state.copy(maxShareInPercent = event.target.asInstanceOf[HTMLInputElement].value.toInt))
    }
  }

  private def updateBlacklistedCoins(): Event => Unit = {
    event => {
      val coinToBlacklist = event.target.asInstanceOf[HTMLInputElement].value.split(",").map(_.toUpperCase)
      setState(state.copy(blacklistedCoins = coinToBlacklist))
      refreshCoins()(event)
    }
  }
}

object MarketData {
  val colors = Seq(
    "Aquamarine",
    "Turquoise",
    "MediumTurquoise",
    "DarkTurquoise",
    "CadetBlue",
    "SteelBlue",
    "LightSteelBlue",
    "PowderBlue",
    "LightBlue",
    "SkyBlue",
    "LightSkyBlue",
    "DeepSkyBlue",
    "DodgerBlue",
    "CornflowerBlue",
    "RoyalBlue",
    "Blue",
    "MediumBlue",
    "DarkBlue",
    "Navy",
    "MidnightBlue",
    "Gold",
    "Yellow",
    "LightYellow",
    "LemonChiffon",
    "LightGoldenrodYellow",
    "PapayaWhip",
    "Moccasin",
    "PeachPuff",
    "PaleGoldenrod",
    "Khaki",
    "DarkKhaki",
    "BurlyWood",
    "GreenYellow",
    "Chartreuse",
    "LawnGreen",
    "Lime",
    "LimeGreen",
    "PaleGreen",
    "LightGreen",
    "MediumSpringGreen",
    "SpringGreen",
    "MediumSeaGreen",
    "SeaGreen",
    "ForestGreen",
    "Green",
    "DarkGreen",
    "YellowGreen",
    "OliveDrab",
    "Olive",
    "DarkOliveGreen",
    "MediumAquamarine",
    "DarkSeaGreen",
    "LightSeaGreen",
    "DarkCyan",
    "Teal",
    "Blues/Cyans",
    "Aqua",
    "Cyan",
    "LightCyan",
    "PaleTurquoise",
    "Tan",
    "RosyBrown",
    "SandyBrown",
    "Goldenrod",
    "DarkGoldenrod",
    "Peru",
    "Chocolate",
    "SaddleBrown",
    "Sienna",
    "Brown")
}
