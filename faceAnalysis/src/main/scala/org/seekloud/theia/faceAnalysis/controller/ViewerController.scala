package org.seekloud.theia.faceAnalysis.controller

import org.seekloud.theia.faceAnalysis.BootJFx
import org.seekloud.theia.faceAnalysis.BootJFx.{captureActor, rmActor}
import org.seekloud.theia.faceAnalysis.common.{Constants, StageContext}
import org.seekloud.theia.faceAnalysis.component.Barrage.updateBarrage
import org.seekloud.theia.faceAnalysis.component.WarningDialog
import org.seekloud.theia.faceAnalysis.core.{CaptureActor, RMActor}
import org.seekloud.theia.faceAnalysis.scene.ViewerScene
import org.seekloud.theia.protocol.ptcl.client2Manager.websocket.AuthProtocol
import org.seekloud.theia.protocol.ptcl.client2Manager.websocket.AuthProtocol._

/**
  * User: shuai
  * Date: 2019/9/25
  * Time: 16:31
  */
class ViewerController(
                        context: StageContext,
                        //  viewScene: ViewerScene,
                        //  rmActor: ActorRef[RMActor.Command]
                      ) extends ViewerScene with UserControllerImpl {


  override def showScene(): Unit = {
    BootJFx.addToPlatform(
      context.switchScene(scene, title = "观看直播页")
    )
  }

  override protected def logout(): Unit = {
    val homeController = new HomeController(context)
    homeController.showScene()
    AnchorController.deleteLoginTemp()
  }

  override def sendCmt(comment: Comment): Unit = {
    rmActor ! RMActor.SendComment(comment)
  }

  override def gotoRoomScene(): Unit = {
    BootJFx.addToPlatform {
      val roomController = new RoomController(context)
      roomController.showScene()
      rmActor ! RMActor.ViewerLeft
    }
  }

  override def wsMessageHandler(msg: AuthProtocol.WsMsgRm): Unit = {
    BootJFx.addToPlatform {
      msg match {
        case msg: HeatBeat =>
          // log.info(s"heartbeat: ${msg.ts}")
          rmActor ! RMActor.HeartBeat


        case msg: RcvComment =>
          log.debug(s"get RcvComment: $msg")
          BootJFx.addToPlatform {
            updateComment(msg)
            updateBarrage(msg)
          }

        case msg: HostDisconnect =>
          BootJFx.addToPlatform {
            WarningDialog.initWarningDialog("主播已断开连线~")
          }
        //rmActor ! rmActor.StopJoinAndWatch

        case HostStopPushStream2Client =>
          BootJFx.addToPlatform({
            WarningDialog.initWarningDialog("主播已停止直播，请换个房间观看哦~")
          })


        case msg: LikeRoomRsp =>
          log.info(s"get like rsp:${msg.msg}")

        case msg: JudgeLikeRsp =>
          BootJFx.addToPlatform(
            if (msg.like) {
              likeBtn.setSelected(true)
              likeBtn.setGraphic(likeIcon)
              log.info(s"inside get judgelikersp true")
            } else {
              likeBtn.setSelected(false)
              likeBtn.setGraphic(unLikeIcon)
              log.info(s"inside get judgelikersp false")
            }
          )


        case msg: UpdateAudienceInfo =>
          log.debug(s"update audienceList.")
          BootJFx.addToPlatform {
            updateWatchingList(msg.AudienceList)
          }
        case _ =>
      }
    }
  }

  override protected def sendLike(like: LikeRoom): Unit = {
    rmActor ! RMActor.SendLike(like)
  }


}
