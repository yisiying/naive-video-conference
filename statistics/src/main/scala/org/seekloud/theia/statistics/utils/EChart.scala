package org.seekloud.theia.statistics.utils

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobal, ScalaJSDefined}


/**
  * User: XuSiRan
  * Date: 2019/5/3
  * Time: 23:41
  * 调用EChart的js库
  */
object EChart {

  @ScalaJSDefined
  class Params(
    val componentType: String,
    val seriesType: String,
    val seriesIndex: Int,
    val seriesName: String,
    val name: String
  ) extends js.Object

  @ScalaJSDefined
  class EChartsInstance extends js.Object{
    def setOption(option: Option): js.UndefOr[js.Function0[Any]] = js.undefined
    def on(event: String, func: js.Function1[Params, Any]): js.UndefOr[Any] = js.undefined
    def clear(): js.UndefOr[js.Function0[Any]] = js.undefined
  }

  @js.native
  @JSGlobal("echarts")
  object ECharts extends js.Object{
    def init(elem: dom.html.Div, style: String = "default"): js.UndefOr[EChartsInstance] = js.native
  }

  @ScalaJSDefined
  class Title(
    val text: String = "test"
  ) extends js.Object

  @ScalaJSDefined
  class Grid(
    val left: String = "3%"
  ) extends js.Object

  @js.native
  @JSGlobal("options")
  def option(){}

  @ScalaJSDefined
  class Tooltip(

  ) extends js.Object

  @ScalaJSDefined
  class Legend(
    val data: js.UndefOr[js.Array[String]] = js.undefined
  ) extends js.Object

  @ScalaJSDefined
  class XAxis(
    val data: js.UndefOr[js.Array[String]] = js.undefined
  ) extends js.Object

  @ScalaJSDefined
  class YAxis(

  ) extends js.Object

  @ScalaJSDefined
  class Series(
    val name: js.UndefOr[String] = js.undefined,
    val `type`: js.UndefOr[String] = js.undefined,
    val smooth: js.UndefOr[Boolean] = js.undefined,
    val data: js.UndefOr[js.Array[Int]] = js.undefined,
    val barWidth: js.UndefOr[String] = js.undefined
  ) extends js.Object

  @ScalaJSDefined
  class Option(
    val title: Title,
    val grid: Grid,
    val tooltip: Tooltip,
    val legend: Legend,
    val xAxis: XAxis,
    val yAxis: YAxis,
    val series: js.Array[Series]
  ) extends js.Object

}
