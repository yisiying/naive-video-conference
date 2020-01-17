package org.seekloud.theia.faceAnalysis.face

import java.awt.Graphics
import java.awt.image.BufferedImage

import javax.swing.JPanel

class ImageSwitchPanel(var _image: Option[BufferedImage] = None) extends JPanel {

  {
    _image.foreach(update)
  }

  def update(image: BufferedImage): Unit = {
    _image = Some(image)
    repaint()
  }

  override def paintComponent(g: Graphics): Unit = {
    //println("paintComponent")
    super.paintComponent(g)
    _image.foreach(img => g.drawImage(img, 0, 0, this)) // see javadoc for more info on the parameters
  }

}
