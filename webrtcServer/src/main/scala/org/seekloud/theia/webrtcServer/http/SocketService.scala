package org.seekloud.theia.webrtcServer.http

import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directives.{handleWebSocketMessages, parameter, path}
import akka.http.scaladsl.server.Route
import akka.actor.typed.scaladsl.AskPattern._
import akka.stream.scaladsl.Flow
import org.seekloud.theia.protocol.ptcl.CommonInfo.LiveInfo
import org.seekloud.theia.webrtcMessage.http.ServiceUtils
import org.slf4j.LoggerFactory
import org.seekloud.theia.webrtcServer.Boot.{executor, liveManager, scheduler, timeout}
import org.seekloud.theia.webrtcServer.core.LiveManager

import scala.concurrent.Future

/**
  * Created by sky
  * Date on 2019/6/14
  * Time at 下午3:52
  * 本文件与前端建立socket连接
  */
trait SocketService extends ServiceUtils {
  private val log = LoggerFactory.getLogger(this.getClass)

  private def userJoin = path("userJoin") {
    parameter(
      'liveId.as[String],
      'liveCode.as[String]
    ) { (liveId, liveCode) =>
      val flowFuture: Future[Flow[Message, Message, Any]] = liveManager ? (LiveManager.GetWebSocketFlow(_, LiveInfo(liveId,liveCode)))
      dealFutureResult(
        flowFuture.map(t => handleWebSocketMessages(t))
      )
    }
  }

  val joinRoute: Route = userJoin
}
