package org.seekloud.theia.rtmpServer.utils


import org.seekloud.theia.protocol.ptcl.CommonInfo.LiveInfo
import org.seekloud.theia.rtmpServer.common.AppSettings._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.seekloud.theia.rtmpServer.Boot.executor

object RMClient extends HttpUtil {
  case class LiveInfoReq(userId:String,token:String)
  case class LiveInfoRsp(liveInfo:Option[LiveInfo],
                         errCode:Int=0,
                         msg:String="ok"
                        )


  val baseUrl = rmProtocol + "://" + rmHostName + ":" + rmPort + "/" + rmUrl
  val rtmpUrl = baseUrl + "/" + "rtmp"

  def getLiveInfo(userId:String,token:String)={
    val url = rtmpUrl + "/getLiveInfo"
    val data = LiveInfoReq(userId,token).asJson.noSpaces

    postJsonRequestSend("getLiveInfo",url,Nil,data).map{
      case Right(json) =>
        decode[LiveInfoRsp](json)
      case Left(error) =>
        log.info(s"get error: ${error.getMessage}")
        Left(error.getMessage)
    }
  }


}
