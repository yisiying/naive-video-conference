package org.seekloud.theia.protocol.ptcl.processer2Manager

/**
  * User: LTy
  * Data: 2019/7/17
  * Time: 14:00
  */
object ProcessorProtocol {

  sealed trait CommonRsp{
    val errCode:Int
    val msg:String
  }

  // startRoom
  case class startRoomInfo(
                            roomId: Long,
                            hostLiveId: String,
                            roomLiveId: String,
                            layout: Int,
                          )

  case class startRoomRsp(
                           errCode: Int = 0,
                           msg: String = "ok"
                         ) extends CommonRsp

  // newConnect
  case class newConnectInfo(
                             roomId: Long,
                             client: String,
                             roomLiveId: String,
                             layout: Int = 1
  )
  case class newConnectRsp(
    errCode:Int = 0,
    msg:String = "ok"
  ) extends CommonRsp

  case class userQuitInfo(
                           roomId: Long,
                           client: String,
                           roomLiveId: String
                         )

  case class userQuitRsp(
                          errCode: Int = 0,
                          msg: String = "ok"
                        ) extends CommonRsp

  // update
  case class UpdateRoomInfo(
                             roomId:Long,
                             layout:Int,
//                             aiMode:Int //为了之后拓展多种模式，目前0为不开ai，1为人脸目标检测
                           )
  case class UpdateRsp(
    errCode:Int = 0,
    msg:String = "ok"
  ) extends CommonRsp

  //closeRoom
  case class CloseRoom(
                        roomId:Long
                      )

  case class CloseRsp(
    errCode:Int = 0,
    msg:String = "ok"
  ) extends CommonRsp

  case class SeekRecord(
                         roomId:Long,
                         startTime:Long
                       )

  //mpd
  case class GetMpd(
                     roomId:Long
                   )

  case class MpdRsp(
    mpd:String,
    rtmp:String,
    errCode:Int = 0,
    msg:String = "ok"
  ) extends CommonRsp

  //录像
  case class RecordInfoRsp(
                            errCode:Int = 0,
                            msg:String = "ok",
                            duration:String
                          ) extends CommonRsp

  case class RecordList(
                         records:List[RecordData]
                       )

  case class RecordData(
                         roomId:Long,
                         startTime:Long
                       )


  //rebuild=========================================

}
