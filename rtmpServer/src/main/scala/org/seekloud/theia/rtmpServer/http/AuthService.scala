package org.seekloud.theia.rtmpServer.http

/**
  * Created by LTy on 19/5/24
  */

import akka.http.scaladsl.server.Directives.path
import org.slf4j.LoggerFactory
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import io.circe._
import io.circe.syntax._
import org.seekloud.theia.rtmpServer.Boot.{authActor, executor, scheduler, timeout}
import org.seekloud.theia.rtmpServer.core.{AuthActor, ConvertManager}
import akka.actor.typed.scaladsl.AskPattern._
import org.seekloud.theia.rtmpServer.utils.{SecureUtil, ServiceUtils}
import org.seekloud.theia.rtmpServer.protocol.CommonErrorCode._
import org.seekloud.theia.rtmpServer.http.SessionBase.{SessionTypeKey, VideoSession, VideoUserInfo, VideoUserSessionKey}
import org.seekloud.theia.rtmpServer.protocol.AuthProtocol.SuccessRsp
import org.seekloud.theia.rtmpServer.protocol.LiveProtocol.{OnConnect, OnPlay, OnPublish}
import org.seekloud.theia.rtmpServer.Boot.convertManager
import org.seekloud.theia.rtmpServer.core.{AuthActor, ConvertManager}
import org.seekloud.theia.rtmpServer.http.SessionBase.VideoUserSessionKey
import org.seekloud.theia.rtmpServer.protocol.LiveProtocol.{OnConnect, OnPlay, OnPublish}
import org.seekloud.theia.rtmpServer.protocol.AuthProtocol.token
import scala.concurrent.Future

trait AuthService extends ServiceUtils with SessionBase {
  private[this] val log = LoggerFactory.getLogger(this.getClass)

  private val connectReq = path("connectReq") {
    entity(as[Either[Error, OnConnect]]) {
      case Right(req) =>
//        println(s"connect-------------client:${req.client_id}")
//        println(req.tcUrl)
        complete(0)
      case Left(e) =>
        complete(1)
    }
  }

  private val publishReq = path("publishReq") { //推流鉴权
    entity(as[Either[Error, OnPublish]]) {
      case Right(req) =>
//        println(s"publish-------------client:${req.client_id}")
//        println(s"publish-------------client:${req.app}")
//        println(s"publish-------------client:${req.ip}")
//        println(s"publish-------------client:${req.param}")
//        println(s"publish-------------client:${req.stream}")
//        println(s"publish-------------client:${req.vhost}")
//        println(s"publish-------------client:$req")
        val params = req.param.drop(1)
        val password = req.stream
        val msg:Future[Int] = authActor ? (AuthActor.Verify4Push(params, password, _))
        dealFutureResult(
          msg.map {
            rsp =>
              println(rsp)
              complete(rsp)
          }
        )
//        complete(0)

      case Left(e) =>
        log.debug("publishReq decode fail.")
        complete(1)
    }
  }

  private val unPublishReq = path("unPublishReq") { //推流鉴权
    entity(as[Either[Error, OnPublish]]) {
      case Right(req) =>
        println(s"unPublish-------------client:${req.client_id}")
        println(s"unpublish-------------client:${req.param}")
        val params = req.param.drop(1)
        var rtmpToken = ""
        params.split("&").foreach{
          t =>
            if(t.contains("rtmpToken")){
              try {
                rtmpToken = t.drop(10)
              } catch {
                case e:Exception=>
                  log.debug(s"Errs 100003, $e")
              }
            }
        }
        token -= rtmpToken
//        val params = req.param.drop(1)
//        if(params.contains("model=0")) {
//          params.split("&").foreach { c =>
//            if (c.contains("roomId")) {
//              convertManager ! ConvertManager.StopPublish(c.drop(7).toLong)
//            }
//          }
//        }
//        if(params.contains("model=4")) {
//          params.split("&").foreach { c =>
//            if(c.contains("roomId")) {
//              val roomId = c.drop(7).toLong
//              convertManager ! StoreRoomState(roomId, 10006)
//            }
//          }
//        }
        complete(0)
      case Left(e) =>
        log.debug("unpublishReq decode fail.")
        complete(1)
    }
  }

  private val playReq = path("playReq") {
    entity(as[Either[Error, OnPlay]]) {
      case Right(req) =>
//        println(s"play-------------client:${req.client_id}")
//        println(s"publish-------------client:${req.param}")
        val params = req.param.drop(1)
        var rtmpToken = ""
        params.split("&").foreach{
          t =>
            if(t.contains("rtmpToken")){
              try {
                rtmpToken = t.drop(10)
              } catch {
                case e:Exception=>
                  log.debug(s"Errs 100003, $e")
              }
            }
        }
        if(token.contains(rtmpToken)){
          complete(0)
        }
        else {
          log.debug("token validate error")
          complete(1)
        }

      case Left(e) =>
        log.debug("publishReq decode fail.")
        complete(1)
    }
  }

  val authRoute: Route = connectReq ~ publishReq ~ playReq ~ unPublishReq
}
