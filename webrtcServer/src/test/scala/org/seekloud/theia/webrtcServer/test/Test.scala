package org.seekloud.theia.webrtcServer.test

import java.nio.channels.Pipe

import io.circe.{Json, JsonObject}
import org.kurento.client.MediaFlowInStateChangeEvent
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.seekloud.theia.protocol.ptcl.CommonInfo.LiveInfo
import org.seekloud.theia.webrtcServer.core.udp.SendPipeline

/**
  * Created by sky
  * Date on 2019/6/12
  * Time at 下午1:54
  */
object Test {
  def main(args: Array[String]): Unit = {
//    val pipe = Pipe.open()
//    SendPipeline.initRtpPush(LiveInfo("1000007*100007", "CODE1000007*100007"), pipe.source(), null)
//    ReceivePipeline
//    Thread.sleep(50000)
  }
}
