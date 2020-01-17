package org.seekloud.theia.rtmpServer.core

import java.nio.channels.Pipe

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import net.sf.ehcache.transaction.xa.commands.Command
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.seekloud.theia.protocol.ptcl.CommonInfo.LiveInfo
import org.slf4j.LoggerFactory
import org.seekloud.theia.rtmpServer.Boot.rtpClientActor
import org.seekloud.theia.rtmpServer.core.ConvertActor.MediaInfo
import org.seekloud.theia.rtmpServer.core.Grabber.InitGrabber

import scala.language.implicitConversions
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object GrabberManager {
  sealed trait Command

  case class TimeOut(msg: String) extends Command

  case class NewObsGrabber(liveInfo: LiveInfo,
                           ObsUrl: String,
                           replyTo: ActorRef[(MediaInfo, ActorRef[Grabber.Command])]) extends Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  case object TimerKey

  private val log = LoggerFactory.getLogger(this.getClass)

  var count = 0

  def create(): Behavior[Command] = {
    Behaviors.setup[Command] {ctx =>
      Behaviors.withTimers[Command]{
        implicit timer =>
          idle(timer)
      }
    }
  }

  def idle(timer: TimerScheduler[Command]): Behavior[Command] = {
    Behaviors.receive[Command] {
      (ctx, msg) =>
        msg match {

          case NewObsGrabber(liveInfo, obsUrl, replyTo) =>
            log.debug(s"NewObsGrabber")
            val grabberActor = getGrabber(ctx, liveInfo.liveId)
            grabberActor ! InitGrabber(obsUrl, replyTo)
            Behaviors.same

          case ChildDead(name, childRef) =>
            log.info(s"-----------------$name is dead")
            Behaviors.same
        }
    }
  }

  def getGrabber(ctx: ActorContext[Command],
                 liveId: String): ActorRef[Grabber.Command] = {
    val childName = s"GrabberActor-$count-$liveId"
    count +=1
    ctx.child(childName).getOrElse{
      log.info(s"create convert actor $childName")
      val actor = ctx.spawn(Grabber.create(liveId), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[Grabber.Command]
  }


}
