package org.seekloud.theia.rtmpServer.core

import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel
import java.nio.channels.Pipe.SourceChannel

import org.seekloud.theia.rtpClient.PushStreamClient
import org.seekloud.theia.rtpClient.Protocol
import org.seekloud.theia.rtpClient.Protocol.{AuthRsp, CloseSuccess, PushStreamError}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.seekloud.theia.protocol.ptcl.CommonInfo.LiveInfo
import org.seekloud.theia.rtmpServer.common.AppSettings
import org.slf4j.LoggerFactory

/**
  * Author: wqf
  * Date: 2019/8/13
  * Time: 16:29
  */

object RtpClientActor{
  private val log = LoggerFactory.getLogger(this.getClass)

  case class Ready(client: PushStreamClient) extends Protocol.Command
  case class AuthInit(liveInfo: LiveInfo, source: SourceChannel, convertActor: ActorRef[ConvertActor.Command]) extends Protocol.Command
  case class PushStream(liveId: String, data: Array[Byte]) extends Protocol.Command
  case class PullStream(liveId: List[String]) extends Protocol.Command
  case object PushStop extends Protocol.Command


  def create(): Behavior[Protocol.Command] = {
    Behaviors.setup[Protocol.Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Protocol.Command] = StashBuffer[Protocol.Command](Int.MaxValue)
      Behaviors.withTimers[Protocol.Command] { implicit timer =>
        val authChannel =DatagramChannel.open()
        authChannel.bind(new InetSocketAddress(AppSettings.magicIp, AppSettings.magicPushPort))
        authChannel.socket().setReuseAddress(true)
        val pushStreamDst = new InetSocketAddress(AppSettings.rtpServerIp, AppSettings.rtpServerPushPort)

        val client = new PushStreamClient(AppSettings.magicIp, AppSettings.magicPushPort, pushStreamDst, ctx.self)
        ctx.self ! Ready(client)
        waiting()
      }
    }
  }

  def waiting()
    (implicit timer: TimerScheduler[Protocol.Command],
      stashBuffer: StashBuffer[Protocol.Command]): Behavior[Protocol.Command] = {
    Behaviors.receive[Protocol.Command] { (ctx, msg) =>
      msg match {
        case Ready(client) =>
          stashBuffer.unstashAll(ctx, work(client))

        case x =>
          stashBuffer.stash(x)
          Behavior.same
      }
    }
  }

  def work(client: PushStreamClient)
    (implicit timer: TimerScheduler[Protocol.Command],
      stashBuffer: StashBuffer[Protocol.Command]): Behavior[Protocol.Command] = {
    Behaviors.setup[Protocol.Command] { context =>
      client.authStart()

      Behaviors.receiveMessage[Protocol.Command]{
        case AuthInit(liveInfo, source, convertActor) =>
          SendPipeline.sourcePool.put(liveInfo.liveId, (source, convertActor))
          client.auth(liveInfo.liveId,liveInfo.liveCode)
          Behaviors.same

        case AuthRsp(liveId, ifSuccess) =>
          println(liveId + " auth " + ifSuccess)
          if(ifSuccess){
            log.info(s"push-102 $liveId")
            SendPipeline.sourcePool.get(liveId) match {
              case Some(s) =>
                val sp = new Thread(new SendPipeline(s._1, liveId))
                sp.start()
                SendPipeline.sendPool.put(liveId, sp)
              case None =>
            }
          } else {
            SendPipeline.sourcePool.get(liveId) match {
              case Some(s) =>
                s._2 ! ConvertActor.Stop
                SendPipeline.sourcePool.remove(liveId)
              case None =>
            }
          }
          Behaviors.same

        case PushStream(liveId, data) =>
          client.pushStreamData(liveId, data)
          Behaviors.same

        case PushStreamError(liveId, errCode, msg) =>
          log.error(s"push stream error: " + msg)
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

