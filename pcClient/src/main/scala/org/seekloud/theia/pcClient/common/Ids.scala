package org.seekloud.theia.pcClient.common

import org.seekloud.theia.pcClient.common.Constants.AudienceStatus
import org.seekloud.theia.pcClient.core.stream.LiveManager.{JoinInfo, WatchInfo}
import org.seekloud.theia.protocol.ptcl.CommonInfo.RecordInfo
import org.slf4j.LoggerFactory

/**
  * Author: zwq
  * Date: 2019/9/23
  * Time: 10:59
  */
object Ids {

  private[this] val log = LoggerFactory.getLogger(this.getClass)


  def getPlayId(audienceStatus: Int, roomId: Option[Long] = None, audienceId: Option[Long] = None, startTime: Option[Long] = None): String = {

    val playId = audienceStatus match {
      case AudienceStatus.LIVE => s"room${roomId.get}"
      case AudienceStatus.CONNECT => s"room${roomId.get}-audience${audienceId.get}"
      case AudienceStatus.RECORD => s"record${roomId.get}-${startTime.get}"
    }

    playId

  }

  def getCameraOption(position: String): Int = {
    position match {
      case "左上" => 0
      case "右上" => 1
      case "右下" => 2
      case "左下" => 3
    }
  }



}
