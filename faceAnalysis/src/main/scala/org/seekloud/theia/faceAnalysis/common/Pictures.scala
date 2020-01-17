package org.seekloud.theia.faceAnalysis.common

import java.awt.image.BufferedImage

import javafx.scene.image.{Image, ImageView}
import javax.imageio.ImageIO
import org.seekloud.theia.faceAnalysis.core.CaptureActor.getClass
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * Author: zwq
  * Date: 2019/8/23
  * Time: 15:41
  * edit by sky for awt
  */
@deprecated
object Pictures {

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  val pictureMap: mutable.HashMap[String, Image] = mutable.HashMap.empty

  @deprecated
  def getPic(picUrl: String): ImageView = {

    if (pictureMap.get(picUrl).nonEmpty) {
      val picCp = pictureMap(picUrl)
      new ImageView(picCp)
    } else {
      val pic = new ImageView(picUrl)
      pictureMap.put(picUrl, pic.getImage)
      pic
    }
  }
}