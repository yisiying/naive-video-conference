package org.seekloud.theia.pcClient.core.collector

import java.awt.image.BufferedImage
import java.io.{File, OutputStream}
import java.nio.ShortBuffer
import java.util.concurrent.{ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import org.bytedeco.javacv._
import org.bytedeco.opencv.opencv_core.{Mat, Rect, Scalar, Size}
import org.bytedeco.opencv.global.{opencv_imgproc => OpenCVProc}
import org.bytedeco.opencv.global.{opencv_core => OpenCVCore}
import org.seekloud.theia.capture.core.CaptureManager
import org.seekloud.theia.capture.processor.ImageConverter
import org.seekloud.theia.capture.protocol.Messages
import org.seekloud.theia.capture.protocol.Messages._
import org.seekloud.theia.capture.sdk.{DeviceUtil, MediaCapture}
import org.seekloud.theia.pcClient.Boot
import org.seekloud.theia.pcClient.component.WarningDialog
import org.seekloud.theia.pcClient.core.stream.{EncodeActor, LiveManager}
import org.seekloud.theia.pcClient.core.stream.LiveManager.{ChangeMediaOption, EncodeConfig, RecordOption}
import org.seekloud.theia.pcClient.core.RmManager.ImgLayout
import org.seekloud.theia.protocol.ptcl.client2Manager.websocket.AuthProtocol.HostStopPushStream2Client
import org.slf4j.LoggerFactory

import scala.collection.mutable
//import org.bytedeco.javacv.FrameRecorder

import concurrent.duration._
import language.postfixOps

/**
  * User: TangYaruo
  * Date: 2019/9/4
  * Time: 14:04
  */
object   CaptureActor {
//  val canvasf = new CanvasFrame("camera")

//  val cancasD = new CanvasFrame("desktop")

  val bottomSize = new Size(640, 480)
  val topSize = new Size(640/4, 480/4)
  val topMask = new Mat(topSize, OpenCVCore.CV_8UC1, new Scalar(1d))
  val bottomMask = new Mat(bottomSize, OpenCVCore.CV_8UC1, new Scalar(1d))
  val toMat = new ToMat()
  val topResizeMat = new Mat()
  val bottomResizeMat = new Mat()
  val canvas = new Mat(bottomSize, OpenCVCore.CV_8UC3, new Scalar(0, 0, 0, 0))
  val topRoi = canvas(new Rect(0, 0, topSize.width(), topSize.height()))
  val bottomRoi = canvas(new Rect(0, 0, bottomSize.width(), bottomSize.height()))

  val converter = new OpenCVFrameConverter.ToIplImage()


  private val log = LoggerFactory.getLogger(this.getClass)

  private val imageConverter = new ImageConverter
//  private val imageConverter2 = new ImageConverter

//  var bufferedImage = new BufferedImage(640, 360, BufferedImage.TYPE_3BYTE_BGR)
//  val converter1 = new Java2DFrameConverter()
//  val converter2 = new Java2DFrameConverter()
//  val converter3 = new Java2DFrameConverter()

  val frameQueue = new java.util.concurrent.LinkedBlockingDeque[Frame](1)

//  val imgLayout = new ImgLayout(true, true, 100, 100, 100, 100)

//  var encoderParameter :ChangeMediaOption = None

  case class MediaFrame(frame: Option[Frame] = None, sample:Option[ShortBuffer] = None)
  val queue = new java.util.concurrent.LinkedBlockingDeque[MediaFrame]()

  type CaptureCommand = ReplyToCommand

  final case class StartEncode(output: Either[File, OutputStream], url: Option[String] = None) extends CaptureCommand

  final case class StopEncode(encoderType: EncoderType.Value) extends CaptureCommand

  final case class ChangeCaptureMode(mediaSource: Int, cameraPosition: Int, imgLayout: Option[ImgLayout]=None) extends CaptureCommand

  final case object StopCapture extends CaptureCommand

  final case class GetMediaCapture(mediaCapture: MediaCapture) extends CaptureCommand

  final case class PushRtmpStream(url: String) extends CaptureCommand

  final case object StopPushRtmp extends CaptureCommand

  final case object CanNotRecordToBili extends CaptureCommand

  final case object OnEncodeException extends CaptureCommand

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends CaptureCommand

  object EncodeType extends Enumeration{
    val Rtp, File, Rtmp = Value
  }

  object MediaType extends Enumeration{
    val Camera, Desktop, Sound = Value
  }


  /*drawer*/
  sealed trait DrawCommand

  final case class DrawImage(image: Image) extends DrawCommand

  final case class SwitchMode(isJoin: Boolean, reset: () => Unit) extends DrawCommand with CaptureCommand

  final case class ReSet(reset: () => Unit, offOrOn: Boolean) extends DrawCommand

  final case class BottomLayer(bottomLayer: Image) extends DrawCommand

  final case object StopDraw extends DrawCommand with CaptureCommand

  private object ENCODE_RETRY_TIMER_KEY

//  private object Sound_key


  sealed trait RequestCommand

  final case object StartAsking extends RequestCommand

  final case object AskFrame extends RequestCommand

  final case object StopAsk extends RequestCommand

  final case object Camera_Key

  final case object Sound_Key

  final case object Desktop_Key


  def create(frameRate: Int, gc: GraphicsContext, isJoin: Boolean, encodeSettings: LiveManager.EncodeConfig, callBackFunc: Option[() => Unit] = None,parent: ActorRef[LiveManager.LiveCommand]): Behavior[CaptureCommand] =
    Behaviors.setup[CaptureCommand] { ctx =>
      log.info("CaptureActor is starting...")
      Behaviors.withTimers[CaptureCommand] { implicit timer =>
        idle(parent,frameRate, gc, isJoin, encodeSettings, mutable.HashMap[EncodeType.Value, ActorRef[EncodeActor.EncodeCmd]](), mutable.HashMap[MediaType.Value, ActorRef[RequestCommand]](), callBackFunc)
      }
    }

  private def idle(
    parent:ActorRef[LiveManager.LiveCommand],
    frameRate: Int,
    gc: GraphicsContext,
    isJoin: Boolean = false,
    encodeSettings: EncodeConfig,
    encodeMap: mutable.HashMap[EncodeType.Value, ActorRef[EncodeActor.EncodeCmd]],
    requestMap: mutable.HashMap[MediaType.Value, ActorRef[RequestCommand]],
    callBackFunc: Option[() => Unit] = None,
    resetFunc: Option[() => Unit] = None,
    mediaCapture: Option[MediaCapture] = None,
    reqActor: Option[ActorRef[Messages.ReqCommand]] = None,
    loopExecutor: Option[ScheduledThreadPoolExecutor] = None,
    imageLoop: Option[ScheduledFuture[_]] = None,
    drawActor: Option[ActorRef[DrawCommand]] = None,
    rtmpIsLive: Boolean = false,
    rtpIsLive: Boolean = false,
    fileNeed: Boolean = false,
    imgLayout: Option[ImgLayout] = None,
    bottomLayer: Option[Frame] = None
  )(
    implicit timer: TimerScheduler[CaptureCommand]
  ): Behavior[CaptureCommand] =
    Behaviors.receive[CaptureCommand] { (ctx, msg) =>
      msg match {
        case msg: GetMediaCapture =>
          idle(parent,frameRate, gc, isJoin,encodeSettings, encodeMap, requestMap, callBackFunc, resetFunc, Some(msg.mediaCapture),
            reqActor, loopExecutor, imageLoop, drawActor, rtmpIsLive, rtpIsLive, fileNeed, imgLayout, bottomLayer)

        case msg: CaptureStartSuccess =>
          log.info(s"MediaCapture start success!")
          CaptureManager.setLatestFrame()
          val drawActor = ctx.spawn(drawer(gc, isJoin), s"CaptureDrawer-${System.currentTimeMillis()}")
//          val executorCamera = new ScheduledThreadPoolExecutor(1)
//          val askImageLoop = executorCamera.scheduleAtFixedRate(
//            () => {
////              msg.manager ! Messages.AskImage
//              msg.manager ! Messages.AskFrame
//            },
//            0,
//            ((1000.0 / frameRate) * 1000).toLong,
//            TimeUnit.MICROSECONDS
//          )
          if(requestMap.get(MediaType.Camera).isEmpty){
            val requestCamera = ctx.spawn(requestFrame(msg.manager, MediaType.Camera), "askCamera")
            requestCamera ! StartAsking
            requestMap.put(MediaType.Camera, requestCamera)
          }

          if(requestMap.get(MediaType.Sound).isEmpty){
            val requestSound = ctx.spawn(requestFrame(msg.manager, MediaType.Sound), "askSound")
            requestSound ! StartAsking
            requestMap.put(MediaType.Sound, requestSound)
          }

          callBackFunc.foreach(func => func())
//          idle(parent,frameRate, gc, isJoin, encodeSettings, encodeMap, callBackFunc, resetFunc, mediaCapture, Some(msg.manager), Some(executorCamera), Some(askImageLoop), Some(drawActor), rtmpIsLive, rtpIsLive, fileNeed)
          idle(parent,frameRate, gc, isJoin, encodeSettings, encodeMap, requestMap, callBackFunc, resetFunc, mediaCapture, Some(msg.manager),
            loopExecutor, imageLoop, Some(drawActor), rtmpIsLive, rtpIsLive, fileNeed, imgLayout, bottomLayer)

        case msg: CannotAccessSound =>
          log.info(s"Sound unavailable.")
          Behaviors.same

        case msg: CannotAccessImage =>
          log.info(s"Image unavailable.")
          Behaviors.same

        case CaptureStartFailed =>
          log.info(s"Media capture start failed. Review your settings.")
          Behaviors.same

        case ManagerStopped =>
          log.info(s"Capture Manager stopped.")
          if (resetFunc.nonEmpty) {
            resetFunc.foreach(func => func())
            mediaCapture.foreach(_.start())
          }
          idle(parent,frameRate, gc, isJoin, encodeSettings, encodeMap, requestMap, callBackFunc, None, mediaCapture, reqActor,
            loopExecutor, imageLoop, drawActor, rtmpIsLive, rtpIsLive, fileNeed, imgLayout, bottomLayer)

        case StreamCannotBeEncoded =>
          log.info(s"Stream cannot be encoded to mpegts.")
          Behaviors.same

        case CannotSaveToFile =>
          log.info(s"Stream cannot be save to file.")
          Behaviors.same

        case msg: ImageRsp =>
//          drawActor.foreach(_ ! DrawImage(msg.latestImage.image))
//          if(rtmpIsLive){
//            encodeMap.get(EncodeType.Rtmp).foreach(_ ! EncodeActor.SendFrame())
//          }
          Behaviors.same

        case msg:FrameRsp=>
          if(msg.latestFrame.frame != null && msg.latestFrame.frame.image != null){
            if(imgLayout.nonEmpty){//如果是组合形式
              val frame = frameQueue.poll()
              if(frame != null){
                val frameClone = imgCmbn(imgLayout.get, msg.latestFrame.frame, frame)

                if(rtmpIsLive){
                  encodeMap.get(EncodeType.Rtmp).foreach(_ ! EncodeActor.SendFrame(frameClone, msg.latestFrame.ts))
                }

                if(rtpIsLive){
                  encodeMap.get(EncodeType.Rtp).foreach(_ ! EncodeActor.SendFrame(frameClone, msg.latestFrame.ts))
                }
                if(fileNeed){
                  encodeMap.get(EncodeType.File).foreach(_ ! EncodeActor.SendFrame(frameClone, msg.latestFrame.ts))
                }

                val image = imageConverter.convert(frameClone)
                drawActor.foreach(_ ! DrawImage(image))
              }

            }else{
//              if(rtmpIsLive){
//                val frameClone = msg.latestFrame.frame.clone()
//                queue.offer(new MediaFrame(frame = Some(frameClone)))
//              }
              if(rtmpIsLive){
                encodeMap.get(EncodeType.Rtmp).foreach(_ ! EncodeActor.SendFrame(msg.latestFrame.frame.clone(), msg.latestFrame.ts))
              }

              if(rtpIsLive){
                encodeMap.get(EncodeType.Rtp).foreach(_ ! EncodeActor.SendFrame(msg.latestFrame.frame, msg.latestFrame.ts))
              }
              if(fileNeed){
                encodeMap.get(EncodeType.File).foreach(_ ! EncodeActor.SendFrame(msg.latestFrame.frame, msg.latestFrame.ts))
              }
              val image = imageConverter.convert(msg.latestFrame.frame.clone())
              drawActor.foreach(_ ! DrawImage(image))
            }
          }
          Behaviors.same

        case msg: DesktopFrameRsp =>
          if(msg.latestFrame.frame != null && msg.latestFrame.frame.image != null){
            if(imgLayout.isEmpty){//如果只要桌面
              val image = imageConverter.convert(msg.latestFrame.frame)
              drawActor.foreach(_ ! DrawImage(image))
              if(rtmpIsLive){
                encodeMap.get(EncodeType.Rtmp).foreach(_ ! EncodeActor.SendFrame(msg.latestFrame.frame, msg.latestFrame.ts))
              }

              if(rtpIsLive){
                encodeMap.get(EncodeType.Rtp).foreach(_ ! EncodeActor.SendFrame(msg.latestFrame.frame, msg.latestFrame.ts))
              }
            }else{
              frameQueue.offer(msg.latestFrame.frame)
            }
          }
          Behaviors.same

        case msg: SoundRsp => //no need yet
          if(rtmpIsLive){
//            queue.offer(new MediaFrame(sample = Some(msg.latestSound.samples)))
            encodeMap.get(EncodeType.Rtmp).foreach(_ ! EncodeActor.SendSample(msg.latestSound.samples, msg.latestSound.ts))
          }
          if(rtpIsLive){
            encodeMap.get(EncodeType.Rtp).foreach(_ ! EncodeActor.SendSample(msg.latestSound.samples, msg.latestSound.ts))
          }
          if(fileNeed){
            encodeMap.get(EncodeType.File).foreach(_ ! EncodeActor.SendSample(msg.latestSound.samples, msg.latestSound.ts))
          }

          Behaviors.same

        case NoImage =>
//          log.info(s"No images yet, try later.")
          Behaviors.same

        case NoSamples =>
          log.info(s"No sound yet, try later.")
          Behaviors.same

        case NoFrame=>
          //log.info(s"No Frame yet, try later.")
          Behaviors.same

        case msg: StartEncode =>
          msg.output match {
            case Right(outputStream) =>
//              if (reqActor.nonEmpty) {
//                reqActor.foreach(_ ! StartEncodeStream(outputStream))
//              } else {
//                timer.startSingleTimer(ENCODE_RETRY_TIMER_KEY, msg, 500.millis)
//              }
              log.info("start encode")
              val streamEncoder = getEncoderActor(ctx, EncodeType.Rtp, encodeSettings, outputStream = Some(outputStream))
              encodeMap.put(EncodeType.Rtp, streamEncoder)
              idle(parent,frameRate, gc, isJoin, encodeSettings, encodeMap, requestMap, callBackFunc, None, mediaCapture,
                reqActor, loopExecutor, imageLoop, drawActor, rtmpIsLive, rtpIsLive = true, fileNeed, imgLayout, bottomLayer)

            case Left(file) =>
              if (reqActor.nonEmpty) {
                reqActor.foreach(_ ! StartEncodeFile(file))
              } else {
                timer.startSingleTimer(ENCODE_RETRY_TIMER_KEY, msg, 500.millis)
              }
              idle(parent,frameRate, gc, isJoin, encodeSettings, encodeMap, requestMap, callBackFunc, None, mediaCapture,
                reqActor, loopExecutor, imageLoop, drawActor, rtmpIsLive, rtpIsLive, true, imgLayout, bottomLayer)
          }


        case PushRtmpStream(url) =>
//          log.info(s"send url to CaptureManager: ${url}")
//          reqActor.foreach(_ ! RecordToBiliBili(url))
          val encodeActor = getEncoderActor(ctx, EncodeType.Rtmp, encodeSettings, rtmpServer=Some(url))
          encodeMap.put(EncodeType.Rtmp, encodeActor)
          idle(parent,frameRate, gc, isJoin, encodeSettings, encodeMap, requestMap, callBackFunc, None, mediaCapture, reqActor,
            loopExecutor, imageLoop, drawActor, true, rtpIsLive, fileNeed, imgLayout, bottomLayer)

        case StopPushRtmp =>
//          reqActor.foreach(_ ! StopRecordToBiliBili)
          encodeMap.get(EncodeType.Rtmp).foreach(_ ! EncodeActor.StopEncode)
          encodeMap.remove(EncodeType.Rtmp)
          idle(parent,frameRate, gc, isJoin, encodeSettings, encodeMap, requestMap, callBackFunc, None, mediaCapture, reqActor,
            loopExecutor, imageLoop, drawActor, false, rtpIsLive, fileNeed, imgLayout, bottomLayer)


        case msg: StopEncode =>
          msg.encoderType match {
            case EncoderType.STREAM =>
                encodeMap.get(EncodeType.Rtmp).foreach( _ ! EncodeActor.StopEncode)
                encodeMap.get(EncodeType.Rtp).foreach( _ ! EncodeActor.StopEncode)
                encodeMap.remove(EncodeType.Rtmp)
                encodeMap.remove(EncodeType.Rtp)
                idle(parent,frameRate, gc, isJoin, encodeSettings, encodeMap, requestMap, callBackFunc, None, mediaCapture,
                  reqActor, loopExecutor, imageLoop, drawActor, false,false, fileNeed, imgLayout, bottomLayer)
            case EncoderType.FILE =>
                //这个代码在录制自己中不会用到，所以是用在哪里的？
                log.info("msg stop encode used==============")
                encodeMap.get(EncodeType.File).foreach(_ ! EncodeActor.StopEncode)
                encodeMap.remove(EncodeType.File)
                idle(parent,frameRate, gc, isJoin, encodeSettings, encodeMap, requestMap, callBackFunc, None, mediaCapture,
                  reqActor, loopExecutor, imageLoop, drawActor, rtmpIsLive, rtpIsLive, false, imgLayout, bottomLayer)
          }

        case msg: SwitchMode =>
          drawActor.foreach(_ ! msg)
          Behaviors.same

        case msg: ChangeMediaOption =>
          mediaCapture.foreach { m =>
            m.stop()
            val re = msg.re.map(DeviceUtil.parseImgResolution)
            m.setOptions(outputBitrate = msg.bit, frameRate = msg.frameRate, imageWidth = re.map(_._1), imageHeight = re.map(_._2),
              needImage = Some(msg.needImage), needSound = Some(msg.needSound))
            log.debug(s"change media settings: ${m.mediaSettings}")
            val offOrOn = msg.needImage
            drawActor.foreach(_ ! ReSet(msg.reset, offOrOn))
          }
          requestMap.foreach(i => i._2 ! StopAsk)
          requestMap.foreach(i => requestMap.remove(i._1))
          idle(parent,frameRate, gc, isJoin, encodeSettings, encodeMap, requestMap, callBackFunc, Some(msg.reset), mediaCapture,
            reqActor, loopExecutor, imageLoop, drawActor, rtmpIsLive, rtpIsLive, fileNeed, imgLayout, bottomLayer)

        case msg: ChangeCaptureMode =>
          log.info(s"captureActor got msg: $msg")
          if(msg.imgLayout.isEmpty){
            msg.mediaSource match {
              case 0 =>
                if(requestMap.get(MediaType.Desktop).isDefined){
                  requestMap.get(MediaType.Desktop).foreach(_ ! StopAsk)
                  requestMap.remove(MediaType.Desktop)
                }
                if(requestMap.get(MediaType.Camera).isEmpty){
                  val requestCamera = ctx.spawn(requestFrame(reqActor.get, MediaType.Camera), "askCamera")
                  requestCamera ! StartAsking
                  requestMap.put(MediaType.Camera, requestCamera)
                }else{
                  //todo 考虑原来的requestCamera是否正常运行
                }
              case 1 =>
                if(requestMap.get(MediaType.Camera).isDefined){
                  requestMap.get(MediaType.Camera).foreach(_ ! StopAsk)
                  requestMap.remove(MediaType.Camera)
                }
                if(requestMap.get(MediaType.Desktop).isEmpty){
                  val requestDesktop = ctx.spawn(requestFrame(reqActor.get, MediaType.Desktop), "askDesktop")
                  requestDesktop ! StartAsking
                  requestMap.put(MediaType.Desktop, requestDesktop)
                }else{
                  //todo 考虑原来的requestDesktop是否正常运行
                }
            }
          }
          else{
            if(msg.mediaSource == 2){
              if(requestMap.get(MediaType.Desktop).isEmpty){
                val requestDesktop = ctx.spawn(requestFrame(reqActor.get, MediaType.Desktop), "askDesktop")
                requestDesktop ! StartAsking
                requestMap.put(MediaType.Desktop, requestDesktop)
              }else {}//todo 考虑原来的requestDesktop是否正常运行

              if(requestMap.get(MediaType.Camera).isEmpty){
                val requestCamera = ctx.spawn(requestFrame(reqActor.get, MediaType.Camera), "askCamera")
                requestCamera ! StartAsking
                requestMap.put(MediaType.Camera, requestCamera)
              }else {}//todo 考虑原来的requestCamera是否正常运行
            }
          }
          idle(parent,frameRate, gc, isJoin, encodeSettings, encodeMap, requestMap, callBackFunc, resetFunc, mediaCapture,
            reqActor, loopExecutor, imageLoop, drawActor, rtmpIsLive, rtpIsLive, fileNeed, msg.imgLayout, bottomLayer)

        case msg: RecordOption =>
//          reqActor.foreach { req =>
//            if (msg.recordOrNot) {
          //             req ! Messages.StartEncodeFile(new File(msg.path.get))
//            } else {
//              req ! Messages.StopEncodeFile
//            }
//          }
          if(msg.recordOrNot){
            val fileEncoder =  getEncoderActor(ctx, EncodeType.File, encodeSettings, file = Some(new File(msg.path.get)))
            encodeMap.put(EncodeType.File,fileEncoder)
            idle(parent,frameRate, gc, isJoin, encodeSettings, encodeMap, requestMap, callBackFunc, Some(msg.reset), mediaCapture,
              reqActor, loopExecutor, imageLoop, drawActor, rtmpIsLive, rtpIsLive, true, imgLayout, bottomLayer)
          }else{
            encodeMap.get(EncodeType.File).foreach(_ ! EncodeActor.StopEncode)
            encodeMap.remove(EncodeType.File)
            idle(parent,frameRate, gc, isJoin, encodeSettings, encodeMap, requestMap, callBackFunc, Some(msg.reset), mediaCapture,
              reqActor, loopExecutor, imageLoop, drawActor, rtmpIsLive, rtpIsLive, false, imgLayout, bottomLayer)
          }

        case StopCapture =>
          log.info(s"Media capture is stopping...")
          imageLoop.foreach(_.cancel(false))
          loopExecutor.foreach(_.shutdown())
          reqActor.foreach(_ ! StopMediaCapture)
          drawActor.foreach(_ ! StopDraw)
          Behaviors.stopped

        case OnEncodeException =>
            Boot.addToPlatform{
              WarningDialog.initWarningDialog("直播结束")
            }
            parent!LiveManager.StopBili
            Behaviors.same

        case CanNotRecordToBili =>
          Boot.addToPlatform{
            WarningDialog.initWarningDialog("连接到b站失败，请确认b站端是否开始直播，或者重开b站端直播")
            parent!LiveManager.StopBili
          }
          Behaviors.same

        case msg:ChildDead[EncodeActor.EncodeCmd] =>
          log.info(s"${msg.name} dead.")
          Behaviors.same

        case StopDraw=>
          drawActor.foreach(_ ! StopDraw)
          Behaviors.same

        case x =>
          log.warn(s"unknown msg in idle: $x")
          Behaviors.unhandled
      }
    }


  private def drawer(
    gc: GraphicsContext,
    isJoin: Boolean,
    needImage: Boolean = true,
    imgLayout: Option[ImgLayout] = None,
    bottomLayer: Option[Image] = None
  ): Behavior[DrawCommand] =
    Behaviors.receive[DrawCommand] { (ctx, msg) =>
      msg match {
        case msg: DrawImage =>
          val sWidth = gc.getCanvas.getWidth
          val sHeight = gc.getCanvas.getHeight
          if (needImage) {
            if (!isJoin) {
              Boot.addToPlatform {
                gc.drawImage(msg.image, 0.0, 0.0, sWidth, sHeight)
              }
            } else {
              Boot.addToPlatform {
                gc.drawImage(msg.image, 0.0, sHeight / 4, sWidth / 2, sHeight / 2)
              }
            }
          }
          Behaviors.same

        case msg: SwitchMode =>
          log.debug(s"Capture Drawer switch mode.")
          CaptureManager.setLatestFrame()
          Boot.addToPlatform (msg.reset())
          drawer(gc, msg.isJoin, needImage, imgLayout)

        case msg: BottomLayer =>
          drawer(gc, isJoin, needImage, imgLayout, Some(msg.bottomLayer))

        case msg: ReSet =>
          log.info("drawer reset")
          Boot.addToPlatform(msg.reset())
          drawer(gc, isJoin, !msg.offOrOn, imgLayout)

        case StopDraw =>
          log.info(s"Capture Drawer stopped.")
          Behaviors.stopped

        case x =>
          log.warn(s"unknown msg in drawer: $x")
          Behaviors.unhandled
      }
    }


  def getEncoderActor(ctx: ActorContext[CaptureCommand],
                      encodeType: EncodeType.Value,
                      encodeConfig: EncodeConfig,
                      rtmpServer: Option[String] = None,
                      file: Option[File] = None,
                      outputStream: Option[OutputStream] = None): ActorRef[EncodeActor.EncodeCmd] = {
    println(s"encodeConfig: ${encodeConfig.imgWidth} + ${encodeConfig.imgHeight}" )
    val recorder = encodeType match {
      case EncodeType.File if file.nonEmpty =>
        new FFmpegFrameRecorder1(file.get, encodeConfig.imgWidth, encodeConfig.imgHeight)
      case EncodeType.Rtmp if rtmpServer.nonEmpty =>
        new FFmpegFrameRecorder1(rtmpServer.get, encodeConfig.imgWidth, encodeConfig.imgHeight)
      case EncodeType.Rtp if outputStream.nonEmpty =>
        new FFmpegFrameRecorder1(outputStream.get, encodeConfig.imgWidth, encodeConfig.imgHeight)
      case _ =>
        log.error("create recorder error, encode type not exist!")
        null
    }

    val childName = s"encoderActor-${encodeType}"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(EncodeActor.create(ctx.self, encodeType, recorder, encodeConfig, rtmpServer, file, outputStream), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[EncodeActor.EncodeCmd]
  }


  def requestFrame(requestActor: ActorRef[Messages.ReqCommand], frameType: MediaType.Value): Behavior[RequestCommand] = {
    Behaviors.setup[RequestCommand]{ctx =>
      Behaviors.withTimers[RequestCommand]{implicit timer =>
        Behaviors.receiveMessage[RequestCommand]{
          case StartAsking =>
            log.info(s"Request-$frameType started.")
            frameType match {
              case MediaType.Camera =>
                timer.startPeriodicTimer(Camera_Key, AskFrame, ((1000/30)*1000).toLong.micros)
              case MediaType.Sound =>
                timer.startPeriodicTimer(Sound_Key, AskFrame, ((1000/50)*1000).toLong.micros)
              case MediaType.Desktop =>
                timer.startPeriodicTimer(Desktop_Key, AskFrame, ((1000/30)*1000).toLong.micros)
            }
            Behaviors.same

          case AskFrame =>
//            log.info("Request got msg: AskFrame")
            frameType match {
              case MediaType.Camera =>
                requestActor ! Messages.AskFrame
              case MediaType.Sound =>
                requestActor ! Messages.AskSamples
              case MediaType.Desktop =>
                requestActor ! Messages.AskDesktopFrame
            }
            Behaviors.same

          case StopAsk =>
            log.info(s"Request-${frameType} stopped")
            frameType match {
              case MediaType.Desktop =>
                timer.cancel(Desktop_Key)
              case MediaType.Camera =>
                timer.cancel(Camera_Key)
              case MediaType.Sound =>
                timer.cancel(Sound_Key)
            }
            Behaviors.stopped
        }
      }
    }
  }

/*  def imgCombination(imgLayout: ImgLayout, topFrame: Frame, bottomFrame: Frame): Frame = {
    if(bufferedImage.getHeight != bottomFrame.imageHeight || bufferedImage.getWidth != bottomFrame.imageWidth){
      bufferedImage = new BufferedImage(bottomFrame.imageWidth, bottomFrame.imageHeight, BufferedImage.TYPE_3BYTE_BGR)
    }
    val graphic = bufferedImage.getGraphics
    val top = converter1.convert(topFrame.clone())
    val bottom = converter2.convert(bottomFrame.clone())
    graphic.drawImage(bottom, 0, 0, bottom.getWidth, bottom.getHeight, null)
    graphic.drawImage(top, imgLayout.x, imgLayout.y, imgLayout.width, imgLayout.height, null)
    val frame = converter3.convert(bufferedImage)
    frame
  }*/

  def imgCmbn(imgLayout: ImgLayout, topFrame: Frame, bottomFrame: Frame) = {
    val topSize = new Size(imgLayout.width, imgLayout.height)
    val bottomMat = toMat.convert(bottomFrame)
    OpenCVProc.resize(bottomMat, bottomResizeMat, bottomSize)
    bottomResizeMat.copyTo(bottomRoi, bottomMask)
    val topMat = toMat.convert(topFrame)
    OpenCVProc.resize(topMat, topResizeMat, topSize)
    val topMask = new Mat(topSize, OpenCVCore.CV_8UC1, new Scalar(1d))
    val topRoi = canvas(new Rect(0, 0, topSize.width(), topSize.height()))
    topResizeMat.copyTo(topRoi, topMask)
    val convertFrame = converter.convert(canvas)
    convertFrame
  }
}
