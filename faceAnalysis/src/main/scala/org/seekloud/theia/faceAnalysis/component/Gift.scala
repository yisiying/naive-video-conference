package org.seekloud.theia.faceAnalysis.component

import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import org.seekloud.theia.faceAnalysis.common.Constants

/**
  * User: shuai
  * Date: 2019/9/25
  * Time: 15:13
  */
object Gift {

  val width = Constants.AppWindow.width * 0.9
  val height = Constants.AppWindow.height * 0.75

  lazy val giftBtnLists: List[Button] = List[Button](
    new Button("", new ImageView("img/clap.png")),
    new Button("", new ImageView("img/cake.png")),
    new Button("", new ImageView("img/flower.png")),
    new Button("", new ImageView("img/car.png")),
    new Button("", new ImageView("img/boat.png")),
    new Button("", new ImageView("img/plane.png")),
    new Button("", new ImageView("img/rocket.png")),
  )

  giftBtnLists.foreach {
    button =>
      button.setPrefSize(37, 36)
  }

  def getGiftGridPane: GridPane = {
    val gridPane = new GridPane
    gridPane.setHgap(0)
    gridPane.setVgap(0)
    gridPane.setPadding(new Insets(0, 0, 0, 0))
    var index = 0
    giftBtnLists.foreach { button =>
      gridPane.add(button, index % 9, index / 9)
      index += 1
    }

    gridPane.setLayoutY(height * 0.73)
    gridPane.setLayoutX(width * 0.63)
    gridPane
  }
}
