package org.seekloud.theia.faceAnalysis.controller

import java.io._
import java.util.regex.Pattern

import javafx.scene.image.ImageView
import org.seekloud.theia.faceAnalysis.common.Constants
import org.seekloud.theia.faceAnalysis.BootJFx
import org.seekloud.theia.faceAnalysis.common.StageContext
import org.seekloud.theia.faceAnalysis.component.WarningDialog
import org.seekloud.theia.faceAnalysis.scene.HomeScene
import org.seekloud.theia.faceAnalysis.utils.RMClient
import org.seekloud.theia.protocol.ptcl.CommonInfo.{RoomInfo, UserInfo}

import scala.util.{Failure, Success}

/**
  * Created by sky
  * Date on 2019/8/19
  * Time at 下午2:34
  * object: public data
  * class: private data
  */
object HomeController {
  case class User(username: String, loginInfo: UserInfo)
  var usersInfo: Option[User] = None
//  var roomInfo:Option[RoomInfo]=None
  var roomInfo:Option[RoomInfo]=Some(RoomInfo(0l,"","",0l,"","","",0,0,None,None))
}
class HomeController(
                      context: StageContext
                    ) extends HomeScene {

  import HomeController._
  import org.seekloud.theia.faceAnalysis.BootJFx.executor

  var hasWaitingGif = false

  val waitingGif = new ImageView("img/waiting.gif")
  waitingGif.setFitHeight(50)
  waitingGif.setFitWidth(50)
  waitingGif.setLayoutX(Constants.AppWindow.width * 0.9 / 2 - 25)
  waitingGif.setLayoutY(Constants.AppWindow.height * 0.75 / 2 - 25)


  override def showScene(): Unit = {
    BootJFx.addToPlatform(
      context.switchScene(scene, title = "pc客户端-主页")
    )
  }

  override protected def login(name: String, pwd: String): Unit = {
    var loginInfo: Option[(String, String)] = None
    if (!(name equals "") && !(pwd equals "")) {
      loginInfo = Some(name, pwd)
      if (loginInfo.nonEmpty) {
        val signIn = {
          if(isEmail(name))
            RMClient.signInByMail(loginInfo.get._1, loginInfo.get._2) // 邮箱登录
          else
            RMClient.signIn(loginInfo.get._1, loginInfo.get._2) // 用户名登录
        }
        signIn.map{
            case Right(rsp) =>
              if (rsp.errCode == 0) {
                //记录当前用户信息
                usersInfo = Some(User(loginInfo.get._1, rsp.userInfo.get))
                roomInfo=Some(rsp.roomInfo.get)
                log.info(s"登录成功：${usersInfo.toString}")
                val chooseController = new ChooseController(context)
                chooseController.showScene()

                deleteLoginTemp()
                createLoginTemp(rsp.userInfo.get)
              }
              else {
                log.error(s"sign in error: ${rsp.msg}")
                BootJFx.addToPlatform {
                  removeLoading()
                  WarningDialog.initWarningDialog(s"${rsp.msg}")
                }
              }
            case Left(e) =>
              log.error(s"sign in server error: $e")
              BootJFx.addToPlatform {
                deleteLoginTemp()
                removeLoading()
                WarningDialog.initWarningDialog(s"服务器错误: $e")
              }
          }
      }
    }else{
      BootJFx.addToPlatform {
        deleteLoginTemp()
        removeLoading()
        WarningDialog.initWarningDialog("检查用户名/密码是否为空")
      }
    }
  }


    override protected def register(email:String,name: String, pwd: String, pwd2: String): Unit=
    {
      var signUpInfo: Option[(String,String, String)] = None
      if (pwd == pwd2) {
        if (!(name equals "") && !(pwd equals "")&& !(email equals "")) {
          if(isEmail(email)){
            signUpInfo = Some(email,name, pwd)
            RMClient.signUp(signUpInfo.get._1.toString, signUpInfo.get._2.toString,signUpInfo.get._3.toString).map {
              case Right(signUpRsp) =>
                if (signUpRsp.errCode == 0) {
                  log.info(s"注册成功：${signUpRsp.toString}")
                  BootJFx.addToPlatform {
                    BUTTON_WHICH = 1
                    reloadGroup()
                  }
                  //group.getChildren.clear()
                  // gotoLoginDialog(Some(signUpInfo.get._1), Some(signUpInfo.get._2))
                }
                else {
                  log.error(s"sign up error: ${signUpRsp.msg}")
                  BootJFx.addToPlatform {
                    WarningDialog.initWarningDialog(s"${signUpRsp.msg}")
                  }
                }
              case Left(error) =>
                log.error(s"sign up server error:$error")
                BootJFx.addToPlatform {
                  WarningDialog.initWarningDialog(s"服务器出错: $error")
                }
            }
          }else{
            BootJFx.addToPlatform {
              WarningDialog.initWarningDialog("邮箱不符合xxx@xxx.xxx格式")
            }
          }
        }else {
          BootJFx.addToPlatform {
            WarningDialog.initWarningDialog("检查用户名/密码/邮箱是否为空")
          }
        }
      }else {
        BootJFx.addToPlatform {
          WarningDialog.initWarningDialog("注册失败：两次输入密码不一致")
        }
      }
    }

  protected def isEmail(email:String):Boolean={
    if (null==email || "".equals(email)){
      false
    }else{
      val regEx1 = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$"
      val p=Pattern.compile(regEx1)
      val m = p.matcher(email)
      m.matches()
    }
  }

  def showLoading(): Unit = {
    BootJFx.addToPlatform {
      if (!hasWaitingGif) {
        group.getChildren.add(waitingGif)
        hasWaitingGif = true
      }
    }
  }

  def removeLoading(): Unit = {
    BootJFx.addToPlatform {
      if (hasWaitingGif) {
        group.getChildren.remove(waitingGif)
        hasWaitingGif = false
      }
    }
  }

  /**
    * 用临时文件内信息登录
    */
  def loginByTemp(): Unit = {
    showLoading()
    val dir = Constants.loginInfoCache
    val files = dir.list.toList
    val prefix = "theia".r
    val suffix = "userLogin".r
    var fileName = ""
    files.foreach { r =>
      if (prefix.findFirstIn(r).isDefined && suffix.findFirstIn(r).isDefined) fileName = r
    }

    if (fileName == "") {
      log.debug(s"no theia login temp")
      removeLoading()
    } else {
      var userInfo: Option[UserInfo] = None
      val file = new File(Constants.loginInfoCachePath, fileName)
      if (file.canRead && file.exists()) {
        val bufferedReader = new BufferedReader(new FileReader(file))
        userInfo = Some(UserInfo(
          bufferedReader.readLine().split(":").last.toLong,
          bufferedReader.readLine().split(":").last,
          bufferedReader.readLine().split(":").last,
          bufferedReader.readLine().split(":").last,
          bufferedReader.readLine().split(":").last.toLong,

        ))
        bufferedReader.close()
      }
      if (userInfo.nonEmpty) {
        RMClient.getRoomInfo(userInfo.get.userId, userInfo.get.token).onComplete {
          case Success(rst) =>
            rst match {
              case Right(rsp) =>
                if (rsp.errCode == 0) {
                  usersInfo = Some(User(userInfo.get.userName,userInfo.get))
                  roomInfo=rsp.roomInfoOpt
                  log.info(s"登录成功：${userInfo.toString}")
                  val chooseController = new ChooseController(context)
                  chooseController.showScene()
                }else{
                  log.error("token error")
                  AnchorController.deleteLoginTemp()
                  removeLoading()
                }
              case Left(e) =>
                log.error(s"loginByTmp get roomInfo error: $e")
                removeLoading()
            }
          case Failure(error) =>
            log.error(s"loginByTmp get roomInfo future failed: $error")
            removeLoading()
        }
      } else {
        removeLoading()
      }
    }
  }

  /**
    * 创建theia登录临时文件
    */
  def createLoginTemp(userInfo: UserInfo): Unit = {

    val file = Constants.loginInfoCache
    val temp = File.createTempFile("theia", "userLogin", file) //为临时文件名称添加前缀和后缀
    if (temp.exists() && temp.canWrite) {
      val bufferedWriter = new BufferedWriter(new FileWriter(temp))
      bufferedWriter.write(s"userId:${userInfo.userId}\n")
      bufferedWriter.write(s"userName:${userInfo.userName}\n")
      bufferedWriter.write(s"headImgUrl:${userInfo.headImgUrl}\n")
      bufferedWriter.write(s"token:${userInfo.token}\n")
      bufferedWriter.write(s"tokenExistTime:${userInfo.tokenExistTime}\n")
      bufferedWriter.close()
    }
    log.debug(s"create theia temp: $temp")
  }

  /**
    * 删除theia登录临时文件
    */
  def deleteLoginTemp(): Unit = {
    val dir = Constants.loginInfoCache
    dir.listFiles().foreach { file =>
      if (file.exists()) file.delete()
      log.debug(s"delete theia temps: ${file.getName}")
    }
  }
}
