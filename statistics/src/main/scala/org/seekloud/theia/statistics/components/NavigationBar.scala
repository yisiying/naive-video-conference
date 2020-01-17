package org.seekloud.theia.statistics.components

import org.seekloud.theia.statistics.common.{Component, Routes}
import org.seekloud.theia.statistics.pages.MainPage
import mhtml._
/**
  * Created by hongruying on 2018/4/8
  */
class NavigationBar(showPage:String) {

  private val showPageVar = Var(showPage)

  def changeShowPage(page: String) = {
    println(s"page=${page}")
    showPageVar := page
  }

  private val navigationList=
    List(NavigationItem(0, "用户概况","/main/user"), NavigationItem(1,"用户列表","/main/userList"),NavigationItem(1, "活跃用户","/main/activeUser"),
      NavigationItem(2, "图文分析","/main/tuwen"),NavigationItem(2, "消息分析","/main/message"),
      NavigationItem(2, "标签管理","/main/tagManager"),NavigationItem(2, "用户筛选","/main/filter")
      ,NavigationItem(2, "菜单分析","/main/menuManager"),NavigationItem(2,"创建二维码","/main/qrCode"))

  private val internalItems = navigationList.map(item => structureItem(item))



  private case class structureItem(body: NavigationItem) extends Component {

    val visibilityClass = showPageVar.map{ p =>
      p == body.page match {
        case true => "bar active"
        case _ => "bar"
      }
    }

    def gotoPage(): Unit = {
      showPageVar := body.page
    }


    override def render =
      <div width="100%">
        <div class ={visibilityClass} width="80%" display ="inline-block" onclick ={() => gotoPage()}>
          <span>{body.name}</span>
        </div>
      </div>

  }
  //
  private val list =
    <div class ="sideList">
      {internalItems.map(t => t.render)}
    </div>



  def render =
    <div class ="leftBar">
      <div class ="top">
        统计分析
      </div>
      <div class ="sideBar">
        {list}
      </div>
    </div>


}

case class NavigationItem(id: Int, name: String,page:String)

object NavigationBar{
  def apply(showPage:String): NavigationBar = new NavigationBar(showPage)
}
