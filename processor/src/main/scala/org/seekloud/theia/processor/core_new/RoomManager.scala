package org.seekloud.theia.processor.core_new

import java.io.File

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.http.scaladsl.model.Uri.Host
import org.seekloud.theia.processor.common.AppSettings.{debugPath, isDebug, isTest}
import org.seekloud.theia.protocol.ptcl.processer2Manager.Processor
import org.seekloud.theia.protocol.ptcl.processer2Manager.Processor._
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * Created by sky
  * Date on 2019/10/22
  * Time at 下午2:27
  *
  * actor由Boot创建
  * 连线房间管理
  * 对接roomManager
  */
object RoomManager {

  private  val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class StartRoom(roomId: Long, hostLiveId: String, roomLiveId: String, layout: Int) extends Command

  case class NewConnection(roomId: Long, client: String, roomLiveId: String, layout: Int) extends Command

  case class UserOut(roomId: Long, client: String, roomLiveId: String) extends Command

  case class CloseRoom(roomId: Long) extends Command

  case class UpdateRoomInfo(roomId: Long, layout:Int ) extends Command

  case class RecorderRef(roomId: Long, ref: ActorRef[RecorderActor.Command]) extends Command

  case class SetSpokesman(roomId: Long, userLiveIdOpt: Option[String], roomLiveId: String) extends Command

  case class UpdateBlock(roomId: Long, userLiveId: String, iOS: Int, aOD: Int, roomLiveId: String) extends Command

  case class ChildDead(roomId: Long, childName: String, value: ActorRef[RoomActor.Command]) extends Command

  def create(): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"roomManager start----")
          work( mutable.Map[Long,ActorRef[RoomActor.Command]]())
      }
    }
  }

  def work(roomInfoMap: mutable.Map[Long, ActorRef[RoomActor.Command]])
          (implicit stashBuffer: StashBuffer[Command],
    timer:TimerScheduler[Command]):Behavior[Command] = {
    log.info(s"roomManager is working")
    Behaviors.receive[Command]{ (ctx, msg) =>
      msg match {
        case StartRoom(roomId, hostLiveId, roomLiveId, layout) =>
          log.info(s"${ctx.self} receive StartRoom msg roomId: ${roomId}")
          val roomActor = getRoomActor(ctx, roomId, roomLiveId, layout)
          roomActor ! RoomActor.NewRoom(roomId, hostLiveId, roomLiveId, layout)
          roomInfoMap.put(roomId, roomActor)
          Behaviors.same

        case NewConnection(roomId, client, roomLiveId, layout) =>
          log.info(s"${ctx.self} receive new connection msg, roomId: $roomId, partnerLiveId: $client")
          val roomActor = getRoomActorOpt(ctx, roomId, roomLiveId)
          roomActor match {
            case Some(actor) => actor ! RoomActor.PartnerIn(roomId, client)
            case None => log.info(s"roomActor:$roomId is not exist")
          }
          Behaviors.same

        case UserOut(roomId, client, roomLiveId) =>
          log.info(s"${ctx.self} receive client out msg, roomId: $roomId, partnerLiveId: $client")
          val roomActor = getRoomActorOpt(ctx, roomId, roomLiveId)
          roomActor match {
            case Some(actor) => actor ! RoomActor.PartnerOut(roomId, client)
            case None => log.info(s"roomActor:$roomId is not exist")
          }
          Behaviors.same

        case SetSpokesman(roomId, userLiveIdOpt, roomLiveId) =>
          log.info(s"${ctx.self} receive set spokesman msg, roomId: $roomId, partnerLiveId: $userLiveIdOpt")
          val roomActor = getRoomActorOpt(ctx, roomId, roomLiveId)
          roomActor match {
            case Some(actor) => actor ! RoomActor.SetSpokesman(roomId, userLiveIdOpt)
            case None => log.info(s"roomActor:$roomId is not exist")
          }
          Behaviors.same

        case UpdateBlock(roomId, userLiveId, iOS, aOD, roomLiveId) =>
          log.info(s"${ctx.self} receive update image or sound msg, roomId: $roomId, partnerLiveId: $userLiveId")
          val roomActor = getRoomActorOpt(ctx, roomId, roomLiveId)
          roomActor match {
            case Some(actor) => actor ! RoomActor.UpdateBlock(roomId, userLiveId, iOS, aOD)
            case None => log.info(s"roomActor:$roomId is not exist")
          }
          Behaviors.same


        case msg:UpdateRoomInfo =>
          log.info(s"${ctx.self} receive a msg${msg}")
          val roomInfo = roomInfoMap.get(msg.roomId)
          if(roomInfo.nonEmpty){
            roomInfo.get ! RoomActor.UpdateRoomInfo(msg.roomId, msg.layout)
          }
          Behaviors.same

        case RecorderRef(roomId, ref) =>
          log.info(s"${ctx.self} receive a msg${msg}")
          val roomActor = roomInfoMap.get(roomId)
          if(roomActor.nonEmpty){
            roomActor.foreach(_ ! RoomActor.Recorder(roomId, ref) )
          }
          Behaviors.same

        case msg:CloseRoom =>
          log.info(s"${ctx.self} receive a msg:${msg} ")
          val roomInfo = roomInfoMap.get(msg.roomId)
          if(roomInfo.nonEmpty){
            roomInfo.get ! RoomActor.CloseRoom(msg.roomId)
          }
          roomInfoMap.remove(msg.roomId)
          Behaviors.same

        case ChildDead(roomId, childName, value) =>
          log.info(s"${childName} is dead ")
          roomInfoMap.remove(roomId)
          Behaviors.same

        case x =>
          log.info(s"${ctx.self} receive an unknown msg $x")
          Behaviors.same
      }
    }
  }

  def getRoomActor(ctx: ActorContext[Command], roomId: Long, roomLiveId: String, layout: Int) = {
    val childName = s"roomActor_${roomId}_$roomLiveId"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(RoomActor.create(roomId, roomLiveId, layout), childName)
      ctx.watchWith(actor, ChildDead(roomId, childName, actor))
      actor
    }.unsafeUpcast[RoomActor.Command]
  }

  private def getRoomActorOpt(ctx: ActorContext[Command], roomId: Long, roomLiveId: String) = {
    val childrenName = s"roomActor_${roomId}_$roomLiveId"
    ctx.child(childrenName).map(_.unsafeUpcast[RoomActor.Command])
  }




}
