package org.seekloud.theia.webrtcServer.kurento.control

import akka.actor.typed.ActorRef
//import com.google.gson.{GsonBuilder, JsonObject}
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe._
import io.circe.parser._
import org.kurento.client._
import org.kurento.jsonrpc.JsonUtils
import org.seekloud.theia.webrtcMessage.ptcl.BrowserJson
import org.seekloud.theia.webrtcMessage.ptcl.BrowserJson.EventId
import org.seekloud.theia.webrtcServer.kurento.UserSession
import org.seekloud.theia.webrtcServer.ptcl.WebSocketSession
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * Created by sky
  * Date on 2019/6/17
  * Time at 上午11:59
  * edit from kurento-rtp-receive
  * 处理控制消息
  */
abstract class Handler() {
  protected val log = LoggerFactory.getLogger(this.getClass)

  protected val userMap = mutable.HashMap[String, UserSession]()

  private def candidate2Json(candidate: IceCandidate): Json = {
    Json.obj(
      ("candidate", Json.fromString(candidate.getCandidate)),
      ("sdpMid", Json.fromString(candidate.getSdpMid)),
      ("sdpMLineIndex", Json.fromInt(candidate.getSdpMLineIndex))
    )
  }

  protected def addWebRtcEventListeners(session: WebSocketSession, webRtcEp: WebRtcEndpoint): Unit = {
    log.info("[Handler::addWebRtcEventListeners] name: {}, sessionId: {}", "", webRtcEp.getName, session.id)
    // Event: The ICE backend found a local candidate during Trickle ICE
    webRtcEp.addIceCandidateFoundListener(new EventListener[IceCandidateFoundEvent]() {
      override def onEvent(ev: IceCandidateFoundEvent): Unit = {
        //        log.debug("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}, candidate: {}", ev.getType, ev.getSource.getName, ev.getTimestamp, ev.getTags, JsonUtils.toJsonObject(ev.getCandidate))
        val message = Json.obj(
          ("id", Json.fromString(EventId.ADD_ICE_CANDIDATE)),
          ("candidate", candidate2Json(ev.getCandidate))
        ).noSpaces
        sendMessage(session.session, BrowserJson.ProtocolMsg(message))
      }
    })
    // Event: The ICE backend changed state
    /*webRtcEp.addIceComponentStateChangeListener(new EventListener[IceComponentStateChangeEvent]() {
      override def onEvent(ev: IceComponentStateChangeEvent): Unit = {
        log.debug("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}, streamId: {}, componentId: {}, state: {}", ev.getType, ev.getSource.getName, ev.getTimestamp, ev.getTags, ev.getStreamId.toString, ev.getComponentId.toString, ev.getState)
      }
    })
    // Event: The ICE backend finished gathering ICE candidates
    webRtcEp.addIceGatheringDoneListener(new EventListener[IceGatheringDoneEvent]() {
      override def onEvent(ev: IceGatheringDoneEvent): Unit = {
        log.debug("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}", ev.getType, ev.getSource.getName, ev.getTimestamp, ev.getTags)
      }
    })
    // Event: The ICE backend selected a new pair of ICE candidates for use
    webRtcEp.addNewCandidatePairSelectedListener(new EventListener[NewCandidatePairSelectedEvent]() {
      override def onEvent(ev: NewCandidatePairSelectedEvent): Unit = {
        log.info("[WebRtcEndpoint::{}] name: {}, timestamp: {}, tags: {}, streamId: {}, local: {}, remote: {}", ev.getType, ev.getSource.getName, ev.getTimestamp, ev.getTags, ev.getCandidatePair.getStreamID, ev.getCandidatePair.getLocalCandidate, ev.getCandidatePair.getRemoteCandidate)
      }
    })*/
  }

  protected def initWebRtcEndpoint(session: WebSocketSession, webRtcEp: WebRtcEndpoint, sdpOffer: String): Unit = {
    addWebRtcEventListeners(session, webRtcEp)
    val name = "user" + session.id + "_webrtcendpoint"
    webRtcEp.setName(name)
    /*
        OPTIONAL: Force usage of an Application-specific STUN server.
        Usually this is configured globally in KMS WebRTC settings file:
        /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini

        But it can also be configured per-application, as shown:

        log.info("[Handler::initWebRtcEndpoint] Using STUN server: 193.147.51.12:3478");
        webRtcEp.setStunServerAddress("193.147.51.12");
        webRtcEp.setStunServerPort(3478);
        */
    // Continue the SDP Negotiation: Generate an SDP Answer
    val sdpAnswer = webRtcEp.processOffer(sdpOffer)
    //    log.info(s"[Handler::initWebRtcEndpoint]name: $name, SDP Offer from browser to KMS:\n$sdpOffer")
    //    log.info(s"[Handler::initWebRtcEndpoint]name: $name, SDP Answer from KMS to browser:\n$sdpAnswer")
    val message = Json.obj(
      ("id", Json.fromString(EventId.PROCESS_SDP_ANSWER)),
      ("sdpAnswer", Json.fromString(sdpAnswer))
    ).noSpaces
    sendMessage(session.session, BrowserJson.ProtocolMsg(message))
  }

  protected def initRtpEndpoint(session: WebSocketSession, rtpEp: RtpEndpoint, sdpOffer: String): Unit = {
    val name = "user" + session.id + "_rtpendpoint"
    rtpEp.setName(name)
    /*
        OPTIONAL: Force usage of an Application-specific STUN server.
        Usually this is configured globally in KMS WebRTC settings file:
        /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini

        But it can also be configured per-application, as shown:

        log.info("[Handler::initWebRtcEndpoint] Using STUN server: 193.147.51.12:3478");
        webRtcEp.setStunServerAddress("193.147.51.12");
        webRtcEp.setStunServerPort(3478);
        */
    // Continue the SDP Negotiation: Generate an SDP Answer
    val sdpAnswer = rtpEp.processOffer(sdpOffer)
    //    log.info(s"[Handler::initRtpEndpoint]name: $name, SDP Offer from browser to KMS:\n$sdpOffer")
    //    log.info(s"[Handler::initRtpEndpoint]name: $name, SDP Answer from KMS to browser:\n$sdpAnswer")
    val message = Json.obj(
      ("id", Json.fromString(EventId.PROCESS_SDP_ANSWER)),
      ("sdpAnswer", Json.fromString(sdpAnswer))
    ).noSpaces
    sendMessage(session.session, BrowserJson.ProtocolMsg(message))
  }

  protected def startWebRtcEndpoint(webRtcEp: WebRtcEndpoint): Unit = { // Calling gatherCandidates() is when the Endpoint actually starts working.
    // In this tutorial, this is emphasized for demonstration purposes by
    // launching the ICE candidate gathering in its own method.
    webRtcEp.gatherCandidates()
  }

  // ADD_ICE_CANDIDATE ---------------------------------------------------------
  protected def handleAddIceCandidate(session: WebSocketSession, jsonMessage: HCursor): Unit = {
    val userOpt = userMap.get(session.id)
    if (userOpt.nonEmpty) {
      try{
        val user = userOpt.get
        val jsonCandidate = jsonMessage.get[Json]("candidate").getOrElse(Json.Null)
        val jsonCandidateCursor = jsonCandidate.hcursor
        val candidate = new IceCandidate(jsonCandidateCursor.get[String]("candidate").right.get, jsonCandidateCursor.get[String]("sdpMid").right.get, jsonCandidateCursor.get[Int]("sdpMLineIndex").right.get)
        val webRtcEp = user.getWebRtcEndpoint
        webRtcEp.addIceCandidate(candidate)
      }catch {
        case exception: Exception=>
          log.error(exception.getMessage)
      }
    } else {
      log.info(s"userMap didn't contain ${session.id}")
    }
  }

  //STOP --------------------
  def handleDisconnect(liveId: String): Unit = {}

  //Error -----------------------------------------
  protected def sendError(session: WebSocketSession, errMsg: String): Unit = {
    if (userMap.contains(session.id)) {
      val message = Json.obj(
        ("id", Json.fromString("ERROR")),
        ("message", Json.fromString(errMsg))
      ).noSpaces
      sendMessage(session.session, BrowserJson.ProtocolMsg(message))

    }
  }

  def handleJsonCursor(session: WebSocketSession, jsonCursor: HCursor): Unit

  def handlePcStart(liveId: String): Unit

  protected def sendMessage(session: ActorRef[BrowserJson.WsMsg], msg: BrowserJson.WsMsg) = {
    session ! msg
  }
}
