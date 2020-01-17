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
  * Time at 上午9:48
  */
case class Rtp2RtcHandler(kurento:KurentoClient,roomId:Int) extends Handler() {
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

    rtpEp.connect(webRtcEp)

    val rtcSdpOffer: String = jsonMessage.get("rtcSdpOffer").getAsString
    initWebRtcEndpoint(session, webRtcEp, rtcSdpOffer)
    startWebRtcEndpoint(webRtcEp)

//    startRtpEndpoint(session, rtpEp)

  }

  override def handleJsonCursor(session: WebSocketSession, jsonCursor: HCursor): Unit = {}

  override def handlePcStart(liveId: String): Unit = {}
}
