package com.mkurth.coinsplasher

import com.mkurth.coinsplasher.domain.NormalisedCoinSymbols
import com.mkurth.coinsplasher.domain.Types.{CoinShare, CoinSymbol}
import com.mkurth.coinsplasher.domain.model.{Coin, CoinBalance, Share}
import com.mkurth.coinsplasher.domain.repo.{MarketCoin, MarketRepo, TradeRepo}
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
  case class Props(tradeRepo: TradeRepo, marketRepo: ReactRef[TargetShare], updateCallback: List[Balance] => Unit)

  case class State(currentBalance: List[Balance], ignoreCoins: List[String], error: Option[String]) {
    def asShare: List[Share] = {
      val sum = currentBalance.map(b => b.value).sum
      currentBalance.map(balance => Share(Coin(balance.coin, 0, 0), balance.value / sum))
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
    State(List(), List(), None)
  }

  private def updateBalance(): Unit = {
    props.tradeRepo.currentBalance(List()).foreach(b => {
      setState(_.copy(currentBalance = CurrentBalance.toBalance(props.marketRepo.current.state.marketCoins, b), error = None))
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

object CurrentBalance {

  val normalisedCoinSymbols: Map[String, String] = Map(
    "BCH" -> "BCC",
    "MIOTA" -> "IOTA"
  )

  def toBalance(marketCoins: List[MarketCoin], b: List[CoinBalance]): List[Balance] = {
    val marketCoinMap: Map[String, BigDecimal] = marketCoins.map(mc => mc.copy(coin = mc.coin.copy(coinSymbol = normalisedCoinSymbols.getOrElse(mc.coin.coinSymbol, mc.coin.coinSymbol)))).map(mc => mc.coin.coinSymbol -> mc.price).toMap
    b.filter(_.amount > BigDecimal(0))
      .map(cb => Balance(cb.coinSymbol, cb.amount, cb.amount * marketCoinMap.getOrElse(cb.coinSymbol, 0)))
  }
}
