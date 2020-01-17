package org.seekloud.theia.webrtcServer.core.udp

import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel
import java.nio.channels.Pipe.SourceChannel

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, Behaviors, StashBuffer, TimerScheduler}
import org.seekloud.theia.shared.rtp.{Protocol, RtpClient}
import org.seekloud.theia.shared.rtp.Protocol.{AuthRsp, PullStreamData, PullStreamReqSuccess, PushStreamError}
import org.slf4j.LoggerFactory
import akka.actor.typed.scaladsl.adapter._
import org.seekloud.theia.protocol.ptcl.CommonInfo.LiveInfo
import org.seekloud.theia.webrtcServer.Boot.system
import org.seekloud.theia.webrtcServer.common.AppSettings
import org.seekloud.theia.webrtcServer.core.LiveActor

import scala.collection.mutable
/**
  * Author: wqf
  * Date: 2019/8/13
  * Time: 16:29
  */

object RtpClientActor{
  private val log = LoggerFactory.getLogger(this.getClass)

  case class Ready(client: RtpClient) extends Protocol.Command
  case class AuthInit(liveInfo: LiveInfo, source: SourceChannel, liveActor: ActorRef[LiveActor.Command]) extends Protocol.Command
  case class PushStream(liveId: String, data: Array[Byte]) extends Protocol.Command
//  case class PullStream(liveId: List[String]) extends Protocol.Command

  def create(): Behavior[Protocol.Command] = {
    Behaviors.setup[Protocol.Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Protocol.Command] = StashBuffer[Protocol.Command](Int.MaxValue)
      Behaviors.withTimers[Protocol.Command] { implicit timer =>
        val authChannel =DatagramChannel.open()
        authChannel.bind(new InetSocketAddress(AppSettings.magicIp, AppSettings.magicPushPort))
        authChannel.socket().setReuseAddress(true)
        val pushStreamDst = new InetSocketAddress(AppSettings.rtpServerIp, AppSettings.rtpServerPushPort)

        //        val pullStreamChannel =DatagramChannel.open()
//        pullStreamChannel.bind(new InetSocketAddress(AppSettings.magicIp, AppSettings.magicPullPort))
//        pullStreamChannel.socket().setReuseAddress(true)
        val client = new RtpClient(Some(authChannel), Some(pushStreamDst), None, None, ctx.self)
        ctx.self ! Ready(client)
        wait1()
      }
    }
  }

  def wait1()
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

  def work(client: RtpClient)
    (implicit timer: TimerScheduler[Protocol.Command],
      stashBuffer: StashBuffer[Protocol.Command]): Behavior[Protocol.Command] = {
    Behaviors.setup[Protocol.Command] { context =>
      client.authStart()

      Behaviors.receiveMessage[Protocol.Command]{
        case AuthInit(liveInfo, source, liveActor) =>
          SendPipeline.sourcePool.put(liveInfo.liveId, (source, liveActor))
          client.auth(liveInfo.liveId,liveInfo.liveCode)
          Behaviors.same

        case AuthRsp(liveId, ifSuccess) =>
          log.info(liveId + " auth " + ifSuccess)
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
                s._2 ! LiveActor.Stop
                SendPipeline.sourcePool.remove(liveId)
              case None =>
            }
          }
          Behaviors.same

//        case PullStream(liveId) =>
//          client.pullStreamData(liveId)
//          Behaviors.same

        case PushStream(liveId, data) =>
          client.pushStreamData(liveId, data)
          Behaviors.same

        case PushStreamError(liveId, errCode, msg) =>
          println("push stream error: " + msg)
          Behaviors.same

//        case PullStreamReqSuccess(liveIds) =>
//          println(liveIds.mkString("#"))
//          Behaviors.same
//
//        case PullStreamData(liveId, data) =>
//        //  println("pull stream " + liveId)
//        //  println(liveId + " receive data:" + new String(data, "UTF-8"))
//          Behaviors.same

        case x =>
          log.info(s"recv unknown msg: $x")
          Behaviors.same
      }
    }
  }
}

