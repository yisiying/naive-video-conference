package org.seekloud.theia.webrtcServer.test.rtp

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{Channels, DatagramChannel, Pipe}
import java.util.concurrent.atomic.AtomicLong

import org.bytedeco.javacv.{FFmpegFrameGrabber, Frame}
import org.seekloud.theia.webrtcServer.utils.RtpUtil
import org.seekloud.theia.webrtcServer.utils.RtpUtil.PT_TYPE

/**
  * Created by sky
  * Date on 2019/7/16
  * Time at 19:39
  * edit from hello_media pencil
  */
object RtpSender {
  def main(args: Array[String]): Unit = {
    //val serverHost = "123.56.108.66"
    val serverHost = "127.0.0.1"
    val serverPort = 50101
    val localPort = 54123
    val sender = new RtpSender(serverHost, serverPort, localPort)
    sender.start()
  }
}

class RtpSender(serverHost: String, serverPort: Int, localPort: Int) {

  import RtpSender._

  object Rtp_header {
    var timestamp = 0l
    var payloadType = 33.toByte //负载类型号96
  }

  var timestamp = 0l
  val increasedSequence = new AtomicLong(0)
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
    val grabber = new FFmpegFrameGrabber("D:\\ideaProject\\theia\\webrtcServer\\src\\main\\data\\1000007_100007.sdp")
    grabber.setOption("protocol_whitelist", "file,rtp,udp")
    grabber.setOption("fflags", "nobuffer")

    val timestamp_increse = (90000 / grabber.getFrameRate).toLong

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
          /** 封装rtp
            * */
          val total_len = 12 + 7 * 188
          val rtp_buf = ByteBuffer.allocate(total_len)

          //          Rtp_header.m = 1
          timestamp += timestamp_increse //到下一个起始帧或者满了7个包，填充完毕
          //设置rtp header
          val rtpHeader = RtpUtil.RtpHeader(PT_TYPE.payloadType_33.toByte, increasedSequence.getAndIncrement(), timestamp, 0l)
          println(RtpUtil.head2buffer(rtpHeader).map { r =>
            rtp_buf.put(r)
            r.toHexString
          }.mkString(" "))
          rtp_buf.put(buf)

          rtp_buf.flip()
          //          println("send", seq, rtp_buf.array().map(i => toHexFromByte(i)).mkString(" "))
          println("send", rtpHeader.seq, rtp_buf.limit())
          udpChannel.send(rtp_buf, targetAddr)
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