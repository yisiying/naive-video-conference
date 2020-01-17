package org.seekloud.theia.phoneClient.pages

import mhtml.{Var, emptyHTML}
import org.scalajs.dom.html.{Input, TextArea}
import org.scalajs.dom.raw.HTMLElement
import org.seekloud.theia.phoneClient.actor.WebSocketRoom
import org.seekloud.theia.phoneClient.common.Components.{InteractiveText, PopWindow}
import org.seekloud.theia.phoneClient.common.Components.InteractiveText.Gift
import org.seekloud.theia.phoneClient.util.RtmpStreamerJs._
import org.seekloud.theia.protocol.ptcl.client2Manager.websocket.AuthProtocol._
import org.seekloud.theia.phoneClient.common.Routes._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem
import org.scalajs.dom
import org.seekloud.theia.protocol.ptcl.client2Manager.http.CommonProtocol.{SearchRoomReq, SearchRoomRsp}
import org.seekloud.theia.phoneClient.common.Routes
import org.seekloud.theia.phoneClient.util.{Globals, Http}
import io.circe.syntax._
import io.circe.generic.auto._
import org.seekloud.theia.protocol.ptcl.CommonInfo.UserDes

class LiveRoomPage() {
  val websocketClient = new WebSocketRoom(wsMessageHandler, wsStartHandler)
  private val barragesList = Var(List[(String,String,String)]())
  private val audienceLists = Var(List[UserDes]())
  private val audienceNums = Var(0)
  private val chooseWindow = Var(0) //0 1 2 3
  private var showSend = Var(false)
  private var isLikeLoading = false
  private var isUnlikeLoading = false
  private var roomLike = Var(0)
  private var anchorUserName = Var("")
//  private val myUserId = if (dom.window.localStorage.getItem("userId")!= null)
//    dom.window.localStorage.getItem("userId").toLong else 123
//  private val headImgUrl = if(dom.window.localStorage.getItem("userHeaderImgUrl") !=null)
//    dom.window.localStorage.getItem("userHeaderImgUrl") else "/theia/roomManager/static/img/headPortrait/1.jpg"

  var isIOS = Var(false)
  var hlsUrl = ""
  var dashUrl = ""
  var dashStreamer = new DashType("")
  var hlsStreamer = new HlsType("")
  var userZaned = Var(false)


  def closeRm = {
    if(!Globals.getOS()){
      dashStreamer.reset(dashStreamer)
    }else{
      hlsStreamer.dispose(hlsStreamer)
    }
    websocketClient.closeWs
  }
  def sendMessage()={
    val b_area = dom.document.getElementById("danmu-input").asInstanceOf[TextArea]
    if(dom.window.localStorage.getItem("isTemUser") != null){
      PopWindow.bottomPop("请登录以使用功能")
    }
    else if(b_area.value.length != 0) {
      val barrage = Comment(dom.window.localStorage.getItem("userId").toLong,dom.window.localStorage.getItem("roomId").toLong,b_area.value)
      b_area.value = ""
      websocketClient.sendMsg(barrage)
    }
    showSend.update(i => false)
  }
  def sendGift(gift: Gift)={
    if(dom.window.localStorage.getItem("isTemUser") != null){
      PopWindow.bottomPop("请登录以使用功能")
    }
    else{
      val number = dom.document.getElementById(gift.number + "send").asInstanceOf[Input].value
      val barrage = Comment(dom.window.localStorage.getItem("userId").toLong,dom.window.localStorage.getItem("roomId").toLong,s"用户：${dom.window.localStorage.getItem("userId").toLong}赠送礼物：${gift.name},${number}个")
      websocketClient.sendMsg(barrage)
    }
  }
  private def wsStartHandler(): Unit ={
    if(dom.window.localStorage.getItem("isTemUser") == null) {
      val barrage = JudgeLike(dom.window.localStorage.getItem("userId").toLong, dom.window.localStorage.getItem("roomId").toLong)
      websocketClient.sendMsg(barrage)
    }
  }
  private def wsMessageHandler(data:WsMsgRm):Unit ={
    data match {
      case LikeRoomRsp(errCode, msg) =>
        if(errCode == 0){
          if(isLikeLoading){
            dom.document.getElementById("user-like").setAttribute("class", "op-item-active")
            userZaned := true
            isLikeLoading = false
          }
          else if(isUnlikeLoading){
            dom.document.getElementById("user-like").setAttribute("class", "op-item")
            isUnlikeLoading = false
            userZaned:=false
          }
        }
        else{
          println(s"like error : $msg, $errCode")
          if(errCode == 1001){
            dom.document.getElementById("user-like").setAttribute("class", "op-item-active")
            dom.document.getElementById("user-unlike").setAttribute("class", "op-item")
          }
          if(errCode == 1002){
            dom.document.getElementById("user-like").setAttribute("class", "op-item")
          }
          isLikeLoading = false
          isUnlikeLoading = false
        }

      case JudgeLikeRsp(like, errCode, msg) =>
        if(errCode == 0){
          if(like){
            userZaned := true
          }
          else{
            userZaned := false
          }
        }
        else{
          println(s"error in get like judge $msg")
        }

      case ReFleshRoomInfo(roomInfo) =>
        //仅用于更新点赞数
        roomLike.update(_ => roomInfo.like)

      case RcvComment(userId,userName,comment,color,extension) =>
        val commentArea = dom.document.getElementById("comments").asInstanceOf[HTMLElement]

        if(userId == -1l){
          barragesList.update(b => b:+("[系统消息]",comment,color))
        }else if(userId ==dom.window.localStorage.getItem("userId").toLong){
          barragesList.update(b => b:+("[自己]",comment, if(color.equals("FFFFFF")) "9b39f4" else color))
        } else{
          barragesList.update(b => b:+(userName,comment,if(color.equals("FFFFFF")) "9b39f4" else color))
        }
        commentArea.scrollTop = commentArea.scrollHeight

      case UpdateAudienceInfo(audienceList) =>
        audienceNums := audienceList.length
        audienceLists := audienceList

      case HostCloseRoom() =>
        websocketClient.closeWs
        PopWindow.bottomPop("主播离开房间")
      case PingPackage =>
      case msg@_ =>
    }
  }

  def connectWebsocket() = {
    val wsurl = rmWebScocketUri(dom.window.localStorage.getItem("userId").toLong,
      dom.window.localStorage.getItem("token"),dom.window.localStorage.getItem("roomId").toLong)
    websocketClient.setup(wsurl)
  }


  def userLike(): Unit ={
    if(dom.window.localStorage.getItem("isTemUser") != null){
      PopWindow.bottomPop("请登录以使用功能")
    }
    else{
      val barrage = LikeRoom(dom.window.localStorage.getItem("userId").toLong, dom.window.localStorage.getItem("roomId").toLong, 1)
      websocketClient.sendMsg(barrage)
      isLikeLoading = true
    }
  }
  def userUnlike(): Unit ={
    if(dom.window.localStorage.getItem("isTemUser") != null){
      PopWindow.bottomPop("请登录以使用功能")
    }
    else{
      val barrage = LikeRoom(dom.window.localStorage.getItem("userId").toLong, dom.window.localStorage.getItem("roomId").toLong, 0)
      websocketClient.sendMsg(barrage)
      isUnlikeLoading = true
    }
  }

  val giftInter: Elem ={
    <div class="giftInter">
      <div class="lc-header">
        <div class="lch-title">赠送礼物</div>
        <div class="lch-close" onclick={() => chooseWindow := 0}><span class="iconfont iconguanbi"></span></div>
      </div>
      <div class="giftInter-content">
        {InteractiveText.giftsList.sortBy(_.cost)map{ gift =>
        <div class="gift-content-phone">
          <div class="gift-desc">
            <img class="gift-img" src={gift.img}></img>
            <div class="gift-text">
              <div class="gift-text-title">
                <div class="gift-text-title-name">{gift.name}</div>
                <img class="gift-text-title-coinImg" src="/theia/roomManager/static/img/gifts/coin.png"></img>
                <div class="gift-text-title-coin">{gift.cost}</div>
              </div>
              <div class="gift-text-desc">{gift.desc}</div>
              <div class="gift-text-tip">{gift.tip}</div>
            </div>
          </div>
          <div class="gift-confirm">
            <div class="gift-number">
              <input type="radio" name="gift-numbers" style="display: none;" id={gift.number + "number1"} class="number1" onclick={() => dom.document.getElementById(gift.number + "send").asInstanceOf[Input].value = "1"}></input>
              <label class="gift-number-check" for={gift.number + "number1"}>1</label>
              <input type="radio" name="gift-numbers" style="display: none;" id={gift.number + "number10"} class="number10" onclick={() => dom.document.getElementById(gift.number + "send").asInstanceOf[Input].value = "10"}></input>
              <label class="gift-number-check" for={gift.number + "number10"}>10</label>
              <input type="radio" name="gift-numbers" style="display: none;" id={gift.number + "number100"} class="number100" onclick={() => dom.document.getElementById(gift.number + "send").asInstanceOf[Input].value = "100"}></input>
              <label class="gift-number-check" for={gift.number + "number100"}>100</label>
            </div>
            <input class="gift-number-input" id={gift.number + "send"} type="number" value="1"></input>
            <button class="gift-send" onclick={()=> sendGift(gift)}>发送</button>
          </div>
        </div>
      }}
      </div>
      <div class="giftInter-confrim"></div>
    </div>
  }


  val roomUserInfo: Elem ={
    <div>
      <div class="roomInfo">
        <div class="liveRoom">
          <img src="/theia/roomManager/static/img/正在直播.gif"></img>
          <div class="roomName">{dom.window.localStorage.getItem("roomName")}</div>
        </div>
        <div class="userOperations">
          <img src={dom.window.localStorage.getItem("headImgUrl")} class="owner-header-img"></img>
          <div class="owner-name">{anchorUserName}</div>
          <div class="audienceNum">{audienceNums} 人正在观看</div>
        </div>
      </div>
      <div class="ownerInfo">
        {userZaned.map{i=>
        if(i){
          <div class="op-item" id="user-like" onclick={()=> userUnlike()}>
            <span class="iconfont icondianzan-copy" style="color:#ef7d41"></span>
            <span class="icondown">{roomLike}</span>
          </div>
        }else{
          <div class="op-item" id="user-like" onclick={()=> userLike()}>
            <span class="iconfont icondianzan-copy"></span>
            <span class="icondown">{roomLike}</span>
          </div>
        }
      }}
        <div class="op-item" onclick={() => chooseWindow := 3}>
          <span class="iconfont iconguanzhongguanli"></span>
          <span class="icondown">当前观众</span>
        </div>
        <div class="op-item" onclick={()=> chooseWindow := 2}>
          <span class="iconfont iconliaotian"></span>
          <span class="icondown">实时聊天</span>
        </div>
        <div class="op-item" onclick={()=> chooseWindow := 1}>
          <span class="iconfont iconliwu iconliwuBlue"></span>
          <span class="icondown">礼物</span>
        </div>
      </div>
    </div>
  }

  val liveComment: Elem ={
    <div class="live-comment">
      <div class="lc-header">
        <div class="lch-title">实时聊天</div>
        <div class="lch-close" onclick={() => chooseWindow := 0}><span class="iconfont iconguanbi"></span></div>
      </div>
      <div class="lc-container">
        <ul id="comments">
          {barragesList.map{ b_list =>
          def createBList(item:(String,String,String))={
            val color = "color: #"+item._3
            if(item._1.equals("[系统消息]")){
              <li class=" barrage-item " style="color:red"><span class=" barrage-item-user">{item._1}: </span>
                <span class=" barrage-item-content">{item._2}</span></li>
            }else if(item._1.equals("[自己]")){
              <li class=" barrage-item "><span class=" barrage-item-user">{item._1}: </span>
                <span class=" barrage-item-content" style={color}>{item._2}</span></li>
            }else{
              <li class=" barrage-item "><span class=" barrage-item-user">{item._1}: </span>
                <span class=" barrage-item-content" style={color}>{item._2}</span></li>
            }

          }
        {b_list.map(createBList)}
        }
          }
        </ul>
      </div>
      <div class="sendDanmu">
        <img src={dom.window.localStorage.getItem("userHeaderImgUrl")} class="userImg"></img>
        <div class="inputContainer">
          <textarea id="danmu-input"  class="danmu-input" maxlength="80" autofocus="true" cols="32" placeholder="输入评论参与实时讨论"
                    oninput={()=> showSend.update(i => true)} ></textarea>
        </div>
        {showSend.map{ ss =>
        if(ss){
          <button class="sd-btn" onclick={()=> sendMessage()}><span>send</span></button>
        }else{
          emptyHTML
        }
      }
        }
      </div>
    </div>
  }

  val currentAudience: Elem = {
    <div class="live-comment">
      <div class="lc-header">
        <div class="lch-title">当前观众</div>
        <div class="lch-close" onclick={() => chooseWindow := 0}><span class="iconfont iconguanbi"></span></div>
      </div>
      <div class="lc-container">
        <div>
        {audienceLists.map(a_list =>{
          def createAudience(item:UserDes) = {
            <div style="display:flex;height:.6rem;border-bottom:.01rem solid #DDD;align-items:center">
              <img src={item.headImgUrl} class="audience-img"></img>
              <div style="#333">{item.userName}</div>
            </div>
          }
          {a_list.map(createAudience)}
        }
        )}
        </div>
      </div>
    </div>
  }


  def userJoinRoom() :Unit = {
    val data = SearchRoomReq(Some(dom.window.localStorage.getItem("userId").toLong),
      dom.window.localStorage.getItem("roomId").toLong).asJson.noSpaces
    Http.postJsonAndParse[SearchRoomRsp](Routes.UserRoutes.searchRoom,data).map{
      case Right(rsp) =>
        if(rsp.errCode==0 && rsp.roomInfo.isDefined){
          anchorUserName:= rsp.roomInfo.get.userName
          val url = rsp.roomInfo.get.mpd.get
          roomLike := rsp.roomInfo.get.like
          dashUrl = url
          hlsUrl = url.replace("index","master").replace("mpd","m3u8")
          dashStreamer = new DashType(dashUrl)
          hlsStreamer = new HlsType(hlsUrl)
          if(!Globals.getOS()){
            //安卓手机播放dash
            dashStreamer.initialize(dashStreamer)
          }else{
            //苹果手机播放hls
            hlsStreamer.initialize(hlsStreamer)
            isIOS := true
          }
          connectWebsocket()
        }
      case Left(e) =>
        println("SearchRoomRsp error")
    }
  }

  def enterRoom() = {
    userJoinRoom()
  }

  def init() = {
    enterRoom()
  }

  def app:xml.Node = {
    init()
    <div id="roompage">
      <div id="living-area" class="living-area">
        {isIOS.map{ i =>
        if(i){
          <video id="hls-video" class="dash-video-player-video video-js vjs-default-skin"
                 muted="muted"
                 controls="controls"
                 x5-playsinline="true"
                 playsinline="true"
                 webkit-playsinline="true">
            <source type="application/x-mpegURL" src={hlsUrl} />
          </video>
        }else{
          <video id="videoplayer" class="dash-video-player-video" muted="muted" controls="true"></video>
        }
      }
        }
      </div>
      {chooseWindow.map{
      case 0 =>
        roomUserInfo
      case 1 =>
        giftInter
      case 2 =>
        liveComment
      case 3 =>
        currentAudience
    }}
    </div>
  }

}
