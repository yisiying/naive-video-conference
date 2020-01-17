package org.seekloud.theia.statistics.components

import org.seekloud.theia.statistics.common.{Component, Routes}
import org.seekloud.theia.statistics.pages.MainPage
import org.seekloud.theia.statistics.utils.{Http, LocalStorageUtil, Shortcut}
import org.scalajs.dom
import mhtml._
import org.scalajs.dom.raw.MouseEvent

import concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem

/**
  * Created by hongruying on 2018/12/10
  * Modified by 13 on 2019/10/11
  *
  * 基于原来的headerBar修改为Statistic使用的headerBar
  */
class HeaderBar() extends Component{

  // TODO 登录状态采用后台鉴权初始化
//  private val isLoginVar = Var(false)

  // TODO 显示状态拟采用input label，避免修改class
//  private val isShow = Var(false)

//  private val headerStyle = width.map{ w =>
//    s"margin-left: ${(w-1180)/2}px;"
//  }

//  private val userDivShowStyle = isLoginVar.map{
//    case true => "display:none;"
//    case false => "display:block;"
//  }

//  private val logoutDisplay = isShow.map{
//    case true => "display:block;"
//    case false => "display:none"
//  }




//  private def showLogout(e: MouseEvent) = {
//    e.preventDefault()
//    isShow := true
//  }

//  private def unshowLogout(e: MouseEvent) = {
//    e.preventDefault()
//    isShow := false
//  }

//<div class ="userDiv" onmouseover ={e:MouseEvent => showLogout(e)} onmouseout ={e:MouseEvent => unshowLogout(e)}>
//
//  private val userIcon =
//    <div class ="userDiv" style ={userDivShowStyle} onmouseover ={e:MouseEvent => showLogout(e)} onmouseout ={e:MouseEvent => unshowLogout(e)}>
//      <div class="user">
//        <span class ="username"></span>
//        <div class ="icon"></div>
//      </div>
//
//    </div>


  def changePage(hash: String, scrollY: Int): Unit ={
    dom.window.location.hash = hash
    dom.window.scrollTo(0, scrollY)
  }

  override def render: Elem =
    <div class ="header-container">
      <div class ="header">
        <div class ="header-title">
          <div>theia统计平台</div>
        </div>
        <div class="header-select">
          <a class="header-option" href="#/AdminMain/" onclick={() => changePage("/AdminMain/", 0)}>数据总览</a>
          <a class="header-option" href="#/AdminMain/" onclick={() => changePage("/AdminMain/", 200)}>数据趋势</a>
          <a class="header-option" href="#/AdminMain/" onclick={() => changePage("/AdminMain/", 650)}>录像数据</a>
        </div>
      </div>
    </div>



}
