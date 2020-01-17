package org.seekloud.theia.pcClient.controller


import java.io.File

import org.seekloud.theia.pcClient.scene.RoomScene.RoomSceneListener
import akka.actor.typed.ActorRef
import javafx.geometry.{Insets, Pos}
import javafx.scene.Group
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control._
import javafx.scene.layout.VBox
import javafx.stage.{FileChooser, Stage}
import org.seekloud.theia.pcClient.Boot
import org.seekloud.theia.pcClient.common._
import org.seekloud.theia.pcClient.core.RmManager
import org.seekloud.theia.pcClient.scene.{HomeScene, HostScene, RoomScene}
import org.seekloud.theia.pcClient.utils.RMClient
import org.seekloud.theia.pcClient.Boot.executor
import org.seekloud.theia.pcClient.component.WarningDialog
import org.seekloud.theia.pcClient.core.RmManager.{GetRecordDetail, GetRoomDetail, GoToWatch}
import org.seekloud.theia.protocol.ptcl.CommonInfo.{RecordInfo, RoomInfo}
import org.slf4j.LoggerFactory


import scala.collection.immutable.VectorBuilder

/**
  * User: Arrow
  * Date: 2019/7/16
  * Time: 12:33
  */
class RoomController(
  context: StageContext,
  roomScene: RoomScene,
  rmManager: ActorRef[RmManager.RmCommand]
) {

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  //  private var roomList: List[RoomInfo] = Nil
  //  private var recordList: List[RecordInfo] = Nil
  var hasWaitingGif = false

  def refreshList = {
    Boot.addToPlatform {
      showLoading()
      if (roomScene.liveMode) {
        updateRoomList()
      } else {
        if (!hasWaitingGif) {
          roomScene.recordList = Nil
          for (i <- 1 to 10) {
            updateRecordList(sortBy = roomScene.recordSort, pageNum = i)
          }
        }
      }
    }
  }

  def updateRoomList(): Unit = {
    RMClient.getRoomList.map {
      case Right(rst) =>
        if (rst.errCode == 0) {
          Boot.addToPlatform {
            removeLoading()
            roomScene.roomList = rst.roomList.get
            roomScene.updateRoomList(roomList = roomScene.roomList)
          }
        } else {
          removeLoading()
          Boot.addToPlatform(
            WarningDialog.initWarningDialog(s"${rst.msg}")
          )
        }
      case Left(e) =>
        log.error(s"get room list error: $e")
        removeLoading()
        Boot.addToPlatform(
          WarningDialog.initWarningDialog("获取房间列表失败")
        )
    }
  }

  def updateRecordList(sortBy: String = "time", pageNum: Int = 1, pageSize: Int = roomScene.recordsPerPage): Unit = {
    RMClient.getRecordList(sortBy: String, pageNum: Int, pageSize: Int).map {
      case Right(rst) =>
        if (rst.errCode == 0) {
          Boot.addToPlatform {
            roomScene.recordList = (pageNum, rst.recordInfo) :: roomScene.recordList
            roomScene.recordsSize = rst.recordNum
            if (roomScene.recordList.size % roomScene.maxPagiNum == 0) {
              removeLoading()
              roomScene.updateRecordList()
            }
          }
        } else {
          removeLoading()
          Boot.addToPlatform(
            WarningDialog.initWarningDialog(s"${rst.msg}")
          )
        }
      case Left(e) =>
        log.error(s"get record list error: $e")
        removeLoading()
        Boot.addToPlatform {
          WarningDialog.initWarningDialog("获取录像列表失败")
        }
    }
  }


  roomScene.setListener(new RoomSceneListener {
    override def enter(roomId: Long, timestamp: Long = 0L): Unit = {
      Boot.addToPlatform {
        showLoading()
        if (roomScene.liveMode && roomScene.roomList.exists(_.roomId == roomId)) {
          rmManager ! GetRoomDetail(roomScene.roomList.find(_.roomId == roomId).get.roomId)
        } else if (!roomScene.liveMode && roomScene.recordList.flatMap(_._2).exists(r => r.roomId == roomId && r.startTime == timestamp)) {
          rmManager ! GetRecordDetail(roomScene.recordList.flatMap(_._2).filter(r => r.roomId == roomId && r.startTime == timestamp).head)
        } else {
          removeLoading()
        }
      }
    }

    override def refresh(): Unit = {
      refreshList
    }


    override def gotoHomeScene(): Unit = {
      rmManager ! RmManager.BackToHome
    }
  })


  def showScene(): Unit = {
    Boot.addToPlatform {
      if (roomScene.liveMode) updateRoomList()
      context.switchScene(roomScene.getScene, title = "直播间online")
    }
  }

  def showLoading(): Unit = {
    Boot.addToPlatform {
      if (!hasWaitingGif) {
        roomScene.group.getChildren.add(roomScene.waitingGif)
        hasWaitingGif = true
      }
    }
  }

  def removeLoading(): Unit = {
    Boot.addToPlatform {
      if (hasWaitingGif) {
        roomScene.group.getChildren.remove(roomScene.waitingGif)
        hasWaitingGif = false
      }
    }
  }

}
