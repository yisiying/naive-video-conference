package org.seekloud.theia.faceAnalysis.core

import java.net.InetSocketAddress
import java.nio.channels.Pipe.SourceChannel

import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.seekloud.theia.faceAnalysis.common.AppSettings
import org.seekloud.theia.protocol.ptcl.CommonInfo.LiveInfo
import org.slf4j.LoggerFactory

import org.seekloud.theia.rtpClient.{Protocol,PushStreamClient}
import org.seekloud.theia.rtpClient.Protocol._

/**
  * Author: wqf
  * Date: 2019/8/13
  * Time: 16:29
  */

object RtpPushActor {
  private val log = LoggerFactory.getLogger(this.getClass)

  case class Ready(client: PushStreamClient) extends Protocol.Command

  case class AuthInit(liveInfo: LiveInfo, source: SourceChannel, captureActor: ActorRef[CaptureActor.Command]) extends Protocol.Command

  case class PushStream(liveId: String, data: Array[Byte]) extends Protocol.Command

  case object PushStop extends Protocol.Command

  def create(): Behavior[Protocol.Command] = {
    Behaviors.setup[Protocol.Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Protocol.Command] = StashBuffer[Protocol.Command](Int.MaxValue)
      Behaviors.withTimers[Protocol.Command] { implicit timer =>
        val pushStreamDst = new InetSocketAddress(AppSettings.rtpServerIp, AppSettings.rtpServerPushPort)

        //        val pullStreamDst = new InetSocketAddress(AppSettings.rtpServerIp, AppSettings.rtpServerPullPort)

        val client = new PushStreamClient(AppSettings.magicIp,AppSettings.magicPushPort,pushStreamDst,ctx.self,s"https://${AppSettings.rtpServerIp}:50443")
        work(client)
      }
    }
  }

  def work(client: PushStreamClient)
          (implicit timer: TimerScheduler[Protocol.Command],
           stashBuffer: StashBuffer[Protocol.Command]): Behavior[Protocol.Command] = {
    Behaviors.setup[Protocol.Command] { context =>
      client.authStart()

      Behaviors.receiveMessage[Protocol.Command] {
        case AuthInit(liveInfo, source, liveActor) =>
          SendPipeline.sourcePool.put(liveInfo.liveId, (source, liveActor))
          client.auth(liveInfo.liveId, liveInfo.liveCode)
          Behaviors.same

        case AuthRsp(liveId, ifSuccess) =>
          log.info(liveId + " auth " + ifSuccess)
          if (ifSuccess) {
            SendPipeline.sourcePool.get(liveId) match {
              case Some(s) =>
                val sp = new Thread(new SendPipeline(s._1, liveId))
                sp.start()
              case None =>
            }
          } else {
            SendPipeline.sourcePool.get(liveId) match {
              case Some(s) =>
                s._2 ! CaptureActor.PushAuthFailed
                SendPipeline.sourcePool.remove(liveId)
              case None =>
            }
          }
          Behaviors.same

        case PushStream(liveId, data) =>
          client.pushStreamData(liveId, data)
          Behaviors.same

        case PushStreamError(liveId, errCode, msg) =>
          log.error("push stream error: " + msg)
          Behaviors.same

        case PushStop=>
          log.info("push stream stop")
          client.close()
          Behaviors.same

        case CloseSuccess=>
          log.info("push stream close success")
          Behaviors.stopped

        case x =>
          log.info(s"recv unknown msg: $x")
          Behaviors.same
      }
    }
  }
}

