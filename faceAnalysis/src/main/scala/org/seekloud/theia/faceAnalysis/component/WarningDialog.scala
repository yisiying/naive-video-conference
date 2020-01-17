package org.seekloud.theia.faceAnalysis.component

import java.awt.event.{ActionEvent, ActionListener}
import java.awt._
import java.util.Optional

import javafx.scene.control.Alert.AlertType
import javafx.scene.control.{Alert, ButtonType}
import javax.swing.JFrame

/**
  * User: TangYaruo
  * Date: 2019/3/8
  * Time: 10:55
  */
object WarningDialog {

  def initWarningDialog(context:String): Unit = {
    val alert = new Alert(AlertType.WARNING)
    alert.setTitle("警告")
    alert.setHeaderText("")
    alert.setContentText(context)
    alert.showAndWait()
  }

  def warningDialog(frame:JFrame):Unit = {
    //todo 设置居中
    val d = new Dialog(frame , "⚠️警告", true)
    d.setLayout(new BorderLayout())
    val b = new Button ("确认")
    b.addActionListener(new ActionListener {
      override def actionPerformed(e: ActionEvent): Unit = {
        d.setVisible(false)
      }
    })
    d.add(new Label("Click button to continue."),BorderLayout.CENTER)
    d.add(b,BorderLayout.SOUTH)
    d.setSize(400,200)
    d.setVisible(true)
  }

}
