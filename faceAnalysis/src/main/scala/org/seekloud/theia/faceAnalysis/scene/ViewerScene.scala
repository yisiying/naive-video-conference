package org.seekloud.theia.faceAnalysis.scene

import javafx.geometry.{Insets, Pos}
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control.Button
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout.{BorderPane, HBox, StackPane, VBox}
import org.seekloud.theia.faceAnalysis.common.Constants
import org.seekloud.theia.faceAnalysis.component.Barrage.barrageView
import org.seekloud.theia.faceAnalysis.component.UserBox.userBox
import org.seekloud.theia.faceAnalysis.scene.panel.{RightPanel, TopPanel}

/**
  * User: shuai
  * Date: 2019/9/25
  * Time: 14:49
  */

trait ViewerScene extends TopPanel with RightPanel{

  protected def gotoRoomScene()


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

    leftAreaBox.getChildren.addAll(topBox,createImageBox)
    leftAreaBox.setSpacing(15)
    leftAreaBox.setPadding(new Insets(5, 10, 15, 60))
    leftAreaBox.setPrefHeight(height)

    def createImageBox: StackPane = {

      val stackPane = new StackPane()
      stackPane.getChildren.addAll(liveImage, barrageView)
      stackPane
    }

    leftAreaBox
  }
}
