package org.seekloud.theia.webClient.pages

import java.util.Date
import java.util.concurrent.TimeUnit

import mhtml._
import io.circe.syntax._
import io.circe.generic.auto._

import scala.xml.{Elem, Node}
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalajs.dom
import org.scalajs.dom.html._
import org.scalajs.dom.raw.{Event, HTMLElement}
import org.seekloud.theia.protocol.ptcl.CommonInfo.{ClientType, RecordInfo, RoomInfo, UserDes}
import org.seekloud.theia.protocol.ptcl.CommonRsp
import org.seekloud.theia.webClient.common.{Page, Routes}
import org.seekloud.theia.webClient.util._
import org.seekloud.theia.protocol.ptcl.client2Manager.http.CommonProtocol._
import org.seekloud.theia.protocol.ptcl.client2Manager.http.RecordCommentProtocol.{AddRecordCommentReq, CommentInfo, GetRecordCommentListReq, GetRecordCommentListRsp}
import org.seekloud.theia.protocol.ptcl.client2Manager.http.StatisticsProtocol.WatchRecordEndReq
import org.seekloud.theia.protocol.ptcl.client2Manager.websocket.AuthProtocol._
import org.seekloud.theia.webClient.actors.WebSocketRoom
import org.seekloud.theia.webClient.common.Components.InteractiveText.Gift
import org.seekloud.theia.webClient.common.Components.{InteractiveText, PopWindow}
import org.seekloud.theia.webClient.common.UserMsg._
import org.seekloud.theia.webClient.common.Routes._
import org.seekloud.theia.webrtcMessage.ptcl.BrowserJson
import org.seekloud.theia.webClient.util.RtmpStreamerJs._
import org.seekloud.theia.webClient.pages.MainPage.recordInfo

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.Future
/**
  * create by zhaoyin
  * 2019/9/17  9:35 PM
  */
class RecordPage(roomId:Long,time:Long) extends Page{

  case class WatchRecordEnd(
    recordId:Long = -1l,//录像id
    inTime:Long = -1l,//用户开始观看录像的时间
  )

  def isTemUser(): Boolean ={
    dom.window.localStorage.getItem("isTemUser") != null
  }

  private var watchRecordEndInfo = WatchRecordEnd()
  private val roomCoverImg = Var(dom.window.sessionStorage.getItem("recordCoverImg"))
  private val videoTime = Var(dom.window.sessionStorage.getItem("recordStartTime"))
  private val videoName = Var(dom.window.sessionStorage.getItem("recordName"))
  val commentInfo = Var(List.empty[CommentInfo])

  def exitRecord(): Future[Unit] ={
    val userId =
      if(isTemUser()) dom.window.sessionStorage.getItem("userId")
      else dom.window.localStorage.getItem("userId")
    val exitUserId = (dom.window.localStorage.getItem("isTemUser"), userId) match{
      case (null, null) => None
      case (null, b) => Some(b.toLong)
      case _ => None
    }
    val data = WatchRecordEndReq(watchRecordEndInfo.recordId, exitUserId, watchRecordEndInfo.inTime, new Date().getTime).asJson.noSpaces
    Http.postJsonAndParse[CommonRsp](Routes.UserRoutes.watchRecordOver,data).map{
      case Right(rsp) =>
        if(rsp.errCode == 0){
          println("退出录像信息发送成功")
        }
        else{
          println(s"退出录像信息发送失败 ${rsp.msg}")
        }
      case Left(e) =>
        println(s"退出录像信息发送失败 $e")
    }
  }

  def sendComment():Unit = {
    val b_area = dom.document.getElementById("ipt-txt").asInstanceOf[TextArea]
    val currentTime = System.currentTimeMillis()
    if(b_area.value.length != 0  && dom.window.localStorage.getItem("isTemUser") == null){
      val userId =
        if(isTemUser()) dom.window.sessionStorage.getItem("userId").toLong
        else dom.window.localStorage.getItem("userId").toLong
      val data = AddRecordCommentReq(roomId,time,b_area.value,currentTime,1l,userId).asJson.noSpaces
      Http.postJsonAndParse[CommonRsp](Routes.UserRoutes.sendCommentInfo,data).map{
        case Right(rsp)=>
          if(rsp.errCode == 0){
            val (userId, userName, userHeaderImgUrl) =
              if(isTemUser()){
                (dom.window.sessionStorage.getItem("userId").toLong,
                  dom.window.sessionStorage.getItem("userName"),
                  dom.window.sessionStorage.getItem("userHeaderImgUrl"))
              }else{
                (dom.window.localStorage.getItem("userId").toLong,
                  dom.window.localStorage.getItem("userName"),
                  dom.window.localStorage.getItem("userHeaderImgUrl"))
              }
            commentInfo.update(c=>c:+CommentInfo(-1,
              roomId,time,b_area.value,currentTime,-1,userId,
              userName,userHeaderImgUrl))
            b_area.value = ""
          }else if(rsp.errCode == 100012){
            PopWindow.commonPop("您已经被封号，无法评论")
          }
        case Left(e) =>
          println("error happen: "+ e)
      }
    }
  }
  def getCommentInfo():Unit={
    val data = GetRecordCommentListReq(roomId,time).asJson.noSpaces
    Http.postJsonAndParse[GetRecordCommentListRsp](Routes.UserRoutes.getCommentInfo,data).map{
      case Right(rsp) =>
        if(rsp.errCode == 0){
          commentInfo := rsp.recordCommentList
          println(123)
        }else{
          commentInfo := List.empty[CommentInfo]
        }
      case Left(e) =>
        println("error happen: " + e)
    }
  }

  private var mp4Url = Var("")
  //https://www.runoob.com/try/demo_source/mov_bbb.mp4
  def watchRecord():Unit = {
    val userId =
      if(isTemUser()) dom.window.sessionStorage.getItem("userId")
      else dom.window.localStorage.getItem("userId")
    val userOption = {
      (dom.window.localStorage.getItem("isTemUser"), userId) match {
        case (null, null) => None
        case (null, b) => Some(b.toLong)
        case _ => None
      }
    }
    val newData = new Date().getTime
    val data = SearchRecord(roomId,time,newData,userOption).asJson.noSpaces
    Http.postJsonAndParse[SearchRecordRsp](Routes.UserRoutes.getOneRecord,data).map{
      case Right(rsp) =>
        if(rsp.errCode==0){
          //获得了url
          mp4Url := rsp.url

          dom.window.sessionStorage.setItem("recordName", rsp.recordInfo.recordName)
          dom.window.sessionStorage.setItem("recordCoverImg", rsp.recordInfo.coverImg)
          dom.window.sessionStorage.setItem("recordStartTime", rsp.recordInfo.startTime.toString)

          watchRecordEndInfo = WatchRecordEnd(rsp.recordInfo.recordId, newData)
          roomCoverImg := rsp.recordInfo.coverImg
          videoTime := rsp.recordInfo.startTime.toString
          videoName := rsp.recordInfo.recordName
          val v = dom.document.getElementById("recordVideo").asInstanceOf[Video]
          v.load()
          v.play()
        }
        else{
          PopWindow.commonPop(s"get url error in watchRecord: ${rsp.msg}")
        }
      case Left(e) =>
        PopWindow.commonPop(s"get url error in watchRecord: $e")
    }
  }

  def videoPlayback(): Unit ={
    val menu = dom.document.getElementById("playback-menu")
    val video = dom.document.getElementById("recordVideo").asInstanceOf[Video]
    menu.setAttribute("class", "playback-menu-close")
    if(video != null){
      video.load()
      video.play()
    }
  }

  def videoOnEnded(): Unit ={
    val menu = dom.document.getElementById("playback-menu")
     menu.setAttribute("class", "playback-menu-open")
  }
  def closeComment()={
    dom.document.getElementById("comment-submit").setAttribute("class","n-comment-submit comment-submit")
    dom.document.getElementById("comment-submit").asInstanceOf[Button].disabled = true
    dom.document.getElementById("ipt-txt").asInstanceOf[TextArea].disabled = true
    headImg := "/theia/roomManager/static/img/头像.png"
  }
  def openComment()={
    dom.document.getElementById("comment-submit").setAttribute("class","comment-submit")
    dom.document.getElementById("comment-submit").asInstanceOf[Button].disabled = false
    dom.document.getElementById("ipt-txt").asInstanceOf[TextArea].disabled = false
    headImg := dom.window.sessionStorage.getItem("userHeaderImgUrl")
  }
  val headImg = Var("/theia/roomManager/static/img/头像.png")
  var s = 0
  def init() = {
    watchRecord()
    s = dom.window.setInterval(()=>{
      if(dom.document.getElementById("comment-submit")!=null){
        if(dom.window.localStorage.getItem("isTemUser") != null){
          closeComment()
        }else{
          openComment()
        }
        dom.window.clearInterval(s)
      }
    },100)
  }
  val comments:Rx[Node] = commentInfo.map{ cf =>
    def createCommentItem(item:CommentInfo) = {
      <div class="rcl-item">
        <div class="user-face">
          <img class="userface" src={item.commentHeadImgUrl}></img>
        </div>
        <div class="rcl-con">
          <div class="rcl-con-name">{item.commentUserName}</div>
          <div class="rcl-con-con">{item.comment}</div>
          <div class="rcl-con-time">{TimeTool.dateFormatDefault(item.commentTime)}</div>
        </div>
      </div>
    }
    <div class="comment-list">
      {cf.map(createCommentItem)}
    </div>
  }


  override def render: Elem = {
    init()
    getCommentInfo()
    <div>
      <div class="audienceInfo" style="margin-left: 250px;margin-top: 20px;width:60%">
        <div class="anchorInfo">
          <div class="showInfo">
            <img id="headImg" src={roomCoverImg} class="showInfo-coverImg"></img>
            <div style="margin-left:20px;color:#222">
              <div class="recordName">{videoName}</div>
              <div class="recordTime" style="color: #808080;font-size: 12px;margin-top: 10px;">{videoTime.map(i=>TimeTool.dateFormatDefault(i.toLong)) }</div>
            </div>
          </div>
        </div>
        <div style="padding-bottom:20px!important" class="dash-video-player anchor-all" id="dash-video-player">
          <div style="position: relative">
            <video id="recordVideo" controls="controls" style="height:500px;width:100%;object-fit: contain;background-color: #000;" onended={()=>videoOnEnded()}>
            <source src={mp4Url} type="video/mp4" ></source>
          </video>
            <div id="playback-menu" class="playback-menu-close">
              <div class="playback-point">
                <img class="playback-button" src="/theia/roomManager/static/img/homePage/replay.png" onclick={()=>videoPlayback()}></img>
                <div class="playback-text">重新播放</div>
              </div>
            </div>
          </div>
        </div>

        <div class="r-comment" id="r-comment">
          <div class="rc-head">全部评论(
            {commentInfo.map{ ci =>
            if(ci.isEmpty){
              0
            }else{
              ci.length
            }
          }
            }
            )</div>
          <div class="rc-content">
            <div class="comment-send">
              <div class="user-face">
                <img class="userface" src={headImg}></img>
              </div>
              <div class="textarea-container">
                <textarea cols="80" name="msg" rows="5" placeholder="请自觉遵守互联网相关的政策法规，严禁发布色情、暴力、反动的言论。" class="ipt-txt" id="ipt-txt"
                          onkeydown={(e:dom.KeyboardEvent)=> if (e.keyCode==13) sendComment()} ></textarea>
                <div class="rsb-button">
                  <button type="submit" class="comment-submit" id="comment-submit" onclick={()=>sendComment()}>发表评论</button>
                </div>
              </div>
            </div>
            {comments}
          </div>

        </div>

      </div>
    </div>
  }
}
