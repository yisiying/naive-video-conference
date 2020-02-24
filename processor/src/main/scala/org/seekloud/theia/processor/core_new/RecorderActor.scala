package org.seekloud.theia.processor.core_new

import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.{File, FileOutputStream, OutputStream}
import java.nio.{ByteBuffer, ShortBuffer}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import org.bytedeco.ffmpeg.global.{avcodec, avutil}
import org.bytedeco.javacv.{FFmpegFrameFilter /*, FFmpegFrameRecorder*/ , Frame, Java2DFrameConverter}
import org.bytedeco.javacv.FFmpegFrameRecorder1
import org.seekloud.theia.processor.Boot.roomManager
import org.seekloud.theia.processor.common.AppSettings.{addTs, bitRate, debugPath, isDebug}
import org.slf4j.LoggerFactory
import org.seekloud.theia.processor.utils.TimeUtil
import org.seekloud.theia.processor.common.AppSettings
import scala.collection.mutable
import org.seekloud.theia.processor.common.Constants.Part


import scala.concurrent.duration._


/**
  * Created by sky
  * Date on 2019/10/22
  * Time at 下午2:30
  *
  * actor由RoomActor创建
  * 编码线程 stream数据传入pipe
  * 合并连线线程
  */
object RecorderActor {

  var audioChannels = 2 //todo 待议
  val sampleFormat = 1 //todo 待议
  var frameRate = 30
  private var hostChannel: Int = _
  private var hostSampleRate: Int = _

  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class UpdateRoomInfo(roomId: Long, layout: Int) extends Command

  case class UpdateClientList(clientLiveId: String, inOrOut: Int) extends Command

  case class Init(num: Int) extends Command

  case object RestartRecord extends Command

  case object StopRecorder extends Command

  case object CloseRecorder extends Command

  case class NewFrame(liveId: String, frame: Frame) extends Command

  case class UpdateRecorder(channel: Int, sampleRate: Int, frameRate: Double, width: Int, height: Int, liveId: String) extends Command

  case object TimerKey4Close

  sealed trait VideoCommand

  case class TimeOut(msg: String) extends Command

  case class Image4Host(frame: Frame) extends VideoCommand

  case class Image4Client(liveId: String, frame: Frame) extends VideoCommand

  case class SetLayout(layout: Int) extends VideoCommand

  case class NewRecord4Ts(recorder4ts: FFmpegFrameRecorder1) extends VideoCommand

  case class RemoveClient(liveId: String) extends VideoCommand

  case object Close extends VideoCommand

  case class Image(var frame: Frame = null)

  private val emptyAudio = ShortBuffer.allocate(1024 * 2)
  private val emptyAudio4one = ShortBuffer.allocate(1152)


  def create(roomId: Long, hostLiveId: String, clientLiveIdMap: mutable.Map[String, Int], roomLiveId: String, layout: Int): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"recorderActor start----")
          avutil.av_log_set_level(-8)
          val recorder4ts = new FFmpegFrameRecorder1(s"${AppSettings.srsServer}$roomLiveId", 640, 480, audioChannels)
          log.info(s"recorder开始推流到：${AppSettings.srsServer}$roomLiveId")
          recorder4ts.setFrameRate(frameRate)
          recorder4ts.setVideoBitrate(bitRate)
          recorder4ts.setVideoCodec(avcodec.AV_CODEC_ID_H264)
          recorder4ts.setAudioCodec(avcodec.AV_CODEC_ID_AAC)
          recorder4ts.setMaxBFrames(0)
          recorder4ts.setFormat("flv")
          try {
            recorder4ts.startUnsafe()
          } catch {
            case e: Exception =>
              log.error(s" recorder meet error when start:$e")
          }
          roomManager ! RoomManager.RecorderRef(roomId, ctx.self) //fixme 取消注释
          ctx.self ! Init(1)
          single(roomId, hostLiveId, clientLiveIdMap, layout, recorder4ts, null, null, (0, 0))
      }
    }
  }

  def single(roomId: Long, hostLiveId: String, clientLiveIdMap: mutable.Map[String, Int], layout: Int,
             recorder4ts: FFmpegFrameRecorder1,
             ffFilter: FFmpegFrameFilter,
             drawer: ActorRef[VideoCommand],
             canvasSize: (Int, Int))(implicit timer: TimerScheduler[Command],
                                     stashBuffer: StashBuffer[Command]): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case Init(num) =>
          if (ffFilter != null) {
            ffFilter.close()
          }
          val n = math.max(num, 2)
          val s = getInitString(n)
          val ffFilterN = new FFmpegFrameFilter(s"$s amix=inputs=$n:duration=longest:dropout_transition=3:weights=1 1[a]", audioChannels)
          ffFilterN.setAudioChannels(n)
          ffFilterN.setSampleFormat(sampleFormat)
          ffFilterN.setAudioInputs(n)
          ffFilterN.start()
          single(roomId, hostLiveId, clientLiveIdMap, layout, recorder4ts, ffFilterN, drawer, canvasSize)

        case UpdateRecorder(channel, sampleRate, f, width, height, liveId) =>
          if (liveId == hostLiveId) {
            log.info(s"$roomId updateRecorder channel:$channel, sampleRate:$sampleRate, frameRate:$f, width:$width, height:$height")
            hostChannel = channel
            hostSampleRate = sampleRate
            recorder4ts.setFrameRate(f)
            recorder4ts.setAudioChannels(channel)
            recorder4ts.setSampleRate(sampleRate)
            ffFilter.setAudioChannels(channel) //todo:channle数需要改吗
            ffFilter.setSampleRate(sampleRate)
            recorder4ts.setImageWidth(width)
            recorder4ts.setImageHeight(height)
            single(roomId, hostLiveId, clientLiveIdMap, layout, recorder4ts, ffFilter, drawer, (640, 480))
          } else {
            Behaviors.same
          }

        case UpdateClientList(clientLiveId, inOrOut) =>
          val num = clientLiveIdMap.size
          if (inOrOut == Part.in) {
            clientLiveIdMap.put(clientLiveId, num + 1)
            roomManager ! RoomManager.RecorderRef(roomId, ctx.self)
          } else {
            clientLiveIdMap.get(clientLiveId) match {
              case Some(index) =>
                clientLiveIdMap.remove(clientLiveId)
                val m = clientLiveIdMap.filter(_._2 > index)
                m.foreach { v =>
                  clientLiveIdMap.update(v._1, v._2 - 1)
                }
              case None =>
                log.info(s"clientLiveId map not contain liveId: $clientLiveId")
            }
          }
          ctx.self ! Init(clientLiveIdMap.size + 1)
          //          single(roomId, hostLiveId, clientLiveIdMap, layout, recorder4ts, ffFilter, drawer, canvasSize)
          Behaviors.same

        case NewFrame(liveId, frame) =>
          if (liveId == hostLiveId) {
            recorder4ts.record(frame)
            Behaviors.same
          } else {
            val canvas = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR)
            val drawer = ctx.spawn(draw(canvas, canvas.getGraphics, List[(String, Image)](), recorder4ts,
              new Java2DFrameConverter(), mutable.Map(liveId -> new Java2DFrameConverter()), new Java2DFrameConverter, layout, "defaultImg.jpg", roomId, (640, 480)), s"drawer_$roomId")
            ctx.self ! NewFrame(liveId, frame)
            work(roomId, hostLiveId, clientLiveIdMap, layout, recorder4ts, ffFilter, drawer, canvasSize)
          }

        case CloseRecorder =>
          try {
            ffFilter.close()
            if (drawer != null) {
              drawer ! Close
            }
          } catch {
            case e: Exception =>
              log.error(s"$roomId recorder close error ---: $e")
          }
          Behaviors.stopped

        case StopRecorder =>
          timer.startSingleTimer(TimerKey4Close, CloseRecorder, 1.seconds)
          Behaviors.same
      }
    }
  }

  def work(roomId: Long, hostLiveId: String, clientLiveIdMap: mutable.Map[String, Int], layout: Int,
           recorder4ts: FFmpegFrameRecorder1,
           ffFilter: FFmpegFrameFilter,
           drawer: ActorRef[VideoCommand],
           canvasSize: (Int, Int))
          (implicit timer: TimerScheduler[Command],
           stashBuffer: StashBuffer[Command]): Behavior[Command] = {
    log.info(s"$roomId recorder to couple behavior")
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case NewFrame(liveId, frame) =>
          if (frame.image != null) {
            if (liveId == hostLiveId) {
              drawer ! Image4Host(frame)
            } else if (clientLiveIdMap.keys.toList.contains(liveId)) {
              drawer ! Image4Client(liveId, frame)
            } else {
              log.info(s"wrong, liveId, work got wrong img")
            }
          }
          if (frame.samples != null) {
            try {
              if (liveId == hostLiveId) {
                ffFilter.pushSamples(0, frame.audioChannels, frame.sampleRate, ffFilter.getSampleFormat, frame.samples: _*)
              } else if (clientLiveIdMap.keys.toList.contains(liveId)) {
                ffFilter.pushSamples(clientLiveIdMap(liveId), frame.audioChannels, frame.sampleRate, ffFilter.getSampleFormat, frame.samples: _*)
              } else {
                log.info(s"wrong liveId, couple got wrong audio")
              }
              val f = ffFilter.pullSamples().clone()
              if (f != null) {
                recorder4ts.recordSamples(f.sampleRate, f.audioChannels, f.samples: _*)
              }
            } catch {
              case ex: Exception =>
                log.debug(s"$liveId record sample error system: $ex")
            }
          }
          Behaviors.same

        case msg: UpdateRoomInfo =>
          log.info(s"$roomId got msg: $msg in work.")
          if (msg.layout != layout) {
            drawer ! SetLayout(msg.layout)
          }
          ctx.self ! RestartRecord
          work(roomId, hostLiveId, clientLiveIdMap, msg.layout, recorder4ts, ffFilter, drawer, canvasSize)

        case UpdateClientList(clientLiveId, inOrOut) =>
          log.info(s"recorder get new partner: $clientLiveId")
          val num = clientLiveIdMap.size
          if (inOrOut == Part.in) {
            clientLiveIdMap.put(clientLiveId, num + 1)
            roomManager ! RoomManager.RecorderRef(roomId, ctx.self)
          } else {
            clientLiveIdMap.get(clientLiveId) match {
              case Some(index) =>
                clientLiveIdMap.remove(clientLiveId)
                val m = clientLiveIdMap.filter(_._2 > index)
                m.foreach { v =>
                  clientLiveIdMap.update(v._1, v._2 - 1)
                }
                drawer ! RemoveClient(clientLiveId)
              case None =>
                log.info(s"clientLiveId map not contain liveId: $clientLiveId")
            }
          }
          ctx.self ! Init(clientLiveIdMap.size + 1)
          //          work(roomId, hostLiveId, clientLiveIdMap, layout, recorder4ts, ffFilter, drawer, canvasSize)
          Behaviors.same

        case Init(num) =>
          log.info(s"recorder init: $num")
          if (ffFilter != null) {
            ffFilter.close()
          }
          val n = math.max(num, 2)
          val s = getInitString(n)
          val ffFilterN = new FFmpegFrameFilter(s"$s amix=inputs=$n:duration=longest:dropout_transition=3:weights=1 1[a]", audioChannels)
          ffFilterN.setAudioChannels(n)
          ffFilterN.setSampleFormat(sampleFormat)
          ffFilterN.setAudioInputs(n)
          ffFilter.setAudioChannels(hostChannel)
          ffFilter.setSampleRate(hostSampleRate)
          ffFilterN.start()
          work(roomId, hostLiveId, clientLiveIdMap, layout, recorder4ts, ffFilterN, drawer, canvasSize)

        case UpdateRecorder(channel, sampleRate, f, width, height, liveId) =>
          Behaviors.same

        case m@RestartRecord =>
          log.info(s"couple state get $m")
          Behaviors.same

        case CloseRecorder =>
          try {
            ffFilter.close()
            drawer ! Close
          } catch {
            case e: Exception =>
              log.error(s"$roomId recorder close error ---")
          }
          Behaviors.stopped

        case StopRecorder =>
          timer.startSingleTimer(TimerKey4Close, CloseRecorder, 1.seconds)
          Behaviors.same

        case x =>
          Behaviors.same
      }
    }
  }

  def draw(canvas: BufferedImage, graph: Graphics, clientFrameList: List[(String, Image)],
           recorder4ts: FFmpegFrameRecorder1, convert1: Java2DFrameConverter, convert2Map: mutable.Map[String, Java2DFrameConverter], convert: Java2DFrameConverter,
           layout: Int = 0, bgImg: String, roomId: Long, canvasSize: (Int, Int)): Behavior[VideoCommand] = {
    Behaviors.setup[VideoCommand] { ctx =>
      Behaviors.receiveMessage[VideoCommand] {
        case t: Image4Host =>
          val img = convert1.convert(t.frame)
          val clientImgList = clientFrameList.reverse.map(i => convert2Map(i._1).convert(i._2.frame))
          /*layout match {
            case 0 =>
              graph.drawImage(img, 0, canvasSize._2 / 4, canvasSize._1 / 2, canvasSize._2 / 2, null)
              graph.drawString("主持人", 24, 24)
              graph.drawImage(clientImg, canvasSize._1 / 2, canvasSize._2 / 4, canvasSize._1 / 2, canvasSize._2 / 2, null)
              graph.drawString("参会者", 344, 24)
            case 1 =>
              graph.drawImage(img, 0, 0, canvasSize._1, canvasSize._2, null)
              graph.drawString("主持人", 24, 24)
              graph.drawImage(clientImg, canvasSize._1 / 4 * 3, 0, canvasSize._1 / 4, canvasSize._2 / 4, null)
              graph.drawString("参会者", 584, 24)
            case 2 =>
              graph.drawImage(clientImg, 0, 0, canvasSize._1, canvasSize._2, null)
              graph.drawString("主持人", 24, 24)
              graph.drawImage(img, canvasSize._1 / 4 * 3, 0, canvasSize._1 / 4, canvasSize._2 / 4, null)
              graph.drawString("参会者", 584, 24)
          }*/
          //          if (clientImgList.length >= 2)
          //            log.info(s"${clientImgList.length}")
          clientImgList.length + 1 match {
            case x if x == 1 =>
              graph.drawImage(img, 0, 0, canvasSize._1, canvasSize._2, null)
              graph.drawString("主持人", 24, 24)

            case x if x == 2 =>
              graph.drawImage(img, 0, canvasSize._2 / 4, canvasSize._1 / 2, canvasSize._2 / 2, null)
              graph.drawString("主持人", 24, 24)
              clientImgList.foreach { clientImg =>
                graph.drawImage(clientImg, canvasSize._1 / 2, canvasSize._2 / 4, canvasSize._1 / 2, canvasSize._2 / 2, null)
                graph.drawString("参会者", 344, 24)
              }

            case x if x == 3 =>
              var n = 0
              graph.drawImage(img, canvasSize._1 / 4, 0, canvasSize._1 / 2, canvasSize._2 / 2, null)
              graph.drawString("主持人", canvasSize._1 / 4 + 24, 24)
              clientImgList.foreach { clientImg =>
                graph.drawImage(clientImg, n * canvasSize._1 / 2, canvasSize._2 / 2, canvasSize._1 / 2, canvasSize._2 / 2, null)
                graph.drawString("参会者", n * canvasSize._1 / 2 + 24, canvasSize._2 / 2 + 24)
                n += 1
              }
            case x if x == 4 =>
              var n = 0
              graph.drawImage(img, 0, 0, canvasSize._1 / 2, canvasSize._2 / 2, null)
              graph.drawString("主持人", 24, 24)
              graph.drawImage(clientImgList.head, canvasSize._1 / 2, 0, canvasSize._1 / 2, canvasSize._2 / 2, null)
              graph.drawString("参会者", 344, 24)
              clientImgList.drop(1).foreach { clientImg =>
                graph.drawImage(clientImg, n * canvasSize._1 / 2, canvasSize._2 / 2, canvasSize._1 / 2, canvasSize._2 / 2, null)
                graph.drawString("参会者", n * canvasSize._1 / 2 + 24, canvasSize._2 / 2 + 24)
                n += 1
              }
          }
          //fixme 此处为何不直接recordImage
          val frame = convert.convert(canvas)
          recorder4ts.record(frame.clone())
          Behaviors.same

        case t: Image4Client =>
          if (clientFrameList.map(_._1).contains(t.liveId)) {
            clientFrameList.filter(c => c._1 == t.liveId).foreach(_._2.frame = t.frame)
            Behaviors.same
          } else {
            log.info(s"get new partner: ${t.liveId} !!!!!!!!!!!!!!!!")
            val newList = (t.liveId, Image(t.frame)) :: clientFrameList
            convert2Map.put(t.liveId, new Java2DFrameConverter())
            graph.clearRect(0, 0, canvasSize._1, canvasSize._2)
            draw(canvas, graph, newList, recorder4ts, convert1, convert2Map, convert, layout, bgImg, roomId, canvasSize)
          }

        case RemoveClient(liveId) =>
          val newList = clientFrameList.filterNot(c => c._1 == liveId)
          convert2Map.remove(liveId)
          draw(canvas, graph, newList, recorder4ts, convert1, convert2Map, convert, layout, bgImg, roomId, canvasSize)

        case Close =>
          log.info(s"drawer stopped")
          recorder4ts.releaseUnsafe()
          Behaviors.stopped

        case t: SetLayout =>
          log.info(s"got msg: $t")
          draw(canvas, graph, clientFrameList, recorder4ts, convert1, convert2Map, convert, t.layout, bgImg, roomId, canvasSize)
      }
    }
  }

  def getInitString(n: Int) = {
    var t = ""
    for (i <- 0 until n) {
      t += s"[$i:a]"
    }
    t
  }

}
