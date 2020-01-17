package org.seekloud.theia.rtmpServer.core

import java.nio.ByteBuffer
import java.nio.channels.Pipe.SourceChannel

import akka.actor.typed.ActorRef
import org.slf4j.LoggerFactory

import scala.collection.mutable
import org.seekloud.theia.rtmpServer.Boot.rtpClientActor

/**
  * Created by sky
  * Date on 2019/7/16
  * Time at 14:12
  * 封装rtp并发出
  */
object SendPipeline {
  val log = LoggerFactory.getLogger(this.getClass)
  val sendPool: mutable.HashMap[String, Thread] = mutable.HashMap.empty
  val sourcePool: mutable.HashMap[String, (SourceChannel, ActorRef[ConvertActor.Command])] = mutable.HashMap.empty
}

class SendPipeline(source: SourceChannel, liveId: String) extends Runnable {
  import SendPipeline.log

  override def run(): Unit = {
    log.info(s"$liveId start push...")
    val buf = ByteBuffer.allocate(7 * 188) //receive
    var r = source.read(buf)
    while (r != -1) {
      if (r > 0) {
        buf.flip()
        rtpClientActor ! RtpClientActor.PushStream(liveId, buf.array().take(buf.remaining()))
      }
      buf.clear()
      r = source.read(buf)
    }
  }
}
