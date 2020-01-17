package org.seekloud.theia.webrtcServer.utils

import java.util.concurrent.atomic.AtomicInteger

/**
  * Author: Tao Zhang
  * Date: 3/30/2019
  * Time: 2:35 PM
  */
class FrameRateCounter(name: String) {

  private var targetSecond = System.currentTimeMillis() / 1000

  private var count = 0
  //private var currentRate = 0
  private val atomCurrentRate = new AtomicInteger(0)

  def update(): Unit = {
    val currentSecond = System.currentTimeMillis() / 1000
    if (currentSecond == targetSecond) {
      count += 1
    } else if (currentSecond == targetSecond + 1) {
      atomCurrentRate.set(count)
      count = 1
      targetSecond += 1
      //println(s"$name Rate: ${atomCurrentRate.get()}")
    } else {
      atomCurrentRate.set(-1)
      count = 1
      targetSecond = currentSecond
    }
  }

  def getFrameRate: Int = atomCurrentRate.get()

}
