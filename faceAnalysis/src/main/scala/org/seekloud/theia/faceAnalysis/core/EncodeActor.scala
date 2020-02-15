package org.seekloud.theia.faceAnalysis.core

import java.io.OutputStream
import java.nio.ShortBuffer
import java.util.concurrent.{ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import org.bytedeco.javacv.{FFmpegFrameRecorder, Frame}
import org.seekloud.theia.faceAnalysis.BootJFx.{blockingDispatcher, captureActor}
import org.slf4j.LoggerFactory
import CaptureActor.{FrameSnapShot, audioChannels, frameDuration, frameRate}

/**
  * Created by sky
  * Date on 2019/8/20
  * Time at 上午10:03
  * ffmpeg encode part
  * video/audio
  */
object EncodeActor {
  private val log = LoggerFactory.getLogger(this.getClass)

  case class SignalInfo(
                         var t: Boolean,
                         sTs: Long
                       )

  trait EncodeCommand

  final case class EncodeInit(outputStream: OutputStream, camWidth: Int, camHeight: Int) extends EncodeCommand

  trait VideoEncodeCommand

  final case class EncodeImage(frame: Frame, encodeFrameNumber: Int) extends VideoEncodeCommand

  trait AudioEncodeCommand

  final case class EncodeSamples(sampleRate: Int, channel: Int, sample: ShortBuffer) extends AudioEncodeCommand

  final case object StopEncode extends EncodeCommand with VideoEncodeCommand with AudioEncodeCommand

  def create(logPrefix: String, frameSnapShot: FrameSnapShot): Behavior[EncodeCommand] = {
    log.info(s"$logPrefix start...")
    Behaviors.setup[EncodeCommand] { ctx =>
      Behaviors.receiveMessage[EncodeCommand] {
        case msg: EncodeInit =>
          log.info(s"$logPrefix EncodeInit")
          val recorder = new FFmpegFrameRecorder(msg.outputStream, msg.camWidth, msg.camHeight, audioChannels)
          //          val recorder = new FFmpegFrameRecorder(s"test_${System.currentTimeMillis()}.ts", msg.camWidth, msg.camHeight, audioChannels)
          recorder.setVideoOption("preset", "ultrafast")
          recorder.setVideoOption("tune", "zerolatency")
          recorder.setVideoOption("crf", "25")

          recorder.setFrameRate(frameRate)
          recorder.setVideoBitrate(2000000)
          recorder.setMaxBFrames(0)
          //          recorder.setVideoOption("crf", "25")

          /*audio*/
          recorder.setAudioOption("crf", "0")
          recorder.setAudioQuality(0)
          recorder.setAudioBitrate(192000)
          recorder.setSampleRate(44100)
          recorder.setAudioChannels(audioChannels)
          //          recorder.setAudioOption("crf", "0")

          recorder.setFormat("mpegts")
          recorder.setFrameNumber(0)
          recorder.startUnsafe()

          val tsInfo = SignalInfo(false, System.currentTimeMillis())
          val videoEncoder = ctx.spawn(encodeVideoState("videoEncoder|", tsInfo, recorder, frameSnapShot), "videoEncoder", blockingDispatcher)
          val audioEncoder = ctx.spawn(encodeAudioState("audioEncoder|", tsInfo, recorder, frameSnapShot), "audioEncoder", blockingDispatcher)
          captureActor ! CaptureActor.Encoder(videoEncoder, audioEncoder)
          idle("idle|", msg.outputStream, recorder)

        case unKnow =>
          log.warn(s"$logPrefix got unknown msg: $unKnow")
          Behavior.same
      }
    }
  }

  private def idle(logPrefix: String, outputStream: OutputStream, recorder: FFmpegFrameRecorder): Behavior[EncodeCommand] = {
    Behaviors.receiveMessage[EncodeCommand] {
      case StopEncode =>
        log.info(s"$logPrefix StopEncode")
        recorder.release()
        recorder.stop()
        outputStream.close()
        Behaviors.stopped

      case unKnow =>
        log.warn(s"$logPrefix got unknown msg: $unKnow")
        Behavior.same
    }
  }

  private def encodeVideoState(
                                logPrefix: String,
                                tsInfo: SignalInfo,
                                recorder: FFmpegFrameRecorder,
                                frameSnapShot: FrameSnapShot
                              ): Behavior[VideoEncodeCommand] =
    Behaviors.withTimers[VideoEncodeCommand] { timer =>
      Behaviors.receive[VideoEncodeCommand] { (ctx, msg) =>
        msg match {
          case msg: EncodeImage =>
            try {
              if (!tsInfo.t) {
                tsInfo.t = true
              }
              recorder.setFrameNumber(msg.encodeFrameNumber)
              recorder.record(msg.frame)
            } catch {
              case ex: Exception =>
                log.error(s"$logPrefix video error: $ex")
                tsInfo.t = false
            }
            Behaviors.same

          case StopEncode =>
            log.info(s"$logPrefix video encoding stopping...")
            Behaviors.stopped

          case unKnow =>
            log.warn(s"$logPrefix video got unknown msg: $unKnow")
            Behavior.same
        }
      }
    }

  private def encodeAudioState(logPrefix: String,
                               tsInfo: SignalInfo,
                               recorder: FFmpegFrameRecorder,
                               frameSnapShot: FrameSnapShot
                              ) = Behaviors.setup[AudioEncodeCommand] { ctx =>
    Behaviors.receiveMessage[AudioEncodeCommand] {
      case msg: EncodeSamples =>
        try {
          if (tsInfo.t) {
            recorder.recordSamples(msg.sampleRate, msg.channel, msg.sample)
          }
        } catch {
          case e: Exception =>
            log.error(s"$logPrefix audio " + e.getMessage)
        }
        Behaviors.same

      case StopEncode =>
        log.info(s"$logPrefix audio encoding stopping...")
        Behaviors.stopped

      case unKnow =>
        log.warn(s"$logPrefix audio got unknown msg: $unKnow")
        Behavior.same
    }
  }
}
