package org.seekloud.theia.webrtcServer.kurento.control

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.ActorRef
import org.kurento.client.{KurentoClient, MediaPipeline, RtpEndpoint, WebRtcEndpoint}
import org.seekloud.theia.webrtcMessage.ptcl.BrowserJson
import org.seekloud.theia.webrtcMessage.ptcl.BrowserJson.EventId
import org.seekloud.theia.webrtcServer.common.AppSettings
import org.seekloud.theia.webrtcServer.kurento.UserSession
import org.seekloud.theia.webrtcServer.ptcl.WebSocketSession
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe._
import io.circe.parser._
import org.seekloud.theia.webrtcServer.Boot.liveManager
import org.seekloud.theia.webrtcServer.core.{LiveActor, LiveManager}
import org.seekloud.theia.webrtcServer.core.LiveManager.PcInit

/**
  * Created by sky
  * Date on 2019/7/1
  * Time at 上午10:17
  * 定义直播和接入连麦流程
  */
object LiveHandler {
  val portId = new AtomicInteger(0)
}

case class LiveHandler(kurento: KurentoClient) extends Handler() {

  import LiveHandler.portId

  protected def handleAnchorSdpOffer(session: WebSocketSession, jsonMessage: HCursor) = {
    log.info(s"Anchor ${session.id} start push")
    val user: UserSession = new UserSession(session)
    userMap.put(session.id, user)
    val pipeline: MediaPipeline = kurento.createMediaPipeline
    user.setMediaPipeline(pipeline)
    val webRtcEp: WebRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build
    user.setWebRtcEndpoint(webRtcEp)
    val rtpEp: RtpEndpoint = new RtpEndpoint.Builder(pipeline).build
    user.setRtpEndpoint(rtpEp)
    // ---- Endpoint configuration
    val rtcSdpOffer: String = jsonMessage.get[String]("sdpOffer").getOrElse("decode error")
    val myPort = portId.getAndIncrement()
    val audioPort = 50000 + myPort * 8
    val videoPort = 50004 + myPort * 8
    val rtpSdpOffer: String = AppSettings.generateRtpSdp(audioPort, videoPort)

    initWebRtcEndpoint(session, webRtcEp, rtcSdpOffer)
    initRtpEndpoint(session, rtpEp, rtpSdpOffer)
    webRtcEp.connect(rtpEp)
    log.info("[Handler::handleStart] New WebRtcEndpoint: {}", webRtcEp.getName)

    // ---- Endpoint startup

    startWebRtcEndpoint(webRtcEp)

    // todo 处理接收
    session.actor ! LiveActor.InitEncode(videoPort, audioPort)

  }

  protected def handleAudienceSdpOffer(session: WebSocketSession, jsonMessage: HCursor) = {
    log.info(s"Audience ${session.id} start push")
    val user: UserSession = new UserSession(session, false)
    userMap.put(session.id, user)
    userMap.find(_._2.isAnchor) match {
      case Some(value) =>
        //主播为web
        val pipeline: MediaPipeline = value._2.getMediaPipeline
        user.setMediaPipeline(pipeline)
        val webRtcEp: WebRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build
        user.setWebRtcEndpoint(webRtcEp)
        val rtpEp: RtpEndpoint = new RtpEndpoint.Builder(pipeline).build
        user.setRtpEndpoint(rtpEp)
        // ---- Endpoint configuration
        val rtcSdpOffer: String = jsonMessage.get[String]("sdpOffer").getOrElse("decode error")
        val myPort = portId.getAndIncrement()
        val audioPort = 50000 + myPort * 8
        val videoPort = 50004 + myPort * 8
        val rtpSdpOffer: String = AppSettings.generateRtpSdp(audioPort, videoPort)
        initWebRtcEndpoint(session, webRtcEp, rtcSdpOffer)
        initRtpEndpoint(session, rtpEp, rtpSdpOffer)
        webRtcEp.connect(rtpEp)

        log.info("[Handler::handleStart] New WebRtcEndpoint: {}", webRtcEp.getName)

        // ---- Endpoint startup

        println("connect--------------------------------------------")
        startWebRtcEndpoint(webRtcEp)

        webRtcEp.connect(value._2.getWebRtcEndpoint)
        value._2.getWebRtcEndpoint.connect(webRtcEp)
        session.actor ! LiveActor.InitEncode(videoPort, audioPort)

      case None =>
        //主播为pc, userMap此时没有anchor
        println("connect pc--------------------------------------------")
        val pipeline: MediaPipeline = kurento.createMediaPipeline
        user.setMediaPipeline(pipeline)
        val webRtcEp: WebRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build
        user.setWebRtcEndpoint(webRtcEp)
        val rtpEp: RtpEndpoint = new RtpEndpoint.Builder(pipeline).build
        user.setRtpEndpoint(rtpEp)
        // ---- Endpoint configuration
        val rtcSdpOffer: String = jsonMessage.get[String]("sdpOffer").getOrElse("decode error")
        val myPort = portId.getAndIncrement()
        val audioPort = 50000 + myPort * 8
        val videoPort = 50004 + myPort * 8
        val rtpSdpOffer: String = AppSettings.generateRtpSdp(audioPort, videoPort)
        initWebRtcEndpoint(session, webRtcEp, rtcSdpOffer)
        initRtpEndpoint(session, rtpEp, rtpSdpOffer)
        webRtcEp.connect(rtpEp)

        log.info("[Handler::handleStart] New WebRtcEndpoint: {}", webRtcEp.getName)

        // ---- Endpoint startup

        startWebRtcEndpoint(webRtcEp)
        session.actor ! LiveActor.InitEncode(videoPort, audioPort)
    }
  }

  override def handlePcStart(liveId:String): Unit = {


  }

  override def handleDisconnect(liveId: String): Unit = {
    if (liveId.contains(AppSettings.webUserPref)) {
      //处理web断开连接
      userMap.get(liveId) match {
        case Some(value) =>
          if (value.isAnchor) {
            //fixme: 连线者为pc时断开处理
            //处理web主播断开
            value.getMediaPipeline.release()
            userMap.foreach(_._2.socketSession.actor ! LiveActor.Stop)
            userMap.clear()
          } else {
            //处理web连线者断开连接
            if (value.getWebRtcEndpoint != null) {
              value.getWebRtcEndpoint.disconnect(value.getRtpEndpoint)
              userMap.find(_._2.isAnchor) match {
                case Some(anchorValue) =>
                  //主播端为web
                  value.getWebRtcEndpoint.disconnect(anchorValue._2.getWebRtcEndpoint)
                  anchorValue._2.getWebRtcEndpoint.disconnect(value.getWebRtcEndpoint)
                case None =>
                //主播端为pc
              }
            }
            value.socketSession.actor ! LiveActor.Stop
            userMap.remove(liveId)
          }
        case None =>
          log.error("userMap doesn't exist liveId: data lose")
      }
    } else {
      //todo :处理pc断开连接
    }
  }

  override def handleJsonCursor(session: WebSocketSession, jsonCursor: HCursor): Unit = {
    jsonCursor.get[String]("id") match {
      case Right(messageId) =>
        log.info(session.id + "--" + messageId)
        messageId match {
          case EventId.PING =>
            session.session ! BrowserJson.ProtocolMsg(BrowserJson.EventId.PONG.asJson.noSpaces)

          case EventId.Anchor_SDP_OFFER =>
            // Start: Create user session and process SDP Offer
            handleAnchorSdpOffer(session, jsonCursor)

          case EventId.Audience_SDP_OFFER =>
            // Start: Create user session and process SDP Offer
            handleAudienceSdpOffer(session, jsonCursor)

          case EventId.ADD_ICE_CANDIDATE =>
            handleAddIceCandidate(session, jsonCursor)

          case EventId.CONNECT =>
          //连线者为pc，此时由web主播端发来CONNECT消息
          //todo: 为pc连线者创建LiveActor
            jsonCursor.get[String]("audience") match {
              case Right(liveId) =>
                if(liveId.contains(AppSettings.webUserPref)){
                  log.error(s"audience in this CONNECT should be pc")
                }else {
                  liveManager ! PcInit(liveId, this)
                }
              case Left(err) =>
                log.error(s"handle connect get audience error:${err.getMessage()}")
            }

          case EventId.DISCONNECT =>
            jsonCursor.get[String]("liveId") match {
              case Right(liveId) =>
                handleDisconnect(liveId)
              case Left(err) =>
                log.error(s"handle disconnect get liveId error:${err.getMessage()}")
            }
          case _ =>
            // Ignore the message
            sendError(session, "Invalid message, id: " + messageId)
            log.warn("[Handler::handleTextMessage] Skip, invalid message, id: {}", messageId)
        }
      case Left(e) =>
        log.error(s"[Handler::handleTextMessage] Exception: ${e.getMessage}, sessionId: ${session.id}")
    }
  }
}
