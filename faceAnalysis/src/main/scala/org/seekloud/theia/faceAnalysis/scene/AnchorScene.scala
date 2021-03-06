package org.seekloud.theia.faceAnalysis.scene

import javafx.collections.{FXCollections, ObservableList}
import javafx.geometry.{Insets, Pos}
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control._
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout._
import org.seekloud.theia.faceAnalysis.common.{Constants, Pictures}
import org.seekloud.theia.faceAnalysis.component.Barrage.barrageView
import org.seekloud.theia.faceAnalysis.scene.panel.{RightPanel, TopPanel}

import scala.util.Random

/**
  * Created by sky
  * Date on 2019/8/19
  * Time at 下午4:08
  * anchor scene:start/stop/chat
  */
trait AnchorScene extends TopPanel with RightPanel {

  protected def startDevice()

  protected def stopDevice()

  protected def startLive()

  protected def stopLive()

  protected def gotoChooseScene()

  protected def changeAi(index: Byte, value: Byte)


  scene.getStylesheets.addAll(
    this.getClass.getClassLoader.getResource("css/common.css").toExternalForm,
    this.getClass.getClassLoader.getResource("css/scroll.css").toExternalForm,
  )

  val liveToggleButton = new ToggleButton("设备未启动")
  liveToggleButton.getStyleClass.add("toggleButton")
  liveToggleButton.setDisable(true)

  val liveImage = new Canvas(Constants.DefaultPlayer.width * 0.9, Constants.DefaultPlayer.height * 0.9)
  val gc: GraphicsContext = liveImage.getGraphicsContext2D
  val backImg = new Image("img/background.jpg")
  gc.drawImage(backImg, 0, 0, Constants.DefaultPlayer.width * 0.9, Constants.DefaultPlayer.height * 0.9)

  val borderPane: BorderPane = addBorderPane()
  borderPane.setTop(backBox)
  group.getChildren.add(borderPane)

  var leftArea: VBox = _
  var rightArea: VBox = _

  val random = new Random(System.nanoTime())

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
    backButton.setOnAction(_ => gotoChooseScene())
    backButton.getStyleClass.add("topBtn")

    back.getChildren.addAll(backButton)
    back.setAlignment(Pos.BOTTOM_LEFT)

    back
  }

  def addLeftArea(): VBox = {
    val leftAreaBox = new VBox()
    leftAreaBox.getChildren.addAll(topBox,createImageBox, createControlBox)
    leftAreaBox.setSpacing(15)
    leftAreaBox.setPadding(new Insets(5, 10, 15, 60))
    leftAreaBox.setPrefHeight(height)

    def createImageBox: StackPane = {

      val stackPane = new StackPane()
      //      stackPane.setAlignment(Pos.BOTTOM_RIGHT)
      stackPane.getChildren.addAll(liveImage, barrageView)
      stackPane

    }

    def createControlBox: HBox = {
      val deviceToggleButton = new ToggleButton("摄像状态")
      deviceToggleButton.getStyleClass.add("toggleButton")
      deviceToggleButton.setOnAction {
        _ =>
          if (deviceToggleButton.isSelected) {
            startDevice()
          } else {
            stopDevice()
            deviceCloseAction()
          }
      }

      val arrowImage = new ImageView("img/rightArrow.png")
      arrowImage.setFitWidth(20)
      arrowImage.setFitHeight(20)

      val pointToggleButton = new ToggleButton("64点特效")
      pointToggleButton.getStyleClass.add("toggleButton")
      pointToggleButton.setOnAction {
        _ =>
          if (pointToggleButton.isSelected) {
            changeAi(2, 1)
          } else {
            changeAi(2, 0)
          }
      }

      val TDModelControlBox = new ChoiceBox(FXCollections.observableArrayList("无3d特效","男孩头像","女孩头像", "猴子特效"))
      TDModelControlBox.getSelectionModel.select(0)
      TDModelControlBox.setOnAction {
        _ =>
          changeAi(3, TDModelControlBox.getSelectionModel.getSelectedIndex.toByte)
      }

      val leftControlBox = new VBox(deviceToggleButton, liveToggleButton)
      leftControlBox.setSpacing(10)
      leftControlBox.setPrefWidth(180)

      val glassControlBox = new ChoiceBox(FXCollections.observableArrayList("无眼镜", "红色眼镜", "绿色眼镜", "蓝色眼镜"))
      glassControlBox.getSelectionModel.select(0)
      glassControlBox.setOnAction {
        _ =>
          changeAi(0, glassControlBox.getSelectionModel.getSelectedIndex.toByte)
      }

      val beardControlBox = new ChoiceBox(FXCollections.observableArrayList("无胡子", "蓝色胡子", "红色胡子", "橘色胡子"))
      beardControlBox.getSelectionModel.select(0)
      beardControlBox.setOnAction {
        _ =>
          changeAi(1, beardControlBox.getSelectionModel.getSelectedIndex.toByte)
      }

      val centreControlBox = new VBox(glassControlBox, beardControlBox)
      centreControlBox.setSpacing(20)

      val rightControlBox = new VBox(pointToggleButton,TDModelControlBox)
      rightControlBox.setSpacing(10)

      val controlBox = new HBox(leftControlBox, centreControlBox, rightControlBox)
      controlBox.setSpacing(55)

      controlBox
    }

    leftAreaBox

  }

  def deviceCloseAction(): Unit = {
    liveToggleButton.setOnAction {
      _ => ()
    }
    liveToggleButton.setText("设备未启动")
    liveToggleButton.setSelected(false)
    liveToggleButton.setDisable(true)
  }

}
