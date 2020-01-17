package org.seekloud.theia.faceAnalysis.scene

import javafx.collections.FXCollections
import javafx.geometry.{Insets, Pos}
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control.{Button, ChoiceBox, Label, ToggleButton}
import javafx.scene.effect.DropShadow
import javafx.scene.image.{Image, ImageView}
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{BorderPane, HBox, StackPane, VBox}
import javafx.scene.paint.Color
import org.seekloud.theia.faceAnalysis.common.Constants
import org.seekloud.theia.faceAnalysis.common.Constants.Like
import org.seekloud.theia.faceAnalysis.component.Barrage.barrageView
import org.seekloud.theia.faceAnalysis.component.UserBox.userBox
import org.seekloud.theia.faceAnalysis.component.WarningDialog
import org.seekloud.theia.faceAnalysis.controller.{HomeController, RoomController}
import org.seekloud.theia.faceAnalysis.scene.panel.{RightPanel, SceneImpl, TopPanel}
import org.seekloud.theia.protocol.ptcl.client2Manager.websocket.AuthProtocol.LikeRoom

/**
  * User: shuai
  * Date: 2019/9/25
  * Time: 14:49
  */

trait ViewerScene extends TopPanel with RightPanel with SceneImpl{

  protected def gotoRoomScene()


  protected def sendLike(like:LikeRoom)

  scene.getStylesheets.addAll(
    this.getClass.getClassLoader.getResource("css/common.css").toExternalForm,
    this.getClass.getClassLoader.getResource("css/scroll.css").toExternalForm,
  )

  var watchUrl: Option[String] = None
  var liveId: Option[String] = None

  val liveImage = new Canvas(Constants.DefaultPlayer.width * 0.9, 360 * 0.9)
  val gc: GraphicsContext = liveImage.getGraphicsContext2D

  val backImg = new Image("img/background.jpg")
  gc.drawImage(backImg, 0, 0, Constants.DefaultPlayer.width * 0.9, Constants.DefaultPlayer.height * 0.9)
  val borderPane: BorderPane = addBorderPane()
  borderPane.setTop(backBox)
  group.getChildren.add(borderPane)

  var leftArea: VBox = _
  var rightArea: VBox = _

  var likeBtn:ToggleButton= _
  var unLikeIcon:ImageView= _
  var likeIcon:ImageView= _

  def addBorderPane(): BorderPane = {
    leftArea = addLeftArea()
    rightArea = addRightArea()
    val borderPane = new BorderPane
    borderPane.setLeft(leftArea)
    borderPane.setRight(rightArea)
    borderPane
  }

  def backBox: HBox ={
    val back = new HBox()

    val backIcon = new ImageView("img/backBtn.png")
    backIcon.setFitHeight(25)
    backIcon.setFitWidth(25)
    val backButton = new Button("", backIcon)
    backButton.setOnAction(_ => gotoRoomScene())
    backButton.getStyleClass.add("topBtn")

    back.getChildren.addAll(backButton)
    back.setAlignment(Pos.BOTTOM_LEFT)

    back
  }

  def addLeftArea() : VBox ={
    val leftAreaBox = new VBox()

    leftAreaBox.getChildren.addAll(topBox,createImageBox,createlikeBtn)
    leftAreaBox.setSpacing(15)
    leftAreaBox.setPadding(new Insets(5, 10, 15, 60))
    leftAreaBox.setPrefHeight(height)

    def createImageBox: StackPane = {

      val stackPane = new StackPane()
      stackPane.getChildren.addAll(liveImage, barrageView)
      stackPane
    }

    //点赞部分
    def createlikeBtn:ToggleButton={
      unLikeIcon= new ImageView("img/like.png")
      likeIcon = new ImageView("img/liked.png")
      likeIcon.setFitWidth(30)
      likeIcon.setFitHeight(30)
      unLikeIcon.setFitWidth(30)
      unLikeIcon.setFitHeight(30)

      likeBtn = new ToggleButton("", unLikeIcon)

      likeBtn.getStyleClass.add("hostScene-middleArea-tableBtn")
//      val isRecord=false
//      if (!isRecord) {
      likeBtn.setOnAction(_ => {
        if (HomeController.usersInfo.nonEmpty) {
          if (likeBtn.isSelected) {
            log.info(s"~~~userid:${HomeController.usersInfo.get.loginInfo.userId} roomid:${RoomController.currentRoomId}")
            sendLike(LikeRoom(HomeController.usersInfo.get.loginInfo.userId,RoomController.currentRoomId,Like.down))
            likeBtn.setGraphic(likeIcon)
          } else {
            sendLike(LikeRoom(HomeController.usersInfo.get.loginInfo.userId,RoomController.currentRoomId,Like.up))
            likeBtn.setGraphic(unLikeIcon)
          }
        } else {
          WarningDialog.initWarningDialog("请先登陆哦~")
        }
      }
      )
      val shadow = new DropShadow(10, Color.GRAY)
      likeBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
        likeBtn.setEffect(shadow)
      })
      likeBtn.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
        likeBtn.setEffect(null)
      })
//      } else{
//        likeBtn.setDisable(true)
//      }
      likeBtn

    }

    leftAreaBox
  }
}
