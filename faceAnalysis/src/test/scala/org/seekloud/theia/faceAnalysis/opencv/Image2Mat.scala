package org.seekloud.theia.faceAnalysis.opencv

import org.bytedeco.opencv.global.opencv_imgcodecs

/**
  * Created by sky
  * Date on 2019/9/24
  * Time at 下午3:39
  */
object Image2Mat {
  val l=opencv_imgcodecs.imread("test.jpg")

  def main(args: Array[String]): Unit = {

    println(l.`type`())
  }
}
