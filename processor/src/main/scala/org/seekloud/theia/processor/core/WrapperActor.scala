//package org.seekloud.theia.processor.core
//
//import java.io.{File, FileInputStream, FileOutputStream, PipedInputStream, PipedOutputStream}
//
//import akka.actor.typed.{ActorRef, Behavior}
//import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
//import org.slf4j.LoggerFactory
//
//import scala.language.implicitConversions
//import java.nio.ByteBuffer
//import java.nio.channels.AsynchronousCloseException
//import java.nio.channels.Pipe.SourceChannel
//
//import org.seekloud.theia.processor.Boot.sendActor
//import org.seekloud.theia.processor.core.SendActor.Packet4Dispatcher
//import org.seekloud.theia.processor.common.AppSettings._
//
//import scala.concurrent.duration._
//import scala.collection.mutable
//
//object WrapperActor {
//  private val log = LoggerFactory.getLogger(this.getClass)
//
//  trait Command
//
//  case object SendData extends Command
//
//  case object Close extends Command
//
//  case class NewLive(startTime: Long) extends Command
//
//  case class NewHostLive(startTime: Long, source: SourceChannel) extends Command
//
//  case object Timer4Stop
//
//  case object Timer4Send
//
//  case object Stop extends Command
//
//
//  object PayloadType {
//    val newLive: Byte = 1
//    val packet: Byte = 2
//    val closeLive: Byte = 3
//    val heartBeat: Byte = 4
//  }
//
//  //fixme 有何作用？
//  private val liveCountMap = mutable.Map[String, Int]()
//
//
//  def create(roomId: Long, liveId: String, source: SourceChannel, startTime: Long): Behavior[Command] = {
//    Behaviors.setup[Command] { ctx =>
//      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
//      Behaviors.withTimers[Command] {
//        implicit timer =>
//          log.info(s"${ctx.self} init ----")
//          ctx.self ! NewLive(startTime)
//          val out = if(isDebug){
//            val file = new File(s"$debugPath$roomId/${liveId}_out.ts")
//            Some(new FileOutputStream(file))
//          }else{
//            None
//          }
//          work(roomId, liveId,source,ByteBuffer.allocate(1316), out)
//      }
//    }
//  }
//
//
//  def work(roomId: Long,liveId:String, source:SourceChannel, dataBuf:ByteBuffer, out:Option[FileOutputStream])
//          (implicit timer: TimerScheduler[Command],
//           stashBuffer: StashBuffer[Command]): Behavior[Command] = {
//    Behaviors.receive[Command] { (ctx, msg) =>
//      msg match {
//        case NewLive(startTime) =>
//          log.info(s"startTime:$startTime")
//          liveCountMap.put(liveId, 0)
//          dataBuf.clear()
//          dataBuf.putLong(startTime)
//          if (SendActor.output != null) {
//            SendActor.output.write(packTs4Dispatcher(PayloadType.newLive, roomId, dataBuf.array().clone(), 8))
//          }
//          ctx.self ! SendData
//          dataBuf.clear()
//          Behaviors.same
//
//        case SendData =>
//          //fixme dataBuf 何种情况下会==null
//          val r = source.read(dataBuf)
//          dataBuf.flip()
//          if (r > 0) {
//            val data = dataBuf.array().clone()
//            out.foreach(_.write(data))
//            SendActor.client.pushStreamData(liveId, data.take(r))
//            val dataArray = packTs4Dispatcher(PayloadType.packet, roomId, data, r)
//            if (SendActor.output != null) {
//              SendActor.output.write(dataArray)
//            }
//            if (liveCountMap.getOrElse(liveId, 0) < 5) {
//              log.info(s"$liveId send data --")
//              liveCountMap.update(liveId, liveCountMap(liveId) + 1)
//            }
//            ctx.self ! SendData
//            dataBuf.clear()
//          } else {
//            log.info(s"wrapperActor got nothing, $r")
//          }
//
//          Behaviors.same
//
//        case m@NewHostLive(startTime, newSource) =>
//          log.info(s"got msg: $m")
//          timer.startSingleTimer(Timer4Send, NewLive(startTime), 500.millis)
//          work(roomId, liveId, newSource, dataBuf, out)
//
//        case Close =>
//          timer.startSingleTimer(Timer4Stop, Stop, 500.milli)
//          Behaviors.same
//
//        case Stop =>
//          log.info(s"$roomId wrapper stopped ----")
//          source.close()
//          try {
//            if (SendActor.output != null) {
//              SendActor.output.write(packTs4Dispatcher(PayloadType.closeLive, roomId, dataBuf.array().clone(), 0))
//            }
//          } catch {
//            case e: Exception =>
//              log.info(s"sendActor connect tcp to distributor failed $e")
//              sendActor ! SendActor.ReConnectSocket
//          }
//          dataBuf.clear()
//          out.foreach(_.close())
//          Behaviors.stopped
//
//        case x =>
//          log.info(s"${ctx.self} got an unknown msg:$x")
//          Behaviors.same
//      }
//    }
//  }
//
//  def packTs4Dispatcher(payload: Byte, roomId: Long, ts: Array[Byte], valid: Long) = {
//    val packBuf = ByteBuffer.allocate(1327) // 1 + 8 + 2 + 188
//    packBuf.clear()
//    packBuf.put(payload)
//    packBuf.put(toByte(roomId, 8))
//    packBuf.put(toByte(valid, 2))
//    packBuf.put(ts)
//    packBuf.flip()
//    packBuf.array()
//  }
//  def toByte(num: Long, byte_num: Int) = {
//    (0 until byte_num).map { index =>
//      (num >> ((byte_num - index - 1) * 8) & 0xFF).toByte
//    }.toArray
//  }
//
//
//}
