package org.seekloud.theia.phoneClient.common.Components

import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.Event
import mhtml._
import org.seekloud.theia.phoneClient.Main


/**
  * create by 13
  * 2019/7/19  12:17 AM
  */
object PopWindow {

  val showPop = Var(emptyHTML)

  <img src="/theia/roomManager/static/img/loading.gif"></img>

  //阻止事件冒泡防止弹窗消失
  def stopCancel(e: Event, id: String): Unit = {
    //stopPropagation防止事件冒泡
    e.stopPropagation()
    dom.document.getElementById(id).setAttribute("disabled", "")
  }

  def commonPop(text: String): Unit ={
    showPop := {
      <div class="pop-background" onclick={(e: Event)=> showPop := emptyHTML}>
        <div class="pop-main" onclick={(e: Event)=>stopCancel(e,"pop-common")}>
          <div class="pop-header"></div>
          <div class="pop-content">
            <div class="pop-text">{text}</div>
          </div>
          <div class="pop-confirm">
            <div class="pop-button" onclick={(e: Event)=> showPop := emptyHTML}>确认</div>
          </div>
        </div>
      </div>
    }
  }
  def gotoPersonal()={
    Main.changeTab("Personal")
    showPop := emptyHTML
  }
  //底部弹出弹窗
  def bottomPop(text: String): Unit ={
    showPop := {
      <div class="bottom-background" onclick={(e: Event)=> showPop := emptyHTML}>
        <div class="bottom-main" onclick={(e: Event)=>stopCancel(e,"pop-common")}>
          <div class="bottom-header"></div>
          <div class="bottom-content">
            <div class="bottom-text">{text}</div>
          </div>
          <div class="bottom-confirm">
            <div class="bottom-button" onclick={(e: Event)=> gotoPersonal()}>确认</div>
            <div class="bottom-button" onclick={(e: Event)=> showPop := emptyHTML}>取消</div>
          </div>
        </div>
      </div>
    }
  }


//  var currImgNum = Math.floor(Math.random()*8)+1
//  var initHeadImg = "/theia/roomManager/static/img/headPortrait/"+currImgNum+".jpg"
//  def changeHeadImg()={
//    //    dom.document.getElementById("imgBubble").setAttribute("style","display:none")
//    var headNum = Math.floor(Math.random()*9)
//    while (currImgNum == headNum){
//      headNum = Math.floor(Math.random()*9)
//    }
//    currImgNum = headNum
//    dom.document.getElementById("random-head").setAttribute("src","/theia/roomManager/static/img/headPortrait/"+headNum.toInt+".jpg")
//  }
//
//  def changeUserInfo(userId:Long):Unit = {
//    val name = dom.document.getElementById("change-username").asInstanceOf[Input].value
//    val src = dom.document.getElementById("random-head").asInstanceOf[Image].src
//    if(name==""){
//      JsFunc.alert("昵称不能为空！")
//    }else{
//      Http.getAndParse[CommonRsp](Routes.UserRoutes.nickNameChange(userId,name)).map{
//        case Right(nameRsp)=>
////          showUserName := name
//        case Left(value)=>
//      }
//      val img2file = new ChangeImg2File(src)
//      val imgFile = img2file.changeImg2File(img2file)
//      var form = new FormData()
//      form.append("fileUpload", imgFile)
//      Http.postFormAndParse[ImgChangeRsp](Routes.UserRoutes.uploadImg(0,userId.toString),form).map{
//        case Right(imgRsp)=>
//          JsFunc.alert("更改个人信息成功！")
////          showPersonCenter := emptyHTML
//          dom.document.getElementById("userHeadImg").asInstanceOf[Image].src= imgRsp.url
//        case Left(value)=>
//          JsFunc.alert("更改个人信息失败")
////          showPersonCenter := emptyHTML
//      }
//    }
//  }


}
