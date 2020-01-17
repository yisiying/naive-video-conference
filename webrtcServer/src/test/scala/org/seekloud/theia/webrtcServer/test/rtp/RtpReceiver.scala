package org.seekloud.theia.webrtcServer.test.rtp

import java.net.InetSocketAddress
import java.nio.{ByteBuffer, ShortBuffer}
import java.nio.channels.{Channels, DatagramChannel, Pipe}

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream
import javax.sound.sampled.{AudioFormat, AudioSystem, DataLine, TargetDataLine}
import org.bytedeco.javacv.{FFmpegFrameGrabber, Frame, Java2DFrameConverter}
import org.seekloud.theia.webrtcServer.utils.RtpUtil

/**
  * Created by sky
  * Date on 2019/7/16
  * Time at 19:45
  * edit from hello_media pencil
  */
object RtpReceiver {
  def main(args: Array[String]): Unit = {
    val port = 50101
    val rtpReceiver = new RtpReceiver(port)
    rtpReceiver.start()
  }

}

class RtpReceiver(port: Int) {

  private val receivePipe = Pipe.open()

  private val remoteVideoQueue = new java.util.concurrent.ArrayBlockingQueue[Frame](200)

  private val udpChannel = DatagramChannel.open()
  udpChannel.socket().setReuseAddress(true)
  //udpChannel.socket().bind(new InetSocketAddress("0.0.0.0", port))
  udpChannel.socket().bind(new InetSocketAddress("127.0.0.1", port))

  val pipe = Pipe.open()
  val source = pipe.source()
  val sink = pipe.sink()
  val dataInputStream = Channels.newInputStream(source)

  //  val byteInputStream = new ByteInputStream()

  private val buf = ByteBuffer.allocate(32 * 1024)
  private val rtpBuf = ByteBuffer.allocate(188 * 7)

  def start(): Unit = {
    println(s"Receiver start at [$port]")

    val receiveThread = new Thread(() => {

      while (true) {
        buf.clear()
        udpChannel.receive(buf)
        buf.flip()
        println(s"receiver got pkt size=${buf.limit()}")

        val recvBytes = buf.array().take(buf.limit())
        if (recvBytes.length < 12)
          println("length of rtp packet less than 12")
        else if (((recvBytes.head & 0xFF) >>> 6) != 2)
          println(s"unsupported rtp packet version ${recvBytes.head >>> 6}")
        else {
          val rtpHeader = RtpUtil.buffer2Head(recvBytes.take(12))
          val rtpBody = recvBytes.drop(12)

          //          println("receive", toLong(seq), toLong(ssrc), recvBytes.map(i => toHexFromByte(i)).mkString(" "))
          println("receive", rtpHeader.seq, rtpHeader.ssrc)
          rtpBuf.put(rtpBody)
          rtpBuf.flip()
          sink.write(rtpBuf)
          rtpBuf.clear()
          //          byteInputStream.read(rtpBody)
        }

      }
    })


    val decodeThread = new Thread(() => {
      val decoder = new FFmpegFrameGrabber(dataInputStream)


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
