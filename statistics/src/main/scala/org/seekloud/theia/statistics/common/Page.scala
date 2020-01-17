package org.seekloud.theia.statistics.common

import scala.language.implicitConversions
import scala.xml.Elem
/**
  * Created by hongruying on 2018/4/8
  */
trait Page extends Component {

  def render: Elem

}

object Page{
  implicit def page2Element(page: Page): Elem = page.render
}