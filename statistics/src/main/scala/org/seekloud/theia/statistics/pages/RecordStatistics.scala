package org.seekloud.theia.statistics.pages

import java.util.Date

import org.scalajs.dom
import org.scalajs.dom.html.{Div, Input, Video}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.UndefOr
import org.seekloud.theia.protocol.ptcl.client2Manager.http.StatisticsProtocol.{LoginInDataByDayReq, LoginInDataByHourReq, LoginInDataInfo, LoginInDataRsp, WatchDataByDayReq, WatchDataByHourInfo, WatchDataByHourReq, WatchDataByHourRsp, WatchProfileDataByRecordIdReq, WatchProfileDataByRecordIdRsp, WatchProfileInfo}
import org.seekloud.theia.statistics.common.{Page, Routes}
import org.seekloud.theia.statistics.utils.{EChart, Http, TimeTool}

import scala.xml.Elem
import io.circe.generic.auto._
import io.circe.syntax._
import mhtml.{Rx, Var}
import org.seekloud.theia.protocol.ptcl.CommonInfo.RecordInfo
import org.seekloud.theia.protocol.ptcl.client2Manager.http.CommonProtocol.{GetRecordListRsp, SearchRecord, SearchRecordRsp}
import org.seekloud.theia.statistics.components.PopWindow

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem

/**
  * User: 13
  * Date: 2019/10/11
  * Time: 15:07
  */
class RecordStatistics(recordId: Long) extends Page{

  val watchProfileInfo = Var(WatchProfileInfo(0,0,0,0))
  val mp4Url = Var("")
  var trendTitle = "12天"

  def init(): Unit ={
    dom.window.scrollTo(0, 0)
    obtainVideoStat()
    obtainVideoStatByDay()
  }

  def obtainVideoStat(): Unit ={
    val watchObserveUrl = Routes.AdminRoutes.watchObserve
    Http.postJsonAndParse[WatchProfileDataByRecordIdRsp](watchObserveUrl, WatchProfileDataByRecordIdReq(recordId).asJson.noSpaces).map{
      case Right(rsp) =>
        if(rsp.errCode==0 && rsp.data.isDefined){
          mp4Url := rsp.url
          rsp.data.foreach(a => watchProfileInfo := a)
          dom.document.getElementById("recordVideo").asInstanceOf[Video].load()
        }
        else if(rsp.errCode==0 && rsp.data.isEmpty){
          PopWindow.commonPop(s"视频(id $recordId)没有数据")
        }
        else{
          println(s"error happen:${rsp.msg}")
        }
      case Left(e) =>
        println(s"error happen:$e")
    }
  }

  def obtainVideoStatByDay(): Unit ={
    val watchObserveUrl = Routes.AdminRoutes.watchObserveByDay
    val startTime = new Date().getTime - 12 * 24 * 60 * 60 * 1000l
    val endTime = new Date().getTime
    Http.postJsonAndParse[WatchDataByHourRsp](watchObserveUrl, WatchDataByDayReq(recordId, startTime, endTime).asJson.noSpaces).map {
      case Right(rsp) =>
        if (rsp.errCode == 0) {
          trendTitle = "12天"
          addChart1(rsp.data, isDay = true)
        } else {
          println(s"error happen:${rsp.msg}")
        }
      case Left(e) =>
        println(s"error happen:$e")
    }
  }

  def obtainVideoStatByHour(startTime: Long): Unit ={
    val watchObserveUrl = Routes.AdminRoutes.watchObserveByHour
    val endTime = startTime + 24 * 60 * 60 * 1000l
    Http.postJsonAndParse[WatchDataByHourRsp](watchObserveUrl, WatchDataByHourReq(recordId, startTime, endTime).asJson.noSpaces).map {
      case Right(rsp) =>
        if (rsp.errCode == 0) {
          trendTitle = TimeTool.dateFormatDefault(startTime).take(10)
          addChart1(rsp.data, isDay = false)
        } else {
          println(s"error happen:${rsp.msg}")
        }
      case Left(e) =>
        println(s"error happen:$e")
    }
  }

  def addChart1(stat: List[WatchDataByHourInfo], isDay: Boolean): Unit ={
    val eChart = EChart.ECharts
    val newChart = eChart.init(dom.document.getElementById("video-trend-chart").asInstanceOf[Div], "light")
    val myChart = newChart.get

    val tooltip = new EChart.Tooltip()
    val xAxis = new EChart.XAxis(stat.map{t =>
      if(isDay){
        TimeTool.dateFormatDefault(t.timestamp).take(10)
      }
      else{
        TimeTool.dateFormatDefault(t.timestamp).takeRight(8)
      }
    }.toJSArray)
    val yAxis = new EChart.YAxis()

    val title = new EChart.Title(s"观看统计: $trendTitle")
    val grid = new EChart.Grid("8%")
    val legend = new EChart.Legend(Array("用户访问次数", "游客访问次数", "用户访问人数").toJSArray)
    val series = Array(
      new EChart.Series(s"用户访问次数","line",false,stat.map(_.pv4SU).toJSArray),
      new EChart.Series(s"游客访问次数","line",false,stat.map(_.pv4TU).toJSArray),
      new EChart.Series(s"用户访问人数","line",false,stat.map(_.uv4SU).toJSArray)
    ).toJSArray
    val option = new EChart.Option(title, grid, tooltip, legend, xAxis, yAxis, series)
    myChart.clear()
    myChart.setOption(option)
    myChart.on("click", param => {
      if(param.componentType == "series"){
        obtainVideoStatByHour(TimeTool.parseDate_yyyy_mm_dd(param.name).getTime.toLong)
      }
    })
  }

  def videoStat(info: WatchProfileInfo): Elem =
    <div class="video-list">
      <div class="video-stats">
        <div class="video-stat">
          <div class="stat-dat">{info.pv4SU}</div>
          <div class="stat-title">用户观看次数</div>
        </div>
        <div class="video-stat">
          <div class="stat-dat">{info.pv4TU}</div>
          <div class="stat-title">游客观看次数</div>
        </div>
        <div class="video-stat">
          <div class="stat-dat">{info.uv4SU}</div>
          <div class="stat-title">用户观看人数</div>
        </div>
        <div class="video-stat">
          <div class="stat-dat">{info.watchTime / 60} min {info.watchTime % 60} s</div>
          <div class="stat-title">用户观看时长</div>
        </div>
      </div>
    </div>

  val videoElem: Elem =
    <div class="video-list">
      <div class="video-container">
        <video id="recordVideo" controls="controls" style="width: 100%;object-fit: contain;background-color: rgb(0, 0, 0);vertical-align: middle;" onended={()=> println("end") }>
          <source src={mp4Url} type="video/mp4" ></source>
        </video>
      </div>
    </div>

  val videoTrend: Elem =
    <div class="video-list">
      <div id="video-trend-chart" style="width: 900px; height: 350px;"></div>
    </div>

  override def render: Elem = {
    dom.window.setTimeout(()=>init(), 0)
    <div class="video-lists">
      {watchProfileInfo.map{videoStat}}
      {videoElem}
      {videoTrend}
    </div>
  }

}
