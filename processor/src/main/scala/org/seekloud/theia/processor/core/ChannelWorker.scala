//package org.seekloud.theia.processor.core
//
///**
//  * User: yuwei
//  * Date: 7/15/2019
//  */
//import java.io.{File, FileInputStream, FileOutputStream, OutputStream, PipedInputStream, PipedOutputStream}
//
//import akka.actor.typed.{ActorRef, Behavior}
//import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
//import akka.http.scaladsl.server.util.TupleOps.Join
//import org.slf4j.LoggerFactory
//import org.seekloud.theia.processor.common.AppSettings._
//
//import scala.language.implicitConversions
//import scala.collection.mutable
//import scala.concurrent.duration._
//import java.net.{DatagramSocket, InetSocketAddress}
//import java.nio.ByteBuffer
//import java.nio.channels.{DatagramChannel, Pipe}
//
//import org.seekloud.theia.processor.Boot
//import org.seekloud.theia.processor.Boot.{blockingDispatcher, grabberManager, roomManager}
//import org.seekloud.theia.processor.core.GrabberManager.PullStreamSuccess
//import org.seekloud.theia.processor.core.RoomManager.{CloseRoom, Switch2Single}
//import org.seekloud.theia.rtpClient.PullStreamClient
//import org.seekloud.theia.rtpClient.Protocol._
//import org.seekloud.theia.processor.common.AppSettings.rtpServerDst
//
//import scala.util.Try
//
///**
//  * User: yuwei
//  * Date: 7/15/2019
//  */
//object ChannelWorker {
//
//  val log = LoggerFactory.getLogger(this.getClass)
//  private final val InitTime = Some(5.minutes)
//
////  sealed trait Command
//
//  case object Stop extends Command
//
//  case class RoomClose(lives: List[String]) extends Command
//
//  case class NewBuffer(data: Array[Byte]) extends Command
//
//  case class DispatchData(data: Array[Byte]) extends Command
//
//  case class NewSend(live: String) extends Command
//
//  case object TimerKey
//
//  case object TimeKey4Clean
//
//  case object CloseWriter extends Command
//
//  case class NewLive(live: String, roomId:Long, status:String, pos: Pipe.SinkChannel) extends Command
//
//  case class Recorder(rec: ActorRef[RecorderActor.Command]) extends Command
//
//  case class RecorderRef(roomId: Long, liveId:String, ref: ActorRef[RecorderActor.Command]) extends Command
//
//  case object Listen extends Command
//
//  case class Timer4NewLive(live:String)
//
//  case class StartNewLive(live:String, pop:Pipe.SinkChannel,output:Option[FileOutputStream]) extends Command
//
//  case class Ready(client: PullStreamClient) extends Command
//  case class PushStream(liveId: String) extends Command
//  case object PullStream extends Command
//  case class CleanStream(liveId:String) extends Command
//
//  case class GetTs(reply:ActorRef[Long]) extends Command
//
//  case class LiveInfo(roomId:Long, status:String)
//
//  var pullClient:PullStreamClient = _
//
//  private val liveWriterMap = mutable.Map[String, ActorRef[Command]]()
//  private val liveInfoMap = mutable.Map[String, LiveInfo]()
//  private val liveCountMap = mutable.Map[String,Int]()
//  private val roomHostMap = mutable.Map[Long, String]()//记录一个房间里主播流的变化情况
//
//  object Rtp_header {
//    var m = 0
//    var timestamp = 0l
//    var payloadType = 111.toByte //负载类型号96
//  }
//
//  def create(): Behavior[Command] = {
//    Behaviors.setup[Command] { ctx =>
//      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
//      Behaviors.withTimers[Command] {
//        implicit timer =>
//          log.info(s"channelWorker start----")
//          val pullStreamDst = new InetSocketAddress(rtpToHost, 61040)
//          val host = "0.0.0.0"
//          val port = getRandomPort()
//          val client = new PullStreamClient(host,port,pullStreamDst,ctx.self,rtpServerDst)
//          pullClient = client
//          ctx.self ! Ready(client)
//          wait()
//      }
//    }
//  }
//
//  def getRandomPort() = {
//    val channel = DatagramChannel.open()
//    val port = channel.socket().getLocalPort
//    channel.close()
//    port
//  }
//
//  def wait()(implicit timer: TimerScheduler[Command],
//           stashBuffer: StashBuffer[Command]): Behavior[Command] = {
//    Behaviors.receive[Command] { (ctx, msg) =>
//      msg match {
//        case m@Ready(client) =>
//          log.info(s"got msg: $m")
//          stashBuffer.unstashAll(ctx, work(client))
//
//        case x =>
//          stashBuffer.stash(x)
//          Behavior.same
//      }
//    }
//  }
//
//  def work(client:PullStreamClient)(implicit timer: TimerScheduler[Command],
//             stashBuffer: StashBuffer[Command]): Behavior[Command] = {
//    client.pullStreamStart()
//    Behaviors.receive[Command] { (ctx, msg) =>
//      msg match {
//        case m@NewLive(live, roomId, status, pos) =>
//          log.info(s"got msg: $m")
//          val output = if(isDebug){
//            val file = new File(s"$debugPath$roomId/${live}_in.ts")
//            Some(new FileOutputStream(file))
//          } else None
//
//          liveInfoMap.put(live,LiveInfo(roomId,status))
//          liveCountMap.put(live, 0)
//          ctx.self ! StartNewLive(live, pos, output)
//          if(status == "host"){
//            roomHostMap.put(roomId, live)
//          }
//          Behaviors.same
//
//        case m@StartNewLive(live, pos,output) =>
//          log.info(s"got msg: $m")
//          val writer = ctx.spawn(write(pos, live, output), "writer" + live,blockingDispatcher)
//          liveWriterMap.put(live, writer)
//          ctx.self ! PullStream
//          Behaviors.same
//
//        case PullStream =>
//          val lives = liveWriterMap.keys.toList
//          client.pullStreamData(lives)
//          Behaviors.same
//
//        case StreamStop(liveId) =>
//          ctx.self ! CleanStream(liveId)
//          Behaviors.same
//
//        case GetTs(reply) =>
//          val ts = client.getServerTimestamp()
//          reply ! ts
//          Behaviors.same
//
//        case CleanStream(liveId) =>
//          log.info(s"$liveId does not have stream for 30 secs and kill it")
//          val infoOpt = liveInfoMap.get(liveId)
//          if(infoOpt.isDefined) {
//            val info = infoOpt.get
//            if(info.status=="host") {
//              if(roomHostMap.get(info.roomId).isDefined && roomHostMap(info.roomId) == liveId){
//                //roomManager ! CloseRoom(info.roomId)
//                roomHostMap.remove(info.roomId)
//              }
//            }else{
//              //roomManager ! Switch2Single(info.roomId)
//            }
//            liveInfoMap.remove(liveId)
//          }else{
//            log.debug(s"$liveId info not exist.")
//          }
//          Behaviors.same
//
//        case m@PullStreamReqSuccess(liveIds) =>
//          log.info(s"got msg: $m")
//          liveIds.foreach{liveId =>
//            grabberManager ! PullStreamSuccess(liveId)
//          }
//          Behaviors.same
//
//        case m@NoStream(lives) =>
//          log.error(s"got msg: $m")
//          Behaviors.same
//
//        case PullStreamPacketLoss =>
//          Behaviors.same
//
//        case PullStreamData(liveId, seq, data) =>
//          if(Boot.showStreamLog){
//            log.info(s"streamData $liveId $seq")
//          }
//          if(liveWriterMap.get(liveId).isDefined){
//            val writer = liveWriterMap(liveId)
//            if(liveCountMap.getOrElse(liveId,0) < 5){
//              log.info(s"$liveId get stream --")
//              liveCountMap.update(liveId, liveCountMap(liveId) +1)
//            }
//            writer ! NewBuffer(data)
//          }else{
//            log.error(s"cant't find liveWrite for liveId=$liveId")
//          }
//          Behaviors.same
//
//
//        case RoomClose(lives) =>
//          log.info(s"got RoomClose Msg: $lives")
//          lives.foreach{
//            i =>
//              liveWriterMap.get(i).foreach{
//                w =>
//                  w ! CloseWriter
//              }
//              liveWriterMap.remove(i)
//          }
//          ctx.self ! PullStream
//          Behaviors.same
//
//        case x =>
//          log.info(s"recv unknown msg: $x")
//          Behaviors.same
//      }
//    }
//  }
//  def write(pos: Pipe.SinkChannel, live: String, out:Option[FileOutputStream])(implicit timer: TimerScheduler[Command],
//                                                  stashBuffer: StashBuffer[Command]): Behavior[Command] = {
//    log.info(s"$live writer start--")
//    Behaviors.receive[Command] { (ctx, msg) =>
//      msg match {
//        case NewBuffer(data) =>
//          if(Boot.showStreamLog){
//            log.info(s"NewBuffer $live ${data.length}")
//          }
//          out.foreach(_.write(data))
//          if(GrabberManager.pipeMap.get(live).isDefined) {
//            pos.write(ByteBuffer.wrap(data))
//          }else {
//            log.info(s"pipe 4 $live writer not exists")
//          }
//          Behaviors.same
//
//        case CloseWriter =>
//          log.info(s"$live writer close--")
//          pos.close()
//          out.foreach(_.close())
//          Behaviors.stopped
//      }
//    }
//  }
//
//}
