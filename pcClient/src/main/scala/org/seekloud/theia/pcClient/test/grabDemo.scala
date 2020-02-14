package org.seekloud.theia.pcClient.test

import javax.swing.JFrame
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.{CanvasFrame, FFmpegFrameGrabber, FFmpegFrameRecorder, OpenCVFrameGrabber}

/**
  * @user: wanruolong
  * @date: 2019/11/1 14:47
  *
  */
object grabDemo {
  def main(args: Array[String]): Unit = {
    val url = "E:/迅雷下载/fourweddings/四个婚礼和一个葬礼第102季/Four.Weddings.and.a.Funeral.S01E01.mp4"
//    val grabber = new FFmpegFrameGrabber(url)
    val grabber = new OpenCVFrameGrabber(0)
//    grabber.getFrameRate
    println(grabber.getFrameRate)
    val canvas = new CanvasFrame("fromeFile")
    canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
//    canvas.setAlwaysOnTop(true)
//    println(grabber.hasAudio)
//    println(grabber.hasVideo)
    grabber.start()
    println("grab start success.")
//    val frame = grabber.grab()
    val urlServer = "rtmp://txy.live-send.acg.tv/live-txy/?streamname=live_44829093_50571972&key=faf3125e8c84c88ad7f05e4fcc017149"
    val record = new FFmpegFrameRecorder(urlServer, 640, 480)
    record.setFrameRate(30)
    record.setVideoBitrate(2000000)
    println(grabber.getVideoCodec)
//    record.setFormat(grabber.getFormat)
//    record.setVideoCodec(avcodec.AV_CODEC_ID_H264)
    record.setVideoCodec(grabber.getVideoCodec)
    record.setFormat("flv")

    record.start()
    while (true){
      val frame = grabber.grab()

      if(frame.image != null){
//        canvas.showImage(frame.clone())
//        Thread.sleep(20)
//        println(grabber.getFrameRate)
        record.record(frame.clone())
      }
    }
  }
}
