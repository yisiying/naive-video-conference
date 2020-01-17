package org.seekloud.theia.faceAnalysis.opencv

import org.bytedeco.opencv.global.{opencv_core, opencv_imgcodecs, opencv_imgproc, opencv_photo}
import org.bytedeco.opencv.opencv_core.{Mat, Point, Rect, Size}
import org.seekloud.theia.faceAnalysis.model.FaceAnalysis

/**
  * Created by sky
  * Date on 2019/9/3
  * Time at 下午2:03
  */
object LOGO {
  val mat = opencv_imgcodecs.imread("/Users/sky/IdeaProjects/theia/faceAnalysis/src/test/scala/org/seekloud/theia/faceAnalysis/face/face.jpeg")
  val glass=opencv_imgcodecs.imread("/Users/sky/IdeaProjects/theia/faceAnalysis/model/glass2.png")


  def main(args: Array[String]): Unit = {
    // 定义感兴趣区域(位置，logo图像大小)// 定义感兴趣区域(位置，logo图像大小)
    Thread.sleep(3000)
    val t1=System.currentTimeMillis()
    val resize = new Mat()
    opencv_imgproc.resize(glass, resize, new Size(60, 40))

    println(System.currentTimeMillis()-t1)
//    val ROI = mat.apply(new Rect(0, 0, resize.cols, resize.rows))
//    opencv_core.addWeighted(ROI, 1.0, resize, 0.3, 0.0, ROI)


    val src_mask = Mat.zeros(resize.size(), resize.`type`()).asMat()
    opencv_core.bitwise_or(resize, src_mask, src_mask)
    opencv_core.bitwise_not(src_mask, src_mask)

    val p = new Point()
    p.x(60)
    p.y(40)

    opencv_photo.seamlessClone(resize, mat, src_mask, p, mat, opencv_photo.MIXED_CLONE)

    println(System.currentTimeMillis()-t1)
    opencv_imgcodecs.imwrite("test.jpg",mat)
  }
}
