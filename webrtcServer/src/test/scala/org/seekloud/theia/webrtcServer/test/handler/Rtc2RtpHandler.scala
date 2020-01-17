package org.seekloud.theia.webrtcServer.test.handler

import com.google.gson.{GsonBuilder, JsonObject}
import io.circe.HCursor
import org.kurento.client.{KurentoClient, MediaPipeline, RtpEndpoint, WebRtcEndpoint}
import org.seekloud.theia.webrtcServer.kurento.UserSession
import org.seekloud.theia.webrtcServer.kurento.control.Handler
import org.seekloud.theia.webrtcServer.ptcl.WebSocketSession

/**
  * Created by sky
  * Date on 2019/6/18
  * Time at 上午9:47
  */
case class Rtc2RtpHandler(kurento:KurentoClient,roomId:Int) extends Handler() {
  protected val gson = new GsonBuilder().create
  protected def handleProcessSdpOffer(session: WebSocketSession, jsonMessage: JsonObject): Unit = {
    log.info("[Handler::handleStart] User count: {}", userMap.size)
    log.info("[Handler::handleStart] New user, id: {}", session.id)
    val user: UserSession = new UserSession(session)
    userMap.put(session.id, user)
    // ---- Media pipeline
    log.info("[Handler::handleStart] Create Media Pipeline")
    val pipeline: MediaPipeline = kurento.createMediaPipeline
    user.setMediaPipeline(pipeline)
    val webRtcEp: WebRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build
    user.setWebRtcEndpoint(webRtcEp)
    val rtpEp: RtpEndpoint = new RtpEndpoint.Builder(pipeline).build
    user.setRtpEndpoint(rtpEp)
    // ---- Endpoint configuration
    val rtcSdpOffer: String = jsonMessage.get("rtcSdpOffer").getAsString
    val rtpSdpOffer: String = jsonMessage.get("rtpSdpOffer").getAsString
    initWebRtcEndpoint(session, webRtcEp, rtcSdpOffer)
    initRtpEndpoint(session, rtpEp, rtpSdpOffer)
    webRtcEp.connect(rtpEp)

    log.info("[Handler::handleStart] New WebRtcEndpoint: {}", webRtcEp.getName)

    // ---- Endpoint startup

    startWebRtcEndpoint(webRtcEp)
  }


  override def handleJsonCursor(session: WebSocketSession, jsonCursor: HCursor): Unit = {}

  override def handlePcStart(liveId: String): Unit = {}
}
