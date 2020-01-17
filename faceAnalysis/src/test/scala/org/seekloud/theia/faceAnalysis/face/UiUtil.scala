package org.seekloud.theia.faceAnalysis.face

import javax.swing.JFrame

/**
  * Author: Tao Zhang
  * Date: 6/14/2019
  * Time: 9:48 AM
  */
object UiUtil {



  def startPlayer(title: String = "TestFFmpegDevice"): ImageSwitchPanel = {
    val jFrame = new JFrame(title)
    jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    jFrame.setSize(1920, 1080)
    val imagePanel = new ImageSwitchPanel()
    jFrame.getContentPane.add(imagePanel)
    jFrame.setVisible(true)
    imagePanel
  }


}
