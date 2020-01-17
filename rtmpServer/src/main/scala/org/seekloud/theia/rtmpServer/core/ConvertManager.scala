package org.seekloud.theia.rtmpServer.core

/**
  * User: yuwei
  * Date: 2019/5/26
  * Time: 12:22
  */
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.http.scaladsl.server.util.TupleOps.Join
import org.seekloud.theia.protocol.ptcl.CommonInfo.LiveInfo
import org.slf4j.LoggerFactory
import org.seekloud.theia.rtmpServer.Boot.executor

import scala.language.implicitConversions
import scala.collection.mutable
import scala.concurrent.duration._

object ConvertManager {

  private val log = LoggerFactory.getLogger(this.getClass)
  private final val InitTime = Some(5.minutes)

  private final case object BehaviorChangeKey

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  case class NewLive(liveInfo:LiveInfo, obsUrl: String) extends Command

  case class StopPublish(roomId:Long) extends Command

  case class StartConvertActor(liveInfo:LiveInfo, obsUrl:String) extends Command

  case object TimerKey

  sealed trait Command

  case class TimeOut(msg: String) extends Command

  var count = 0

  private val roomMap = mutable.HashMap[Long, ActorRef[ConvertActor.Command]]()
  val roomClientMap = mutable.HashMap[Long, String]() //roomId, clientUrl(推流url)g
  val roomHostMap = mutable.HashMap[Long, (String, String, String)]()
  val roomCastMap = mutable.HashMap[Long, String]() //roomId, serverUrl

  def create():Behavior[Command] = {
    Behaviors.setup[Command]{
      ctx=>
        Behaviors.withTimers[Command] {
          implicit timer =>
            idle(timer)
        }
    }
  }

  def idle(timer:TimerScheduler[Command]):Behavior[Command] = {
    Behaviors.receive[Command] {
      (ctx,msg) =>
//        log.info(s"got msg:$msg")
        msg match {

          case l:NewLive =>
            val convertActor = getConvertActor(ctx, l.liveInfo, l.obsUrl)
            convertActor ! ConvertActor.Start
            Behaviors.same

          case t:StopPublish =>
            if(roomMap.get(t.roomId).isDefined){
              roomMap(t.roomId) ! ConvertActor.StopOver
            }
            roomMap.remove(t.roomId)
            roomClientMap.remove(t.roomId)
            roomHostMap.remove(t.roomId)
            roomCastMap.remove(t.roomId)
            Behaviors.same

          case ChildDead(name, childRef) =>
            log.info(s"-----------------$name is dead")
            Behaviors.same
        }
    }

  }

  private def getConvertActor(ctx: ActorContext[Command], liveInfo:LiveInfo, obsUrl:String): ActorRef[ConvertActor.Command] = {
    val childName = s"UserActor-$count-${liveInfo.liveId}"
    count +=1
    ctx.child(childName).getOrElse{
      log.info(s"create convert actor $childName")
      val actor = ctx.spawn(ConvertActor.idle(liveInfo, obsUrl), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[ConvertActor.Command]
  }

}
