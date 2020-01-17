package org.seekloud.theia.webrtcServer.test.rtp

import java.io.OutputStream
import java.util.concurrent.BlockingQueue

import org.bytedeco.javacv.{FFmpegFrameGrabber, FFmpegFrameRecorder, Frame}

/**
  * Created by sky
  * Date on 2019/7/16
  * Time at 19:40
  * copy from hello_media @Tao Zhang
  */
class Grabber(grabber: FFmpegFrameGrabber, frameQueue: BlockingQueue[Frame], outputStream: OutputStream) extends Runnable {

  override def run(): Unit = {
    grabber.startUnsafe()
    println("grabber started.")

    //ffmpeg -i p4_out1.mp4 -codec copy -bsf: h264_mp4toannexb -f h264 p4_out5.264


    val recorder = new FFmpegFrameRecorder(outputStream, grabber.getImageWidth, grabber.getImageHeight, 0)
    println(grabber.getImageWidth, grabber.getImageHeight)
    println(recorder.getVideoCodec, grabber.getVideoCodec)
    println(recorder.getAudioCodec, grabber.getAudioCodec)
    recorder.setOption("fflags", "nobuffer")
    recorder.setOption("tune", "zerolatency")
    recorder.setInterleaved(true)
    recorder.setVideoOption("preset", "ultrafast")
    recorder.setFrameRate(grabber.getFrameRate)
    recorder.setVideoBitrate(grabber.getVideoBitrate)
    recorder.setGopSize(50)
    recorder.setVideoCodec(grabber.getVideoCodec)
    recorder.setMaxBFrames(0)
    //    recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC)
    recorder.setFormat("mpegts")
    recorder.startUnsafe()
    println("set recorder success...")

    while (!Thread.interrupted()) {
      val frame = grabber.grab()
      recorder.record(frame)
    }

    grabber.close()
    //    recorder.close()
    outputStream.close()
    println("close all.")

  }
}
