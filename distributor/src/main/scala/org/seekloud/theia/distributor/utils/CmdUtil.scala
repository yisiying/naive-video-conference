package org.seekloud.theia.distributor.utils

import java.io.{BufferedReader, BufferedWriter, InputStreamReader}
import java.nio.charset.Charset
import java.util
import java.util.concurrent.TimeUnit

import org.seekloud.theia.distributor.Boot.executor
import org.slf4j.LoggerFactory

import scala.concurrent.Future


/**
 * User: Jason
 * Date: 2019/10/12
 * Time: 15:51
 */

object CmdUtil {
  private val log = LoggerFactory.getLogger(this.getClass)

  def exeCmd4Linux(commandStr: String): Future[Int] = {
    var br: BufferedReader = null
    var br4Error: BufferedReader = null

    //    val commandStr = "cat /home/sk75/distributor/test/init-stream0.m4s /home/sk75/distributor/test/chunk-stream0*.m4s >> /home/sk75/distributor/test/video.mp4"+
    //                     "; cat /home/sk75/distributor/test/init-stream1.m4s /home/sk75/distributor/test/chunk-stream1*.m4s >> /home/sk75/distributor/test/audio.mp4"

    val rst = Future {
        val p: Process = Runtime.getRuntime.exec(Array[String]("/bin/sh", "-c", commandStr))
        p.waitFor(10, TimeUnit.SECONDS)
        br = new BufferedReader(new InputStreamReader(p.getInputStream, Charset.forName("GBK")))
        br4Error = new BufferedReader(new InputStreamReader(p.getErrorStream, Charset.forName("GBK")))
        var line = ""
        val sb = new StringBuilder()
        while ( {
          line = br.readLine
          line != null
        }) {
          sb.append(line + "\n")
        }
        while ( {
          line = br4Error.readLine
          line != null
        }) {
          sb.append(line + "\n")
        }
        p.destroy()
        log.info(sb.toString)
        1
//      catch {
//        case e: Exception =>
//          e.printStackTrace()
//      } finally if (br != null) br.close();if (br4Error != null) try br4Error.close()
//      catch {
//        case e: Exception =>
//          e.printStackTrace()
//      }
    }.recover {
      case ex: Exception =>
        log.warn(s"errorStream read error: $ex")
        if (br != null) br.close()
        if (br4Error != null) br4Error.close()
        -1
    }
    rst
  }

  def exeCmd4Windows(commandStr: String): Future[Int] = {
    var br: BufferedReader = null
    var br4Error: BufferedReader = null

    //    val commandStr = "cat /home/sk75/distributor/test/init-stream0.m4s /home/sk75/distributor/test/chunk-stream0*.m4s >> /home/sk75/distributor/test/video.mp4"+
    //                     "; cat /home/sk75/distributor/test/init-stream1.m4s /home/sk75/distributor/test/chunk-stream1*.m4s >> /home/sk75/distributor/test/audio.mp4"

    val rst = Future {
      val p: Process = Runtime.getRuntime.exec("cmd.exe /c " + commandStr)
      p.waitFor(10, TimeUnit.SECONDS)
      br = new BufferedReader(new InputStreamReader(p.getInputStream, Charset.forName("GBK")))
      br4Error = new BufferedReader(new InputStreamReader(p.getErrorStream, Charset.forName("GBK")))
      var line = ""
      val sb = new StringBuilder()
      while ( {
        line = br.readLine
        line != null
      }) {
        sb.append(line + "\n")
      }
      while ( {
        line = br4Error.readLine
        line != null
      }) {
        sb.append(line + "\n")
      }
      p.destroy()
      System.out.println(sb.toString)
      1
    }.recover {
      case ex: Exception =>
        log.warn(s"errorStream read error: $ex")
        if (br != null) br.close()
        if (br4Error != null) br4Error.close()
        -1
    }
    rst
//    catch {
//      case e: Exception =>
//        e.printStackTrace()
//    } finally if (br != null) br.close(); if (br4Error != null) try br4Error.close()
//    catch {
//      case e: Exception =>
//        e.printStackTrace()
//    }
  }

  def exeFFmpeg(cmdStr: String): Process = {
    //    val fCommandStr = ffmpeg + " -i /home/sk75/distributor/test/video.mp4 -i /home/sk75/distributor/test/audio.mp4 -c:v copy -c:a copy /home/sk75/distributor/test/final.mp4"
    val cmd: util.ArrayList[String] = new util.ArrayList[String]()
    cmdStr.split(" ").foreach(cmd.add)
    val pb: ProcessBuilder = new ProcessBuilder(cmd)
    val processor = pb.inheritIO().start()
    processor
  }

  def exeFFmpegWithLog(cmdStr: String, writer: BufferedWriter, displayInConsole: Boolean = false): Either[String, Process] = {
    var br: BufferedReader = null
    var br4Error: BufferedReader = null
    val cmd: util.ArrayList[String] = new util.ArrayList[String]()
    cmdStr.split(" ").foreach(cmd.add)
    val pb: ProcessBuilder = new ProcessBuilder(cmd)

    val rst = try {
      val process = pb.start()
      br = new BufferedReader(new InputStreamReader(process.getInputStream, Charset.forName("GBK")))
      br4Error = new BufferedReader(new InputStreamReader(process.getErrorStream, Charset.forName("GBK")))
      var line = ""
      val logThread = new Thread(() => {
        try {
          var flag = true
          var counter = 0
          while ( flag ) {
            if (br.ready()) {
              if (br.readLine() != line) {
                line = br.readLine()
                writer.append(TimeUtil.timeStamp2MoreDetailDate(System.currentTimeMillis())+ "  " + line + "\n")
//                print(line + "\n")
              }
              else if (line != "" && br.readLine() == line){
                line = ""
                flag = false
              }
              else if (line == ""){
                flag = false
              }
            }
            else {
              Thread.sleep(100)
              counter += 1
              if (counter > 100) {
                flag = false
              }
            }
          }
//          writer.close()
        } catch {
          case e: Exception =>
            log.error(s"logThread catch exception: $e")
        }
      })
      val logThread4err = new Thread(() => {
        try {
          var flag4err = true
          var counter = 0
          while ( flag4err ) {
            if (br4Error.ready()) {
              counter = 0
              if (br4Error.readLine() != line) {
                line = br4Error.readLine()
                writer.flush()
                writer.append(TimeUtil.timeStamp2MoreDetailDate(System.currentTimeMillis())+ "  " + line + "\n")
                if (displayInConsole) print(TimeUtil.timeStamp2MoreDetailDate(System.currentTimeMillis())+ "  " + line + "\n")
              }
              else if (line != "" && br4Error.readLine() == line){
                line = ""
                flag4err = false
              }
            }
            else {
              Thread.sleep(100)
              counter += 1
              if (counter > 100) {
                flag4err = false
              }
            }
          }
          // process.destroy()  //主动关闭线程
          writer.close()
        } catch {
          case e: Exception =>
            log.error(s"logThread4err catch exception: $e")
        }
      })
      logThread.start()
      logThread4err.start()
      Right(process)
    } catch {
      case ex: Exception =>
        log.warn(s"out put Stream read error: $ex")
        if (br != null) br.close()
        if (br4Error != null) br4Error.close()
        writer.close()
        Left(ex.getMessage)
    }
    rst
  }

}
