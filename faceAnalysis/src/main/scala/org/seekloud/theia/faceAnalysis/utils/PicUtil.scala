package org.seekloud.theia.faceAnalysis.utils

import java.nio.ByteBuffer

import org.bytedeco.javacpp.indexer.UByteIndexer
import org.bytedeco.opencv.global.opencv_imgcodecs
import org.bytedeco.opencv.opencv_core.Mat
import org.seekloud.theia.faceAnalysis.grpc.GrpcAgent

object PicUtil {
  def mat2byteArray(frame: Long, mat: Mat) = {
    val t1 = System.currentTimeMillis()
    val indexer: UByteIndexer = mat.createIndexer()
    val w = mat.cols()
    val h = mat.rows()
    val s = 3 * w * h
    val byteBuffer = ByteBuffer.allocate(s)
    for (i <- 0 until s) {
      val d = indexer.get(i)
      byteBuffer.put(d.toByte)
    }
//    println(s"change use ${System.currentTimeMillis() - t1}")
    GrpcAgent.predict(frame, w, h, 3, byteBuffer.array())
  }

  def main(args: Array[String]): Unit = {
    val l = opencv_imgcodecs.imread("C:\\Users\\yuwei\\Desktop\\11.jpg")
    //    mat2byteArray(
    println("done")
    Thread.sleep(10000)
  }

}
