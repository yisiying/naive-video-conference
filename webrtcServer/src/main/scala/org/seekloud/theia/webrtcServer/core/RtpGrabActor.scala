package org.seekloud.theia.webrtcServer.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.seekloud.theia.webrtcServer.common.AppSettings
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, Future, TimeoutException}
import scala.util.{Failure, Success, Try}
import org.seekloud.theia.webrtcServer.Boot.executor
import org.seekloud.theia.webrtcServer.ptcl.MediaInfo
import org.seekloud.theia.webrtcServer.utils.FileUtil

import scala.concurrent.duration._

/**
  * Created by sky
  * Date on 2019/7/17
  * Time at 16:29
  */
object RtpGrabActor {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  case class StartGrab(url: String, replyTo: ActorRef[(MediaInfo, FFmpegFrameGrabber)]) extends Command

  case class InitRecord(grabber: FFmpegFrameGrabber, recordVideo: ActorRef[RtpRecordActor.VideoCommand], recordAudio: ActorRef[RtpRecordActor.AudioCommand]) extends Command

  case class InitEncode(grabber: FFmpegFrameGrabber, encodeVideo: ActorRef[RtpRecordActor.VideoCommand], encodeAudio: ActorRef[RtpRecordActor.AudioCommand]) extends Command

  case object NextGrab extends Command

  def create(): Behavior[Command] = {
    log.info(s"grabActor starting....")
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        init()
      }
    }
  }

  private def init()(
    implicit timer: TimerScheduler[Command],
    stashBuffer: StashBuffer[Command]
  ): Behavior[Command] = Behaviors.receive[Command] { (ctx, msg) =>
    msg match {
      case StartGrab(url, replyTo) =>
        val grabber = new FFmpegFrameGrabber(AppSettings.sdpPath + url)
        log.info("start set grab")
        grabber.setOption("protocol_whitelist", "file,rtp,udp")
        grabber.setOption("fflags", "nobuffer")
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
            //            FileUtil.saveFile(i.toString, "info.txt")
            replyTo ! (i, grabber)
            log.info("start success grab")
            Behaviors.same
          case Failure(e) =>
            log.error(e.getMessage)
            Behaviors.stopped
        }

      case msg: InitRecord =>
        ctx.self ! NextGrab
        work(msg.grabber, msg.recordVideo, msg.recordAudio)

      case msg: InitEncode =>
        ctx.self ! NextGrab
        work(msg.grabber, msg.encodeVideo, msg.encodeAudio)

      case unKnow =>
        log.warn(s"init got unknown msg: $unKnow")
        Behavior.same
    }
  }

  private def work(grabber: FFmpegFrameGrabber, recordVideo: ActorRef[RtpRecordActor.VideoCommand], recordAudio: ActorRef[RtpRecordActor.AudioCommand])(
    implicit timer: TimerScheduler[Command],
    stashBuffer: StashBuffer[Command]
  ): Behavior[Command] = Behaviors.receive[Command] { (ctx, msg) =>
    msg match {
      case NextGrab =>
        try {
          Await.result(Future(grabber.grab()), 1.seconds) match {
            case null =>
              log.debug("get null")
              Behaviors.same
            case value =>
              if (value.image != null) {
//                log.info(s"video: ${value.timestamp}")
                recordVideo ! RtpRecordActor.NextFrame(value.clone())
              }
              if (value.samples != null) {
//                log.info(s"audio: ${value.timestamp}")
                recordAudio ! RtpRecordActor.NextFrame(value.clone())
              }
              ctx.self ! NextGrab
              Behaviors.same
          }
        } catch {
          case exception: TimeoutException =>
            log.error(s"grabActor timeout")
            try {
              //              grabber.stop()
              Behaviors.stopped
            } catch {
              case exception: Exception =>
                log.error("grabActor stopped error")
                Behaviors.stopped
            }

        }
    }

  }

  private def work4Encode(grabber: FFmpegFrameGrabber, encoder: ActorRef[RtpRecordActor.EncodeCommand])(
    implicit timer: TimerScheduler[Command],
    stashBuffer: StashBuffer[Command]
  ): Behavior[Command] = Behaviors.receive[Command] { (ctx, msg) =>
    msg match {
      case NextGrab =>
        try {
          Await.result(Future(grabber.grab()), 1.seconds) match {
            case null =>
              log.debug("get null")
              Behaviors.same
            case value =>
              //              log.info(s"get frame ${value.timestamp}")
              encoder ! RtpRecordActor.NextFrame(value.clone())
              //              encoder ! RtpRecordActor.NextPacket(value)
              ctx.self ! NextGrab
              Behaviors.same
          }
        } catch {
          case exception: TimeoutException =>
            log.error(s"grabActor timeout")
            try {
              //              grabber.stop()
              Behaviors.stopped
            } catch {
              case exception: Exception =>
                log.error("grabActor stopped error")
                Behaviors.stopped
            }

        }
    }

  }
}
