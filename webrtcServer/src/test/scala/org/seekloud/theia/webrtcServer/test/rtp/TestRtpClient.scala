package org.seekloud.theia.webrtcServer.test.rtp

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{DatagramChannel, Pipe}

import akka.actor.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, Behaviors, StashBuffer, TimerScheduler}
import org.seekloud.theia.shared.rtp.{Protocol, RtpClient}
import org.seekloud.theia.shared.rtp.Protocol.{AuthRsp, PullStreamData, PullStreamPacketLoss, PullStreamReqSuccess, PushStreamError}
import org.slf4j.LoggerFactory
import akka.actor.typed.scaladsl.adapter._
import org.bytedeco.javacv.{FFmpegFrameGrabber, Frame}
import org.seekloud.theia.webrtcServer.Boot.system
import org.seekloud.theia.webrtcServer.test.rtp.TestActor.PullStream
import sun.nio.ch.ChannelInputStream

/**
  * Author: wqf
  * Date: 2019/8/13
  * Time: 16:29
  */

object TestRtpClient {

  import TestActor.Ready

  def main(args: Array[String]): Unit = {
    val clientActor = system.spawn(TestActor.create(), "TestActor")
    val pushStreamChannel = DatagramChannel.open()
    pushStreamChannel.bind(new InetSocketAddress(1234))
    val pullStreamChannel = DatagramChannel.open()
    pullStreamChannel.bind(new InetSocketAddress(4578))
    pushStreamChannel.socket().setReuseAddress(true)
    pullStreamChannel.socket().setReuseAddress(true)
    val pushStreamDst = new InetSocketAddress("10.1.29.248", 30683)
    val pullStreamDst = new InetSocketAddress("10.1.29.248", 30684)
    val client = new RtpClient(Some(pushStreamChannel), Some(pushStreamDst), Some(pullStreamChannel), Some(pullStreamDst), clientActor)
    clientActor ! Ready(client)
    //    var liveIds = List.empty[String]
    val testLiveIds = (0 until 1).map { index =>
      s"liveIdTest-$index"
    }.toList
    clientActor ! PullStream(List("PCId0sabx7hzbd4kokFvV8VS"))
  }
}

object TestActor {
  private val log = LoggerFactory.getLogger(this.getClass)

  case class Ready(client: RtpClient) extends Protocol.Command

  case class PushStream(liveId: String) extends Protocol.Command

  case class PullStream(liveId: List[String]) extends Protocol.Command

  case class Auth(liveId: String, liveCode: String) extends Protocol.Command

  def create(): Behavior[Protocol.Command] = {
    Behaviors.setup[Protocol.Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Protocol.Command] = StashBuffer[Protocol.Command](Int.MaxValue)
      Behaviors.withTimers[Protocol.Command] { implicit timer =>
        wait1()
      }
    }
  }

  def wait1()
           (implicit timer: TimerScheduler[Protocol.Command],
            stashBuffer: StashBuffer[Protocol.Command]): Behavior[Protocol.Command] = {
    Behaviors.receive[Protocol.Command] { (ctx, msg) =>
      msg match {
        case Ready(client) =>
          stashBuffer.unstashAll(ctx, work(client))

        case x =>
          stashBuffer.stash(x)
          Behavior.same
      }
    }
  }

  private val remoteVideoQueue = new java.util.concurrent.ArrayBlockingQueue[Frame](200)

  val remoteDataDrawer = new Thread(new Drawer("remote video", remoteVideoQueue))

  val mediaPipe = Pipe.open()
  val sink = mediaPipe.sink()
  val source = mediaPipe.source()
  sink.configureBlocking(false)
  //          source.configureBlocking(false)
  val inputStream = new ChannelInputStream(source)

  var decodeThread: Thread = new Thread(() => {
    val decoder = new FFmpegFrameGrabber(inputStream)
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

  def work(client: RtpClient)
          (implicit timer: TimerScheduler[Protocol.Command],
           stashBuffer: StashBuffer[Protocol.Command]): Behavior[Protocol.Command] = {
    Behaviors.setup[Protocol.Command] { context =>

      client.authStart()
      client.pullStreamStart()
      var i = 0
      Behaviors.receiveMessage[Protocol.Command] {

        case Auth(liveId, liveCode) =>
          client.auth(liveId, liveCode)
          Behaviors.same

        case AuthRsp(liveId, ifSuccess) =>
          println(liveId + " auth " + ifSuccess)
          if (ifSuccess) {
            context.self ! PushStream(liveId)
            context.self ! PullStream(List[String](liveId))
          }
          Behaviors.same

        case PullStream(liveId) =>
          println(liveId)
          client.pullStreamData(liveId)
          Behaviors.same

        case PushStream(liveId) =>
          i = i + 1
          if (i % 10000000 == 1) {
            i = 0
            //  println("push stream " + liveId)
            client.pushStreamData(liveId, "123456".getBytes("UTF-8"))
          }
          context.self ! PushStream(liveId)
          Behaviors.same

        case PushStreamError(liveId, errCode, msg) =>
          println("push stream error: " + msg)
          Behaviors.same

        case PullStreamReqSuccess(liveIds) =>
          println("liveids=====" + liveIds.mkString("#"))
//          decodeThread.start()
//          remoteDataDrawer.start()
          Behaviors.same

        case msg: PullStreamData =>
          if (msg.data.nonEmpty) {
//            log.debug(s"****************pull-${msg.data.length}*************")
            try {
              sink.write(ByteBuffer.wrap(msg.data))
              Behaviors.same
            } catch {
              case ex: Exception =>
                log.warn(s"sink write pulled data error: $ex. Stop StreamPuller")
                if (!client.pullStreamThread.isInterrupted) {
                  log.info(s"interrupt thread-${client.pullStreamThread}")
                  client.pullStreamThread.interrupt()
                }
                Behaviors.stopped
            }
          } else {
            Behaviors.same
          }

        case PullStreamPacketLoss =>
          println("pull stream again")
          Behaviors.same

        case x =>
          log.info(s"recv unknown msg: $x")
          Behaviors.same
      }
    }
  }
}

