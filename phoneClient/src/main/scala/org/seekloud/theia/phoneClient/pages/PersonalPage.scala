package org.seekloud.theia.phoneClient.pages

import mhtml._
import org.scalajs.dom.Event
import org.scalajs.dom.html.Input
import org.scalajs.dom
import org.seekloud.theia.phoneClient.common.Components.PopWindow
import org.seekloud.theia.phoneClient.common.Routes
import org.seekloud.theia.protocol.ptcl.client2Manager.http.CommonProtocol._
import org.seekloud.theia.phoneClient.util.{Http, JsFunc}
import org.scalajs.dom
import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.syntax._
import io.circe.generic.auto._
/**
  * create by zhaoyin
  * 2019/9/2  11:55 AM
  */
object PersonalPage {

  //所有本页面弹窗的共用实体showPop
  val showPop = Var(emptyHTML)

  val isLogin = Var(dom.window.localStorage.getItem("userName") != null && dom.window.localStorage.getItem("isTemUser") == null) //userName

  val personPage = isLogin.map(i=>
    if(i){
      <div class="personalInfo">
        <img src={dom.window.localStorage.getItem("userHeaderImgUrl")} class="loginImg"></img>
        <div class="loginUserName">{dom.window.localStorage.getItem("userName")}</div>
        <div class="loginOut" onclick={()=>logOut()}>登出</div>
      </div>
    }else{
      <div class="personalInfo">
        <img src="/theia/roomManager/static/img/头像.png" class="headImg"></img>
        <div class="loginTips">登录后，即可参与互动</div>
        <div class="loginButton" onclick={()=> loginPop()}>登录</div>
        {showPop}
        <div class="registerTips" onclick={()=> registerPop()}>还没有账号？点击注册</div>
      </div>
    }
  )

  def logOut() = {
    dom.window.localStorage.removeItem("userName")
    dom.window.localStorage.removeItem("userHeaderImgUrl")
    dom.window.localStorage.removeItem("userId")
    dom.window.localStorage.removeItem("token")
    isLogin := false
  }

  def login(): Unit = {
    val account = dom.document.getElementById("login-account").asInstanceOf[Input].value
    val password = dom.document.getElementById("login-password").asInstanceOf[Input].value
    if(account =="" || password ==""){
      JsFunc.alert("请完整填写！")
    }else{
      val data = SignIn(account, password).asJson.noSpaces
      Http.postJsonAndParse[SignInRsp](Routes.UserRoutes.userLogin,data).map{
        case Right(rsp)=>
          if(rsp.errCode == 0){
            //登录之后获取到房间信息和用户信息
            if(rsp.userInfo.isDefined){
              dom.window.localStorage.removeItem("isTemUser")
              dom.window.localStorage.setItem("userName",account)
              dom.window.localStorage.setItem("userHeaderImgUrl",rsp.userInfo.get.headImgUrl)
              dom.window.localStorage.setItem("userId",rsp.userInfo.get.userId.toString)
              dom.window.localStorage.setItem("token",rsp.userInfo.get.token)
              isLogin := true
              showPop := emptyHTML
            }else{
              PopWindow.commonPop("don't get userInfo")
            }
          }else{
            PopWindow.commonPop("error code:  " + rsp.errCode + rsp.msg)
          }
        case Left(error)=>
          PopWindow.commonPop("error left:  " + error)
      }
    }
  }
  def loginPop(): Unit ={
    val loginButton = Var(<div class="pop-button" onclick={()=>login()}>GO</div>)
    showPop := {
      <div class="pop-background" onclick={(e: Event)=> showPop := emptyHTML}>
        <div class="pop-main" onclick={(e: Event)=>e.stopPropagation()}>
          <div class="pop-title">用户登录</div>
          <div class="pop-content">
            <input class="pop-input" id="login-account" placeholder="用户名"></input>
            <input class="pop-input" id="login-password" placeholder="密码"></input>
          </div>
          <label class="pop-tip" for="pop-emailLogin" onclick={ (e: Event) => emailLoginPop()}>试试邮箱登录？</label>
          <div class="pop-confirm">
            {loginButton}
          </div>
        </div>
      </div>
    }
  }

  def emailLogin():Unit = {
    val account = dom.document.getElementById("login-email-account").asInstanceOf[Input].value
    val password = dom.document.getElementById("login-email-password").asInstanceOf[Input].value
    if(account == "" || password == ""){
      JsFunc.alert("请完整填写！")
    }else{
      val data = SignInByMail(account, password).asJson.noSpaces
      Http.postJsonAndParse[SignInRsp](Routes.UserRoutes.userLoginByMail,data).map{
        case Right(rsp)=>
          if(rsp.errCode == 0){
            //登录之后获取到房间信息和用户信息
            if(rsp.userInfo.isDefined){
              dom.window.localStorage.removeItem("isTemUser")
              dom.window.localStorage.setItem("userName", rsp.userInfo.get.userName)
              dom.window.localStorage.setItem("userHeaderImgUrl", rsp.userInfo.get.headImgUrl)
              dom.window.localStorage.setItem("token", rsp.userInfo.get.token)
              dom.window.localStorage.setItem("userId", rsp.userInfo.get.userId.toString)
              isLogin := true
              showPop := emptyHTML
            }else{
              PopWindow.commonPop("don't get userInfo")
            }
          }
          else{
            PopWindow.commonPop("error code:  " + rsp.errCode + rsp.msg)
          }
        case Left(error)=>
          PopWindow.commonPop("error left:  " + error)
      }
    }
  }
  def emailLoginPop(): Unit ={
    val emailLoginButton = Var(<div class="pop-button" onclick={()=>emailLogin()}>GO</div>)
    showPop := {
      <div class="pop-background" onclick={(e: Event)=> showPop := emptyHTML}>
        <div class="pop-main" onclick={(e: Event)=>e.stopPropagation()}>
          <div class="pop-title">邮箱登录</div>
          <div class="pop-content">
            <input class="pop-input" id="login-email-account" placeholder="邮箱账号"></input>
            <input class="pop-input" id="login-email-password" placeholder="密码"></input>
          </div>
          <label class="pop-tip" for="pop-login" onclick={ (e: Event)=> loginPop()}>返回用户登录？</label>
          <div class="pop-confirm">
            {emailLoginButton}
          </div>
        </div>
      </div>
    }
  }

  def register():Unit = {
    val email = dom.document.getElementById("register-email").asInstanceOf[Input].value
    val account = dom.document.getElementById("register-account").asInstanceOf[Input].value
    val password = dom.document.getElementById("register-password").asInstanceOf[Input].value
    val password2 = dom.document.getElementById("register-password2").asInstanceOf[Input].value
    if(!email.trim.equals("") && !account.trim.equals("") && !password.trim.equals("")&& !password2.trim.equals("")){
      if(password.equals(password2)){
        val redirectUrl = s"https://${dom.document.location.host}/theia/webClient"
        val data = SignUp(email, account, password, redirectUrl).asJson.noSpaces
        Http.postJsonAndParse[SignUpRsp](Routes.UserRoutes.userRegister,data).map {
          case Right(rsp) =>
            if (rsp.errCode == 0) {
              PopWindow.commonPop("注册成功")
              showPop := emptyHTML
              //注册之后还需要登录
            } else {
              PopWindow.commonPop(s"error happened: ${rsp.msg}")
            }
          case Left(error) =>
            PopWindow.commonPop(s"error: $error")
        }
      }
      else{
        PopWindow.commonPop("输入相同的密码！")
      }
    }else{
      PopWindow.commonPop("注册项均不能为空！")
    }
  }
  def registerPop(): Unit ={
    val registerButton = Var(<div class="pop-button" onclick={()=>register()}>GO</div>)
    showPop := {
      <div class="pop-background" onclick={(e: Event) => showPop := emptyHTML}>
        <div class="pop-main" onclick={(e: Event)=>e.stopPropagation()}>
          <div class="pop-header"></div>
          <div class="pop-title">用户注册</div>
          <div class="pop-content">
            <input class="pop-input" id="register-email" placeholder="邮箱"></input>
            <input class="pop-input" id="register-account" placeholder="注册用户名"></input>
            <input class="pop-input" id="register-password" placeholder="注册密码"></input>
            <input class="pop-input" id="register-password2" placeholder="确认密码"></input>
          </div>
          <div class="pop-confirm">
            {registerButton}
          </div>
        </div>
      </div>
    }
  }

  def app:xml.Node = {
    <div style="flex-grow: 2; overflow: scroll;">
      {personPage}
    </div>
  }
}
