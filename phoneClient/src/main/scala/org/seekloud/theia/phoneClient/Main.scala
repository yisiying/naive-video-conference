package org.seekloud.theia.phoneClient

import mhtml._
import org.scalajs.dom
import org.seekloud.theia.phoneClient.common.Components.PopWindow
import org.seekloud.theia.phoneClient.common.PageSwitcher
import org.seekloud.theia.phoneClient.pages.{LiveListPage, LiveRoomPage, PersonalPage, RecordListPage, RecordRoomPage}
import org.seekloud.theia.protocol.ptcl.CommonInfo.{RoomInfo, UserInfo}
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * create by zhaoyin
  * 2019/8/30  3:46 PM
  */
object Main extends PageSwitcher{

  var preLive: Option[LiveRoomPage] = None
  var preRecord: Option[RecordRoomPage] = None

  def hashChangeHandle(): Unit ={
    preRecord.foreach(_.exitRecord().foreach(_ => preRecord = None))
  }

  val currentPage = currentHashVar.map { current =>
    //hash值变化时必须执行的函数
    hashChangeHandle()
    current match {
      case "Live" :: Nil =>
        tabChoose := "Live"
        if (dom.window.localStorage.getItem("roomId") != null) {
          preLive.foreach(_.closeRm)
          clearRecordInfo()
          clearRoomInfo()
        }
        new LiveListPage().app
      case "Live" :: liveId :: Nil =>
        tabChoose := "liveRoom"
        preLive = Some(new LiveRoomPage())
        preLive.get.app
      case "Record" :: Nil =>
        clearRecordInfo()
        tabChoose := "Record"
        new RecordListPage().app
      case "Record" :: roomId :: time :: Nil =>
        tabChoose := "RecordRoom"
        if (dom.window.localStorage.getItem("recordName") != null){
          preRecord = Some(new RecordRoomPage(dom.window.localStorage.getItem("recordCoverImg"), dom.window.localStorage.getItem("recordName"), dom.window.localStorage.getItem("recordStartTime").toLong, roomId.toLong))
          preRecord.get.app
        }
        else
          new RecordListPage().app
      case "Personal" :: Nil =>
        tabChoose := "Personal"
        PersonalPage.app
      case _ =>
        dom.window.location.hash = s"#/Live"
        emptyHTML
    }
  }
  def clearRoomInfo() = {
    if(dom.window.localStorage.getItem("roomId") != null){
      dom.window.localStorage.removeItem("roomId")
      dom.window.localStorage.removeItem("coverImgUrl")
      dom.window.localStorage.removeItem("headImgUrl")
      dom.window.localStorage.removeItem("roomName")
    }
  }
  def clearRecordInfo()={
    if(dom.window.localStorage.getItem("recordName") != null){
      dom.window.localStorage.removeItem("recordName")
      dom.window.localStorage.removeItem("recordCoverImg")
      dom.window.localStorage.removeItem("recordStartTime")
    }
  }

  val tabChoose = Var("Live")
  val footerTab = tabChoose.map(i=>
    if(i == "Live"){
      <div class="footerTabs">
        <div class="footerTabs-item icontabs-choose">
          <div class="iconfont iconzhibobofangshexiangjitianxianxianxing" style="font-size:.25rem"></div>
          <div class="iconfont-text">直播</div>
        </div>
        <div class="footerTabs-item icontabs-nochoose" onclick={()=>changeTab("Record")}>
          <div class="iconfont icontubiao- " style="font-size:.25rem"></div>
          <div class="iconfont-text">视频</div>
        </div>
        <div class="footerTabs-item icontabs-nochoose" onclick={()=>changeTab("Personal")}>
          <div class="iconfont iconzhanghu" style="font-size:.25rem"></div>
          <div class="iconfont-text">个人中心</div>
        </div>
      </div>
    }else if(i=="Record"){
      <div class="footerTabs">
        <div class="footerTabs-item icontabs-nochoose" onclick={()=>changeTab("Live")} >
          <div class="iconfont iconzhibobofangshexiangjitianxianxianxing" style="font-size:.25rem"></div>
          <div class="iconfont-text">直播</div>
        </div>
        <div class="footerTabs-item icontabs-choose">
          <div class="iconfont icontubiao- " style="font-size:.25rem"></div>
          <div class="iconfont-text">视频</div>
        </div>
        <div class="footerTabs-item icontabs-nochoose" onclick={()=>changeTab("Personal")}>
          <div class="iconfont iconzhanghu" style="font-size:.25rem"></div>
          <div class="iconfont-text">个人中心</div>
        </div>
      </div>
    }else if(i=="Personal"){
      <div class="footerTabs">
        <div class="footerTabs-item icontabs-nochoose" onclick={()=>changeTab("Live")}>
          <div class="iconfont iconzhibobofangshexiangjitianxianxianxing" style="font-size:.25rem"></div>
          <div class="iconfont-text">直播</div>
        </div>
        <div class="footerTabs-item icontabs-nochoose" onclick={()=>changeTab("Record")} >
          <div class="iconfont icontubiao- " style="font-size:.25rem"></div>
          <div class="iconfont-text">视频</div>
        </div>
        <div class="footerTabs-item icontabs-choose">
          <div class="iconfont iconzhanghu" style="font-size:.25rem"></div>
          <div class="iconfont-text">个人中心</div>
        </div>
      </div>
    }else emptyHTML
  )
  def changeTab(str: String)={
    tabChoose:=str
    dom.window.location.hash = "/" + str
  }

  def show(): Cancelable = {
    switchPageByHash()
    val page =
      <div class="top" id="top">
        {currentPage}
        {footerTab}
        {PopWindow.showPop}
      </div>
    mount(dom.document.body, page)
  }

  def main(args: Array[String]): Unit = {
    show()
  }
}
