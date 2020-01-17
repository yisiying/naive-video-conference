package org.seekloud.theia.webClient.pages

import io.circe.syntax._
import io.circe.generic.auto._
import mhtml._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.ExecutionContext.Implicits.global
import org.seekloud.theia.protocol.ptcl.client2Manager.http.CommonProtocol._
import org.seekloud.theia.protocol.ptcl.CommonInfo.{RecordInfo, RoomInfo}
import org.seekloud.theia.webClient.common.Components.PopWindow
import org.seekloud.theia.webClient.common.{Page, Routes}
import org.seekloud.theia.webClient.util.{Globals, Http, JsFunc, TimeTool}
import org.seekloud.theia.webClient.pages.MainPage.recordInfo
import org.seekloud.theia.webClient.pages.MainPage.{recordId, recordTime}

import scala.xml.Elem

class HomePage extends Page{
  private val recordList = Var(List.empty[RecordInfo])
  private val recordNumber = Var(0)
  private var recordPageNum = 0
  private val showPages = 5
  private val perPageSize = 10 //使用分页插件，一页10个
  private val liveList = Var(List.empty[RoomInfo])
  private val liveNumber = Var(0)
  private var goToPage = 0
  //观看录像

  def isTemUser(): Boolean ={
    dom.window.localStorage.getItem("isTemUser") != null
  }


  def watchRecord(record:RecordInfo) = {
    recordInfo = record
    dom.window.sessionStorage.setItem("recordName", record.recordName)
    dom.window.sessionStorage.setItem("recordCoverImg", record.coverImg)
    dom.window.sessionStorage.setItem("recordStartTime", record.startTime.toString)
    if(isTemUser()){
      //如果没有登录，就获取临时用户信息
      MainPage.temUserLogin(-1l)
    }
    //若已经登录，就直接跳转进入观众页
    dom.window.location.hash = s"#/Record/${recordInfo.roomId}/${recordInfo.startTime}"
  }

  private val recordArea:Rx[Elem] = recordList.map{ rl =>

    def createRecordItem(item: RecordInfo)={

      <div class="recordItem" onclick={()=>watchRecord(item)}>
        <div class="recordVideo">
          <img class="record-img" src={item.coverImg}></img>
          <div class="s-info">
            <div class="record-user">{item.userName}</div>
            <img class="img-dianzan" src="/theia/roomManager/static/img/homePage/like.png"></img>
            <div class="record-like">{item.likeNum}</div>
          </div>
        </div>
        <div class="recordDesc">
          <div class="r-title">{item.recordName}</div>
          <div class="r-u-info">
            <div class="r-id"></div>
            <div class="r-time">{TimeTool.parseDateLikeBiliBili(item.startTime)}</div>
          </div>
        </div>
      </div>
    }

    <div class="record" style="margin-top: 40px;">
      <div class="zone-title">
        <div class="record-head-img">
          <img class="img-record" src="/theia/roomManager/static/img/homePage/record.png"></img>
        </div>
        <div class="record-head-text">录像</div>
        <div class="record-head-number">当前共有<span>{recordNumber}</span>个录像</div>
        <div class="record-head-refresh">
          <img class="img-refresh" src="/theia/roomManager/static/img/homePage/refresh.png" style="float: right;" onclick={()=>getRecordList("time",1,10)}></img>
        </div>
      </div>
      <div class="recordItem-list">
        {rl.map(createRecordItem)}
      </div>
    </div>

  }
  def goToNextPage() = {
    val ele = dom.document.getElementById("bp-4-element").asInstanceOf[HTMLElement]
    for(i <- 0 until  ele.childElementCount){
      if(ele.childNodes.item(i).asInstanceOf[HTMLElement].className.contains("active")){
        goToPage = ele.childNodes.item(i).childNodes.item(0).asInstanceOf[HTMLElement].textContent.toInt
      }
    }
    getRecordList("time",goToPage,perPageSize)
  }
  //观看直播
  def watchLive(room:RoomInfo):Unit = {
    //跳转进入audience页面
    dom.window.sessionStorage.setItem("roomName",room.roomName)
    if(dom.window.localStorage.getItem("userName") == null){
      dom.window.sessionStorage.setItem("roomId",room.roomId.toString)
      dom.window.sessionStorage.setItem("coverImgUrl",room.coverImgUrl)
      dom.window.sessionStorage.setItem("headImgUrl",room.headImgUrl)
      //如果没有登录，就获取临时用户信息
      //TODO 这里同下面一样
      MainPage.temUserLogin(room.roomId)
    }else{
      //若已经登录，就直接跳转进入观众页
      //TODO 不再直接跳转进入观众页，而是先判断里面有没有流
      //TODO 如果房间没有流，就刷新房间列表
      val data = SearchRoomReq(Some(dom.window.localStorage.getItem("userId").toLong),room.roomId).asJson.noSpaces
      Http.postJsonAndParse[SearchRoomRsp](Routes.UserRoutes.searchRoom,data).map{
        case Right(rsp) =>
          if(rsp.errCode==0 && rsp.roomInfo.isDefined){
            dom.window.localStorage.setItem("roomId",room.roomId.toString)
            dom.window.localStorage.setItem("coverImgUrl",room.coverImgUrl)
            dom.window.localStorage.setItem("headImgUrl",room.headImgUrl)
            dom.window.location.hash = s"#/Live/${room.roomId}"
          }else{
            if(rsp.errCode == 100006) {
              PopWindow.commonPop("服务器请求失败，请稍后再试")
            }else if(rsp.errCode == 100005){
              PopWindow.commonPop("服务器请求失败，请稍后再试")
            }else if(rsp.errCode == 100008){
              PopWindow.commonPop("主播已关闭房间")
            }else if(rsp.errCode == 100009){
              PopWindow.commonPop("主播已关闭直播")
            }else{
              PopWindow.commonPop(s"获取视频内容失败: ${rsp.msg}")
            }
            getRoomList()
          }
        case Left(e) =>
          println("SearchRoomRsp error")
      }
    }
  }

  private val liveArea:Rx[Elem] = liveList.map{list =>

    def createLiveItem(item: RoomInfo)={
      <div class="recordItem" onclick={()=>watchLive(item)}>
        <div class="recordVideo">
          <img class="record-img" src={item.coverImgUrl}></img>
          <div class="s-info">
            <div class="record-user">{item.userName}</div>
            <img class="img-dianzan" src="/theia/roomManager/static/img/homePage/like.png"></img>
            <div class="record-like">{item.like}</div>
          </div>
        </div>
        <div class="recordDesc">
          <div class="r-title">{item.roomName}</div>
        </div>
      </div>
    }

    <div class="record">
      <div class="zone-title">
        <div class="record-head-img">
          <img class="img-record" src="/theia/roomManager/static/img/正在直播.gif"></img>
        </div>
        <div class="record-head-text">直播</div>
        <div class="record-head-number">当前共有<span>{liveNumber}</span>个直播</div>
        <div class="record-head-refresh">
          <img class="img-refresh" src="/theia/roomManager/static/img/homePage/refresh.png" style="float: right;" onclick={()=>getRoomList()}></img>
        </div>
      </div>
      <div class="recordItem-list">
        {list.map(createLiveItem)}
      </div>
    </div>
  }

  def getRoomList():Unit = {
    Http.getAndParse[RoomListRsp](Routes.UserRoutes.getRoomList).map{
      case Right(rsp) =>
        if(rsp.errCode == 0){
          if(rsp.roomList.isDefined){
            liveList := rsp.roomList.get
            liveNumber := rsp.roomList.get.length
          }
        }else{
          println("RoomListRsp error")
        }
      case Left(e) =>
        println(s"RoomListRsp error: $e")
    }
  }

  def getRecordList(sortBy:String,pageNum:Int,pageSize:Int):Unit={
    val recordListUrl = Routes.UserRoutes.getRecordList(sortBy,pageNum,pageSize)
    Http.getAndParse[GetRecordListRsp](recordListUrl).map{
      case Right(rsp) =>
        if(rsp.errCode == 0){
          recordList := rsp.recordInfo
          recordNumber := rsp.recordNum
          recordPageNum = if(rsp.recordNum % perPageSize != 0) (rsp.recordNum / perPageSize) +1 else rsp.recordNum / perPageSize
          Globals.pagePaginator("bp-4-element",pageNum,showPages,recordPageNum)
        }
      case Left(e) =>
        println(s"errors happen: $e")
    }
  }

  override def render: Elem = {
    //获取所有直播
    getRoomList()
    //获取第一页的录像
    getRecordList("time",1,perPageSize)
    <div>
      {liveArea}
      {recordArea}
      <div id="record-pageContainer">
        <ul id="bp-4-element" onclick={()=>goToNextPage()}></ul>
      </div>
    </div>
  }

}
