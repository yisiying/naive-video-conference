package org.seekloud.theia.phoneClient.pages

import java.util.Date

import mhtml.{Rx, Var, emptyHTML}
import org.scalajs.dom
import org.scalajs.dom.html.{TextArea, Video}
import org.seekloud.theia.phoneClient.common.Routes
import org.seekloud.theia.phoneClient.util.{Http, TimeTool}
import org.seekloud.theia.protocol.ptcl.client2Manager.http.RecordCommentProtocol.{AddRecordCommentReq, CommentInfo, GetRecordCommentListReq, GetRecordCommentListRsp}
import io.circe.syntax._
import io.circe.generic.auto._
import org.seekloud.theia.phoneClient.common.Components.PopWindow
import org.seekloud.theia.protocol.ptcl.CommonRsp
import org.seekloud.theia.protocol.ptcl.client2Manager.http.CommonProtocol.{SearchRecord, SearchRecordRsp}
import org.seekloud.theia.protocol.ptcl.client2Manager.http.StatisticsProtocol.WatchRecordEndReq

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.{Elem, Node}

/**
  * create by zhaoyin
  * 2019/9/20  5:14 PM
  */
class RecordRoomPage(coverImg: String, roomName: String, time:Long, roomId: Long) {

  case class WatchRecordEnd(
    recordId:Long = -1l,//录像id
    inTime:Long = -1l,//用户开始观看录像的时间
  )
  private var watchRecordEndInfo = WatchRecordEnd()
  val mp4Url =  Var("https://media.seekloud.com:50443/theia/distributor/getRecord/1000040/1569206265328/record.mp4")
  val commentInfo = Var(List.empty[CommentInfo])

  def exitRecord(): Future[Unit] ={
    val exitUserId = (dom.window.localStorage.getItem("isTemUser"), dom.window.localStorage.getItem("userId")) match{
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
   if( dom.window.localStorage.getItem("isTemUser") ==null){
      val b_area = dom.document.getElementById("com-input").asInstanceOf[TextArea]
      val currentTime = System.currentTimeMillis()
      if(b_area.value.length != 0  && dom.window.localStorage.getItem("userId") != null){
        val data = AddRecordCommentReq(roomId,time,b_area.value,currentTime,1,dom.window.localStorage.getItem("userId").toLong,None).asJson.noSpaces
        Http.postJsonAndParse[CommonRsp](Routes.UserRoutes.sendCommentInfo,data).map{
          case Right(rsp)=>
            if(rsp.errCode == 0){
              commentInfo.update(c=>c:+CommentInfo(-1,
                roomId,time,b_area.value,currentTime,-1,dom.window.localStorage.getItem("userId").toLong,
                dom.window.localStorage.getItem("userName"),dom.window.localStorage.getItem("userHeaderImgUrl")))
              b_area.value = ""
            }
          case Left(e) =>
            println("error happen: "+ e)
        }
      }
    }else{
     PopWindow.bottomPop("请登录以使用功能")
    }


  }
  def watchRecord():Unit = {
    val userOption: Option[Long] = {
      (dom.window.localStorage.getItem("isTemUser"), dom.window.localStorage.getItem("userId")) match {
        case (null, null) => None
        case (null, b) => Some(b.toLong)
        case _ => None
      }
    }
    val data = SearchRecord(roomId,time,new Date().getTime,userOption).asJson.noSpaces
    Http.postJsonAndParse[SearchRecordRsp](Routes.UserRoutes.getOneRecord,data).map{
      case Right(rsp) =>
        if(rsp.errCode==0){
          //获得了url
          mp4Url := rsp.url
          watchRecordEndInfo = WatchRecordEnd(rsp.recordInfo.recordId, new Date().getTime)
          println(rsp.url)
          val v = dom.document.getElementById("recordVideo").asInstanceOf[Video]
          v.load()
          v.play()
        }
      case Left(e) =>
        println("error happen: "+ e)
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

  def init() = {
    watchRecord()
  }

  val comments:Rx[Node] = commentInfo.map{ cf =>
    def createCommentItem(item:CommentInfo) = {
      <div class="c-item">
        <div class="c-i-headerImg">
          <img src={item.commentHeadImgUrl}></img>
        </div>
        <div class="c-i-content">
          <div class="ci-name">{item.commentUserName}</div>
          <div class="ci-time">{TimeTool.dateFormatDefault(item.commentTime)}</div>
          <div class="ci-comment">{item.comment}</div>
        </div>
      </div>
    }
    <div class="r-comments">
      {cf.map(createCommentItem)}
    </div>
  }

  val recordInfo:Elem = {
    <div class="recordInfo">
      <div class="record-headImg">
        <img src={coverImg}></img>
      </div>
      <div class="record-otherInfo">
        <div class="r-o-name">{roomName}</div>
        <div class="r-o-time">{TimeTool.dateFormatDefault(time)}</div>
      </div>
    </div>
  }
  def app:xml.Elem = {
    init()
    getCommentInfo()
    <div id="record-roompage">
      <div id="record-area" class="record-area">
        <video id="recordVideo" controls="controls" style="height:2.12rem;width:100%;object-fit: contain;background-color: #000;"
               webkit-playsinline="true" playsinline="true" >
          <source src={mp4Url} type="video/mp4" ></source>
          </video>
      </div>
      {recordInfo}
      <div class="c-tag">全部评论</div>
      {comments}
      <div class="sendComment" id="sendComment">
        <div class="r-inputContainer">
          <textarea id="com-input" class="com-input" maxlength="80"  cols="32" placeholder="说点什么吧"></textarea>
        </div>
        <div style="height: 0.3rem;">
          <button class="csd-btn" onclick={()=>sendComment()}><span>send</span></button>
        </div>
      </div>
    </div>
  }
}
