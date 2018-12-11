package com.mkurth.coinsplasher

import com.mkurth.coinsplasher.domain.model.Share
import slinky.core.StatelessComponent
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web
import slinky.web.html._
import slinky.web.svg.{fill, svg}

import scala.math.BigDecimal.RoundingMode

@react class PieChartWithLegend extends StatelessComponent {
  case class Props(sharesWithIndex: Seq[(Share, Int)])
  type Snapshot = Unit

  override def render(): ReactElement = {
    div(
      ul(id := "market-legend", className := "market-legend")(
        props.sharesWithIndex.map({ case (share, idx) =>
          li(key := share.coin.coinSymbol, className := "legend-item")(
            svg(web.svg.width := "20", web.svg.height := "20")(
              rect(web.svg.width := "20", web.svg.height := "20", fill := MarketData.colors(idx))
            ),
            " " + share.coin.coinSymbol,
            s" ${(share.share * 100).setScale(2, RoundingMode.HALF_DOWN)}%"
          )
        })
      ),
      div(className := "market-pie-chart")(
        PieChart(slices = props.sharesWithIndex.map({ case (share, idx) =>
          Slice(share.coin.coinSymbol, MarketData.colors(idx), share.share)
        }))
      )
    )
  }
}
