package org.seekloud.theia.webrtcServer.test.rtp

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{DatagramChannel, Pipe}

import org.bytedeco.javacv.{FFmpegFrameGrabber, Frame}
import sun.nio.ch.ChannelInputStream

/**
  * Created by sky
  * Date on 2019/7/16
  * Time at 19:43
  * copy from hello_media @Tao Zhang
  */
object UdpReceiver {

  def main(args: Array[String]): Unit = {
    val port = 50101
    val udpMediaReceiver = new UdpReceiver(port)
    udpMediaReceiver.start()
  }

}

class UdpReceiver(port: Int) {

  private val receivePipe = Pipe.open()

  private val remoteVideoQueue = new java.util.concurrent.ArrayBlockingQueue[Frame](200)

  private val udpChannel = DatagramChannel.open()
  udpChannel.socket().setReuseAddress(true)
  //udpChannel.socket().bind(new InetSocketAddress("0.0.0.0", port))
  udpChannel.socket().bind(new InetSocketAddress("127.0.0.1", port))

  def start(): Unit = {
    println(s"Receiver start at [$port]")

    val receiveThread = new Thread(() => {
      val buf = ByteBuffer.allocateDirect(64 * 1024)
      val sink = receivePipe.sink()

      while (!Thread.interrupted()) {
        buf.clear()
        udpChannel.receive(buf)
        buf.flip()
        println(s"receiver got pkt size=${buf.remaining()}")
        sink.write(buf)
      }
    })


    val decodeThread = new Thread(() => {
      println("decodeThread 11")
      val source = receivePipe.source()
      println("decodeThread 22")
      val is = new ChannelInputStream(source)
      println("decodeThread 33")
      val decoder = new FFmpegFrameGrabber(is)


      println("decodeThread 44")
      decoder.startUnsafe()
      println("decoder started.")
      var frame = decoder.grab()
      var fCounter = 0
      while (!Thread.interrupted() && frame != null) {
        if (frame.image != null) {
          println(s"receiver got frame [$fCounter]")
          fCounter += 1
          remoteVideoQueue.put(frame.clone())
        }
        frame = decoder.grab()
      }
    })


    val remoteDataDrawer = new Thread(new Drawer("remote video", remoteVideoQueue))

    receiveThread.start()
    decodeThread.start()
    remoteDataDrawer.start()

  }


}
