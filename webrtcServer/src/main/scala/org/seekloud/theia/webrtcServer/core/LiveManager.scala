package org.seekloud.theia.webrtcServer.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.Flow
import org.seekloud.theia.protocol.ptcl.CommonInfo.LiveInfo
import org.seekloud.theia.webrtcMessage.ptcl.BrowserJson
import org.slf4j.LoggerFactory
import org.seekloud.theia.webrtcServer.Boot.{executor, materializer, scheduler, timeout}
import org.seekloud.theia.webrtcServer.core.LiveActor.Stop
import org.seekloud.theia.webrtcServer.kurento.control.Handler

import scala.concurrent.Future

/**
  * Created by sky
  * Date on 2019/7/16
  * Time at 上午11:41
  */
object LiveManager {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  final case class PcInit(liveId: String, handler: Handler) extends Command

  final case class GetWebSocketFlow(replyTo: ActorRef[Flow[Message, Message, Any]], liveInfo: LiveInfo) extends Command

  def create(): Behavior[Command] = {
    log.info(s"LiveManager start...")
    Behaviors.setup[Command] {
      ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            idle()
        }
    }
  }

  private def idle()
                  (
                    implicit timer: TimerScheduler[Command]
                  ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg: GetWebSocketFlow =>
          val userActor = getLiveActor(ctx, msg.liveInfo)
          msg.replyTo ! getWebSocketFlow(userActor)
          Behaviors.same

        case msg:PcInit =>
          val userActor = getLiveActor(ctx, LiveInfo(msg.liveId, ""))
          userActor ! LiveActor.Handler4Pc(msg.handler)
          Behaviors.same

        case unKnow =>
          log.error(s"${ctx.self.path} receive a unknow msg when idle:$unKnow")
          Behaviors.same
      }
    }
  }

  private def getWebSocketFlow(userActor: ActorRef[LiveActor.Command]): Flow[Message, Message, Any] = {
    Flow[Message]
      .collect {
        case TextMessage.Strict(m) =>
          Future.successful(LiveActor.WebSocketMsg(m))
        case TextMessage.Streamed(sm) =>
          sm.runFold(new StringBuilder().result()) {
            case (s, str) => s.++(str)
          }.map { s =>
            LiveActor.WebSocketMsg(s)
          }
      }.mapAsync(parallelism = 3)(identity) //同时处理Strict和Stream
      .via(LiveActor.flow(userActor))
      .map {
        case t: BrowserJson.ProtocolMsg =>
          TextMessage.Strict(t.m)
        case x =>
          log.debug(s"akka stream receive unknown msg=$x")
          TextMessage.apply("")
      }.withAttributes(ActorAttributes.supervisionStrategy(decider))

  }

  private val decider: Supervision.Decider = {
    e: Throwable =>
      e.printStackTrace()
      log.error(s"WS stream failed with $e")
      Supervision.Resume
  }

  private def getLiveActor(ctx: ActorContext[Command], liveInfo: LiveInfo): ActorRef[LiveActor.Command] = {
    val childName = s"LiveActor-${liveInfo.liveId}"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(LiveActor.create(liveInfo), childName)
//      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[LiveActor.Command]
  }
}
