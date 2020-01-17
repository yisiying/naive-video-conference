package org.seekloud.theia.faceAnalysis.model

import java.awt.image.BufferedImage
import java.nio.{ByteBuffer, FloatBuffer}

import org.bytedeco.javacpp.indexer.{ByteIndexer, UByteIndexer}
import org.bytedeco.opencv.global.{opencv_imgcodecs, opencv_imgproc}
import org.bytedeco.opencv.opencv_core.{Mat, Size}

/**
  * Created by sky
  * Date on 2019/9/24
  * Time at 下午5:20
  * 转化游戏引擎cpu_buffer为OpenCV—mat
  * +--------+----+----+----+----+------+------+------+------+
  * |        | C1 | C2 | C3 | C4 | C(5) | C(6) | C(7) | C(8) |
  * +--------+----+----+----+----+------+------+------+------+
  * | CV_8U  |  0 |  8 | 16 | 24 |   32 |   40 |   48 |   56 |
  * | CV_8S  |  1 |  9 | 17 | 25 |   33 |   41 |   49 |   57 |
  * | CV_16U |  2 | 10 | 18 | 26 |   34 |   42 |   50 |   58 |
  * | CV_16S |  3 | 11 | 19 | 27 |   35 |   43 |   51 |   59 |
  * | CV_32S |  4 | 12 | 20 | 28 |   36 |   44 |   52 |   60 |
  * | CV_32F |  5 | 13 | 21 | 29 |   37 |   45 |   53 |   61 |
  * | CV_64F |  6 | 14 | 22 | 30 |   38 |   46 |   54 |   62 |
  * +--------+----+----+----+----+------+------+------+------+
  *
  */
object Java2DConverter {
  def buffer2mat(buffer: ByteBuffer, mat: Mat): Unit = {
    //    val t1 = System.currentTimeMillis()
    val indexer: UByteIndexer = mat.createIndexer()
    val rows = mat.rows()
    val cols = mat.cols()
    for (i <- 0 until rows * cols) {
      indexer.put((i * 3).toLong, buffer.get(i * 4 + 2))
      indexer.put((i * 3 + 1).toLong, buffer.get(i * 4 + 1))
      indexer.put((i * 3 + 2).toLong, buffer.get(i * 4))
    }
    //    println(System.currentTimeMillis() - t1)
  }

  def mat2Buffer(byteBuffer: ByteBuffer, mat: Mat) = {
    //    val t1 = System.currentTimeMillis()
    val indexer: UByteIndexer = mat.createIndexer()
    val w = mat.cols()
    val h = mat.rows()
    val s = 3 * w * h
    for (i <- 0 until s) {
      val d = indexer.get(i)
      byteBuffer.put(d.toByte)
    }
    //    println(s"change use ${System.currentTimeMillis() - t1}")
    byteBuffer.array()
  }

  def mat2FloatBuffer(buffer: FloatBuffer, mat: Mat, size: Int) = {
    assert(mat.cols() * mat.rows() * 3 == size)
    val indexer: UByteIndexer = mat.createIndexer()
    for (i <- 0 until size) {
      val d = indexer.get(i)
      buffer.put(d)
    }
  }

  def main(args: Array[String]): Unit = {
    val mat = opencv_imgcodecs.imread("/Users/gaohan/Downloads/test_0.jpg")
    val size = TfModelFaceDetect.width * TfModelFaceDetect.height * TfModelFaceDetect.channel
    val ib = FloatBuffer.allocate(size)
    mat2FloatBuffer(ib, mat, size)
    ib.flip()
//    t.predict(ib)
    ib.flip()
    //remind 测试时间
//    t.predict(ib)
    //        t.simple_predict(mat);
  }
}
