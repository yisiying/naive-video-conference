package org.seekloud.theia.faceAnalysis.controller

import org.seekloud.theia.faceAnalysis.BootJFx
import org.seekloud.theia.faceAnalysis.BootJFx.rmActor
import org.seekloud.theia.faceAnalysis.common.StageContext
import org.seekloud.theia.faceAnalysis.component.WarningDialog
import org.seekloud.theia.faceAnalysis.core.RMActor
import org.seekloud.theia.faceAnalysis.scene.RoomScene
import org.seekloud.theia.faceAnalysis.utils.RMClient

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * User: shuai
  * Date: 2019/9/24
  * Time: 18:19
  */

class RoomController(context: StageContext) extends RoomScene {

  override def showScene(): Unit = {
    updateRoomList()
    BootJFx.addToPlatform(
      context.switchScene(scene, title = "选择直播间")
    )
  }

  override def watching(roomId: Long): Unit = {

    if(roomList.exists(_.roomId == roomId)){
      val viewerScene = new ViewerController(context)
      viewerScene.showScene()
      rmActor ! RMActor.GetRoomDetail(HomeController.usersInfo.get.loginInfo, roomList.find(_.roomId == roomId).get, viewerScene)
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
