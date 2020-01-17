package org.seekloud.theia.faceAnalysis.component

import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import org.seekloud.theia.faceAnalysis.common.Constants

/**
  * User: shuai
  * Date: 2019/9/25
  * Time: 15:33
  */
object Special {

  val width = Constants.AppWindow.width * 0.9
  val height = Constants.AppWindow.height * 0.75

  lazy val specialBtnLists: List[Button] = List[Button](
    new Button("", new ImageView("img/enlarge.png")),
    new Button("", new ImageView("img/reduce.png")),
  )

  specialBtnLists.foreach {
    button =>
      button.setPrefSize(37, 36)
  }

  def getSpecialGridPane: GridPane = {
    val gridPane = new GridPane
    gridPane.setHgap(0)
    gridPane.setVgap(0)
    gridPane.setPadding(new Insets(0, 0, 0, 0))
    var index = 0
    specialBtnLists.foreach { button =>
      gridPane.add(button, index % 9, index / 9)
      index += 1
    }

    gridPane.setLayoutY(height * 0.73)
    gridPane.setLayoutX(width * 0.63)
    gridPane
  }
}
