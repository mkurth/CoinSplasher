package com.mkurth.coinsplasher

import com.mkurth.coinsplasher.domain._
import com.mkurth.coinsplasher.order.OrderComponent
import com.mkurth.coinsplasher.domain.model.{CoinBalance, Share}
import com.mkurth.coinsplasher.domain.repo.TradeRepo
import slinky.core.Component
import slinky.core.annotations.react
import slinky.core.facade.{ReactElement, ReactRef}
import slinky.web.html.{className, div, h1}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.Try

@JSImport("resources/TradeExecutor.css", JSImport.Default)
@js.native
object TradeExecutorCSS extends js.Object

@react
class TradeExecutor extends Component {

  private val css = TradeExecutorCSS

  case class Props(tradeRepo: TradeRepo,
                   targetShare: Seq[Share] = Seq(),
                   currentBalance: Seq[Balance] = Seq(),
                   marketRef: ReactRef[TargetShare])

  override def render(): ReactElement = {
    div(
      h1("Suggested Trades"),
      div(className := "planned-trades")(
        TradeSolver.solveTrades(
          props.currentBalance.map(b => CoinBalance(b.coin, b.amount)),
          props.targetShare,
          Try(props.marketRef.current.state.marketCoins).getOrElse(Seq())
        ).filter(_.worth > 1).sortBy({
          case SellOrder(_, _, worth) => -1 * worth
          case BuyOrder(_, _, worth) => worth
        }).map(order => OrderComponent(order))
      ))
  }

  type State = Unit

  def initialState: State = {}
}
