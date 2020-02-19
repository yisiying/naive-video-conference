package org.seekloud.theia.protocol.ptcl.distributor2Manager

/**
  * User: LTy
  * Data: 2019/7/17
  * Time: 14:00
  */
object DistributorProtocol {

  sealed trait CommonRsp{
    val errCode:Int
    val msg:String
  }

  final case class ErrorRsp(
                             errCode: Int,
                             msg: String
                           ) extends CommonRsp

  final case class SuccessRsp(
                               errCode: Int = 0,
                               msg: String = "ok"
                             ) extends CommonRsp

  // 控制流相关
  case class StartPullReq(
                           roomId:Long,
                           liveId:String
                         )

  case class StartPullRsp(
                           errCode:Int,
                           msg:String,
                           liveAdd:String,
                           startTime:Long
                         ) extends CommonRsp

  case class FinishPullReq(
                            liveId:String
                          )

  case class CheckStreamReq(
                             liveId:String
                           )

  case class CheckStreamRsp(
    errCode:Int = 0,
    msg:String = "ok",
    status:String = "正常"
  ) extends CommonRsp

  val StreamError = CheckStreamRsp(100001, "streamError", "没有流信息")
  val RoomError = CheckStreamRsp(100002, "roomError", "没有流对应的房间")


  //录像相关
  case class SeekRecord(
                         roomId:Long,
                         startTime:Long
                       )

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

  //管理页面展示信息
  case class GetTransLiveInfoRsp(
                                errCode:Int = 0,
                                msg: String ="ok",
                                info: List[liveInfo]
                              )extends CommonRsp

  case class GetPullStreamInfoRsp(
                                   errCode:Int = 0,
                                   msg: String ="ok",
                                   info: List[PullPackageInfo]
                                 )

  case class liveInfo(
                      roomId: Long,
                      liveId: String,
                      port: Long,
                      status: String,
                      Url: String
                    )

  case class StreamPackageLoss(liveId:String, packageLossInfo: PackageLossInfo)

  case class PackageLossInfo(lossScale60: Double,  lossScale10: Double, lossScale2: Double)

  case class BandwidthInfo(in60s: Double, in10s: Double, in2s: Double)

  case class PullPackageInfo(liveId:String, host:String, port:Int, packageInfo: PackageLossInfo, bandwidthInfo: BandwidthInfo)


  case class CloseStreamReq(liveId: String)

  case class FinishPullRsp(
                      errCode:Int = 0,
                      msg: String ="ok",
                    )extends CommonRsp

  //注册页面信息
  case class UserInfo(
                       userName: String,
                       passWord: String,
                     )

  case class UserSessionInfo(
                       userName: String,
                     )


  case class GetSessionRsp(
                            sessionInfo: Option[UserSessionInfo],
                            errCode: Int = 0,
                            msg: String = "Ok"
                          ) extends CommonRsp

}
