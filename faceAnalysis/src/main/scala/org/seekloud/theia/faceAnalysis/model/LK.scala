package org.seekloud.theia.faceAnalysis.model

import org.seekloud.theia.faceAnalysis.common.AppSettings
import org.slf4j.{Logger, LoggerFactory}
import org.seekloud.theia.faceAnalysis.common.AppSettings._

/**
  * User: gaohan
  * Date: 2019/10/30
  * Time: 15:59
  * 关键点平滑处理
  */
object LK {

  private val log = LoggerFactory.getLogger(this.getClass)


  def caculate(preLandmark: List[Array[(Int, Int)]], nowLandmark: List[Array[(Int,Int)]]) = {
    for (i <- 0 to nowLandmark.length) {
      for (j <- 0 to preLandmark.length) {
        if (iou(preLandmark(j), nowLandmark(i)) > iou_thres) {
          smooth(preLandmark(j), nowLandmark(i))
        } else {
          nowLandmark(i)
        }
      }
    }
  }


  def iou(preLandmark: Array[(Int, Int)], nowLandmark: Array[(Int, Int)]) = {

    val rec4now = Array(getMin(nowLandmark)._1, getMin(nowLandmark)._2, getMax(nowLandmark)._1, getMax(nowLandmark)._2)
    val rec4pre = Array(getMin(preLandmark)._1, getMin(preLandmark)._2, getMax(preLandmark)._1, getMax(preLandmark)._2)

    val rec4nowS = (rec4now(2) - rec4now(0)) * (rec4now(3) - rec4now(1))
    val rec4preS = (rec4pre(2) - rec4pre(0)) * (rec4pre(3) - rec4pre(1))

    val sumArea = rec4nowS + rec4preS

    val x1 = math.max(rec4now(0), rec4pre(0))
    val y1 = math.max(rec4now(1), rec4pre(1))
    val x2 = math.min(rec4now(2), rec4pre(2))
    val y2 = math.min(rec4now(3), rec4pre(3))

    val intersect = math.max(0, x2 - x1) * math.max(0, y2 - y1)

    val result = intersect.toDouble / (sumArea - intersect).toDouble

    result

  }

  def smooth(preLandmark: Array[(Int, Int)], nowLandmark: Array[(Int, Int)]) = { //pre和now的array长度都是68
    var lk = Array[(Int, Int)]()
    for (i <- 0 to nowLandmark.length) {

      val dis = math.sqrt((nowLandmark(i)._1 - preLandmark(i)._1) * (nowLandmark(i)._1 - preLandmark(i)._1) + (nowLandmark(i)._2 - preLandmark(i)._2) * (nowLandmark(i)._2 - preLandmark(i)._2))
      if (dis < AppSettings.thres) {
        //fixme 此处bug
        lk = Array(preLandmark(i))
      } else {
        lk = Array(doMovingAverage(nowLandmark(i), preLandmark(i))) //取两点之间的平滑处理
      }
    }
    lk

  }

  def doMovingAverage(nowPoint: (Int, Int), prePoint: (Int, Int)) = {
    val x = alpha * nowPoint._1 + (1 - alpha) * prePoint._1
    val y = alpha * nowPoint._2 + (1 - alpha) * prePoint._2
    (x.toInt, y.toInt)

  }

  def getMax(array: Array[(Int, Int)]) = {
    val x = array.map(a => a._1).max
    val y = array.map(b => b._2).max
    (x, y)
  }

  def getMin(array: Array[(Int, Int)]) = {
    val x = array.map(a => a._1).min
    val y = array.map(b => b._2).min
    (x, y)
  }

}
