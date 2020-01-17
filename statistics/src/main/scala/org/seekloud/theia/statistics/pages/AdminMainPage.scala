package org.seekloud.theia.statistics.pages

import java.util.Date

import org.scalajs.dom
import org.scalajs.dom.html.{Div, Input}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.UndefOr
import org.seekloud.theia.protocol.ptcl.client2Manager.http.StatisticsProtocol.{AdminRecordInfo, AdminRecordInfoReq, LoginInDataByDayReq, LoginInDataByHourReq, LoginInDataInfo, LoginInDataRsp, getRecordDataByAdminRsp}
import org.seekloud.theia.statistics.common.{Page, Routes}
import org.seekloud.theia.statistics.utils.{EChart, Http, TimeTool}

import scala.xml.Elem
import io.circe.generic.auto._
import io.circe.syntax._
import mhtml.{Rx, Var}
import org.scalajs.dom.raw.{Event, HTMLElement}
import org.seekloud.theia.protocol.ptcl.CommonInfo.RecordInfo
import org.seekloud.theia.protocol.ptcl.client2Manager.http.CommonProtocol.GetRecordListRsp
import org.seekloud.theia.statistics.components.SwitchPageMod
import org.seekloud.theia.statistics.utils.layDate._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * User: 13
  * Date: 2019/10/11
  * Time: 10:35
  */
object AdminMainPage extends Page{

  val recordMethod = Var("time")
  val trendTitle = Var("12天")
//  val recordInfos = Var(List.empty[AdminRecordInfo])
  val loginInDataInfos = Var(List.empty[LoginInDataInfo])
  var switchPageMod = new SwitchPageMod[AdminRecordInfo](obtainRecord("time"))

  def init(): Unit ={
    switchPageMod.initPageInfo()
    laydate.render(
      new Options {
        override val elem: UndefOr[String] = "#people-date"
        override val `type`: UndefOr[String] = "date"
        override val range: UndefOr[Any] = true
        override val value: UndefOr[String] = TimeTool.dateFormatDefault(new Date().getTime - 12 * 24 * 60 * 60 * 1000l, "yyyy-MM-dd") + " - " + TimeTool.dateFormatDefault(new Date().getTime, "yyyy-MM-dd")
        override def done(): UndefOr[js.Function0[Any]] = dom.window.setTimeout({ ()=>obtainStat1()}, 500).asInstanceOf[js.Function0[Any]]
      }
    )
    obtainStat0()
    obtainStat1()
  }

  //录像信息获取
  def obtainRecord(method: String)(page: Int, pageSize: Int): Future[List[AdminRecordInfo]] ={
    recordMethod := method
    val recordListUrl = Routes.AdminRoutes.getRecordDataByAdmin
    val req = AdminRecordInfoReq(method,page,pageSize)
    Http.postJsonAndParse[getRecordDataByAdminRsp](recordListUrl, req.asJson.noSpaces).map{
      case Right(rsp) =>
        if(rsp.errCode == 0){
          rsp.data
        }else{
          println(s"errors happen rsp: ${rsp.msg}")
          Nil
        }
      case Left(e) =>
        println(s"errors happen: $e")
        Nil
    }
  }

  //全局索引图表信息获取
  def obtainStat0(): Unit ={
    Http.getAndParse[LoginInDataRsp](Routes.AdminRoutes.getLoginData).map{
      case Right(rsp) =>
        if(rsp.errCode==0){
          loginInDataInfos := rsp.data
        }else{
          println(s"error happen:${rsp.msg}")
        }
      case Left(e) =>
        println(s"error happen:$e")
    }
  }

  //时间索引图表信息获取
  def obtainStat1(): Unit ={
    val startTime = dom.document.getElementById("people-date").asInstanceOf[Input].value.take(10)
    val endTime = dom.document.getElementById("people-date").asInstanceOf[Input].value.takeRight(10)
    val data = LoginInDataByDayReq(TimeTool.parseDate_yyyy_mm_dd(startTime).getTime.toLong, TimeTool.parseDate_yyyy_mm_dd(endTime).getTime.toLong).asJson.noSpaces
    Http.postJsonAndParse[LoginInDataRsp](Routes.AdminRoutes.loginDataByDay,data).map{
      case Right(rsp) =>
        if(rsp.errCode==0){
          trendTitle := "12天"
          addChart1(rsp.data, isDay = true)
        }else{
          println(s"error happen:${rsp.msg}")
        }
      case Left(e) =>
        println(s"error happen:$e")
    }
  }

  //时间索引图表信息获取(小时)
  def obtainStat1ByHour(startTime: Long): Unit ={
    val endTime = startTime + 24 * 60 * 60 * 1000l
    val data = LoginInDataByDayReq(startTime, endTime).asJson.noSpaces
    Http.postJsonAndParse[LoginInDataRsp](Routes.AdminRoutes.loginDataByHour,data).map{
      case Right(rsp) =>
        if(rsp.errCode==0){
          dom.document.getElementById("people-date").asInstanceOf[Input].value = TimeTool.dateFormatDefault(startTime, "yyyy-MM-dd")
          addChart1(rsp.data, isDay = false)
        }else{
          println(s"error happen:${rsp.msg}")
        }
      case Left(e) =>
        println(s"error happen:$e")
    }
  }

  def addChart1(stat: List[LoginInDataInfo], isDay: Boolean): Unit ={
    val eChart = EChart.ECharts
    val newChart = eChart.init(dom.document.getElementById("stat-trend-chart").asInstanceOf[Div], "light")
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

    val title = new EChart.Title("")
    val grid = new EChart.Grid("3%")
    val legend = new EChart.Legend(Array("访问次数","访问人数").toJSArray)
    val series = Array(
      new EChart.Series("访问次数","line",false, stat.map(_.pv).toJSArray),
      new EChart.Series("访问人数","line",false, stat.map(_.uv).toJSArray)
    ).toJSArray
    val option = new EChart.Option(title = title, grid = grid, tooltip = tooltip, legend = legend, xAxis = xAxis, yAxis = yAxis, series = series)
    myChart.clear()
    myChart.setOption(option)
    myChart.on("click", param => {
      if(param.componentType == "series"){
        obtainStat1ByHour(TimeTool.parseDate_yyyy_mm_dd(param.name).getTime.toLong)
      }
    })
  }

  val statAllElem: Rx[Elem] = loginInDataInfos.map{ infos =>
    <div class="stat-list">
      <div class="stat-list-title">数据总览</div>
      <div class="stat-all-card">
        <div class="card-content">
          <div class="card-tit-1">日登录人数</div>
          <div class="card-dat-1">{infos.find(_.dayNum == 1).map(_.uv)}</div>
          <div class="card-tit-2">日登录次数
            <span class="card-dat-2">{infos.find(_.dayNum == 1).map(_.pv)}</span>
          </div>
        </div>
        <div class="card-content">
          <div class="card-tit-1">周登录人数</div>
          <div class="card-dat-1">{infos.find(_.dayNum == 7).map(_.uv)}</div>
          <div class="card-tit-2">周登录次数
            <span class="card-dat-2">{infos.find(_.dayNum == 7).map(_.pv)}</span>
          </div>
        </div>
        <div class="card-content">
          <div class="card-tit-1">月登录人数</div>
          <div class="card-dat-1">{infos.find(_.dayNum == 30).map(_.uv)}</div>
          <div class="card-tit-2">月登录次数
            <span class="card-dat-2">{infos.find(_.dayNum == 30).map(_.pv)}</span>
          </div>
        </div>
      </div>
    </div>
  }

  val statTrend: Elem =
    <div class="stat-list">
      <div class="stat-list-title">登录趋势
          <div class="stat-list-select">
          <input type="text" id="people-date"></input>
        </div>
      </div>
      <div id="stat-trend-chart" style="width: 1050px; height: 350px;"></div>
    </div>

  val statRecord: Rx[Elem] = switchPageMod.getIndexData.map{ infos =>

    def methodStyle(thisMethod: String) ={
      recordMethod.map{ method =>
        if(method.equals(thisMethod)){
          "active"
        }
        else{
          "non-active"
        }
      }
    }

    def recordInfoCard(info: AdminRecordInfo): Elem ={
      <div class="record-card">
        <img class="record-img" src={info.coverImg}></img>
        <div class="record-info-col">
          <div class="record-title">
            <div class="anchor">{info.userName}</div>
            <div class="anchor">id:{info.recordId}</div>
            <div class="title">{info.recordName}</div>
          </div>
          <div class="record-time">{TimeTool.dateFormatDefault(info.startTime)}</div>
          <div class="record-stat">
            <div class="stat">赞：{info.likeNum}</div>
            <div class="stat">观看：{info.observeNum}</div>
            <div class="stat">用户观看次数：{info.pv4SU}</div>
            <div class="stat">用户观看人数：{info.uv4SU}</div>
            <div class="stat">游客观看次数：{info.pv4TU}</div>
          </div>
        </div>
        <div class="record-buttons">
          <div class="record-input" onclick={()=>dom.window.location.hash = s"#/RecordStat/${info.recordId}"}>详细数据</div>
        </div>
      </div>
    }

    <div class="stat-list">
      <div class="stat-list-title">录像数据
        <div class="stat-list-select">
          <div name="stat-list-radio" id="radio-view" class={methodStyle("view")} onclick={()=>
            switchPageMod.f = obtainRecord("view")
            switchPageMod.initPageInfo()
          }>浏览数目</div>
          <div name="stat-list-radio" id="radio-time" class={methodStyle("time")} onclick={()=>
            switchPageMod.f = obtainRecord("time")
            switchPageMod.initPageInfo()
          }>发布时间</div>
        </div>
      </div>
      {if(infos.nonEmpty){
        <div class="record-lists">
          {infos.map{ info =>recordInfoCard(info)}}
        </div>
      }else{
        <div class="wait-text">数据缓冲···</div>
      }}
      {switchPageMod.pageDiv}
    </div>
  }

  override def render: Elem ={
    dom.window.setTimeout(()=>init(), 0)
    <div class="stat-all-top">
      <div class="stat-all">
        {statAllElem}
        {statTrend}
        {statRecord}
      </div>
    </div>
  }

}
