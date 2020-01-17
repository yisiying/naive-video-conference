package org.seekloud.theia.faceAnalysis.controller

import org.seekloud.theia.faceAnalysis.BootJFx
import org.seekloud.theia.faceAnalysis.BootJFx.{captureActor, rmActor}
import org.seekloud.theia.faceAnalysis.common.{Constants, StageContext}
import org.seekloud.theia.faceAnalysis.component.Barrage.updateBarrage
import org.seekloud.theia.faceAnalysis.component.WarningDialog
import org.seekloud.theia.faceAnalysis.core.{CaptureActor, RMActor}
import org.seekloud.theia.faceAnalysis.scene.AnchorScene
import org.seekloud.theia.protocol.ptcl.client2Manager.websocket.AuthProtocol
import org.seekloud.theia.protocol.ptcl.client2Manager.websocket.AuthProtocol._

/**
  * Created by sky
  * Date on 2019/8/19
  * Time at 下午4:10
  */
object AnchorController {
  var anchorControllerOpt: Option[AnchorController] = None

  /**
    * 删除theia登录临时文件
    */
  def deleteLoginTemp(): Unit = {
    val dir = Constants.loginInfoCache
    dir.listFiles().foreach { file =>
      if (file.exists()) file.delete()
    }
  }
}

class AnchorController(context: StageContext) extends AnchorScene with UserControllerImpl {
  import AnchorController.deleteLoginTemp
  override def showScene(): Unit = {
    BootJFx.addToPlatform(
      context.switchScene(scene, title = "pc客户端-主播页")
    )
  }

  override def startDevice(): Unit = {
    topBox.setMouseTransparent(true)
    captureActor ! CaptureActor.DevicesReady(gc) //开启设备
  }

  override def stopDevice(): Unit = {
    topBox.setMouseTransparent(false)
    captureActor ! CaptureActor.DeviceOff //关闭摄像,若有推流将停止推流
    if (liveToggleButton.isSelected) {
      rmActor ! RMActor.StopLive //通知roomManager推流停止
    }
  }

  override def startLive(rtpSelected: Boolean, rtmpSeleted: Boolean, rtmpurl: Option[String]): Unit = {
    rmActor ! RMActor.AnchorLiveReq(rtpSelected, rtmpSeleted, rtmpurl) //向roomManager请求liveId、liveCode
  }

  override def stopLive(): Unit = {
    captureActor ! CaptureActor.StopEncode //停止推流
    rmActor ! RMActor.StopLive //通知roomManager推流停止
  }

  override def changeAi(index: Byte, value: Byte): Unit = {
    captureActor ! CaptureActor.ChangeAi(index, value)
  }

  override def sendCmt(comment: Comment): Unit = {
    rmActor ! RMActor.SendComment(comment)
  }

  override protected def logout(): Unit = {
    val homeController = new HomeController(context)
    homeController.showScene()
    deleteLoginTemp()
  }

  override protected def gotoChooseScene(): Unit = {
    stopLive()
    BootJFx.addToPlatform {
      val chooseController = new ChooseController(context)
      chooseController.showScene()
    }
  }

  override protected def changeLiveModel(liveModel: Int): Unit = {
    if(liveModel == 1){
      if(liveCheck2.isSelected && liveToggleButton.isSelected){
        liveCheck1.setSelected(false)
        WarningDialog.initWarningDialog("请先停止推流，再切换推流方式")
      }
    }
    if(liveModel == 2) {
      if(liveCheck1.isSelected && liveToggleButton.isSelected){
        liveCheck2.setSelected(false)
        WarningDialog.initWarningDialog("请先停止推流，再切换推流方式")
      }
    }

  }

  //remind handle msg from roomManager
  override def wsMessageHandler(msg: AuthProtocol.WsMsgRm): Unit = {
    msg match {
      case msg: HeatBeat =>
        // log.info(s"heartbeat: ${msg.ts}")
        rmActor ! RMActor.HeartBeat

      case msg: StartLiveRsp =>
        log.info(s"get StartLiveRsp: $msg")
        if (msg.errCode == 0) {
          captureActor ! CaptureActor.StartLive(msg.liveInfo.get.liveId, msg.liveInfo.get.liveCode)
        } else {
          BootJFx.addToPlatform {
            WarningDialog.initWarningDialog(s"${msg.msg}")
          }
        }

      case msg: RcvComment =>
        log.debug(s"get RcvComment: $msg")
        log.info(s"get RcvComment: $msg")
        BootJFx.addToPlatform {
          updateComment(msg)
          updateBarrage(msg)
        }

      case msg: UpdateAudienceInfo =>
        log.debug(s"update audienceList.")
        BootJFx.addToPlatform {
          updateWatchingList(msg.AudienceList)
        }
      case _ =>
    }
  }

  def deviceReadyAction(): Unit = {
    BootJFx.addToPlatform {
      liveToggleButton.setDisable(false)
      liveToggleButton.setText("推流状态")
      liveToggleButton.setOnAction {
        _ =>
          val liveRtpCheck = liveCheck1.isSelected
          val liveRtmpCheck = liveCheck2.isSelected
          val rtmpReg = rtmpText.getText().split("\\?", 2)
          val rtmpServer = if(liveRtmpCheck) rtmpReg.head + "/" + passText.getText() + "?" + rtmpReg.last else ""
          if(liveRtpCheck || liveRtmpCheck){
            if(liveToggleButton.isSelected){
              startLive(liveRtpCheck, liveRtmpCheck,Some(rtmpServer))
            }else {
              stopLive()
            }
          } else {
            liveToggleButton.setSelected(false)
            WarningDialog.initWarningDialog("请先选择直播方式")

            }
          }


      }
  }
}
