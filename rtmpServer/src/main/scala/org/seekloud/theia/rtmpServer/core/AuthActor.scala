package org.seekloud.theia.rtmpServer.core

/**
  * Created by LTy on 19/5/24
  */

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import org.seekloud.theia.rtmpServer.utils.{RMClient, SecureUtil}
import org.slf4j.LoggerFactory
import org.seekloud.theia.rtmpServer.Boot.{convertManager, executor, grabberManager}
import org.seekloud.theia.rtmpServer.common.AppSettings.srsHost
import ConvertManager._
import org.seekloud.theia.protocol.ptcl.CommonInfo._
import org.seekloud.theia.rtmpServer.protocol.AuthProtocol.token

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.{Failure, Success}


object AuthActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class SaveParams(clientId:Long,params:String) extends Command

  case class GetLiveInfo(userId:String,rtmpToken:String,password:String, sender:ActorRef[Int]) extends Command

  case class Verify4Push(param:String,password:String,sender:ActorRef[Int]) extends Command

  case class VerifyToken4Broadcaster(liveInfo:LiveInfo,userId:String,rtmpToken: String,password:String, sender:ActorRef[Int]) extends Command

  case class Verify4Pull(params:String,sender:ActorRef[Int]) extends Command

  private var sign:Long = 0l

  val behaviors: Behavior[Command] = {
    log.debug(s"AuthActor start...")
    Behaviors.setup[Command] {
      ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            val paramMap = mutable.HashMap[Long, String]() // clientId -> params
            idle(paramMap)
        }
    }
  }

  def idle(paramMap:mutable.HashMap[Long, String])(implicit timer: TimerScheduler[Command]): Behavior[Command] =
    Behaviors.receive[Command] {
      (ctx, msg) =>
        msg match {
          case AuthActor.SaveParams(clientId, params) =>
            paramMap.put(clientId, params)
            Behaviors.same

          case GetLiveInfo(userId, rtmpToken, password, sender) =>

            RMClient.getLiveInfo(userId,rtmpToken).map{
              case Right(rsp) =>
                if(rsp.errCode == 0){
                  log.info("getLiveInfo success")
                  sender ! 0
                  ctx.self ! VerifyToken4Broadcaster(rsp.liveInfo.get, userId, rtmpToken, password, sender)
                }else{
                  sender ! 1
                  log.info("getLiveInfo failed")
                }

              case Left(error) =>
                  sender ! 1
                  log.info(s"getLiveInfo data failed: $error")
            }

            Behaviors.same


          case Verify4Push(params, password, sender) =>
            log.info(s"receive msg: $params")
            var rtmpToken: Option[String] = None
            var userId:Option[String] = None
            params.split("&").foreach { c =>
              if (c.contains("rtmpToken")) {
                try {
                  rtmpToken = Some(c.drop(10))
                  token += rtmpToken.getOrElse("null")
                } catch {
                  case e:Exception=>
                    log.debug(s"Errs 100001, $e")
                }
              }else if(c.contains("userId")){
                try {
                  userId = Some(c.drop(7))
                } catch {
                  case e:Exception=>
                    log.debug(s"Errs 100002, $e")
                }
              }
            }

            rtmpToken match {
              case Some(rToken) =>
                log.info(s"rtmpToken: $rToken")
                val id = userId.getOrElse("")
                println(s"id:$id")
                ctx.self ! GetLiveInfo(id, rToken, password, sender)

              case x =>
                log.debug("Some errs happened in Verify4Push.")
                log.debug(s"Wrong params:$params")
                sender ! 1
            }
            Behaviors.same

          case AuthActor.Verify4Pull(params, sender) =>
            var token: String = ""
            var roomId = 1l
            var userId = 1l
            var model = 0
            params.split("&").foreach { c =>
              if (c.contains("token")) {
                token = c.drop(6)
              } else if (c.contains("roomId")) {
                try{
                  roomId = c.drop(7).toLong
                }catch{
                  case e:Exception=>
                    log.debug(s"Errs 100004, $e")
                }
              } else if (c.contains("model")) {
                try{
                  model = c.drop(6).toInt
                }catch{
                  case e:Exception=>
                    log.debug(s"Errs 100005, $e")
                }
              } else if (c.contains("userId")) {
                try{
                  userId = c.drop(7).toLong
                }catch{
                  case e:Exception=>
                    log.debug(s"Errs 100006, $e")
                }
              }
            }
            model match {

              case x =>
                log.debug("Some errs happened in Verify4Pull.")
                log.debug(s"Wrong params:$params")
                sender ! 1
            }
            Behaviors.same

          case VerifyToken4Broadcaster(liveInfo, userId, rtmpToken, password, sender) =>
            val obsUrl = s"rtmp://$srsHost/live/$password?rtmpToken=$rtmpToken&userId=$userId"
            convertManager ! NewLive(liveInfo, obsUrl)
            Behaviors.same

          case x =>
            log.error(s"${ctx.self.path} receive an unknown msg when idle:$x")
            Behaviors.unhandled
        }
    }

}
