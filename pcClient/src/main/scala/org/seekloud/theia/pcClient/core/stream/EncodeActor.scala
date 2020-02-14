package org.seekloud.theia.pcClient.core.stream

import java.io.{File, OutputStream}
import java.nio.ShortBuffer

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import org.seekloud.theia.pcClient.core.collector.CaptureActor.queue
import org.bytedeco.javacv.{FFmpegFrameRecorder, Frame, FFmpegFrameRecorder1}
import org.slf4j.LoggerFactory
import org.seekloud.theia.pcClient.core.collector.CaptureActor
import org.seekloud.theia.pcClient.core.collector.CaptureActor.EncodeType
import org.seekloud.theia.pcClient.core.stream.LiveManager.EncodeConfig

import scala.concurrent.Future
import scala.util.{Failure, Success}
import org.seekloud.theia.capture.sdk.MediaCapture.executor

import concurrent.duration._
/**
  * @user: wanruolong
  * @date: 2019/12/3 21:31
  *
  */
object EncodeActor {

  val log = LoggerFactory.getLogger(this.getClass)

  sealed trait EncodeCmd

  final case object StartEncodeSuccess extends EncodeCmd

  final case object StartEncodeFailure extends EncodeCmd

  final case class SendFrame(frame:Frame, ts: Long) extends EncodeCmd

  final case class SendSample(samples: ShortBuffer, ts: Long) extends EncodeCmd

  final case object StopEncode extends EncodeCmd

  final case object Record extends EncodeCmd

  object ENCODE_START_KEY


  def create(parent: ActorRef[CaptureActor.CaptureCommand],
             encodeType: EncodeType.Value,
             encoder: FFmpegFrameRecorder1,
             encodeConfig: EncodeConfig,
             rtmpServer: Option[String] = None,
             file: Option[File] = None,
             outputStream: Option[OutputStream] = None): Behavior[EncodeCmd] =
    Behaviors.setup[EncodeCmd]{ ctx =>
      Behaviors.withTimers[EncodeCmd]{implicit timer =>
        val format = if(encodeType==EncodeType.Rtp)"mpegts"else if(encodeType == EncodeType.Rtmp) "flv" else "flv"

        encoder.setFormat(format)
        encoder.setInterleaved(true)
        encoder.setGopSize(60)
        encoder.setMaxBFrames(0)

        /*video*/
        encoder.setVideoOption("tune", "zerolatency")
        encoder.setVideoOption("preset", "ultrafast")
        encoder.setVideoOption("crf", "23")
        //    encoder.setVideoOption("keyint", "1")
        encoder.setVideoBitrate(encodeConfig.videoBitRate)
        encoder.setVideoCodec(encodeConfig.videoCodec)
        encoder.setFrameRate(encodeConfig.frameRate)

        /*audio*/
        encoder.setAudioOption("crf", "0")
        encoder.setAudioQuality(0)
        encoder.setAudioBitrate(192000)
        encoder.setSampleRate(44100)
        encoder.setAudioChannels(encodeConfig.channels)
        encoder.setAudioCodec(encodeConfig.audioCodec)

        Future {
          log.info(s"Encoder-$encodeType is starting...")
          encoder.startUnsafe()

        }.onComplete {
          case Success(_) =>
            log.info(s"Encoder-$encodeType started.")
            ctx.self ! StartEncodeSuccess
          case Failure(e) =>
//            ctx.self ! StartEncodeFailure
//            log.debug("encoder begin failure" + e)
            encodeType match {
              case EncodeType.Rtp =>
                log.info(s"rtpEncoder start failed: $e")

              case EncodeType.Rtmp =>
                ctx.self ! StartEncodeFailure
                log.info(s"rtmpEncoder start failed: $e")
//                parent ! CaptureActor.CanNotRecordToBili
              case EncodeType.File =>
                log.info(s"fileEncoder start failed: $e")
            }

            }
        work(parent, encodeType, encoder, encodeConfig, rtmpServer, file, outputStream)
      }
    }

  def work(parent: ActorRef[CaptureActor.CaptureCommand],
           encodeType: EncodeType.Value,
           encoder: FFmpegFrameRecorder1,
           encoderConfig: EncodeConfig,
           rtmpServer: Option[String] = None,
           file: Option[File] = None,
           outputStream: Option[OutputStream] = None,
           ensure:Option[Boolean]= Some(false),
           startTime: Long = 0
           )(
          implicit timer: TimerScheduler[EncodeCmd]
  ): Behavior[EncodeCmd] = {
    Behaviors.receive[EncodeCmd]{ (ctx, msg) =>
      msg match {
        case StartEncodeSuccess =>
//          ctx.self ! Record
          work(parent, encodeType, encoder, encoderConfig, rtmpServer, file, outputStream, Some(true))

        case StartEncodeFailure =>
          println(encodeType)
//          val encoderNew = encodeType match {
//            case EncodeType.Rtmp => new FFmpegFrameRecorder(rtmpServer.get, encoderConfig.imgWidth, encoderConfig.imgHeight)
//          }
          val encoderNew = new FFmpegFrameRecorder(rtmpServer.get, encoderConfig.imgWidth, encoderConfig.imgHeight)
          try{
            log.info(s"Encoder-$encodeType is starting...")
            encoderNew.startUnsafe()
            log.info(s"Encoder-$encodeType started.")
            ctx.self ! StartEncodeSuccess
          }catch {
            case e: Exception =>
              log.info(s"Encoder-$encodeType start failed.")
              timer.startSingleTimer(ENCODE_START_KEY, StartEncodeFailure, 3.seconds)
//            case Failure(e) =>
//              timer.startSingleTimer(ENCODE_START_KEY, StartEncodeFailure, 3.seconds)
//              log.debug("encoder begin failure" + e)
          }

          Behaviors.same

        case msg: SendFrame =>
          val ts = if(startTime == 0) System.nanoTime() else startTime
          val videoTs = System.nanoTime() - ts
          if(ensure.get){
            try{
              //              encoder.setTimestamp(startTime * ((1000/encoderConfig.frameRate)*1000).toLong)

//              if(videoTs/1000>encoder.getTimestamp){
//                println(s"timeInterv：${System.nanoTime()/1000 - msg.ts/1000}")
//                println(s"${videoTs/1000} -> ${encoder.getTimestamp} = ${videoTs/1000-encoder.getTimestamp}=====:number${encoder.getFrameNumber}")
//                encoder.setTimestamp(videoTs/1000)
//              }

              encoder.record(msg.frame)

            }catch{
              case ex:Exception=>
                log.error(s"[$encodeType] encode image frame error: $ex")
                if(ex.getMessage.startsWith("av_interleaved_write_frame() error")){
//                  parent! CaptureActor.OnEncodeException
                  ctx.self ! StopEncode
                }
            }
          }
          work(parent, encodeType, encoder, encoderConfig, rtmpServer, file, outputStream, ensure, ts)
//          Behaviors.same

        case msg:SendSample =>
          if(ensure.get){
            try{
//              println(s"audio:${System.currentTimeMillis()}")
              //              println(s"audio:${msg.ts}")
              encoder.recordSamples(encoderConfig.sampleRate.toInt, encoderConfig.channels, msg.samples)
            }catch{
              case ex:Exception=>
                log.error(s"[$encodeType] encode audio frame error: $ex")
                }
            }
          Behaviors.same

        case StopEncode =>
          try {
            encoder.releaseUnsafe()
            log.info(s"release encode resources.")
          } catch {
            case ex: Exception =>
              log.error(s"release encode error: $ex")
              //ex.printStackTrace()
          }
          Behaviors.stopped

/*        case Record =>

            val frame = queue.poll()
            if(frame != null){
              if(frame.frame.isDefined && frame.frame.get.image != null){
                encoder.setTimestamp(frameNum * ((1000/encoderConfig.frameRate)*1000).toLong)
                encoder.record(frame.frame.get)
                ctx.self ! Record
                work(parent, encodeType, encoder, encoderConfig, rtmpServer, file, outputStream, ensure, frameNum+1)

              }
              else{
                ctx.self ! Record
                Behaviors.same
              }
              if(frame.sample.isDefined){
                encoder.recordSamples(44100, 2, frame.sample.get)
                ctx.self ! Record
                Behaviors.same
              }else{
                ctx.self ! Record
                Behaviors.same
              }
            } else{
              ctx.self ! Record
              Behaviors.same
            }*/

        case x =>
          log.info(s"rec unknown msg: $x")
          Behaviors.unhandled
      }
    }
  }


}
