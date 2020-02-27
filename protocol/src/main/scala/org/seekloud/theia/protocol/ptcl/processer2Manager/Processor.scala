package org.seekloud.theia.protocol.ptcl.processer2Manager

object Processor {

  sealed trait CommonRsp {
    val errCode: Int
    val msg: String
  }

  // startRoom
  case class StartRoom(
                        roomId: Long,
                        hostLiveId: String,
                        roomLiveId: String,
                        layout: Int,
                      )

  case class StartRoomRsp(
                           errCode: Int = 0,
                           msg: String = "ok"
                         ) extends CommonRsp

  /** url:processor/newConnect
    * post
    */
  case class NewConnect(
                         roomId: Long,
                         client: String,
                         roomLiveId: String,
                         layout: Int = 1
                       )

  case class NewConnectRsp(
                            errCode: Int = 0,
                            msg: String = "ok"
                          ) extends CommonRsp

  case class UserQuit(
                       roomId: Long,
                       client: String,
                       roomLiveId: String
                     )

  case class UserQuitRsp(
                          errCode: Int = 0,
                          msg: String = "ok"
                        ) extends CommonRsp

  case class SetSpokesman(
                           roomId: Long,
                           userLiveIdOpt: Option[String],
                           roomLiveId: String
                         )

  case class SetSpokesmanRsp(
                              errCode: Int = 0,
                              msg: String = "ok"
                            ) extends CommonRsp

  case class UpdateImageOrSound(
                                 roomId: Long,
                                 userLiveId: String,
                                 iOS: Int,
                                 aOD: Int,
                                 roomLiveId: String
                               )

  case class UpdateImageOrSoundRsp(
                                    errCode: Int = 0,
                                    msg: String = "ok"
                                  ) extends CommonRsp

  case class ChangeHost(roomId: Long, newHostLiveId: String, roomLiveId: String)

  case class ChangeHostRsp(errCode: Int = 0, msg: String = "ok") extends CommonRsp


  /** url:processor/closeRoom
    * post
    */
  case class CloseRoom(
                        roomId: Long
                      )

  case class CloseRoomRsp(
                           errCode: Int = 0,
                           msg: String = "ok"
                         ) extends CommonRsp


  /** url:processor/update
    * post
    */
  case class UpdateRoomInfo(
                             roomId: Long,
                             layout: Int
                           )

  case class UpdateRsp(
                        errCode: Int = 0,
                        msg: String = "ok"
                      ) extends CommonRsp


}
