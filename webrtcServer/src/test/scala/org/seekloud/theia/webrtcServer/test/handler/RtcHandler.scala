package org.seekloud.theia.webrtcServer.test.handler

import com.google.gson.{GsonBuilder, JsonObject}
import io.circe.HCursor
import org.kurento.client.{KurentoClient, MediaPipeline, WebRtcEndpoint}
import org.seekloud.theia.webrtcServer.kurento.UserSession
import org.seekloud.theia.webrtcServer.kurento.control.Handler
import org.seekloud.theia.webrtcServer.ptcl.WebSocketSession

/**
  * Created by sky
  * Date on 2019/6/17
  * Time at 下午2:33
  * 实现建立连接控制
  */
case class RtcHandler(kurento:KurentoClient, roomId:Int) extends Handler() {
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
    webRtcEp.connect(webRtcEp)
    // ---- Endpoint configuration
    val sdpOffer: String = jsonMessage.get("sdpOffer").getAsString
    initWebRtcEndpoint(session, webRtcEp, sdpOffer)

    log.info("[Handler::handleStart] New WebRtcEndpoint: {}", webRtcEp.getName)

    // ---- Endpoint startup

    startWebRtcEndpoint(webRtcEp)

    // ---- Debug
    // final String pipelineDot = pipeline.getGstreamerDot();
    // try (PrintWriter out = new PrintWriter("pipeline.dot")) {
    //   out.println(pipelineDot);
    // } catch (IOException ex) {
    //   log.error("[Handler::start] Exception: {}", ex.getMessage());
    // }
  }

  override def handleJsonCursor(session: WebSocketSession, jsonCursor: HCursor): Unit = {}

  override def handlePcStart(liveId: String): Unit = {}


}
