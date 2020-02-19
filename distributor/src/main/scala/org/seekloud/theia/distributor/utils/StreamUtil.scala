package org.seekloud.theia.distributor.utils

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.nio.charset.Charset
import java.util
import java.util.Scanner
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

import org.bytedeco.javacpp.Loader
import org.seekloud.theia.distributor.common.AppSettings.recordLocation


object StreamUtil {
  val ffmpeg = Loader.load(classOf[org.bytedeco.ffmpeg.ffmpeg])
  val ffprobe = Loader.load(classOf[org.bytedeco.ffmpeg.ffprobe])
  val fileLocation = "/Users/angel/Downloads/test.mp4"
  val streamLocation = "udp://127.0.0.1:41100"

  val flag :AtomicBoolean = new AtomicBoolean(false)
  val logInfo = new StringBuilder()
  val logError = new StringBuilder()

  def readStream1(inStream:InputStream) = {
    try{
      val scanner = new Scanner(inStream, "UTF-8")
      val text = scanner.useDelimiter("\\A").next()
      scanner.close()
      text
    }catch {
      case e:Exception=>
        e.printStackTrace()
        "error"
    }
  }

  def readStream2(inStream:InputStream) = {
    val count = inStream.available()
//    println(count)
    var readBytes = 0
    val buf = new Array[Byte](8192)
    while ({
      readBytes = inStream.read(buf)
      readBytes > 0
    })
      logInfo.append(new String(buf, 0, readBytes))
    println(s"log: ${logInfo.toString()}")
    println(s"length: ${logInfo.length()}")
  }

  def testThread2(process:ProcessBuilder) =
  {
    val processor = process.start()
    val br = processor.getInputStream
    //    val br4err = new BufferedReader(new InputStreamReader(processor.getErrorStream))

    readStream2(br)

    br.close()
    //    br4err.close()
    processor.destroy()

  }

  def testThread(process:ProcessBuilder,outInfo:StringBuilder) = new Thread(()=>
  {
    val processor = process.start()
    val br = new BufferedReader(new InputStreamReader(processor.getInputStream))
    //    val br4err = new BufferedReader(new InputStreamReader(processor.getErrorStream))
    var line:String = ""
    while ({
      line = br.readLine()
      line != null
    }){
      outInfo.append(line + "\n")
    }
    flag.set(true)
    br.close()
    //    br4err.close()
    processor.destroy()

  })

  def test_stream(url:String) = {
    val pb_streams = new ProcessBuilder(ffprobe,"-of","csv","-show_streams",url)
    val log = new StringBuilder()
    val thread = testThread(pb_streams, log)
    thread.start()
    val start = System.currentTimeMillis()
    var end = start
    try {
      while (end - start < 1000 && !flag.get()){
        TimeUnit.MILLISECONDS.sleep(100)
        end = System.currentTimeMillis()
      }
    }catch {
      case e:Exception =>
        e.printStackTrace()
    }
    thread.interrupt()
    if (log.length != 0) true
    else false
  }



  def main(args: Array[String]): Unit = {
      testThread2(new ProcessBuilder(ffprobe,"-of","csv","-show_streams","format=duration",fileLocation))
//    println(test_stream(fileLocation))
  }
}
