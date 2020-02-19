package org.seekloud.theia.distributor.core

import java.io.{BufferedWriter, File, FileWriter}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import org.bytedeco.javacpp.Loader
import org.slf4j.LoggerFactory
import org.seekloud.theia.distributor.common.AppSettings._
import org.seekloud.theia.distributor.Boot.saveManager
import org.seekloud.theia.distributor.utils.{CmdUtil, TimeUtil}

import scala.language.implicitConversions

/**
  * User: yuwei
  * Date: 2019/8/26
  * Time: 20:09
  */

object EncodeActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case object Stop extends Command

  case class ReStart(port: Int, startTime:Long) extends Command

  case object CloseEncode extends Command

  case object SaveRecord extends Command

  case class ChildDead(roomId: Long, childName: String, value: ActorRef[SaveActor.Command]) extends Command

  case object TimerKey4Close

  case object TimerKey4SaveRecord

  case object NewFFmpeg extends Command

  class CreateFFmpeg(roomId: Long, port: Int, startTime:Long){
    private var process: Process = _
    private var recordProcess: Process = _

    private var writer: BufferedWriter = _

    def createDir(): AnyVal = {
      val fileLoc = new File(s"$fileLocation$roomId/")
      if(!fileLoc.exists()){
        fileLoc.mkdir()
      }
      val recordLoc = new File(s"$recordLocation$roomId/")
      if(!recordLoc.exists()){
        recordLoc.mkdir()
      }
      val recordLocST = new File(s"$recordLocation$roomId/$startTime/")
      if(!recordLocST.exists()){
        recordLocST.mkdir()
      }
    }

    def removeFile(): AnyVal = {
      val f = new File(s"$fileLocation$roomId/")
      if(f.exists()) {
        f.listFiles().map{
          e =>
            e.delete()
        }
        f.delete()
      }
    }

    def start(): Unit = {
      val ffmpeg = Loader.load(classOf[org.bytedeco.ffmpeg.ffmpeg])
//      val pb = new ProcessBuilder(ffmpeg,"-analyzeduration","10000000","-probesize","1000000","-i", s"udp://127.0.0.1:$port", "-b:v", "1M","-bufsize","1M", "-c:v","copy","-vtag","avc1","-f", "dash", "-window_size", "20", "-extra_window_size", "20", "-hls_playlist", "1", s"$fileLocation$roomId/index.mpd")
      if(!testModel){
//        val pb = new ProcessBuilder(ffmpeg,"-analyzeduration","10000000","-probesize","1000000","-i",s"udp://127.0.0.1:$port","-b:v","1M","-bufsize","1M","-f","dash","-window_size","20","-extra_window_size","20","-hls_playlist","1",s"$fileLocation$roomId/index.mpd")
//        val process = pb.inheritIO().start()
//        this.process = process
        val cmd = ffmpeg + s" -analyzeduration 10000000 -probesize 1000000 -i udp://127.0.0.1:$port -b:v 1M -bufsize 1M -f dash -use_template 1 -use_timeline 1 -seg_duration 4 -window_size 20 -extra_window_size 20 -hls_playlist 1 $fileLocation$roomId/index.mpd"
//        "-adaptation_sets \"id=0,streams=v id=1,streams=a\" "
        //        val cmd = ffmpeg + s" -f mpegts -i udp://127.0.0.1:$port -b:v 1M -bufsize 1M -f dash -window_size 20 -extra_window_size 20 -hls_playlist 1 $fileLocation$roomId/index.mpd"
//        this.process = CmdUtil.exeFFmpeg(cmd)
        val file = new File(s"$encodeLogPath/encodeLog-$roomId-${TimeUtil.timeStamp2DetailDate(startTime).replaceAll(" ","-")}")
        if (!file.exists()) {
          file.createNewFile()
          log.debug(s"create distributor log info: $file")
        }
        if (file.exists() && file.canWrite) {
          writer = new BufferedWriter(new FileWriter(file))
          CmdUtil.exeFFmpegWithLog(cmd, writer, displayInConsole = true) match {
            case Right(processAndLog) =>
              process = processAndLog

            case Left(e) =>
              log.info(s"execute ffmpeg cmd error, $e")
          }
        }
      } else {
//        val pb = new ProcessBuilder( ffmpeg,"-f","mpegts","-i",s"udp://127.0.0.1:$port","-b:v","1M","-bufsize","1M","-f","dash","-window_size","20","-extra_window_size","20","-hls_playlist","1","/Users/litianyu/Downloads/dash/index.mpd")
//        val process = pb.inheritIO().start()
//        this.process = process
          val cmd = ffmpeg + s" -analyzeduration 10000000 -probesize 5000000 -i udp://127.0.0.1:$port -b:v 1M -bufsize 1M -f dash -use_timeline 0 -use_template 1 -window_size 2 -extra_window_size 2 -seg_duration 5 -single_file 0 -hls_playlist 1 C:/Users/Administrator/Videos/shencheng/"
          this.process = CmdUtil.exeFFmpeg(cmd)
      }
    }

    def close(): Unit ={
      if(this.process != null){
        this.process.destroyForcibly()
      }
      if(this.recordProcess != null){
        this.recordProcess.destroyForcibly()
      }
//      this.process4Record.destroyForcibly()
      log.info(s"ffmpeg close successfully---")
    }
  }

  def create(roomId: Long, port: Int, startTime:Long): Behavior[Command] = {
    Behaviors.setup[Command] { _  =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"encodeActor_$roomId start!")
          val fFmpeg = new CreateFFmpeg(roomId, port, startTime)
          fFmpeg.removeFile() //删除之前的直播文件
          fFmpeg.createDir()
          fFmpeg.start()
//          fFmpeg.saveRecord()
          work(roomId, port, fFmpeg, startTime)
      }
    }
  }

  def work(roomId: Long, port: Int, ffmpeg:CreateFFmpeg, startTime:Long)(implicit timer: TimerScheduler[Command],
    stashBuffer: StashBuffer[Command]): Behavior[Command] = {
    Behaviors.receive[Command] { (_, msg) =>
      msg match {
        case Stop =>
          log.info(s"stop the encode video for roomId:${roomId}.")
          ffmpeg.close()
//          ffmpeg.removeFile()
          saveManager ! SaveManager.NewSave(startTime, roomId)
          Behaviors.stopped

        case ReStart(newPort, newStartTime) =>
          ffmpeg.close()
          //          ffmpeg.removeFile()
          saveManager ! SaveManager.NewSave(newStartTime, roomId)
          val newFfmpeg = new CreateFFmpeg(roomId, newPort, newStartTime)
          newFfmpeg.createDir()
          newFfmpeg.start()
          log.info("reStart the encode video.")
          work(roomId, newPort, newFfmpeg, newStartTime)
      }
    }
  }


}









