package org.seekloud.theia.rtmpServer.core

/**
  * User: yuwei
  * Date: 2019/5/26
  * Time: 12:21
  */

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.scaladsl.AskPattern._
import org.slf4j.LoggerFactory
import org.seekloud.theia.rtmpServer.Boot.{blockingDispatcher, convertManager, executor, grabberManager, rtpClientActor}
import java.io.{File, OutputStream}
import java.nio.{Buffer, ByteBuffer, ShortBuffer}

import scala.concurrent.duration._
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import org.bytedeco.javacv._
import java.nio.Buffer
import java.nio.channels.{Channels, Pipe}

import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacpp.Loader
import org.opencv.video.Video
import org.seekloud.theia.protocol.ptcl.CommonInfo.LiveInfo
import org.seekloud.theia.rtmpServer.common.AppSettings
import org.seekloud.theia.rtmpServer.common.AppSettings

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util._
import org.seekloud.theia.rtmpServer.Boot.{scheduler, timeout}
import org.seekloud.theia.rtmpServer.core.Grabber.InitEncode

object ConvertActor {

  private val log = LoggerFactory.getLogger(this.getClass)
  private final val InitTime = Some(5.minutes)
//  var process: Process = _

  case class MediaInfo(
                        imageWidth: Int,
                        imageHeight: Int,
                        pixelFormat: Int,
                        frameRate: Double,
                        videoCodec: Int,
                        videoBitrate: Int,
                        audioChannels: Int,
                        audioBitrate: Int,
                        sampleFormat: Int,
                        sampleRate: Int
                      )

  case class SignalInfo(
                         var t: Boolean,
                         sTs: Long
                       )

  sealed trait Command

  case object Start extends Command

  case object StartTest extends Command

  case object StopOver extends Command

  case object Stop extends Command

  case object TimerKey

  case class TimeOut(msg: String) extends Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  trait FrameCommand

  case object FrameTimeKey

  case object FrameTimeOut extends FrameCommand

  case class NextFrame(f: Frame) extends FrameCommand

  def idle(liveInfo:LiveInfo, obsUrl:String):Behavior[Command] = {
    Behaviors.withTimers[Command] { timer =>
      Behaviors.receive[Command] { (ctx, msg) =>
        log.info(s"${ctx.self} rev msg:$msg")
        msg match {
          case Start =>
            val pipe = Pipe.open() //ts流通道

//            rtpClientActor ! RtpClientActor.AuthInit(liveInfo, pipe.source(), ctx.self)
//            log.info(s"${liveInfo.liveId} start convert")
//            val ffmpeg = Loader.load(classOf[org.bytedeco.ffmpeg.ffmpeg])
//            val cmd = ffmpeg + s" -f mpegts -i $obsUrl -b:v 1M -bufsize 1M ${AppSettings.fileLocation}${liveInfo.liveId}/out.flv"
//            val pb = new ProcessBuilder(cmd)
//            process = pb.inheritIO().start()

            val rspFuture: Future[(MediaInfo, ActorRef[Grabber.Command])] = grabberManager ? (GrabberManager.NewObsGrabber(liveInfo, obsUrl, _))
            rspFuture.map { r =>
              val recorder = initEncoder(r._1, Channels.newOutputStream(pipe.sink()))
              val encoder = ctx.spawn(encodeState(r._1, recorder), "encoder")

              r._2 ! InitEncode(encoder)
            }

            Behaviors.same

          case ChildDead(name, childRef) =>
            log.info(s"$childRef is dead----")
            Behaviors.same

          case StopOver =>
            timer.startSingleTimer(TimerKey, Stop, 500.milli)
            Behaviors.same

          case Stop =>
            log.info(s"${ctx.self.path} stop")
            Behaviors.stopped

          case x =>
            log.info(s"${ctx.self.path} receive unknown msg: $x")
            Behaviors.same
        }
      }
    }
  }

  private def encodeState(info: MediaInfo, recorder: FFmpegFrameRecorder) = Behaviors.setup[FrameCommand] { ctx =>
    Behaviors.withTimers[FrameCommand] { timer =>
      Behaviors.receiveMessage[FrameCommand] {
        case m: NextFrame =>
          if (m.f != null) {
//            recorder.setTimestamp(m.f.timestamp)
            recorder.record(m.f)
          }
          Behaviors.same

        case unKnow =>
          log.warn(s"encode video got unknown msg: $unKnow")
          Behavior.same
      }
    }
  }

  def initEncoder(info: MediaInfo, outputStream: OutputStream): FFmpegFrameRecorder = {
    val recorder = new FFmpegFrameRecorder(outputStream, info.imageWidth, info.imageHeight, info.audioChannels)
    recorder.setOption("fflags", "nobuffer")
    recorder.setOption("tune", "zerolatency")
    recorder.setVideoOption("preset", "ultrafast")
    recorder.setFrameRate(30)
    recorder.setVideoBitrate(2000000)
    recorder.setMaxBFrames(0)

    recorder.setAudioQuality(0)
    recorder.setAudioBitrate(192000)
    recorder.setSampleRate(44100)
    recorder.setAudioChannels(2)
    recorder.setFormat("mpegts")
    recorder.startUnsafe()
    recorder
  }

}