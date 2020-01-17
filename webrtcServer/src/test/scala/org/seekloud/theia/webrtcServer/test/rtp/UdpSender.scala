package org.seekloud.theia.webrtcServer.test.rtp

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{Channels, DatagramChannel, Pipe}

import org.bytedeco.javacv.{FFmpegFrameGrabber, Frame}


/**
  * Created by sky
  * Date on 2019/7/16
  * Time at 19:39
  * copy from hello_media @Tao Zhang
  */
object UdpSender {
  def main(args: Array[String]): Unit = {
    //val serverHost = "123.56.108.66"
    val serverHost = "127.0.0.1"
    val serverPort = 50101
    val localPort = 54123
    val sender = new UdpSender(serverHost, serverPort, localPort)
    sender.start()
  }
}

class UdpSender(serverHost: String, serverPort: Int, localPort: Int) {

  private val sendPipe = Pipe.open()

  private val udpChannel = DatagramChannel.open()
  udpChannel.socket().setReuseAddress(true)
  //udpChannel.socket().bind(new InetSocketAddress("0.0.0.0", localPort))
  udpChannel.socket().bind(new InetSocketAddress("127.0.0.1", localPort))
  val targetAddr = new InetSocketAddress(serverHost, serverPort)
  private val localVideoQueue = new java.util.concurrent.ArrayBlockingQueue[Frame](200)



  def start(): Unit = {
    println("Client Start:")
    println(s"target server:$targetAddr ")
    val grabber = new FFmpegFrameGrabber("D:\\test\\test1.sdp")
    grabber.setOption("protocol_whitelist", "file,rtp,udp")
    grabber.setOption("fflags", "nobuffer")
    val grabThread =
      new Thread(new Grabber(
        grabber,
        localVideoQueue,
        Channels.newOutputStream(sendPipe.sink()))
      )

    //    val localDataDrawer =
    //      new Thread(new Drawer("local video", localVideoQueue))


    val sendThread = new Thread(() => {
      val buf = ByteBuffer.allocateDirect(7 * 188)
      val source = sendPipe.source()
      var r = source.read(buf)
      while (r != -1) {
        if (r > 0) {
          buf.flip()
          udpChannel.send(buf, targetAddr)
        }
        buf.clear()
        r = source.read(buf)
      }
    })

    sendThread.start()
    //    localDataDrawer.start()
    grabThread.start()

    println("begin sleep.")
    Thread.sleep(1000 * 300000)
    grabThread.interrupt()
    println("STOPPED.")


  }


}