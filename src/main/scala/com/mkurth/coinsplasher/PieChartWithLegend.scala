package com.mkurth.coinsplasher

import com.mkurth.coinsplasher.domain.Types.CoinSymbol
import com.mkurth.coinsplasher.domain.model.Share
import slinky.core.StatelessComponent
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web
import slinky.web.html._
import slinky.web.svg.{fill, svg}

import scala.math.BigDecimal.RoundingMode

@react class PieChartWithLegend extends StatelessComponent {
  case class Props(shares: Seq[Share])
  type Snapshot = Unit

  import PieChartWithLegend.colors

  private def colorForCoin(coinSymbol: CoinSymbol): String = {
    colors(coinSymbol.hashCode % colors.length)
  }

  override def render(): ReactElement = {
    div(
      ul(id := "market-legend", className := "market-legend")(
        props.shares.map(share =>
          li(key := share.coin.coinSymbol, className := "legend-item")(
            svg(web.svg.width := "20", web.svg.height := "20")(
              web.svg.rect(web.svg.width := "20", web.svg.height := "20", fill := colorForCoin(share.coin.coinSymbol))
            ),
            " " + share.coin.coinSymbol,
            s" ${(share.share * 100).setScale(2, RoundingMode.HALF_DOWN)}%"
          )
        )
      ),
      div(className := "market-pie-chart")(
        PieChart(slices = props.shares.map(share =>
          Slice(share.coin.coinSymbol, colorForCoin(share.coin.coinSymbol), share.share)
        ))
      )
    )
  }
}

object PieChartWithLegend {
  val colors: Seq[String] = Seq(
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
