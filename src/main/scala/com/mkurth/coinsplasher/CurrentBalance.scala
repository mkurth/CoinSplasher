package com.mkurth.coinsplasher

import com.mkurth.coinsplasher.domain.Types.{CoinShare, CoinSymbol}
import com.mkurth.coinsplasher.domain.model.{Coin, Share}
import com.mkurth.coinsplasher.domain.repo.TradeRepo
import slinky.core.Component
import slinky.core.annotations.react
import slinky.core.facade.{ReactElement, ReactRef}
import slinky.web.html._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@JSImport("resources/CurrentBalance.css", JSImport.Default)
@js.native
object CurrentBalanceCSS extends js.Object

case class Balance(coin: CoinSymbol, amount: CoinShare, value: BigDecimal)

@react class CurrentBalance extends Component {

  private val css = CurrentBalanceCSS
  case class Props(tradeRepo: TradeRepo, marketRepo: ReactRef[TargetShare], updateCallback: Seq[Balance] => Unit)

  case class State(currentBalance: Seq[Balance], ignoreCoins: Seq[String], error: Option[String]) {
    def asShare: Seq[Share] = {
      val sum = currentBalance.map(b => b.value * b.amount).sum
      currentBalance.map(balance => Share(Coin(balance.coin, 0, 0), balance.amount * balance.value / sum))
        .filter(_.share > 0.001)
        .sortBy(_.share * -1)
    }
  }

  override def componentDidUpdate(prevProps: Props, prevState: State): Unit = {
    if(prevState != state) {
      props.updateCallback(state.currentBalance)
    }
    super.componentDidUpdate(prevProps, prevState)
  }

  override def initialState: State = {
    updateBalance()
    State(Seq(), Seq(), None)
  }

  private def updateBalance(): Unit = {
    props.tradeRepo.currentBalance(Seq()).foreach(b => {
      val mc = props.marketRepo.current.state.marketCoins.map(mc => mc.coin.coinSymbol -> mc.price).toMap
      val balances = b.filter(_.amount > 0).map(cb => Balance(cb.coinSymbol, cb.amount, cb.amount * mc.getOrElse(cb.coinSymbol, 0)))
      setState(_.copy(currentBalance = balances, error = None))
    })
  }

  override def render(): ReactElement = {
    div(className := "balance")(h1("Your current Balance"),
      button(onClick := (_ => updateBalance()))("refresh"),
      div(state.error.getOrElse("")),
      div(className := "slidecontainer")(
        div(className := "market-pie-chart")(
          if (state.currentBalance.nonEmpty) {
            PieChartWithLegend(state.asShare)
          } else {
            div("please provide api credentials")
          }
        )
      )
    )
  }

}
