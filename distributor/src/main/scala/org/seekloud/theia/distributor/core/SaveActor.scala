package org.seekloud.theia.distributor.core

import java.io.{BufferedWriter, File, FileWriter}

import akka.actor.Cancellable
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import org.bytedeco.javacpp.Loader
import org.slf4j.LoggerFactory
import org.seekloud.theia.distributor.common.AppSettings._
import org.seekloud.theia.distributor.utils.{CmdUtil, TimeUtil}
import org.seekloud.theia.distributor.Boot.{executor, scheduler}

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.util.{Failure, Success}
object SaveActor {

  class CreateFFmpeg(roomId: Long, startTime: Long) {
    private var process: Process = _

    private var writer: BufferedWriter = _

    var isDelete = false

    var tryTimes = 0
    var tryTime = 0

    var firstTimer: Cancellable = _
    var secondTimer: Cancellable = _

    def createLogInfo(logInfo: String): Unit = {
      val file = new File(s"$recordLocation$roomId/$startTime/")
      val temp = File.createTempFile("distributor", "LogInfo", file) //为临时文件名称添加前缀和后缀
      if (temp.exists() && temp.canWrite) {
        val bufferedWriter = new BufferedWriter(new FileWriter(temp))
        bufferedWriter.write(s"$logInfo")
        bufferedWriter.close()
      }
      log.debug(s"create distributor log info: $temp")
    }

    def removeFile(): AnyVal = {
      val f = new File(s"$fileLocation$roomId/")
      if (f.exists()) {
        f.listFiles().map {
          e =>
            isDelete = true
            e.delete()
        }
        f.delete()
      }
    }

    def removeVideo(path: String): AnyVal = {
      val f = new File(path)
      if (f.exists()) {
        f.delete()
      }
    }

    def removeFile4F(): Unit= {
      log.info(s"Ready to delete $roomId-$startTime video and audio files, ${tryTime}th")
      tryTime += 1
      val f = new File(s"$recordLocation$roomId/$startTime/record.mp4")
      if (f.exists()) {
        removeVideo(s"$recordLocation$roomId/$startTime/video.mp4")
        removeVideo(s"$recordLocation$roomId/$startTime/audio.mp4")
      }
      else if(tryTime < 5){
        secondTimer = scheduler.scheduleOnce(10 seconds, () => removeFile4F())
      }
      else {
        secondTimer.cancel()
        removeVideo(s"$recordLocation$roomId/$startTime")
      }
    }


    def saveRecord(): Unit = {
      val ffmpeg = Loader.load(classOf[org.bytedeco.ffmpeg.ffmpeg])
      //      val pb = new ProcessBuilder(ffmpeg, "-i", s"$recordLocation$roomId/$startTime/record.ts", "-b:v", "1M", "-movflags", "faststart", s"$recordLocation$roomId/$startTime/record.mp4")
      //      val process = pb.start()
      //      this.process = process

        val commandStr = s"cat $fileLocation$roomId/init-stream0.m4s $fileLocation$roomId/chunk-stream0*.m4s >> $recordLocation$roomId/$startTime/video.mp4" +
                         s"; cat $fileLocation$roomId/init-stream1.m4s $fileLocation$roomId/chunk-stream1*.m4s >> $recordLocation$roomId/$startTime/audio.mp4"
        CmdUtil.exeCmd4Linux(commandStr).onComplete {
          case Success(a) =>
            periodlyRm()
            if (a == 1) {
              val f = new File(s"$recordLocation$roomId/$startTime/video.mp4")
              if (f.exists()) {
                log.info(s"record startTime: $startTime")
                val cmdStr = ffmpeg + s" -i $recordLocation$roomId/$startTime/video.mp4 -i $recordLocation$roomId/$startTime/audio.mp4 -c copy -b:v 1M -movflags faststart $recordLocation$roomId/$startTime/record.mp4"
                val file = new File(s"$saveLogPath/saveLog-$roomId-${TimeUtil.timeStamp2DetailDate(startTime).replaceAll(" ","-")}")
                if (!file.exists()) {
                  file.createNewFile()
                  log.debug(s"create distributor log info: $file")
                }
                if (file.exists() && file.canWrite) {
                  writer = new BufferedWriter(new FileWriter(file))
                  CmdUtil.exeFFmpegWithLog(cmdStr, writer) match {
                    case Right(processAndLog) =>
                      process = processAndLog

                    case Left(e) =>
                      log.info(s"execute ffmpeg cmd error, $e")
                  }
                }
              }
              else {
                log.info("video and audio files don't exist")
              }
            }
            else {
              log.info("cat video and audio files failed")
            }

          case Failure(e) =>
            periodlyRm()
            log.info(s"record error: $e")
        }
    }

    def close(): Unit = {
      if (this.process != null) this.process.destroyForcibly()
      if (writer != null) this.writer.close()
      log.info(s"ffmpeg close successfully---")
    }

    def periodlyRm(): Unit = {
      log.info(s"Ready to delete useless $roomId-$startTime files, ${tryTimes}th")
      tryTimes += 1
      val f = new File(s"$recordLocation$roomId/$startTime/video.mp4")
      if (!isDelete) {
        if (f.exists()) {
          removeFile()
        }
        else if(tryTimes < 5){
          firstTimer = scheduler.scheduleOnce(10 seconds, () => periodlyRm())
        }
        else {
          log.info(s"Files below $fileLocation$roomId/ weren't deleted regularly.")
          firstTimer.cancel()
          removeFile()
        }
      }
    }
  }

  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case object Timer4Die

  case object Stop extends Command

  def create(roomId: Long, startTime:Long): Behavior[Command] = {
    Behaviors.setup[Command] { _ =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"$roomId -- $startTime saveActor start...")
          val fFmpeg = new CreateFFmpeg(roomId,  startTime)
          fFmpeg.saveRecord()
          timer.startSingleTimer(Timer4Die, Stop, 5.minutes)
          work(roomId, fFmpeg)
      }
    }
  }
    def work(roomId: Long, ffmpeg: CreateFFmpeg)(implicit timer: TimerScheduler[Command],
                                                           stashBuffer: StashBuffer[Command]): Behavior[Command] = {
      Behaviors.receive[Command] { (_, msg) =>
        msg match {
          case Stop =>
            ffmpeg.close()
            ffmpeg.removeFile4F()
            log.info(s"$roomId saveActor stopped --")
            Behaviors.stopped
        }
      }
    }
}
