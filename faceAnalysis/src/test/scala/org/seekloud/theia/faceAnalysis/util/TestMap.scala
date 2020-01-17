package org.seekloud.theia.faceAnalysis.util

import scala.collection.mutable
/**
  * Created by sky
  * Date on 2019/9/2
  * Time at 上午11:03
  */
object TestMap {
  def main(args: Array[String]): Unit = {
    val m=mutable.HashMap[Int,Int]()

    m.put(1,1)
    m.put(2,2)

    println(m.remove(1))
  }
}
