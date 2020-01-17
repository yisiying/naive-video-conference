package org.seekloud.theia.faceAnalysis.component

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.layout.{HBox, VBox}
import org.seekloud.theia.faceAnalysis.controller.HomeController

/**
  * User: shuai
  * Date: 2019/9/25
  * Time: 16:39
  */
object UserBox {

  def userBox : HBox ={
    val userInfo = new HBox()
    val anchorImage = new ImageView("img/anchor.png")
    anchorImage.setFitHeight(35)
    anchorImage.setFitWidth(35)
    val anchorLabel = new Label("",anchorImage)

    val userInfoBox = new VBox()
    val userName = new Label(s"${HomeController.roomInfo.get.userName}")
    userName.getStyleClass.add("hostScene-rightArea-label")
    val userId = new Label(s"${HomeController.roomInfo.get.userId}")
    userId.getStyleClass.add("hostScene-rightArea-label")
    userInfoBox.getChildren.addAll(userName, userId)
    userInfoBox.setAlignment(Pos.CENTER_LEFT)

    userInfo.getChildren.addAll(anchorLabel,userInfoBox)
    userInfo.setAlignment(Pos.BOTTOM_LEFT)
    userInfo.getStyleClass.add("hostScene-leftArea-label")
    userInfo.setSpacing(10)

    userInfo
  }
}
