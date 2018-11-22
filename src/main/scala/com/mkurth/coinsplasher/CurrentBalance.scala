package com.mkurth.coinsplasher

import com.mkurth.coinsplasher.domain.model.CoinBalance
import com.mkurth.coinsplasher.domain.repo.TradeRepo
import slinky.core.Component
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._

import scala.concurrent.ExecutionContext.Implicits.global

@react class CurrentBalance extends Component {

  case class Props(repo: TradeRepo)

  case class State(currentBalance: Seq[CoinBalance], ignoreCoins: Seq[String])

  override def initialState: State = {
    props.repo.currentBalance(Seq()).foreach(balance => setState(state.copy(currentBalance = balance)))
    State(Seq(), Seq())
  }

  override def render(): ReactElement = {
    div(h1("Your current Balance"),
      div(className := "slidecontainer")(
        div(className := "market-pie-chart")(
          if(state.currentBalance.nonEmpty){
            PieChart(slices = state.currentBalance.zipWithIndex.map({ case (balance, idx) =>
              Slice(balance.coinSymbol, MarketData.colors(idx), balance.amount)
            }))
          } else {
            div("please provide api credentials")
          }
        )
      )
    )
  }

}
