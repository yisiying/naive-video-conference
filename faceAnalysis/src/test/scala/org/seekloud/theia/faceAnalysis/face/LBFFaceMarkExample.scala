package org.seekloud.theia.faceAnalysis.face

import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.opencv.global.opencv_videoio
import org.bytedeco.opencv.opencv_core._
import org.bytedeco.opencv.opencv_videoio.VideoCapture
import org.seekloud.theia.faceAnalysis.model.FaceAnalysis

import scala.collection.mutable.ArrayBuffer

/**
  * Created by sky
  * Date on 2019/8/19
  * Time at 下午4:45
  */
object LBFFaceMarkExample {
  /**
    * print mat
    **/
  def printMat(mat: Mat) = {
    for (i <- 0 until mat.rows()) {
      for (j <- 0 until mat.cols()) {
        print(mat.ptr(i, j).get(0))
      }
      print("\n")
    }
    println("\n")
  }


  def main(args: Array[String]): Unit = {
    val player = UiUtil.startPlayer("test")
    FaceAnalysis

    // Set up webcam for video capture
    val cam = new VideoCapture(0)
    cam.set(opencv_videoio.CAP_PROP_FRAME_WIDTH, 640)
    cam.set(opencv_videoio.CAP_PROP_FRAME_HEIGHT, 480)
    println(cam.get(opencv_videoio.CAP_PROP_FRAME_WIDTH), cam.get(opencv_videoio.CAP_PROP_FRAME_HEIGHT))
    // Variable to store a video frame and its grayscale
    val mat = new Mat

    val biConv = new Java2DFrameConverter

    val aiBuffer = ArrayBuffer[FaceAnalysis.AiInfo]()

    // Read a frame
    var t2 = System.currentTimeMillis()
    while (cam.read(mat)) {
      val t1 = System.currentTimeMillis()
      aiBuffer.clear()
      FaceAnalysis.find64(mat).foreach { f =>
//        FaceAnalysis.detectBeard(f, aiBuffer)
//        FaceAnalysis.detectGlass(f, aiBuffer)
//        FaceAnalysis.detectPoint(f, aiBuffer)
      }
      t2 = System.currentTimeMillis()
      println(s"detector use ${t2 - t1}")
      val frame = FaceAnalysis.draw(mat, aiBuffer)
      println(s"draw user ${System.currentTimeMillis() - t2}")
      val bi = biConv.convert(frame)

      player.update(bi)
    }
  }
}
