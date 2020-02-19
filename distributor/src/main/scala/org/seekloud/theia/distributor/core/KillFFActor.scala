package org.seekloud.theia.distributor.core

import java.net.InetSocketAddress
import java.text.SimpleDateFormat

import scala.language.implicitConversions
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.bytedeco.javacpp.Loader
import org.slf4j.LoggerFactory
import org.seekloud.theia.distributor.common.AppSettings._
import org.seekloud.theia.distributor.common.AppSettings.recordLocation
import org.seekloud.theia.protocol.ptcl.distributor2Manager.DistributorProtocol.RecordData
import org.seekloud.theia.distributor.Boot.liveManager

import scala.collection.mutable
import scala.concurrent.duration._


object KillFFActor {
  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case object Stop extends Command


  def create(): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"KillFFActor start----")
          if (testKillFF) {

            timer.startSingleTimer(Stop, Stop, timeDelay.millis)
            work(mutable.Map[String, Long]())
          } else {

            work(mutable.Map[String, Long]())
          }
      }
    }
  }

  def timeDelay: Long ={//返回现在离24点所差的时间
    val a = new java.util.Date
    val df = new SimpleDateFormat("yyyy-MM-dd ")
    val df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val b = df.format(a)
    val b2 = df2.format(a)
    val time1 = df2.parse(b2).getTime
    val time2 = df2.parse(b+"23:59:59").getTime
    time2-time1//剩余时间
  }


  //  val killFFThread = new Thread(() => {
  //    try {
  //      while (true) {
  //        Thread.sleep(Int.MaxValue)
  //
  //
  //      }
  //    } catch {
  //      case e: Exception =>
  //        log.error(s"killFFThread catch exception: $e")
  //    }
  //  })

  def work(roomLiveMap: mutable.Map[String, Long])(implicit timer: TimerScheduler[Command],
             stashBuffer: StashBuffer[Command]): Behavior[Command] = {
    log.info("-----------------------测试是否杀ff线程")
    //    killFFThread.start()
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {

//        case Stop =>
//          roomLiveMap.foreach{ e =>
//            liveManager ! LiveManager.CheckStream(e._1)
//            liveManager ! LiveManager.liveStop(e._1)
//          }
//          Behaviors.same

        case Stop =>

          liveManager ! LiveManager.KillFF
          timer.startSingleTimer(Stop, Stop, 24.hour)
          Behaviors.same

        case _ =>
          Behaviors.same
      }
    }
  }


}