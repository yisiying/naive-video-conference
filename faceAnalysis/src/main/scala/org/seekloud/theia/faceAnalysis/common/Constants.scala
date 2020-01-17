package org.seekloud.theia.faceAnalysis.common

import java.io.File
import javafx.scene.paint.Color

/**
  * Created by sky
  * Date on 2019/8/19
  * Time at 下午2:16
  */
object Constants {

  val cachePath: String = s"${System.getProperty("user.home")}/.theia/faceAnalysis"

  val cacheFile = new File(cachePath)
  if (!cacheFile.exists()) cacheFile.mkdirs()

  val loginInfoCachePath: String = cachePath + "/login"

  object AppWindow {
    val width = 1200
    val height = 920
  }
  object DefaultPlayer {
    val width = 640
    val height = 480
  }
  object Like{
    val up=1
    val down=0
  }

  val loginInfoCache = new File(loginInfoCachePath)
  if (!loginInfoCache.exists()) loginInfoCache.mkdirs()

  @deprecated
  val barrageColors = List(
    Color.PINK,
    Color.HOTPINK,
    Color.WHITE,
    Color.RED,
    Color.ORANGE,
    Color.YELLOW,
    Color.GREEN,
    Color.CYAN,
    Color.BLUE,
    Color.PURPLE,
    Color.BROWN,
    Color.BURLYWOOD,
    Color.CHOCOLATE,
    Color.GOLD,
    Color.GREY
  )
}
