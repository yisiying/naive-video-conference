//package org.seekloud.theia.processor.core
//
//import java.io.OutputStream
//import java.net.{InetSocketAddress, Socket}
//import java.nio.{Buffer, ByteBuffer}
//
//import scala.concurrent.duration._
//import akka.actor.typed.Behavior
//import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
//import org.slf4j.LoggerFactory
//import org.seekloud.theia.processor.common.AppSettings._
//import org.seekloud.theia.processor.core.WrapperActor.PayloadType
//
//import scala.collection.mutable
//import scala.concurrent.duration._
//
//
///**
//  * User: yuwei
//  * Date: 2019/8/28
//  * Time: 20:32
//  * edit by sky 2019/10/18
//  * 对接distributor线程
//  */
//object SocketActor {
//  private val log = LoggerFactory.getLogger(this.getClass)
//
//  trait Command
//
//  case object Timer4Heart
//
//  case object Timer4InitSocket
//
//  case object Time4ReConnect
//
//  val heartBeat: Byte = 4
//
//  val buf = new Array[Byte](1316)
//
//  case class Packet4Dispatcher(dataArray: Array[Byte]) extends Command
//
//  case object InitSocket extends Command
//
//  case object HeartBeat extends Command
//
//  //fixme 此处需要
//  val roomMap = mutable.Map[Long, Long]()
//
//  def create(): Behavior[Command] = {
//    Behaviors.setup[Command] { ctx =>
//      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
//      Behaviors.withTimers[Command] {
//        implicit timer =>
//          log.info(s"create| init...")
//          timer.startPeriodicTimer(Timer4Heart, HeartBeat, 2.minutes)
//          ctx.self ! InitSocket
//          init("init|")
//      }
//    }
//  }
//
//  def init(
//            logPrefix: String
//          )(implicit timer: TimerScheduler[Command],
//            stashBuffer: StashBuffer[Command]) = Behaviors.receive[Command] { (ctx, msg) =>
//    msg match {
//      case InitSocket =>
//        val socket = try {
//          new Socket(distributorHost, 30391)
//        } catch {
//          case e: Exception =>
//            log.info(s"$logPrefix connect tcp failed $e")
//            null
//        }
//        if (socket != null) {
//          log.info(s"$logPrefix connect tcp success")
//          val dataBuf = ByteBuffer.allocate(1316)
//          roomMap.foreach { r =>
//            dataBuf.clear()
//            dataBuf.putLong(r._2)
//            ctx.self ! SocketActor.Packet4Dispatcher(packTs4Dispatcher(PayloadType.newLive, r._1, dataBuf.array().clone(), 8))
//          }
//          work("work|", socket.getOutputStream)
//        } else {
//          timer.startSingleTimer(Timer4InitSocket, InitSocket, 10.seconds)
//          Behaviors.same
//        }
//
//      case x =>
//        //remind 忽略所有其他消息
//        Behaviors.same
//    }
//  }
//
//  def work(
//            logPrefix: String,
//            output: OutputStream
//          )(implicit timer: TimerScheduler[Command],
//            stashBuffer: StashBuffer[Command]): Behavior[Command] = {
//    Behaviors.receive[Command] { (ctx, msg) =>
//      msg match {
//        case t: Packet4Dispatcher =>
//          try {
//            output.write(t.dataArray)
//            Behaviors.same
//          } catch {
//            case e: Exception =>
//              log.info(s"$logPrefix connect tcp to $distributorHost failed $e")
//              ctx.self ! InitSocket
//              init("init|")
//          }
//
//        case HeartBeat =>
//          ctx.self ! Packet4Dispatcher(packTs4Dispatcher(heartBeat, 0, buf.clone(), 1))
//          Behaviors.same
//
//        case x =>
//          log.error(s"$logPrefix receive unknown $x")
//          Behaviors.same
//
//      }
//    }
//  }
//
//  def packTs4Dispatcher(payload: Byte, roomId: Long, ts: Array[Byte], valid: Long) = {
//    val packBuf = ByteBuffer.allocate(1327) // 1 + 2 + 8 + 188*7
//    //fixme 是否需要
//    packBuf.clear()
//    packBuf.put(payload)
//    packBuf.put(toByte(roomId, 8))
//    packBuf.put(toByte(valid, 2))
//    packBuf.put(ts)
//    packBuf.flip()
//    packBuf.array()
//  }
//
//  def toByte(num: Long, byte_num: Int) = {
//    (0 until byte_num).map { index =>
//      (num >> ((byte_num - index - 1) * 8) & 0xFF).toByte
//    }.toArray
//  }
//}
