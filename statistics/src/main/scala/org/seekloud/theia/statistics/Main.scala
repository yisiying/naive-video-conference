package org.seekloud.theia.statistics

import org.seekloud.theia.statistics.components.HeaderBar
import org.seekloud.theia.statistics.pages.MainPage

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  * User: Taoz
  * Date: 6/3/2017
  * Time: 1:03 PM
  */
object Main {
  def main(args: Array[String]): Unit ={
    run()
  }


  def run(): Unit = {
    MainPage.show()
  }



}
