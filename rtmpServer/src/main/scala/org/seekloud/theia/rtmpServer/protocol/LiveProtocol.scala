package org.seekloud.theia.rtmpServer.protocol

/**
  * Created by LTy on 19/5/26
  */
object LiveProtocol {
  case class RoomInfo(id:Long, roomName:String)
  case class GetRoomListRsp(roomList:List[RoomInfo], errCode:Int = 0, msg:String="ok")

  case class OnConnect(
    action:String,
    client_id:Long,
    ip:String,
    vhost:String,
    app:String,
    tcUrl:String,
    pageUrl:String

  )

  case class OnPublish(
                        action:String,
                        client_id:Long,
                        ip:String,
                        vhost:String,
                        app:String,
                        stream:String,
                        param:String
                      )

  case class OnPlay(
                     action:String,
                     client_id:Long,
                     ip:String,
                     vhost:String,
                     app:String,
                     stream:String,
                     param:String,
                     pageUrl:String
                   )

  case class StateCodeRsp(
                         stateCode:Int,
                         errCode:Int = 0,
                         msg:String="ok"
                         )
}

