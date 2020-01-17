package org.seekloud.theia.faceAnalysis.utils

import org.seekloud.theia.faceAnalysis.common.Routes
import org.seekloud.theia.protocol.ptcl.client2Manager.http.CommonProtocol._
import org.slf4j.LoggerFactory

import scala.concurrent.Future


object RMClient extends HttpUtil {

  import io.circe.generic.auto._
  import io.circe.parser.decode
  import io.circe.syntax._
  import org.seekloud.theia.faceAnalysis.BootJFx.executor

  private val log = LoggerFactory.getLogger(this.getClass)

  def signUp(email:String,username: String, pwd: String): Future[Either[Throwable, SignUpRsp]] = {

    val methodName = "signUp"
    val url = Routes.signUp

//    fixme 头像等信息补充
    val data = SignUp(email, username, pwd, "").asJson.noSpaces

    postJsonRequestSend(methodName, url, Nil, data).map {
      case Right(jsonStr) =>
        decode[SignUpRsp](jsonStr)
      case Left(error) =>
        log.debug(s"sign up error: $error")
        Left(error)
    }

  }

  def signIn(userName: String, pwd: String): Future[Either[Throwable, SignInRsp]] = {

    val methodName = "signIn"
    val url = Routes.signIn
    val data = SignIn(userName, pwd).asJson.noSpaces
    postJsonRequestSend(methodName, url, Nil, data).map {
      case Right(jsonStr) =>
        decode[SignInRsp](jsonStr)
      case Left(error) =>
        log.debug(s"sign in error: $error")
        Left(error)
    }
  }

  def signInByMail(email: String, pwd: String): Future[Either[Throwable, SignInRsp]] = {

    val methodName = "signInByMail"
    val url = Routes.signInByMail
    val data = SignInByMail(email, pwd).asJson.noSpaces
    postJsonRequestSend(methodName, url, Nil, data).map {
      case Right(jsonStr) =>
        log.debug(s"sign in by mail")
        decode[SignInRsp](jsonStr)
      case Left(error) =>
        log.debug(s"sign in by mail error: $error")
        Left(error)
    }
  }

  def getRoomInfo(userId: Long, token: String): Future[Either[Throwable, RoomInfoRsp]] = {

    val methodName = "getRoomInfo"
    val url = Routes.getRoomInfo

    val data = GetRoomInfoReq(userId, token).asJson.noSpaces
    postJsonRequestSend(methodName, url, Nil, data, needLogRsp = false).map {
      case Right(jsonStr) =>
        decode[RoomInfoRsp](jsonStr)
      case Left(error) =>
        log.error(s"user-$userId getRoomInfo error: $error")
        Left(error)
    }
  }

  def getRoomList: Future[Either[Throwable, RoomListRsp]] = {
    val methodName = "getRoomList"
    val url = Routes.getRoomList

    getRequestSend(methodName, url, Nil).map {
      case Right(jsonStr) =>
        decode[RoomListRsp](jsonStr)
      case Left(error) =>
        log.debug(s"getRoomList error.")
        Left(error)

    }

  }

  def searchRoom(userId: Option[Long] = None, roomId: Long): Future[Either[Throwable, SearchRoomRsp]] = {

    val methodName = "searchRoom"
    val url = Routes.searchRoom

    val data = SearchRoomReq(userId, roomId).asJson.noSpaces

    postJsonRequestSend(methodName, url, Nil, data, needLogRsp = false).map {
      case Right(jsonStr) =>
        decode[SearchRoomRsp](jsonStr)
      case Left(error) =>
        log.debug(s"searchRoom error: $error")
        Left(error)
    }
  }

}
