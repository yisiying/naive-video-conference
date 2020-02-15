package org.seekloud.theia.faceAnalysis.core

import java.nio.channels.{Channels, Pipe}
import java.nio.{ByteBuffer, ByteOrder, ShortBuffer}
import java.util.concurrent.{ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import javax.sound.sampled._
import org.bytedeco.javacv.{Frame, FrameRecorder, Java2DFrameConverter, JavaFXFrameConverter}
import org.bytedeco.opencv.global.{opencv_imgcodecs, opencv_videoio}
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_videoio.VideoCapture
import org.seekloud.theia.faceAnalysis.BootJFx
import org.seekloud.theia.faceAnalysis.common.{AppSettings, Constants, Pictures, Routes}
import org.slf4j.LoggerFactory
import org.seekloud.theia.faceAnalysis.BootJFx.{blockingDispatcher, rmActor, rtpPushActor}
import EncodeActor._
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javax.imageio.ImageIO
import org.seekloud.theia.faceAnalysis.model.{FaceAnalysis, RenderEngine}
import org.seekloud.theia.protocol.ptcl.CommonInfo.LiveInfo

import concurrent.duration._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable

/**
  * Created by sky
  * Date on 2019/8/20
  * Time at 上午10:02
  * videoCapture/audioCapture
  */
object CaptureActor {
  private val log = LoggerFactory.getLogger(this.getClass)
  private val WEBCAM_DEVICE_INDEX = 0
  private val AUDIO_DEVICE_INDEX = 4
  val frameRate = 30
  val frameDuration: Float = 1000.0f / 30
  val audioChannels = 1
  private var frameCount = 0
  val aiMap = new mutable.HashMap[Byte, Byte].empty
  private var camWidth = 0
  private var camHeight = 0
  private var liveState = false


  //延时buffer
  /**
    * 定时器使用audioCapture中的定时
    * 其他需要定时器如编码可发送消息
    **/
  case class FrameSnapShot(
                            var frameNumber: Int = 0, //最新视频帧   编码视频帧=frameNumber-delay  delay=10
                            var encodeFrameNumber: Int = 0,
                            var aiBuffer: ArrayBuffer[FaceAnalysis.AiInfo] = ArrayBuffer[FaceAnalysis.AiInfo](), //定位数据
                            var mat: Mat = null, //实时采集数据
                            var frame: Frame = null
                          )

  trait Command

  final case class DevicesReady(gc: GraphicsContext) extends Command

  final case class ChangeAi(changeIndex: Byte, changeValue: Byte) extends Command

  final case class StartLive(liveId: String, liveCode: String) extends Command

  final case object PushAuthFailed extends Command

  final case object StopEncode extends Command with DrawCommand with AudioCommand

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  trait VideoCommand

  final case object ReadMat extends VideoCommand

  trait AudioCommand

  final case class Encoder(videoEncoder: ActorRef[EncodeActor.VideoEncodeCommand], audioEncoder: ActorRef[AudioEncodeCommand]) extends Command with AudioCommand

  final case class SetSampleInterval(line: TargetDataLine, audioBytes: Array[Byte]) extends AudioCommand

  final case class Sample(line: TargetDataLine, audioBytes: Array[Byte]) extends AudioCommand

  trait DrawCommand

  final case object DrawFrame extends DrawCommand

  final case object DrawKey

  final case object Count extends DrawCommand

  final case object TimerKey4Count

  final case object TimerKey4Draw

  trait DetectCommand

  final case object TimerKey4Detect

  final case object Detect extends DetectCommand

  /**
    * 控制消息
    **/
  final case object DeviceOn extends Command with VideoCommand with AudioCommand with DrawCommand with DetectCommand

  final case object DeviceOff extends Command with VideoCommand with AudioCommand with DrawCommand with DetectCommand

  private def changeAiOption(msg: ChangeAi): Unit = {
    msg.changeValue match {
      case 0 =>
        aiMap.remove(msg.changeIndex)
        msg.changeIndex match {
          case 3 =>
            RenderEngine.gamePause()
          case _ =>
        }
      case _ =>
        aiMap.put(msg.changeIndex, msg.changeValue)
        msg.changeIndex match {
          case 0 | 1 =>
            FaceAnalysis.setDrawMat(msg.changeIndex, msg.changeValue)
          case 3 =>
            FaceAnalysis.setDrawMat(msg.changeIndex, msg.changeValue)
            RenderEngine.gameGoOn()
          case _ =>
        }
    }
  }

  def create(): Behavior[Command] = Behaviors.setup[Command] { ctx =>
    log.info("create| start..")
    idle("idle|")
  }


  private def idle(
                    logPrefix: String,
                  ): Behavior[Command] = Behaviors.receive[Command] { (ctx, msg) =>
    msg match {
      case msg: DevicesReady =>
        log.info(s"$logPrefix receive deviceOn")
        try {
          val cam = new VideoCapture(WEBCAM_DEVICE_INDEX)
          cam.set(opencv_videoio.CAP_PROP_FRAME_WIDTH, Constants.DefaultPlayer.width)
          cam.set(opencv_videoio.CAP_PROP_FRAME_HEIGHT, Constants.DefaultPlayer.height)
          camWidth = cam.get(opencv_videoio.CAP_PROP_FRAME_WIDTH).toInt
          camHeight = cam.get(opencv_videoio.CAP_PROP_FRAME_HEIGHT).toInt
          log.info(s"$logPrefix device width=${cam.get(opencv_videoio.CAP_PROP_FRAME_WIDTH)} height=${cam.get(opencv_videoio.CAP_PROP_FRAME_HEIGHT)} fps=${cam.get(opencv_videoio.CAP_PROP_FPS)}")

          val frameSnapShot = FrameSnapShot()
          val detectActor = ctx.spawn(detect("detect|", frameSnapShot), "detectActor", blockingDispatcher)
          val drawActor = ctx.spawn(draw("draw|", msg.gc, frameSnapShot), "drawActor", blockingDispatcher)
          val videoActor = ctx.spawn(videoCapture("videoCapture|", cam, frameSnapShot,detectActor,drawActor), "videoActor", blockingDispatcher)
          val audioActor = ctx.spawn(audioCapture("audioCapture|", frameSnapShot), "audioActor", blockingDispatcher)

          ctx.self ! DeviceOn
          rmActor ! RMActor.DeviceReady
          work("work|", detectActor, drawActor, videoActor, audioActor, frameSnapShot)
        } catch {
          case ex: Exception =>
            log.debug(s"camera grabber start error: $ex")
            Behaviors.same
        }

      case msg: ChangeAi =>
        log.info(s"$logPrefix receive ai option $msg")
        changeAiOption(msg)
        Behaviors.same

      case unKnow =>
        log.error(s"$logPrefix receive a unknow $unKnow")
        Behaviors.same
    }
  }

  // act as new state
  private def work(logPrefix: String,
                   detectActor: ActorRef[DetectCommand],
                   drawActor: ActorRef[DrawCommand],
                   videoActor: ActorRef[VideoCommand],
                   audioActor: ActorRef[AudioCommand],
                   frameSnapShot: FrameSnapShot,
                   encodeActor: Option[ActorRef[EncodeActor.EncodeCommand]] = None
                  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case DeviceOn =>
          log.info(s"$logPrefix start media devices.")
          videoActor ! DeviceOn
          detectActor ! DeviceOn
          audioActor ! DeviceOn
          drawActor ! DeviceOn
          Behaviors.same

        case DeviceOff =>
          log.info(s"$logPrefix stop media devices.")
          videoActor ! DeviceOff
          drawActor ! DeviceOff
          detectActor ! DeviceOff
          audioActor ! DeviceOff
          encodeActor.foreach(_ ! EncodeActor.StopEncode)
          idle("idle")

        case StopEncode =>
          liveState = false
          log.info(s"$logPrefix receive StopEncode")
          drawActor ! StopEncode
          audioActor ! StopEncode
          encodeActor.foreach(_ ! EncodeActor.StopEncode)
          work(logPrefix, detectActor, drawActor, videoActor, audioActor, frameSnapShot, None)

        case msg: ChangeAi =>
          log.info(s"$logPrefix receive ai option $msg")
          changeAiOption(msg)
          Behaviors.same

        case msg: StartLive =>
          log.info(s"$logPrefix receive StartLive $msg")
          val pipe = Pipe.open() //ts流通道
          rtpPushActor ! RtpPushActor.AuthInit(LiveInfo(msg.liveId, msg.liveCode), pipe.source(), ctx.self)
          val encodeActor = ctx.spawn(EncodeActor.create("create|", frameSnapShot), "encodeActor", blockingDispatcher)
          encodeActor ! EncodeActor.EncodeInit(Channels.newOutputStream(pipe.sink()), camWidth, camHeight)
          work(logPrefix, detectActor, drawActor, videoActor, audioActor, frameSnapShot, Some(encodeActor))

        case msg: Encoder =>
          log.info(s"$logPrefix receive Encoder")
          audioActor ! msg
          Behaviors.same

        case PushAuthFailed =>
          log.info(s"$logPrefix receive PushAuthFailed")
          //todo:待处理
          Behaviors.same

        case unKnow =>
          log.error(s"$logPrefix receive a unknow $unKnow")
          Behaviors.same
      }
    }

  // act as new actor spawned by the parent.ctx
  private def videoCapture(logPrefix: String,
                           cam: VideoCapture,
                           frameSnapShot: FrameSnapShot,
                           detectActor: ActorRef[DetectCommand],
                           drawActor: ActorRef[DrawCommand]
                          ): Behavior[VideoCommand] =
    Behaviors.receive[VideoCommand] { (ctx, msg) =>
      msg match {
        case DeviceOn =>
          log.info(s"$logPrefix Media camera start.")
          ctx.self ! ReadMat
          Behaviors.same

        case ReadMat =>
          val frame = new Mat
          if (cam.read(frame)) {
            frameSnapShot.mat = frame
            detectActor ! Detect
            drawActor ! DrawFrame
            frameSnapShot.frameNumber += 1
          } else {
            //fixme 此处存在error
            log.error(s"$logPrefix readMat error")
            System.exit(0)
          }

          ctx.self ! ReadMat
          Behaviors.same

        case DeviceOff =>
          log.info(s"$logPrefix Media camera stopped.")
          cam.release()
          Behaviors.stopped

        case unKnow =>
          log.error(s"$logPrefix receive a unknow $unKnow")
          Behaviors.same
      }
    }

  // act as new actor spawned by the parent.ctx
  private def audioCapture(logPrefix: String,
                           frameSnapShot: FrameSnapShot,
                           audioFormat: Option[AudioFormat] = None,
                           targetLine: Option[TargetDataLine] = None,
                           audioLoop: Option[ScheduledFuture[_]] = None,
                           audioExecutor: Option[ScheduledThreadPoolExecutor] = None,
                           videoEncoder: Option[ActorRef[EncodeActor.VideoEncodeCommand]] = None,
                           audioEncoder: Option[ActorRef[EncodeActor.AudioEncodeCommand]] = None
                          ): Behavior[AudioCommand] =
    Behaviors.withTimers[AudioCommand] { timer =>
      Behaviors.receive[AudioCommand] { (ctx, msg) =>
        msg match {
          case msg: Sample =>
            try {
              if (liveState && frameSnapShot.frame != null) {
                videoEncoder.foreach { v =>
                  frameSnapShot.encodeFrameNumber += 1
                  v ! EncodeActor.EncodeImage(frameSnapShot.frame, frameSnapShot.encodeFrameNumber - 1)
                }
              }
              //              log.info(s"$logPrefix videoMapSize:${frameSnapShot.videoMap.size},audioMapSize:${frameSnapShot.audioMap.size}")
              // Read from the line... non-blocking
              val nBytesRead = msg.line.read(msg.audioBytes, 0, msg.line.available)
              // Since we specified 16 bits in the AudioFormat,
              // we need to convert our read byte[] to short[]
              // (see source from FFmpegFrameRecorder.recordSamples for AV_SAMPLE_FMT_S16)
              // Let's initialize our short[] array
              val nSamplesRead = nBytesRead / 2
              val samples = new Array[Short](nSamplesRead)
              // Let's wrap our short[] into a ShortBuffer and
              // pass it to recordSamples
              ByteBuffer.wrap(msg.audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer.get(samples)
              val sBuff = ShortBuffer.wrap(samples, 0, nSamplesRead)
              if (liveState && frameSnapShot.frame != null) {
                audioEncoder.foreach(_ ! EncodeActor.EncodeSamples(audioFormat.get.getFrameRate.toInt, audioFormat.get.getChannels, sBuff))
              }

            } catch {
              case e: FrameRecorder.Exception =>
                e.printStackTrace()
            }
            Behaviors.same

          case DeviceOn =>
            log.info(s"$logPrefix receive DeviceOn")
            /**
              * 设置音频编码器
              * 采样率:44.1k;采样率位数:16位;立体声(stereo);是否签名;true:
              * big-endian字节顺序,false:little-endian字节顺序
              */
            val audioFormat = new AudioFormat(44100.0F, 16, audioChannels, true, false)
            // 通过AudioSystem获取本地音频混合器信息
            val minfoSet: Array[Mixer.Info] = AudioSystem.getMixerInfo
            // 通过AudioSystem获取本地音频混合器
            val mixer: Mixer = AudioSystem.getMixer(minfoSet(AUDIO_DEVICE_INDEX))
            // 通过设置好的音频编解码器获取数据线信息
            val dataLineInfo = new DataLine.Info(classOf[TargetDataLine], audioFormat)

            var targetLineOpt: Option[TargetDataLine] = None
            try {
              // 打开并开始捕获音频
              // 通过line可以获得更多控制权
              // 获取设备：TargetDataLine line
              // =(TargetDataLine)mixer.getLine(dataLineInfo);
              val line = AudioSystem.getLine(dataLineInfo).asInstanceOf[TargetDataLine]
              targetLineOpt = Some(line)
              line.open(audioFormat)
              line.start()
              // 获得当前音频采样率
              val sampleRate = audioFormat.getSampleRate.toInt
              // 获取当前音频通道数量
              val numChannels = audioFormat.getChannels
              // 初始化音频缓冲区(size是音频采样率*通道数)
              val audioBufferSize = sampleRate * numChannels
              val audioBytes = new Array[Byte](audioBufferSize)
              ctx.self ! SetSampleInterval(line, audioBytes)
            } catch {
              case ex: Exception =>
                log.error(s"line start error: $ex")
            }
            audioCapture(logPrefix, frameSnapShot, Some(audioFormat), targetLineOpt, audioLoop, audioExecutor, videoEncoder, audioEncoder)

          case msg: SetSampleInterval =>
            //这里是设置了定时器
            log.info(s"$logPrefix receive SetSampleInterval $msg  ")
            var aExecutor: Option[ScheduledThreadPoolExecutor] = None
            var aLoop: Option[ScheduledFuture[_]] = None
            //              log.debug(s"video frameRate: $frameRate")
            val audioExecutor = new ScheduledThreadPoolExecutor(1)
            aExecutor = Some(audioExecutor)
            val audioLoop =
              audioExecutor.scheduleAtFixedRate(
                () => {
                  ctx.self ! Sample(msg.line, msg.audioBytes)
                },
                0,
                (frameDuration * 1000).toLong,
                TimeUnit.MICROSECONDS)
            aLoop = Some(audioLoop)
            audioCapture(logPrefix, frameSnapShot, audioFormat, targetLine, aLoop, aExecutor, videoEncoder, audioEncoder)

          case msg: Encoder =>
            log.info(s"$logPrefix receive AudioEncoder")
            liveState = true
            frameSnapShot.encodeFrameNumber = 0
            audioCapture(logPrefix, frameSnapShot, audioFormat, targetLine, audioLoop, audioExecutor, Some(msg.videoEncoder), Some(msg.audioEncoder))

          case DeviceOff =>
            log.info(s"$logPrefix receive DeviceOff ")
            audioLoop.foreach(_.cancel(false))
            audioExecutor.foreach(_.shutdown())
            targetLine.foreach { line =>
              line.stop()
              line.flush()
              line.close()
            }
            videoEncoder.foreach(_ ! EncodeActor.StopEncode)
            audioEncoder.foreach(_ ! EncodeActor.StopEncode)
            Behaviors.stopped

          case StopEncode =>
            log.info(s"$logPrefix receive StopEncode ")
            videoEncoder.foreach(_ ! EncodeActor.StopEncode)
            audioEncoder.foreach(_ ! EncodeActor.StopEncode)
            audioCapture(logPrefix, frameSnapShot, audioFormat, targetLine, audioLoop, audioExecutor, None, None)

          case unKnown =>
            log.error(s"$logPrefix receive a unknown $unKnown")
            Behaviors.same
        }
      }
    }


  private val imageConverter = new JavaFXFrameConverter()

  // act as new actor spawned by the parent.ctx
  private def draw(
                    logPrefix: String,
                    gc: GraphicsContext,
                    frameSnapShot: FrameSnapShot
                  ): Behavior[DrawCommand] =
    Behaviors.withTimers[DrawCommand] { timer =>
      Behaviors.receive[DrawCommand] { (ctx, msg) =>
        msg match {
          case DrawFrame => //渲染特效、渲染画面
            if (frameSnapShot.mat != null) {
              val t1 = System.currentTimeMillis()
              frameCount += 1

              val frame = FaceAnalysis.draw(frameSnapShot.mat, frameSnapShot.aiBuffer, aiMap.contains(3))

              frameSnapShot.frame = frame
              val image = imageConverter.convert(frame)
              BootJFx.addToPlatform {
                val sWidth = gc.getCanvas.getWidth
                val sHeight = gc.getCanvas.getHeight
                gc.drawImage(image, 0.0, 0.0, sWidth, sHeight)
              }
              log.debug(s"$logPrefix draw use: ${System.currentTimeMillis() - t1}")
            }
            Behaviors.same

          case Count =>
            log.info(s"$logPrefix frameRate = $frameCount")
            frameCount = 0
            Behaviors.same

          case DeviceOn =>
            log.info(s"$logPrefix start")
            //            ctx.self ! DrawFrame
            timer.startPeriodicTimer(TimerKey4Count, Count, 1.seconds)
            //            timer.startPeriodicTimer(TimerKey4Draw, DrawFrame, frameDuration.millis)
            Behaviors.same

          case DeviceOff =>
            log.info(s"$logPrefix Media drawer stopped.")
            timer.cancel(TimerKey4Count)
            val backImg = new Image("img/background.jpg")
            BootJFx.addToPlatform {
              val sWidth = gc.getCanvas.getWidth
              val sHeight = gc.getCanvas.getHeight
              gc.drawImage(backImg, 0.0, 0.0, sWidth, sHeight)
            }
            Behaviors.stopped

          case StopEncode =>
            log.info(s"$logPrefix receive StopEncode ")
            draw(logPrefix, gc, frameSnapShot)

          case unKnown =>
            log.error(s"$logPrefix receive a unknown $unKnown")
            Behaviors.same
        }
      }
    }

  private def detect(
                      logPrefix: String,
                      frameSnapShot: FrameSnapShot
                    ): Behavior[DetectCommand] =
    Behaviors.withTimers[DetectCommand] { timer =>
      Behaviors.receive[DetectCommand] { (ctx, msg) =>
        msg match {
          case Detect =>
            if (frameSnapShot.mat != null) {
              if (aiMap.isEmpty) {
                frameSnapShot.aiBuffer.clear()
              } else {
                val t1 = System.currentTimeMillis()
                //缓存根据实时采集mat分析得到的ai点
                val aiBuffer = new ArrayBuffer[FaceAnalysis.AiInfo]()
                FaceAnalysis.find64(frameSnapShot.mat).foreach { f =>
                  if (aiMap.contains(3)) {
                    FaceAnalysis.detectModel(f.get(0), aiBuffer)
                  } else {
                    if (aiMap.get(0).nonEmpty) {
                      FaceAnalysis.detectGlass(f, aiBuffer)
                    }
                    if (aiMap.get(1).nonEmpty) {
                      FaceAnalysis.detectBeard(f, aiBuffer)
                    }
                    if (aiMap.get(2).nonEmpty) {
                      FaceAnalysis.detectPoint(f, aiBuffer)
                    }
                  }
                  //remind 释放内存
                  f.clear()
                  f.close()
                }
                frameSnapShot.aiBuffer = aiBuffer
                log.debug(s"$logPrefix detect use ${System.currentTimeMillis() - t1}")
              }
            }
            Behaviors.same

          case DeviceOn =>
            log.info(s"$logPrefix start")
            //            timer.startPeriodicTimer(TimerKey4Detect, Detect, frameDuration.millis)
            Behaviors.same

          case DeviceOff =>
            log.info(s"$logPrefix stop")
            Behaviors.stopped

          case unKnown =>
            log.error(s"$logPrefix receive a unknown $unKnown")
            Behaviors.same
        }
      }
    }

}