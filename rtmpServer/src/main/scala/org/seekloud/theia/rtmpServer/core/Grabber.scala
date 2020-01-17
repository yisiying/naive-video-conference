package org.seekloud.theia.rtmpServer.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.slf4j.LoggerFactory
import org.bytedeco.ffmpeg.global._

import scala.language.implicitConversions
import scala.collection.mutable
import scala.concurrent.{Await, Future, TimeoutException}
import scala.concurrent.duration._
import scala.util._
import org.seekloud.theia.rtmpServer.Boot.executor
import org.seekloud.theia.rtmpServer.core.ConvertActor.{MediaInfo, NextFrame, FrameCommand}

object Grabber {
  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class InitGrabber(obsUrl: String, replyTo: ActorRef[(MediaInfo, ActorRef[Grabber.Command])]) extends Command

  case class InitEncode(encode: ActorRef[FrameCommand]) extends Command

  case object NextGrab extends Command

  case object Stop extends Command

  case object TimeKey extends Command


  def create(liveId: String): Behavior[Command] = {
    log.info(s"grabberActor starting....")
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        idle(liveId, None)
      }
    }
  }


  def idle(liveId: String, grabber: Option[FFmpegFrameGrabber])(
    implicit timer: TimerScheduler[Command],
    stashBuffer: StashBuffer[Command]
  ): Behavior[Command] = Behaviors.receive[Command] { (ctx, msg) =>
    msg match {
      case InitGrabber(obsUrl, replyTo) =>
        val grabber = new FFmpegFrameGrabber(obsUrl)
        log.info(s"向srs拉取从obs推来的流:$obsUrl")
        Try(grabber.start()) match {
          case Success(_) =>
            val i = MediaInfo(
              grabber.getImageWidth,
              grabber.getImageHeight,
              grabber.getPixelFormat,
              grabber.getFrameRate,
              grabber.getVideoCodec,
              grabber.getVideoBitrate,
              grabber.getAudioChannels,
              grabber.getAudioBitrate,
              grabber.getSampleFormat,
              grabber.getSampleRate
            )
            replyTo ! (i, ctx.self)
            log.info("start success grab")

          case Failure(e) =>
            log.error(s"grabber start failed: ${e.getMessage}")
            Behaviors.stopped
        }

        idle(liveId, Some(grabber))

      case InitEncode(encode) =>
        ctx.self ! NextGrab
        work(liveId, grabber.get, encode)

      case x =>
        log.info(s"${ctx.self.path} receive unknown msg: $x")
        Behaviors.same
    }
  }


  private def work(liveId: String, grabber: FFmpegFrameGrabber, record: ActorRef[FrameCommand])(
    implicit timer: TimerScheduler[Command],
    stashBuffer: StashBuffer[Command]
  ): Behavior[Command] = {
    var isFirst = true
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case NextGrab =>
//          log.info(s"NextGrab")
          val value = grabber.grab()
          if (value != null)
            record ! NextFrame(value.clone())
          ctx.self ! NextGrab
          Behaviors.same
//          try {
//            Await.result(Future(grabber.grab()), if(isFirst) {isFirst = false;20.seconds} else 1.seconds) match {
//              case null =>
//                log.debug("get null")
//                Behaviors.same
//
//              case value =>
//                if (value != null)
//                  record ! NextFrame(value.clone())
//                ctx.self ! NextGrab
//                Behaviors.same
//            }
//          } catch {
//            case exception: TimeoutException =>
//              log.error(s"grabberActor timeout")
//              try {
//                Behaviors.stopped
//              } catch {
//                case exception: Exception =>
//                  log.error("grabberActor stopped error")
//                  Behaviors.stopped
//              }
//
//          }

        case x =>
          log.info(s"${ctx.self.path} receive unknown msg: $x")
          Behaviors.same
      }

    }
  }

}
