package org.seekloud.theia.webClient
import org.seekloud.theia.webClient.pages.MainPage
import org.scalajs.dom
import scala.scalajs.js.annotation.JSExport

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import org.seekloud.theia.webClient.pages.MainPage
/**
  * create by zhaoyin
  * 2019/7/17  2:35 PM
  */

@JSExportTopLevel("front.Main")
object Main {
  def main(args: Array[String]): Unit ={
    run()
  }

  @JSExport
  def run(): Unit = {
    MainPage.show()
  }
}
