package org.seekloud.theia.faceAnalysis.face

import org.bytedeco.opencv.global.{opencv_core, opencv_imgcodecs, opencv_imgproc, opencv_photo}
import org.bytedeco.opencv.opencv_core.{Mat, Point, Rect, Size}
import org.seekloud.theia.faceAnalysis.model.FaceAnalysis

import scala.collection.mutable.ArrayBuffer

/**
  * Created by sky
  * Date on 2019/8/20
  * Time at 下午2:31
  */
object AddGlass {
  def testSeamlessClone = {
    val srcMat = opencv_imgcodecs.imread("/Users/sky/IdeaProjects/theia/faceAnalysis/src/test/scala/org/seekloud/theia/faceAnalysis/face/face.jpeg")
    //    FaceAnalysis.drawGlass(srcMat)

    val glass = opencv_imgcodecs.imread("/Users/sky/IdeaProjects/theia/faceAnalysis/src/main/resources/img/glass2.png")

    val p = new Point()
    p.x()
    p.y(0)


    val src_mask = Mat.zeros(glass.size(), glass.`type`()).asMat()
    //    printMat(src_mask)
    opencv_core.bitwise_or(glass, src_mask, src_mask)
    opencv_core.bitwise_not(src_mask, src_mask)

    opencv_photo.seamlessClone(glass, srcMat, src_mask, p, src_mask, opencv_photo.MIXED_CLONE)


    opencv_imgcodecs.imwrite("/Users/sky/IdeaProjects/theia/faceAnalysis/src/test/scala/org/seekloud/theia/faceAnalysis/face/face2.jpeg", src_mask)

  }

  def testGlass = {
    val srcMat = opencv_imgcodecs.imread("/Users/sky/IdeaProjects/theia/faceAnalysis/src/test/scala/org/seekloud/theia/faceAnalysis/face/face.jpeg")

    val aiBuffer = ArrayBuffer[FaceAnalysis.AiInfo]()
    FaceAnalysis.find64(srcMat).foreach(f => FaceAnalysis.detectGlass4Seamless(f, aiBuffer))
    FaceAnalysis.draw(srcMat, aiBuffer)

    opencv_imgcodecs.imwrite("/Users/sky/IdeaProjects/theia/faceAnalysis/src/test/scala/org/seekloud/theia/faceAnalysis/face/face2.jpeg", srcMat)

  }

  def main(args: Array[String]): Unit = {
    testGlass
  }
}
