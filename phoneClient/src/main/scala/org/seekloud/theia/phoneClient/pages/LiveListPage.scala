package org.seekloud.theia.phoneClient.pages

import org.seekloud.theia.protocol.ptcl.client2Manager.http.CommonProtocol._
import org.seekloud.theia.phoneClient.common.Routes
import org.seekloud.theia.phoneClient.util.{Http, JsFunc, TimeTool}

import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.syntax._
import io.circe.generic.auto._
import mhtml._
import org.scalajs.dom
import org.scalajs.dom.raw.{Event, HTMLElement}
import org.seekloud.theia.protocol.ptcl.CommonInfo._
import org.seekloud.theia.phoneClient.Main
import org.seekloud.theia.phoneClient.common.Components.PopWindow

import scala.xml.{Elem, Node}

class LiveListPage {
  private val isGetRecord = Var(false)
  private val roomInUse = Var(List.empty[RoomInfo])
  private val pageSize = 5
  private var page = 0

  val endNode = Var(emptyHTML)

  private val roomLists: Rx[Elem] = roomInUse.map{roomLists=>
    if(roomLists.isEmpty){
      <div>
        {isGetRecord.map{
        case true =>
          <div class="roomEmpty">
            <img src="/theia/roomManager/static/img/header_3.jpeg" class="roomEmpty-Img"></img>
            <div class="roomEmpty-Text">暂时还没有直播(°Д°≡°Д°)</div>
          </div>
        case false =>
          emptyHTML
      }}
      </div>
    }
    else{

      def check(liveRoom: RoomInfo) = {
        <div onclick={()=>watchLive(liveRoom)} class="video-item">
          <div class="video-img">
            <img class="video-cover" src={liveRoom.coverImgUrl}></img>
            <div class="video-b-info">
              <div class="video-watch">{liveRoom.observerNum}<span style="padding-left: .03rem">观看</span></div>
              <div class="video-like">{liveRoom.like}<span style="padding-left: .03rem">点赞</span></div>
            </div>
          </div>
          <div class="video-info">
            <img class="video-uploader" src={liveRoom.headImgUrl}></img>
            <div class="video-text">
              <div class="video-title">{liveRoom.roomName}</div>
              <div class="video-sec-info">
                <div class="video-user-name">{liveRoom.userName}</div>
                <div class="video-time"></div>
              </div>
            </div>
          </div>
        </div>
      }

      <div class="video-lists">
        {roomLists.map{ recordInfo =>check(recordInfo)}}
        {endNode}
      </div>
    }

  }

  def temUserLogin(roomId: Long): Unit ={
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
            //更新用户刷新页面
            dom.window.location.hash = s"#/Live/$roomId"
            dom.window.location.reload()
          }
        }
        else{
          PopWindow.commonPop(s"error in temUserLogin: ${rsp.msg}")
        }
      case Left(e) =>
        PopWindow.commonPop(s"left error in temUserLogin: $e")
    }
  }

  def watchLive(room:RoomInfo): Unit = {
    dom.window.localStorage.setItem("roomId",room.roomId.toString)
    dom.window.localStorage.setItem("coverImgUrl",room.coverImgUrl)
    dom.window.localStorage.setItem("headImgUrl",room.headImgUrl)
    dom.window.localStorage.setItem("roomName",room.roomName)
    if(dom.window.localStorage.getItem("userName") == null){
      //如果没有登录，就获取临时用户信息
      temUserLogin(room.roomId)
    }else{
      //若已经登录，就直接跳转进入观众页
      dom.window.location.hash = s"#/Live/${room.roomId}"
    }
  }

  def getRoomList(num: Int):Unit = {
    Http.getAndParse[RoomListRsp](Routes.UserRoutes.getRoomList).map{
      case Right(rsp) =>
        if(rsp.errCode == 0){
          if(rsp.roomList.isDefined){
            if(page * num >= rsp.roomList.get.length){
              println("没有更多了")
              endNode := <div class="list-end">没有更多了</div>
            }
            else{
              roomInUse.update(list => list ::: rsp.roomList.get.slice(page * num, page * num + num))
              page += 1
            }
          }
        }else{
          println("RoomListRsp error")
        }
      case Left(e) =>
        println(s"RoomListRsp error: $e")
    }.foreach(_ => isGetRecord := true)
  }

  def init(): Unit = {
    getRoomList(pageSize)
  }

  def loadByScroll(e: Event): Unit ={
    val top = e.target.asInstanceOf[HTMLElement].scrollTop
    val windowHeight = e.target.asInstanceOf[HTMLElement].clientHeight
    val divHeight = e.target.asInstanceOf[HTMLElement].scrollHeight
    if(top + windowHeight >= divHeight - 500 && top + windowHeight <= divHeight + 1) {
      getRoomList(pageSize)
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
    <div style="flex-grow: 2; overflow: scroll; background:white" onscroll={(e: Event)=>putOffEvent(e, loadByScroll)}>
      {roomLists}
    </div>
  }

}
