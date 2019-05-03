
package com.mkurth.coinsplasher

import slinky.core.StatelessComponent
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.svg._

import scala.annotation.tailrec

case class Slice(title: String, color: String, value: BigDecimal)

@react class PieChart extends StatelessComponent {

  case class Props(slices: List[Slice])

  def renderPaths(slices: List[Slice]): ReactElement = {
    val total: BigDecimal = slices.map(_.value).sum
    slices.toList match {
      case head :: Nil => circle(
        r := 1,
        cx := 0,
        cy := 0,
        fill := head.color,
        key := "0")
      case Nil => circle(
        r := 1,
        cx := 0,
        cy := 0,
        fill := "#FEFEFE",
        key := "0")
      case actualList =>
        drawSlice(total, actualList)()
    }
  }

  def getCoordinatesForPercent(percent: BigDecimal): (BigDecimal, BigDecimal) = {
    val x = Math.cos((2 * Math.PI * percent).doubleValue())
    val y = Math.sin((2 * Math.PI * percent).doubleValue())
    BigDecimal(x) -> BigDecimal(y)
  }

  def getPathData(startX: BigDecimal, startY: BigDecimal, largeArcFlag: BigDecimal, endX: BigDecimal, endY: BigDecimal): String = {
    s"""
       |M $startX $startY
       |A 1 1 0 $largeArcFlag 1 $endX $endY
       |L 0 0
     """.stripMargin
  }

  @tailrec
  final def drawSlice(total: BigDecimal, slices: List[Slice], paths: List[ReactElement] = List())
               (cumulativePercent: BigDecimal = 0): List[ReactElement] = {
    slices match {
      case Nil => paths
      case head :: tail =>
        val valuePercentage = head.value / total
          val longArc = if (valuePercentage <= 0.5) 0 else 1
          val (startX, startY) = getCoordinatesForPercent(cumulativePercent)
          val (endX, endY) = getCoordinatesForPercent(cumulativePercent + valuePercentage)
          val pathData = getPathData(startX, startY, longArc, endX, endY)
          val p: ReactElement = path(d := pathData, fill := head.color, id := head.title, key := head.title)
        drawSlice(total, tail, paths.::(p))(cumulativePercent + valuePercentage)
    }
  }

  override def render(): ReactElement = {
    svg(viewBox := s"-1 -1 2 2")(
      g(transform := "rotate(-0.25turn)")(
        renderPaths(props.slices)
      )
    )
  }
}