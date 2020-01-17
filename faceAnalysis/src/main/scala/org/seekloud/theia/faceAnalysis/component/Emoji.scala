package org.seekloud.theia.faceAnalysis.component

import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.layout.GridPane
import javafx.scene.text.Font
import org.seekloud.theia.faceAnalysis.common.Constants

/**
  * User: shuai
  * Date: 2019/9/25
  * Time: 15:03
  */
object Emoji {

  val emojiFont="Segoe UI Emoji"

  val width = Constants.AppWindow.width * 0.9
  val height = Constants.AppWindow.height * 0.75

  lazy val emojiBtnLists: List[Button] = List[Button](
    new Button("\uD83E\uDD2D"), //🤭
    new Button("\uD83D\uDE03"), //😃
    new Button("\uD83D\uDE06"), //😆
    new Button("\uD83D\uDE02"), //😂
    new Button("\uD83D\uDE42"), //🙂
    new Button("\uD83D\uDC8E"), //💎
    new Button("\uD83D\uDC84"), //💄
    new Button("\uD83D\uDC8D"), //💍
    new Button("\uD83D\uDC51"), //👑

    new Button("\uD83D\uDE07"), //😇
    new Button("\uD83D\uDE05"), //😅
    new Button("\uD83D\uDE0D"), //😍
    new Button("\uD83D\uDE18"), //😘
    new Button("\uD83E\uDD11"), //🤑
    new Button("\uD83D\uDC37"), //🐷
    new Button("\uD83D\uDD76"), //🕶
    new Button("\uD83C\uDFC6"), //🏆
    new Button("\uD83C\uDF39"), //🌹

    new Button("\uD83D\uDE0E"), //😎
    new Button("\uD83E\uDD10"), //🤐
    new Button("\uD83D\uDE12"), //😒
    new Button("\uD83D\uDE37"), //😷
    new Button("\uD83D\uDE2D"), //😭
    new Button("☀"), //☀
    new Button("⛈"), //⛈
    new Button("\uD83D\uDD25"), //🔥
    new Button("❄"), //❄

    new Button("\uD83D\uDCAF"), //💯
    new Button("❤"), //❤
    new Button("\uD83D\uDCA2"), //💢
    new Button("\uD83D\uDC4B"), //👋
    new Button("\uD83D\uDC4C"), //👌
    new Button("\uD83D\uDC31"), //🐱
    new Button("\uD83D\uDC36"), //🐶
    new Button("\uD83C\uDF52"), //🍒
    new Button("\uD83E\uDD42"), //🥂
  )

  emojiBtnLists.foreach {
    button =>
      button.setFont(Font.font(emojiFont, 12))
      button.setPrefSize(37, 36)
  }

  def getEmojiGridPane: GridPane = {
    val gridPane = new GridPane
    gridPane.setHgap(0)
    gridPane.setVgap(0)
    gridPane.setPadding(new Insets(0, 0, 0, 0))
    var index = 0
    emojiBtnLists.foreach { button =>
      gridPane.add(button, index % 9, index / 9)
      index += 1
    }
    gridPane.setLayoutY(height * 0.56)
    gridPane.setLayoutX(width * 0.63)
    gridPane
  }
}
