package org.seekloud.theia.webrtcServer.http

import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.model.headers.HttpOriginRange
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import ch.megard.akka.http.cors.scaladsl.model.HttpOriginMatcher
import org.seekloud.theia.webrtcMessage.http.ServiceUtils
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * User: Taoz
  * Date: 8/26/2016
  * Time: 10:27 PM
  */
trait HttpService extends ServiceUtils
  with SocketService {

  implicit val system: ActorSystem

  implicit val executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler

  private val crossSetting = CorsSettings.defaultSettings.withAllowedOrigins(
    HttpOriginMatcher.*
  )

  lazy val httpRoutes: Route = ignoreTrailingSlash {
    pathPrefix("webrtcServer") {
      cors(crossSetting) {
        joinRoute ~ path("test") {
          complete("ok")
        }
      }
    }
  }


}
