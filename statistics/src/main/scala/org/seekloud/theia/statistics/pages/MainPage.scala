package org.seekloud.theia.statistics.pages

import org.seekloud.theia.statistics.common.PageSwitcher
import mhtml.{Cancelable, Rx, Var, mount}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.seekloud.theia.statistics.components.HeaderBar

import scala.xml.Elem

/**
  * Created by hongruying on 2018/4/8
  */
object MainPage extends PageSwitcher {

//  var widthx = dom.document.body.clientWidth
//
//  val widthxVar = Var(widthx)
//
//
//  dom.window.addEventListener("resize", handleResize, false)
//
//  def handleResize = { e:Event =>
//    println(s"client change size, width=${widthx}")
//    widthx = dom.document.body.clientWidth
//    widthxVar := widthx
//  }


//  private val headerStyle = widthxVar.map{ w =>
//    s"margin-left: ${(w-1180)/2}px;"
//  }

//  val header = new HeaderBar(widthxVar)
//  val sideBar = new NavigationBar("/main/user")


  override def switchPageByHash(): Unit = {
    val tokens = {
      val t = getCurrentHash.split("/").toList
      if (t.nonEmpty) {
        t.tail
      } else Nil
    }

    println(s"currentHash=$tokens")
    switchToPage(tokens)
  }

  //currentPage, 包含mainDiv和导航栏
  private val currentPage: Rx[Elem] = currentPageHash.map {

    case "AdminMain" :: Nil =>
      <div>
        {new HeaderBar().render}
        {AdminMainPage.render}
      </div>

    case "RecordStat" :: recordId :: Nil =>
      <div>
        {new HeaderBar().render}
        {new RecordStatistics(recordId.toLong).render}
      </div>

    case Nil =>
      <div>
        {new HeaderBar().render}
        {AdminMainPage.render}
      </div>

    case _ => <div>TODO</div>

  }

  def show(): Cancelable = {
    switchPageByHash()
    val page =
      <div>
        {currentPage}
      </div>
    mount(dom.document.body, page)
  }

}
