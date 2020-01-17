package org.seekloud.theia.faceAnalysis.controller

import org.seekloud.theia.faceAnalysis.BootJFx
import org.seekloud.theia.faceAnalysis.BootJFx.captureActor
import org.seekloud.theia.faceAnalysis.common.StageContext
import org.seekloud.theia.faceAnalysis.component.WarningDialog
import org.seekloud.theia.faceAnalysis.core.{CaptureActor, RMActor}
import org.seekloud.theia.faceAnalysis.scene.RoomScene
import org.seekloud.theia.faceAnalysis.utils.RMClient
import org.seekloud.theia.faceAnalysis.BootJFx.rmActor
import org.seekloud.theia.faceAnalysis.scene.RoomScene.AlbumInfo
import org.seekloud.theia.protocol.ptcl.CommonInfo.RoomInfo

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * User: shuai
  * Date: 2019/9/24
  * Time: 18:19
  */
object RoomController{
  var currentRoomId:Long = _
}

class RoomController(context: StageContext) extends RoomScene {

  import RoomController._

  override def showScene(): Unit = {
    updateRoomList()
    BootJFx.addToPlatform(
      context.switchScene(scene, title = "选择直播间")
    )
  }

  override def watching(roomInfo: AlbumInfo,index:Int): Unit = {
    currentRoomId=roomInfo.roomId
    if(roomList.exists(_.roomId == currentRoomId)){
      currentRoomId=roomInfo.roomId
      val viewerScene = new ViewerController(context)
      viewerScene.showScene()
      //todo 开始观看直播
      rmActor ! RMActor.GetRoomDetail(HomeController.usersInfo.get.loginInfo, roomList.find(_.roomId == currentRoomId).get, viewerScene)
    }

  }

  override def gotoChooseScene(): Unit = {
    BootJFx.addToPlatform {
      val chooseController = new ChooseController(context)
      chooseController.showScene()
    }
  }

  def updateRoomList(): Unit = {
    RMClient.getRoomList.map {
      case Right(rst) =>
        if (rst.errCode == 0) {
          BootJFx.addToPlatform {
            roomList = rst.roomList.get
            updateRoomList(roomList = roomList)
          }
        } else {
          BootJFx.addToPlatform(
            WarningDialog.initWarningDialog(s"${rst.msg}")
          )
        }
      case Left(e) =>
        log.error(s"get room list error: $e")
        BootJFx.addToPlatform(
          WarningDialog.initWarningDialog("获取房间列表失败")
        )
    }
  }

}
