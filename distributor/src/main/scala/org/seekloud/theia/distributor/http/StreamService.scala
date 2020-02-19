package org.seekloud.theia.distributor.http

import akka.http.scaladsl.server.Directives.{as, complete, entity, path, pathPrefix, _}
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.model.HttpOriginMatcher
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import io.circe.Error
import io.circe.generic.auto._
import org.seekloud.theia.distributor.core.{LiveManager, PullActor,KillFFActor}
import org.seekloud.theia.distributor.protocol.CommonErrorCode._
import akka.actor.typed.scaladsl.AskPattern._
import org.seekloud.theia.distributor.Boot.{executor, liveManager, pullActor, scheduler, timeout,killFFActor}
import org.seekloud.theia.distributor.core.LiveManager.{liveStop, updateRoom}
import org.seekloud.theia.protocol.ptcl.distributor2Manager.DistributorProtocol._
import org.seekloud.theia.distributor.utils.ServiceUtils
import org.slf4j.LoggerFactory

import scala.concurrent.Future

trait StreamService extends ServiceUtils with SessionBase {

  private val log = LoggerFactory.getLogger(this.getClass)


  private val settings = CorsSettings.defaultSettings.withAllowedOrigins(
    HttpOriginMatcher.*
  )

  val newLive: Route = (path("startPull") & post) {

      entity(as[Either[Error, StartPullReq]]) {
        case Right(req) =>
          log.info(s"post method newLiveInfo.")
          val startTime = System.currentTimeMillis()
          liveManager ! updateRoom(req.roomId, req.liveId, startTime)
          val addr = s"/theia/distributor/getFile/${req.roomId}/index.mpd"
          complete(StartPullRsp(0, s"got liveId${req.liveId}", liveAdd = addr, startTime))

        case Left(_) =>
          complete(StartPullRsp(1000103, "parse json error", "", 0l))
      }

  }

  val stopLive: Route = (path("finishPull") & post) {

      entity(as[Either[Error, FinishPullReq]]) {
        case Right(req) =>
          log.info(s"post method stopLiveInfo.")
          liveManager ! liveStop(req.liveId)
          complete(SuccessRsp())

        case Left(_) =>
          complete(parseJsonError)
      }

  }

  val checkLive: Route = (path("checkStream") & post) {

      entity(as[Either[Error, CheckStreamReq]]) {
        case Right(req) =>
          log.info(s"post method checkLiveInfo.")
          val rst: Future[CheckStreamRsp] = liveManager ? (LiveManager.CheckLive(req.liveId, _))
          dealFutureResult(rst.map
          (rsp =>
            complete(rsp)
          )
          )

        case Left(e) =>
          complete(parseJsonError)
      }

  }

  val getTransLiveInfo: Route = (path("getTransLiveInfo") & get) {
    //        log.info(s"post method getAllLiveInfo.")
      dealFutureResult {
        val rst = liveManager ? LiveManager.GetAllLiveInfo
        rst.map {
          rsp =>
            complete(rsp)
        }
      }
  }

  val getPullStreamInfo = (path("getPullStreamInfo") & get) {
    val FutureRsp : Future[GetPullStreamInfoRsp] = pullActor ? (PullActor.GetAllInfo(_))
    dealFutureResult(FutureRsp.map(rsp =>
      complete(rsp)
    ))
  }

  val killFF = (path("killFF") & post) {
    entity(as[Either[Error, FinishPullReq]]) {
      case Right(req) =>
        log.info(s"post method KillFF.")
        killFFActor ! KillFFActor.Stop
        complete(SuccessRsp())

      case Left(_) =>
        complete(parseJsonError)
    }
  }

  val userSessionJudge = (path("userSessionJudge") & get) {
    adminAuth{ user =>

          val data = UserSessionInfo(user.userInfo.userName)
          complete(GetSessionRsp(Some(data)))


    }
  }






  val streamRoute: Route = pathPrefix("admin") {
    getTransLiveInfo  ~ getPullStreamInfo
  } ~ newLive ~ stopLive ~ checkLive ~ userSessionJudge ~ killFF
}
