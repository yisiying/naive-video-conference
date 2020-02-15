package org.seekloud.theia.faceAnalysis.scene

import javafx.geometry.{Insets, Pos}
import javafx.scene.control.{Button, Label, Pagination}
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{BorderPane, HBox, VBox}
import javafx.scene.text.{Font, Text}
import org.seekloud.theia.faceAnalysis.scene.panel.SceneImpl
import org.seekloud.theia.protocol.ptcl.CommonInfo.RoomInfo

/**
  * User: shuai
  * Date: 2019/9/24
  * Time: 17:46
  */
object RoomScene{

  case class AlbumInfo(
    roomId: Long,
    roomName: String,
    roomDes: String,
    userId: Long,
    userName: String,
    headImgUrl: String,
    coverImgUrl: String,
    observerNum: Int,
    like: Int,
    streamId: Option[String] = None,
    recordId: Long = 0L,
    timestamp: Long = 0l,
    duration: String = ""
  )

  implicit class RichRoomInfo(r: RoomInfo) {
    def toAlbum: AlbumInfo =
      AlbumInfo(
        r.roomId,
        r.roomName,
        r.roomDes,
        r.userId,
        r.userName,
        r.headImgUrl,
        r.coverImgUrl,
        r.observerNum,
        r.like,
        streamId = r.rtmp
      )
  }
}

trait RoomScene extends SceneImpl {
  import RoomScene._

  scene.getStylesheets.add(
    this.getClass.getClassLoader.getResource("css/common.css").toExternalForm
  )

  def gotoChooseScene(): Unit

  def watching(roomId: Long) : Unit

  var roomList: List[RoomInfo] = Nil

  val liveInfo = new Text("")
  liveInfo.setFont(Font.font(15))
  val liveBox = new HBox(liveInfo)
  liveBox.setAlignment(Pos.CENTER_LEFT)
  liveBox.setPadding(new Insets(10, 110, 0, 400))

  val borderPane = new BorderPane()
  borderPane.setTop(backBox)
  group.getChildren.add(borderPane)

  def backBox: HBox ={
    val back = new HBox()

    val backIcon = new ImageView("img/backBtn.png")
    backIcon.setFitHeight(25)
    backIcon.setFitWidth(25)
    val backButton = new Button("", backIcon)
    backButton.setOnAction(_ => gotoChooseScene())
    backButton.getStyleClass.add("topBtn")

    back.getChildren.addAll(backButton)
    back.setAlignment(Pos.BOTTOM_LEFT)

    back
  }

  def createOnePage(pageIndex: Int, itemsPerPage: Int, albumList: List[AlbumInfo]): VBox = {
    val vBox = new VBox()
    vBox.setPadding(new Insets(5, 110, 15, 110))
    vBox.setSpacing(10)
    val hBox1 = new HBox()
    hBox1.setSpacing(25)
    val hBox2 = new HBox()
    hBox2.setSpacing(25)
    val totalLen = albumList.length
    val start = pageIndex * itemsPerPage + 1
    val end = (pageIndex + 1) * itemsPerPage
    for (i <- start to (start + 2)) {
      if (i <= totalLen) {
        val roomIcon = new ImageView("img/room.png")
        roomIcon.setFitHeight(150)
        roomIcon.setFitWidth(150)
        val roomLabel = new Label("",roomIcon)
        roomLabel.addEventHandler(MouseEvent.MOUSE_CLICKED, (_: MouseEvent) => {
          watching(albumList(i - 1).roomId)
        })

        val roomName = new Label(s"${albumList(i - 1).roomName}")
        roomName.getStyleClass.add("roomScene-roomName")

        val userName = new Label(s"${albumList(i - 1).userName}")
        userName.getStyleClass.add("roomScene-userName")

        val audienceNumIcon = new ImageView("img/watching.png")
        audienceNumIcon.setFitHeight(20)
        audienceNumIcon.setFitWidth(20)
        val audienceNum = new Label(s"${albumList(i - 1).observerNum}", audienceNumIcon)
        audienceNum.getStyleClass.add("roomScene-userName")

        val nameDateBox = new HBox()
        nameDateBox.setAlignment(Pos.CENTER_LEFT)
        nameDateBox.setSpacing(20)
        nameDateBox.getChildren.addAll(roomName)

        val hBox = new HBox()
        hBox.setAlignment(Pos.CENTER_LEFT)
        hBox.setSpacing(60)
        hBox.getChildren.addAll(userName, audienceNum)

        val roomBox = new VBox()
        roomBox.getChildren.addAll(roomLabel, nameDateBox, hBox)
        hBox1.getChildren.add(roomBox)
        hBox1.setSpacing(150)
      }
    }
    for (i <- (start + 3) to end) {

      if (i <= totalLen) {
        val roomIcon = new ImageView("img/room.png")
        roomIcon.setFitHeight(150)
        roomIcon.setFitWidth(150)
        val roomLabel = new Label("",roomIcon)
        roomLabel.addEventHandler(MouseEvent.MOUSE_CLICKED, (_: MouseEvent) => {
          watching(albumList(i - 1).roomId)
        })

        val roomName = new Label(s"${albumList(i - 1).roomName}")
        roomName.getStyleClass.add("roomScene-roomName")

        val userName = new Label(s"${albumList(i - 1).userName}")
        userName.getStyleClass.add("roomScene-userName")

        val audienceNumIcon = new ImageView("img/watching.png")
        audienceNumIcon.setFitHeight(20)
        audienceNumIcon.setFitWidth(20)
        val audienceNum = new Label(s"${albumList(i - 1).observerNum}", audienceNumIcon)
        audienceNum.getStyleClass.add("roomScene-userName")


        val nameDateBox = new HBox()
        nameDateBox.setAlignment(Pos.CENTER_LEFT)
        nameDateBox.setSpacing(20)

        nameDateBox.getChildren.addAll(roomName)


        val hBox = new HBox()
        hBox.setAlignment(Pos.CENTER_LEFT)
        hBox.setSpacing(60)
        hBox.getChildren.addAll(userName, audienceNum)
        val roomBox = new VBox()
        roomBox.getChildren.addAll(roomLabel, nameDateBox, hBox)
        hBox2.getChildren.add(roomBox)
        hBox2.setSpacing(150)
      }
    }
    vBox.getChildren.addAll(hBox1, hBox2)
    vBox
  }



  def updateRoomList(roomList: List[RoomInfo] = Nil): Unit = {
    if (roomList.isEmpty) {
      val label = new Label("暂无房间")
      label.setFont(Font.font("Verdana", 30))
      label.setPadding(new Insets(200, 0, 0, 0))
      borderPane.setCenter(label)
    } else {
      val itemsPerPage = 6
      val pageNum = if (roomList.length % itemsPerPage.toInt == 0) {
        roomList.length / itemsPerPage.toInt
      }
      else {
        roomList.length / itemsPerPage.toInt + 1
      }
      val pagination = new Pagination(pageNum, 0)
      pagination.setPageFactory((pageIndex: Integer) => {
        if (pageIndex >= pageNum)
          null
        else {
          createOnePage(pageIndex, itemsPerPage, roomList.map(_.toAlbum))
        }
      })
      val center = new VBox(10)
      liveInfo.setText(s"当前共有${roomList.length}个直播")
      center.getChildren.addAll(liveBox, pagination)
      borderPane.setCenter(center)
    }

  }
}
