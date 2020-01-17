package org.seekloud.theia.faceAnalysis.core

import java.nio.ByteBuffer
import java.nio.channels.Pipe.SourceChannel
import akka.actor.typed.ActorRef
import org.slf4j.LoggerFactory

import scala.collection.mutable
import org.seekloud.theia.faceAnalysis.BootJFx.rtpPushActor

object SendPipeline {
  val log = LoggerFactory.getLogger(this.getClass)
  val sourcePool: mutable.HashMap[String, (SourceChannel, ActorRef[CaptureActor.Command])] = mutable.HashMap.empty
}

class SendPipeline(source: SourceChannel, liveId: String) extends Runnable {
  import SendPipeline.log

  override def run(): Unit = {
    log.info(s"$liveId start push...")
    val buf = ByteBuffer.allocate(7 * 188) //receive
    while (true) {
      buf.clear()
      val r = source.read(buf)
      if (r != -1) {
        buf.flip()
        rtpPushActor ! RtpPushActor.PushStream(liveId, buf.array().take(buf.remaining()))
      }
    }
  }
}
