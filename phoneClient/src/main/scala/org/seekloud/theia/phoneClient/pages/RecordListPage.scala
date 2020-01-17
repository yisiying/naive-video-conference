package org.seekloud.theia.phoneClient.pages

import org.seekloud.theia.phoneClient.common.Routes
import org.seekloud.theia.phoneClient.util.{Http, TimeTool}
import org.seekloud.theia.protocol.ptcl.client2Manager.http.CommonProtocol.{GetRecordListRsp, GetTemporaryUserRsp}

import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.syntax._
import io.circe.generic.auto._
import mhtml.{Rx, Var, emptyHTML}
import org.scalajs.dom
import org.scalajs.dom.raw.{Event, HTMLElement}
import org.seekloud.theia.phoneClient.common.Components.PopWindow
import org.seekloud.theia.protocol.ptcl.CommonInfo.{RecordInfo, RoomInfo}

import scala.xml.Node
/**
  * create by zhaoyin
  * 2019/9/20  5:13 PM
  */
class RecordListPage {
  private val isGetRecord = Var(false)
  private val roomInUse = Var(List.empty[RecordInfo])
  private val pageSize = 5
  private var page = 1

  val endNode = Var(emptyHTML)

  private val roomLists = roomInUse.map{roomLists=>
    if(roomLists.isEmpty){
      <div>
        {isGetRecord.map{
        case true =>
          <div class="roomEmpty">
            <img src="/theia/roomManager/static/img/header_3.jpeg" class="roomEmpty-Img"></img>
            <div class="roomEmpty-Text">暂时还没有录像(°Д°≡°Д°)</div>
          </div>
        case false =>
          emptyHTML
      }}
      </div>
    }
    else{

      def check(record: RecordInfo) = {
        <div onclick={()=>watchRecord(record)} class="video-item">
          <div class="video-img">
            <img class="video-cover" src={record.coverImg}></img>
            <div class="video-b-info">
              <div class="video-long">{if(record.duration.equals("")) "00:00:00" else record.duration.take(8)}</div>
              <div class="video-watch">{record.observeNum}<span style="padding-left: .03rem">观看</span></div>
              <div class="video-like">{record.likeNum}<span style="padding-left: .03rem">点赞</span></div>
            </div>
          </div>
          <div class="video-info">
            <img class="video-uploader" src={record.headImg}></img>
            <div class="video-text">
              <div class="video-title">{record.recordName}</div>
              <div class="video-sec-info">
                <div class="video-user-name">{record.userName}</div>
                <div class="video-time">{TimeTool.parseDateLikeBiliBili(record.startTime)}</div>
              </div>
            </div>
          </div>
        </div>
      }

      <div class="video-lists" id="video-lists">
        {roomLists.map{ recordInfo =>check(recordInfo)}}
        {endNode}
      </div>
    }

  }


  def temUserLogin(): Unit ={
    Http.getAndParse[GetTemporaryUserRsp](Routes.UserRoutes.temporaryUser).map{
      case Right(rsp) =>
        if(rsp.errCode == 0){
          rsp.userInfoOpt.foreach{ userNewInfo =>
            dom.window.localStorage.setItem("userName", userNewInfo.userName)
            dom.window.localStorage.setItem("userHeaderImgUrl", userNewInfo.headImgUrl)
            dom.window.localStorage.setItem("userId", userNewInfo.userId.toString)
            dom.window.localStorage.setItem("token", userNewInfo.token.toString)
            //更改mainPage里的无用户为临时用户
            dom.window.localStorage.setItem("isTemUser", "1")
          }
        }
        else{
          PopWindow.commonPop(s"error in temUserLogin: ${rsp.msg}")
        }
      case Left(e) =>
        PopWindow.commonPop(s"left error in temUserLogin: $e")
    }
  }

  def watchRecord(record:RecordInfo): Unit = {
    dom.window.localStorage.setItem("recordName", record.recordName)
    dom.window.localStorage.setItem("recordCoverImg", record.coverImg)
    dom.window.localStorage.setItem("recordStartTime", record.startTime.toString)
    if(dom.window.localStorage.getItem("userName") == null){
      //如果没有登录，就获取临时用户信息
      temUserLogin()
    }
    dom.window.location.hash = s"#/Record/${record.roomId}/${record.startTime}"
  }

  def getRecordList(sortBy:String,pageNum:Int,pageSize:Int):Unit = {
    val recordListUrl = Routes.UserRoutes.getRecordList(sortBy,pageNum,pageSize)
    Http.getAndParse[GetRecordListRsp](recordListUrl).map{
      case Right(rsp) =>
        if(rsp.errCode == 0){
          if(rsp.recordInfo.isEmpty){
            println("没有更多了")
            endNode := <div class="list-end">没有更多了</div>
          }
          else{
            roomInUse.update(list => list ::: rsp.recordInfo)
            page += 1
          }
        }
        else{
          println(s"get record errors: ${rsp.msg}")
        }
      case Left(e) =>
        println(s"errors happen: $e")
    }.foreach(_ => isGetRecord := true)
  }

  def init(): Unit = {
    getRecordList("time", page, pageSize)
  }

  def loadByScroll(e: Event): Unit ={
    val top = e.target.asInstanceOf[HTMLElement].scrollTop
    val windowHeight = e.target.asInstanceOf[HTMLElement].clientHeight
    val divHeight = e.target.asInstanceOf[HTMLElement].scrollHeight
//    println(s"top:$top + windowHeight:$windowHeight = divHeight$divHeight")
    if(top + windowHeight >= divHeight - 500 && top + windowHeight <= divHeight + 1) {
      getRecordList("time", page, pageSize)
    }
    isRunning = false
  }

  //限制onscroll事件短时间内只发送一次
  var isRunning = false
  def putOffEvent(e: Event, method: _ => Unit): Unit ={
    if(! isRunning){
      dom.window.setTimeout(()=>loadByScroll(e: Event), 200)
      isRunning = true
    }
  }

  def app:xml.Node = {
    init()
    <div style="flex-grow: 2; overflow: scroll;
    background: white;
    -webkit-overflow-scrolling: touch;
    height: 100%;" onscroll={(e: Event)=>putOffEvent(e, loadByScroll)}>
      {roomLists}
    </div>
  }

}
