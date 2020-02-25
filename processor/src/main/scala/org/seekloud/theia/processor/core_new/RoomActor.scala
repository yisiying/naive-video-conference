package org.seekloud.theia.processor.core_new

import java.io.{File, InputStream, OutputStream}
import java.nio.channels.Channels
import java.nio.channels.Pipe.{SinkChannel, SourceChannel}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.seekloud.theia.processor.common.AppSettings.{debugPath, isDebug}
import org.seekloud.theia.processor.stream.PipeStream
import org.seekloud.theia.processor.common.Constants.Part
import org.seekloud.theia.processor.core_new.RoomManager.UpdateBlock
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.collection.mutable

/**
  * Created by sky
  * Date on 2019/10/22
  * Time at 下午2:28
  *
  * actor由RoomManager创建
  * 连线房间
  * 管理多路grabber和一路recorder
  */
object RoomActor {

  private  val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class NewRoom(roomId: Long, hostLiveId: String, roomLiveId: String, layout: Int) extends Command

  case class PartnerIn(roomId: Long, partnerLiveId: String) extends Command

  case class PartnerOut(roomId: Long, partnerLiveId: String) extends Command

  case class UpdateRoomInfo(roomId: Long, layout: Int) extends Command

  case class Recorder(roomId: Long, recorderRef: ActorRef[RecorderActor.Command]) extends Command

  case class CloseRoom(roomId: Long) extends Command

  case class ChildDead4Grabber(roomId: Long, childName: String, value: ActorRef[GrabberActor.Command]) extends Command// fixme liveID

  case class ChildDead4Recorder(roomId: Long, childName: String, value: ActorRef[RecorderActor.Command]) extends Command

  case class SetSpokesman(roomId: Long, userLiveIdOpt: Option[String]) extends Command

  case class UpdateBlock(roomId: Long, userLiveId: String, iOS: Int, aOD: Int) extends Command

  case class ClosePipe(liveId: String) extends Command

  case object Timer4Stop

  case object Stop extends Command

  case class Timer4PipeClose(liveId: String)

  val pipeMap = mutable.Map[String, PipeStream]()

  def create(roomId: Long, roomLiveId: String, layout: Int): Behavior[Command] = {
    Behaviors.setup[Command]{ ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"grabberManager start----")
          work(mutable.Map[Long, mutable.Map[String, ActorRef[GrabberActor.Command]]](), mutable.Map[Long, ActorRef[RecorderActor.Command]](), mutable.Map[Long, List[String]]())
      }
    }
  }

  def work(
            //    grabberMap: mutable.Map[Long, List[ActorRef[GrabberActor.Command]]],
            grabberMap: mutable.Map[Long, mutable.Map[String, ActorRef[GrabberActor.Command]]],
            recorderMap: mutable.Map[Long,ActorRef[RecorderActor.Command]],
            roomLiveMap: mutable.Map[Long, List[String]]
  )(implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]):Behavior[Command] = {
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {

        case NewRoom(roomId: Long, hostLiveId: String, roomLiveId: String, layout: Int) =>
          log.info(s"${ctx.self} receive a NewRoom msg, roomId: $roomId")
          if (isDebug) {
            val file = new File(debugPath + roomId)
            if (!file.exists()) {
              file.mkdir()
            }
          }
          val recorderActor = getRecorderActor(ctx, roomId, hostLiveId, roomLiveId, layout)
          val grabber4host = getGrabberActor(ctx, roomId, hostLiveId, recorderActor)
          //          val grabber4client = getGrabberActor(ctx, roomId, client, recorderActor)
          grabberMap.put(roomId, mutable.Map(hostLiveId -> grabber4host))
          recorderMap.put(roomId, recorderActor)
          roomLiveMap.put(roomId, List(hostLiveId, roomLiveId))
          Behaviors.same

        case PartnerIn(roomId, partnerLiveId) =>
          log.info(s"get new partner join in, liveId: $partnerLiveId")
          val grabber4partner = getGrabberActor(ctx, roomLiveMap.keys.head, partnerLiveId, recorderMap(roomId))
          if (grabberMap.get(roomId).nonEmpty) {
            grabberMap(roomId).put(partnerLiveId, grabber4partner)
          } else {
            log.info(s"$roomId grabber not exist")
          }
          if (roomLiveMap.get(roomId).nonEmpty) {
            val newRoomLiveList = partnerLiveId :: roomLiveMap(roomId)
            roomLiveMap.update(roomId, newRoomLiveList)
          } else {
            log.info(s"room:$roomId not exist")
          }
          recorderMap(roomId) ! RecorderActor.UpdateClientList(partnerLiveId, Part.in)
          Behaviors.same

        case PartnerOut(roomId, partnerLiveId) =>
          log.info(s"partner out, liveId: $partnerLiveId")
          if (grabberMap(roomId).get(partnerLiveId).nonEmpty) {
            grabberMap(roomId)(partnerLiveId) ! GrabberActor.StopGrabber
            grabberMap(roomId).remove(partnerLiveId)
          } else {
            log.info(s"$partnerLiveId grabber not exist")
          }
          if (roomLiveMap.get(roomId).nonEmpty) {
            val newRoomLiveList = roomLiveMap(roomId).filterNot(_ == partnerLiveId)
            roomLiveMap.update(roomId, newRoomLiveList)
          }
          recorderMap(roomId) ! RecorderActor.UpdateClientList(partnerLiveId, Part.out)
          Behaviors.same

        case UpdateRoomInfo(roomId, layout) =>
          if(recorderMap.get(roomId).nonEmpty) {
            recorderMap.get(roomId).foreach(_ ! RecorderActor.UpdateRoomInfo(roomId,layout ))
          } else {
            log.info(s"${roomId} recorder not exist")
          }
          Behaviors.same

        case msg:Recorder =>
          log.info(s"${ctx.self} receive a msg $msg")
          val grabberActor = grabberMap.get(msg.roomId)
          if(grabberActor.isDefined){
            grabberActor.get.values.toList.foreach(_ ! GrabberActor.Recorder(msg.recorderRef))
          } else {
            log.info(s"${msg.roomId} grabbers not exist")
          }
          Behaviors.same

        case SetSpokesman(roomId, userLiveIdOpt) =>
          if (recorderMap.get(roomId).nonEmpty) {
            recorderMap(roomId) ! RecorderActor.ChangeSpokesman(userLiveIdOpt)
          } else {
            log.info(s"${roomId}  recorder not exist when setSpokesman")
          }
          Behaviors.same

        case UpdateBlock(roomId, userLiveId, iOS, aOD) =>
          if (recorderMap.get(roomId).nonEmpty) {
            recorderMap(roomId) ! RecorderActor.UpdateBlock(userLiveId, iOS, aOD)
          } else {
            log.info(s"${roomId}  recorder not exist when setSpokesman")
          }
          Behaviors.same

        case CloseRoom(roomId) =>
          log.info(s"${ctx.self} receive a msg $msg")
          if(grabberMap.get(roomId).nonEmpty){
            grabberMap(roomId).values.toList.foreach(_ ! GrabberActor.StopGrabber)
            grabberMap.remove(roomId)
          } else {
            log.info(s"${roomId}  grabbers not exist when closeRoom")
          }
          if(recorderMap.get(roomId).nonEmpty) {
            recorderMap(roomId) ! RecorderActor.StopRecorder
            recorderMap.remove(roomId)
          } else{
            log.info(s"${roomId}  recorder not exist when closeRoom")

          }
          if(roomLiveMap.get(roomId).nonEmpty){
            roomLiveMap.remove(roomId)
          } else {
            log.info(s"${roomId}  pipe not exist when closeRoom")
          }
          timer.startSingleTimer(Timer4Stop, Stop, 1500.milli)
          Behaviors.same

        case Stop =>
          log.info(s"${ctx.self} stopped ------")
          Behaviors.stopped

        case ChildDead4Grabber(roomId, childName, value) =>
          log.info(s"${childName} is dead ")
          grabberMap.remove(roomId)
          Behaviors.same

        case ChildDead4Recorder(roomId, childName, value) =>
          log.info(s"${childName} is dead ")
          recorderMap.remove(roomId)
          Behaviors.same

      }

    }
  }

  def getGrabberActor(ctx: ActorContext[Command], roomId: Long, liveId: String, recorderRef: ActorRef[RecorderActor.Command]) = {
    val childName = s"grabberActor${roomId}_$liveId"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(GrabberActor.create(roomId, liveId, recorderRef), childName)
      ctx.watchWith(actor,ChildDead4Grabber(roomId, childName, actor))
      actor
    }.unsafeUpcast[GrabberActor.Command]
  }

  def getRecorderActor(ctx: ActorContext[Command], roomId: Long, hostLiveId: String, roomLiveId: String, layout: Int) = {
    val childName = s"recorderActor_${roomId}_$roomLiveId"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(RecorderActor.create(roomId, hostLiveId, mutable.Map[String, Int](), roomLiveId, layout), childName)
      ctx.watchWith(actor,ChildDead4Recorder(roomId, childName, actor))
      actor
    }.unsafeUpcast[RecorderActor.Command]
  }





}
