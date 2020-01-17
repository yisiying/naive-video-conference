package org.seekloud.theia.statistics.common

import mhtml.{Var, mount}
import org.scalajs.dom
import org.scalajs.dom.HashChangeEvent
import org.scalajs.dom.raw.Event

/**
  * User: Taoz
  * Date: 6/3/2017
  * Time: 1:46 PM
  */
trait PageSwitcher {

  import scalatags.JsDom.short._

  var currentMainPageHash: Var[String] = Var("main")
  var currentPageHash: Var[List[String]] = Var(Nil)

  private val bodyContent = div(*.height := "100%").render

  def getCurrentHash: String = dom.window.location.hash


  private[this] var internalTargetHash = ""


  //init.
  {
    val func = {
      e: HashChangeEvent =>
        //only handler browser history hash changed.
        if (internalTargetHash != getCurrentHash) {
          println(s"hash changed, new hash: $getCurrentHash")
          internalTargetHash =getCurrentHash
          switchPageByHash()
        }
    }
    dom.window.addEventListener("hashchange", func, useCapture = false)
  }


  protected def switchToPage(pageName: List[String]): Unit = {
    currentPageHash.update(_ => pageName)
  }

  def getCurrentPageName: Var[List[String]] = currentPageHash

  def switchPageByHash(): Unit

}
