package org.seekloud.theia.webrtcServer.kurento

import org.kurento.client.MediaPipeline
import org.kurento.client.RtpEndpoint
import org.kurento.client.WebRtcEndpoint
import org.seekloud.theia.webrtcServer.ptcl.WebSocketSession

/**
  * Created by sky
  * Date on 2019/6/14
  * Time at 上午11:50
  *
  * 保存用户媒体信息
  */
class UserSession(
                   val socketSession: WebSocketSession,
                   val isAnchor: Boolean = true
                 ) {
  private var mediaPipeline: MediaPipeline = null
  private var rtpEp: RtpEndpoint = null
  private var webRtcEp: WebRtcEndpoint = null
  private var sdpOffer: String = null

  def getMediaPipeline: MediaPipeline = mediaPipeline

  def setMediaPipeline(mediaPipeline: MediaPipeline): Unit = {
    this.mediaPipeline = mediaPipeline
  }

  def getRtpEndpoint: RtpEndpoint = rtpEp

  def setRtpEndpoint(rtpEp: RtpEndpoint): Unit = {
    this.rtpEp = rtpEp
  }

  def getWebRtcEndpoint: WebRtcEndpoint = webRtcEp

  def setWebRtcEndpoint(webRtcEp: WebRtcEndpoint): Unit = {
    this.webRtcEp = webRtcEp
  }

  def getSdpOffer: String = sdpOffer

  def setSdpOffer(s: String) = this.sdpOffer = s

}
