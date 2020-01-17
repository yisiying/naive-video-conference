package org.seekloud.theia.rtmpServer.http

import akka.http.scaladsl.server.Directives.path
import akka.actor.typed.scaladsl.AskPattern._
import org.slf4j.LoggerFactory
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.seekloud.theia.rtmpServer.common.AppSettings.srsHost
import io.circe.generic.auto._
import io.circe._
import org.seekloud.theia.rtmpServer.Boot.{authActor, executor, scheduler, timeout}
import org.seekloud.theia.rtmpServer.core.{AuthActor, ConvertManager}
import org.seekloud.theia.rtmpServer.utils.ServiceUtils
import org.seekloud.theia.rtmpServer.protocol.CommonErrorCode._
import org.seekloud.theia.rtmpServer.protocol.LiveProtocol
import org.seekloud.theia.rtmpServer.protocol.AuthProtocol.SuccessRsp
import org.seekloud.theia.rtmpServer.core.{AuthActor, ConvertManager}
import org.seekloud.theia.rtmpServer.protocol.LiveProtocol

import scala.concurrent.Future
/**
  * User: yuwei
  * Date: 2019/5/28
  * Time: 19:38
  */
trait RoomService extends ServiceUtils with SessionBase {

  private[this] val log = LoggerFactory.getLogger(this.getClass)


}
