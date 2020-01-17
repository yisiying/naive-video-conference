package org.seekloud.theia.webrtcServer.test.ffmpeg

import org.bytedeco.javacpp.Loader

/**
  * Created by sky
  * Date on 2019/8/10
  * Time at 上午11:47
  */
object Test {
  def main(args: Array[String]): Unit = {
    val ffmpeg = Loader.load(classOf[org.bytedeco.ffmpeg.ffmpeg])
    val pb = new ProcessBuilder(ffmpeg, "-protocol_whitelist", "file,rtp,udp", "-fflags", "nobuffer",
      "-i", "D:\\test\\test1.sdp", "-vcodec", "copy", "-acodec", "aac", "-f", "mpegts", "test.ts")
    pb.inheritIO().start().waitFor()
  }
}
