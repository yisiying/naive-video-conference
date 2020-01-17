package org.seekloud.theia.faceAnalysis.scene

import javafx.geometry.{Insets, Pos}
import javafx.scene.control.Button
import javafx.scene.image.ImageView
import javafx.scene.layout.{BorderPane, HBox, VBox}
import javafx.scene.text.Text
import org.seekloud.theia.faceAnalysis.scene.panel.SceneImpl

/**
  * User: shuai
  * Date: 2019/9/24
  * Time: 14:56
  */

trait ChooseScene extends SceneImpl {
  def gotoLive() : Unit
  def gotoWatch() :Unit

  scene.getStylesheets.add(
    this.getClass.getClassLoader.getResource("css/common.css").toExternalForm
  )

  val borderPane: BorderPane = addBorderPane()
  val img = new ImageView("img/room_bg.jpg")
  img.setFitWidth(width)
  img.setFitHeight(height)
  group.getChildren.add(img)
  group.getChildren.add(borderPane)

  def addBorderPane(): BorderPane = {
    val borderPane = new BorderPane
    borderPane.setCenter(centerArea)

    borderPane
  }

  def centerArea : HBox ={

    val liveIcon = new ImageView("img/live.png")
    liveIcon.setFitHeight(300)
    liveIcon.setFitWidth(300)
    val liveButton = new Button("",liveIcon)
    liveButton.setPrefSize(300,300)
    liveButton.getStyleClass.add("homeScene-bottomBtn")
    liveButton.setOnAction { _ =>
      gotoLive()
    }
    val liveText = new Text("去直播")
    liveText.getStyleClass.add("homeScene-bottomText")
    val liveBox = new VBox()
    liveBox.getChildren.addAll(liveButton,liveText)
    liveBox.setAlignment(Pos.CENTER)
    liveBox.setSpacing(20)

    val watchIcon = new ImageView("img/watch.png")
    watchIcon.setFitHeight(300)
    watchIcon.setFitWidth(300)
    val watchButton = new Button("",watchIcon)
    watchButton.setPrefSize(300,300)
    watchButton.getStyleClass.add("homeScene-bottomBtn")
    watchButton.setOnAction{ _ =>
      gotoWatch()
    }
    val watchText = new Text("看直播")
    watchText.getStyleClass.add("homeScene-bottomText")
    val watchBox = new VBox()
    watchBox.getChildren.addAll(watchButton,watchText)
    watchBox.setSpacing(20)
    watchBox.setAlignment(Pos.CENTER)

    val chooseArea = new HBox()
    chooseArea.getChildren.addAll(liveBox,watchBox)
    chooseArea.setAlignment(Pos.CENTER)
    chooseArea.setSpacing(150)
    chooseArea.setPadding(new Insets(100,50,10,100))

    chooseArea
  }
}
