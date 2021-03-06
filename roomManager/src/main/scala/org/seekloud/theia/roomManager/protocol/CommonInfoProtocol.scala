package org.seekloud.theia.roomManager.protocol

import org.seekloud.theia.protocol.ptcl.CommonInfo
import org.seekloud.theia.protocol.ptcl.CommonInfo.RoomInfo

object CommonInfoProtocol {

  //fixme isJoinOpen,liveInfoMap字段移到这里
  final case class WholeRoomInfo(
                                var roomInfo:RoomInfo,
                                //var recordStartTime: Option[Long] = None,
                                var layout:Int = CommonInfo.ScreenLayout.EQUAL,
                                var aiMode:Int = 0
                                )

}
