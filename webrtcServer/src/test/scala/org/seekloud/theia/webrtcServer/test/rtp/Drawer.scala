package org.seekloud.theia.webrtcServer.test.rtp

import java.util.concurrent.{BlockingQueue, TimeUnit}

import org.bytedeco.javacv.{Frame, Java2DFrameConverter}
import org.seekloud.theia.webrtcServer.utils.{FrameRateCounter, UiUtil}

/**
  * Created by sky
  * Date on 2019/7/16
  * Time at 19:40
  * copy from hello_media @Tao Zhang
  */
class Drawer(title: String, queue: BlockingQueue[Frame]) extends Runnable {
  val converter = new Java2DFrameConverter()
  val frCounter = new FrameRateCounter(title)
  private val player = UiUtil.startPlayer(title)

  override def run(): Unit = {
    var c = 0
    println("Drawer started ------------")
    var frame = queue.poll(30, TimeUnit.SECONDS)
    println("22 ------------")

    while (frame != null && !Thread.interrupted()) {
      //println("33 ------------")
      c += 1
      frCounter.update()
      val img = converter.convert(frame)
      player.update(img)
      frame = queue.poll(10, TimeUnit.SECONDS)
      if (c % 100 == 0) {
        println(s"local frame count [$c], frame rate [${frCounter.getFrameRate}]")
      }

    }

    println(" ------------------   draw finished    !!!!!")

  }
}
