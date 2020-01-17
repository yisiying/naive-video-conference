package org.seekloud.theia.webrtcServer.core

import java.io.OutputStream

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.{FFmpegFrameRecorder, Frame}
import org.bytedeco.opencv.opencv_core.Buffer
import org.seekloud.theia.webrtcServer.ptcl.MediaInfo
import org.slf4j.LoggerFactory
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
  * Created by sky
  * Date on 2019/7/17
  * Time at 16:29
  */
object RtpRecordActor {
  private val log = LoggerFactory.getLogger(this.getClass)

  case class SignalInfo(
                         var t: Boolean,
                         sTs: Long
                       )

  trait Command

  case class InitRtc2Rtp(info: MediaInfo, outputStream: OutputStream, replyTo: ActorRef[(ActorRef[VideoCommand], ActorRef[AudioCommand])]) extends Command

  case class InitRtp2Rtc(info: MediaInfo, replyTo: ActorRef[(ActorRef[VideoCommand], ActorRef[AudioCommand])])

  trait VideoCommand

  trait AudioCommand

  trait EncodeCommand

  case object FrameTimeKey

  case object FrameTimeOut extends VideoCommand

  case class NextFrame(f: Frame) extends VideoCommand with AudioCommand with EncodeCommand

  case class NextPacket(p: AVPacket) extends EncodeCommand

  def create(): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      Behaviors.receiveMessage[Command] {
        case msg: InitRtc2Rtp =>
          val recorder = new FFmpegFrameRecorder(msg.outputStream, msg.info.imageWidth, msg.info.imageHeight, msg.info.audioChannels)
          //          val recorder = new FFmpegFrameRecorder(s"test_${System.currentTimeMillis()}.ts", msg.info.imageWidth, msg.info.imageHeight, msg.info.audioChannels)
          recorder.setOption("fflags", "nobuffer")
          //    recorder.setOption("tune", "zerolatency")

          recorder.setOption("-vcodec", "h264")
          recorder.setOption("-acodec", "aac")


          //    recorder.setInterleaved(true)
          //    recorder.setVideoOption("preset", "ultrafast")
          recorder.setFrameRate(25)
          //    recorder.setVideoBitrate(info.videoBitrate)
          //    recorder.setGopSize(50)
          //    recorder.setVideoCodec(info.videoCodec)
          recorder.setMaxBFrames(0)
          //    recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC)
          recorder.setFormat("mpegts")
          recorder.setTimestamp(0l)
          recorder.startUnsafe()
          val tsInfo = SignalInfo(false, System.currentTimeMillis())
          val videoEncoder = ctx.spawn(encodeVideoState(msg.info, tsInfo, recorder), "videoEncoder")
          val audioEncoder = ctx.spawn(encodeAudioState(msg.info, tsInfo, recorder), "audioEncoder")
          msg.replyTo ! (videoEncoder, audioEncoder)
          //          val encoder = ctx.spawn(encodeState(msg.info, msg.outputStream), "encoder")
          //          msg.replyTo ! encoder
          Behaviors.same

        case msg: InitRtp2Rtc =>
          val videoRecorder = ctx.spawn(rtpVideoState("", msg.info), "videoRecorder")
          val audioRecorder = ctx.spawn(rtpAudioState("", msg.info), "audioRecorder")
          msg.replyTo ! (videoRecorder, audioRecorder)
          Behaviors.same

        case unKnow =>
          log.warn(s"recordActor got unknown msg: $unKnow")
          Behavior.same
      }
    }
  }

  private def rtpVideoState(
                             url: String,
                             info: MediaInfo
                           ): Behavior[VideoCommand] =
    Behaviors.setup[VideoCommand] { ctx =>
      log.info("rtpVideoState start init")
      val recorder = new FFmpegFrameRecorder(url, info.imageWidth, info.imageHeight, 0)
      recorder.setOption("protocol_whitelist", "file,rtp,udp")
      //延时关键
      recorder.setOption("tune", "zerolatency")
      //            recorder.setVideoOption("fflags","nobuffer")
      //            recorder.setVideoOption("analyzeduration","0")
      recorder.setGopSize(10)
      recorder.setFrameRate(info.frameRate)
      recorder.setVideoCodec(info.videoCodec)
      recorder.setVideoBitrate(info.videoBitrate)
      Try(recorder.start()) match {
        case Success(_) =>
          log.info(s"video start record.")
          Behaviors.receiveMessage[VideoCommand] {
            case m: NextFrame =>
              try {
                recorder.recordImage(m.f.imageWidth, m.f.imageHeight, m.f.imageDepth, m.f.imageChannels, m.f.imageStride, info.pixelFormat, m.f.image: _*)
              } catch {
                case e: Exception =>
                  log.error("video " + e.getMessage)
              }
              Behaviors.same
            case unKnow =>
              log.warn(s"video got unknown msg: $unKnow")
              Behavior.same
          }
        case Failure(e) =>
          log.info("video record failure")
          log.error(e.getMessage)
          Behaviors.stopped
      }
    }

  private def rtpAudioState(url: String, info: MediaInfo): Behavior[AudioCommand] =
    Behaviors.setup[AudioCommand] { ctx =>
      log.info("rtpAudioState start init")
      val recorder = new FFmpegFrameRecorder(url, info.imageWidth, info.imageHeight, info.audioChannels)
      recorder.setAudioBitrate(info.audioBitrate)
      recorder.setSampleFormat(info.sampleFormat)
      recorder.setSampleRate(info.sampleRate)
      Try(recorder.start()) match {
        case Success(_) =>
          log.info(s"audio start record.")
          Behaviors.receiveMessage[AudioCommand] {
            case m: NextFrame =>
              try {
                recorder.recordSamples(m.f.sampleRate, m.f.audioChannels, m.f.samples: _*)
              } catch {
                case e: Exception =>
                  log.error("audio " + e.getMessage)
              }
              Behaviors.same
            case unKnow =>
              log.warn(s"audio got unknown msg: $unKnow")
              Behavior.same
          }
        case Failure(e) =>
          log.info("audio record failure")
          log.error(e.getMessage)
          Behaviors.stopped
      }
    }


  private def encodeVideoState(info: MediaInfo, tsInfo: SignalInfo, recorder: FFmpegFrameRecorder) = Behaviors.setup[VideoCommand] { ctx =>
    Behaviors.withTimers[VideoCommand] { timer =>
      var image: Frame = null
      timer.startPeriodicTimer(FrameTimeKey, FrameTimeOut, 40.millis)
      Behaviors.receiveMessage[VideoCommand] {
        case m: NextFrame =>
          image = m.f
          Behaviors.same

        case FrameTimeOut =>
          try {
            try {
              if (image != null) {
                if (!tsInfo.t) {
                  tsInfo.t = true
                }
                recorder.recordImage(image.imageWidth, image.imageHeight, image.imageDepth, image.imageChannels, image.imageStride, info.pixelFormat, image.image: _*)
              }
            } catch {
              case e: Exception =>
                //删除超时的videoPacket？？
                tsInfo.t = false
                log.error(s"video ${recorder.getTimestamp} " + e.getMessage)
            }
          } catch {
            case e: Exception =>
              log.error("encode video " + e.getMessage)
          }
          Behaviors.same

        case unKnow =>
          log.warn(s"encode video got unknown msg: $unKnow")
          Behavior.same
      }
    }
  }

  private def encodeAudioState(info: MediaInfo, tsInfo: SignalInfo, recorder: FFmpegFrameRecorder) = Behaviors.setup[AudioCommand] { ctx =>
    Behaviors.receiveMessage[AudioCommand] {
      case m: NextFrame =>
        try {
          if (tsInfo.t) {
            recorder.recordSamples(m.f.sampleRate, m.f.audioChannels, m.f.samples: _*)
          }
        } catch {
          case e: Exception =>
            log.error("encode audio " + e.getMessage)
        }
        Behaviors.same
      case unKnow =>
        log.warn(s"encode audio got unknown msg: $unKnow")
        Behavior.same
    }
  }

  private def encodeState(info: MediaInfo, outputStream: OutputStream) = Behaviors.setup[EncodeCommand] { ctx =>
    //    val recorder = new FFmpegFrameRecorder(outputStream, info.imageWidth, info.imageHeight, info.audioChannels)
    val recorder = new FFmpegFrameRecorder("test.ts", info.imageWidth, info.imageHeight, info.audioChannels)
    recorder.setOption("fflags", "nobuffer")
    //    recorder.setOption("tune", "zerolatency")

    recorder.setOption("-vcodec", "h264")
    recorder.setOption("-acodec", "aac")


    //    recorder.setInterleaved(true)
    //    recorder.setVideoOption("preset", "ultrafast")
    //    recorder.setFrameRate(30)
    //    recorder.setVideoBitrate(info.videoBitrate)
    //    recorder.setGopSize(50)
    //    recorder.setVideoCodec(info.videoCodec)
    //    recorder.setMaxBFrames(0)
    //    recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC)
    recorder.setFormat("mpegts")
    recorder.setTimestamp(0l)

    Try(recorder.start()) match {
      case Success(_) =>
        log.info(s"encode start record.")
        Behaviors.receiveMessage[EncodeCommand] {
          case m: NextFrame =>
            try {
              log.info(s"reset ${recorder.getTimestamp} ${
                if (m.f.image != null) "video"
                else "audio"
              } " +
                s"${m.f.timestamp}")
              recorder.record(m.f)
            } catch {
              case e: Exception =>
                log.error("encode " + e.getMessage)
            }
            Behaviors.same

          case unKnow =>
            log.warn(s"encode got unknown msg: $unKnow")
            Behavior.same
        }
      case Failure(e) =>
        log.info("encode record failure")
        log.error(e.getMessage)
        Behaviors.stopped
    }
  }
}
