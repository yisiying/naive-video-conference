package org.seekloud.theia.faceAnalysis.scene.panel

import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import org.seekloud.theia.faceAnalysis.component.UserBox.userBox

/**
  * Created by sky
  * Date on 2019/10/11
  * Time at 下午5:27
  */
trait TopPanel {
  protected def logout(): Unit

  def initTopBox: HBox ={
    val top = new HBox()

    val logoutIcon = new ImageView("img/logout.png")
    logoutIcon.setFitHeight(25)
    logoutIcon.setFitWidth(25)
    val logoutButton = new Button("注销", logoutIcon)
    logoutButton.setOnAction(_ => logout())
    logoutButton.getStyleClass.add("topBtn")

    top.getChildren.addAll(userBox,logoutButton)
    top.setAlignment(Pos.BOTTOM_LEFT)
    top.setSpacing(390)
    top
  }

  val topBox:HBox = initTopBox
}
