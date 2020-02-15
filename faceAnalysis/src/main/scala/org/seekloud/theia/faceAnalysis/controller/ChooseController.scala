package org.seekloud.theia.faceAnalysis.controller

import org.seekloud.theia.faceAnalysis.BootJFx
import org.seekloud.theia.faceAnalysis.BootJFx.rmActor
import org.seekloud.theia.faceAnalysis.common.StageContext
import org.seekloud.theia.faceAnalysis.core.RMActor
import org.seekloud.theia.faceAnalysis.scene.ChooseScene
/**
  * User: shuai
  * Date: 2019/9/24
  * Time: 15:36
  */

class ChooseController(context: StageContext) extends ChooseScene{

  override def showScene(): Unit = {
    BootJFx.addToPlatform(
      context.switchScene(scene, title = "选择页")
    )
  }

  override def gotoLive() : Unit={
    val anchorScene = new AnchorController(context)
    anchorScene.showScene()
    rmActor ! RMActor.GotoAnchor(HomeController.usersInfo.get.loginInfo, HomeController.roomInfo.get, anchorScene)
  }

  override def gotoWatch(): Unit = {
    val roomScene = new RoomController(context)
    roomScene.showScene()

  }
}
