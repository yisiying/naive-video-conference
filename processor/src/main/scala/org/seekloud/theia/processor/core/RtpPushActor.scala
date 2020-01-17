//package org.seekloud.theia.processor.core
//
//import java.net.InetSocketAddress
//
//import akka.actor.typed.Behavior
//import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
//import org.seekloud.theia.processor.common.AppSettings
//import org.seekloud.theia.processor.utils.NetUtil
//import org.seekloud.theia.rtpClient.{Protocol, PushStreamClient}
//import org.slf4j.LoggerFactory
//
//import scala.collection.mutable
//import scala.concurrent.duration._
//
///**
//  * Created by sky
//  * Date on 2019/10/18
//  * Time at 上午10:36
//  * 推流到rtp线程
//  */
//object RtpPushActor {
//  private val log = LoggerFactory.getLogger(this.getClass)
//  type Command = Protocol.Command
//
//  case class NewLive(liveId: String, liveCode: String) extends Command
//
//  case class PacketPush4Rtp(liveId: String, data: Array[Byte]) extends Command
//
//  case object PushStop extends Command
//
//  case object Time4AuthAgainKey
//
//  private val liveIdCodeMap = mutable.Map[String, String]()
//  private val authCountMap = mutable.Map[String, Int]()
//
//  def create(): Behavior[Command] = {
//    Behaviors.setup[Command] { ctx =>
//      Behaviors.withTimers[Command] {
//        implicit timer =>
//          log.info(s"create| init...")
//          val pushStreamDst = new InetSocketAddress(AppSettings.rtpToHost, 61041)
//          val client = new PushStreamClient("0.0.0.0", NetUtil.getRandomPort(), pushStreamDst, ctx.self, AppSettings.rtpServerDst)
//          client.authStart()
//          work("work|", client)
//      }
//    }
//  }
//
//  def work(logPrefix: String, client: PushStreamClient)(
//    implicit timer: TimerScheduler[Command]
//  ) = Behaviors.receive[Command] { (ctx, msg) =>
//    msg match {
//      case t: NewLive =>
//        client.auth(t.liveId, t.liveCode)
//        log.info(s"${t.liveId} start push auth ----")
//        authCountMap.put(t.liveId, 0)
//        liveIdCodeMap.put(t.liveId, t.liveCode)
//        Behaviors.same
//
//
//      case t: Protocol.AuthRsp =>
//        if (t.ifSuccess) {
//          log.info(s"$logPrefix ${t.liveId} auth successfully ---")
//          liveIdCodeMap.remove(t.liveId)
//          authCountMap.remove(t.liveId)
//        } else {
//          log.info(s"$logPrefix ${t.liveId} auth fails -----")
//          val authTime = authCountMap.getOrElse(t.liveId, 1)
//          if (liveIdCodeMap.get(t.liveId).isDefined && authTime < 5) {
//            timer.startSingleTimer(Time4AuthAgainKey, NewLive(t.liveId, liveIdCodeMap(t.liveId)), 5.seconds)
//            authCountMap.put(t.liveId, authTime + 1)
//          }
//        }
//        Behaviors.same
//
//      case t: PacketPush4Rtp =>
//        client.pushStreamData(t.liveId, t.data)
//        Behaviors.same
//
//      case Protocol.PushStreamError(liveId, errCode, msg) =>
//        log.error(s"$logPrefix push stream error: $liveId" + msg)
//        Behaviors.same
//
//      case PushStop=>
//        log.info(s"$logPrefix push stream stop")
//        client.close()
//        Behaviors.same
//
//      case Protocol.CloseSuccess=>
//        log.info(s"$logPrefix push stream close success")
//        Behaviors.stopped
//
//      case x =>
//        log.info(s"$logPrefix recv unknown msg: $x")
//        Behaviors.same
//    }
//  }
//
//
//}
