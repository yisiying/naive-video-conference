package org.seekloud.theia.statistics.common

import org.seekloud.theia.statistics.pages.MainPage
import org.scalajs.dom

/**
  * User: Taoz
  * Date: 2/24/2017
  * Time: 10:59 AM
  */
object Routes {

  private val base = "/theia/roomManager"

  object UserRoutes{

    private val urlbase = base + "/user"
    private val urlRecord = base + "/record"

    val userRegister = urlbase + "/signUp"

    val userLogin = urlbase + "/signIn"

    val userLoginByMail = urlbase + "/signInByMail"

    val getRoomList = urlbase + "/getRoomList"

    val searchRoom = urlbase + "/searchRoom"

    val temporaryUser = urlbase + "/temporaryUser"

    def getRecordList(sortBy:String,pageNum:Int,pageSize:Int) = urlRecord + s"/getRecordList?sortBy=$sortBy&pageNum=$pageNum&pageSize=$pageSize"

    val getOneRecord = urlRecord + "/searchRecord"
    val watchRecordOver = urlRecord + "/watchRecordOver"
    val getCommentInfo = "/theia/roomManager/recordComment/getRecordCommentList"
    val sendCommentInfo = "/theia/roomManager/recordComment/addRecordComment"

    def uploadImg(imgType:Int, userId:String) = base+s"/file/uploadFile?imgType=$imgType&userId=$userId"

    def nickNameChange(userId:Long,userName:String) = urlbase + s"/nickNameChange?userId=$userId&newName=$userName"
  }

  object AdminRoutes{

    private val urlAdmin = base + "/admin"
    private val urlStat = base + "/statistic"

    val adminSignIn = urlAdmin + "/adminSignIn"

    val admingetUserList = urlAdmin + "/getUserList"

    val adminsealAccount = urlAdmin + "/sealAccount"

    val admincancelSealAccount = urlAdmin + "/cancelSealAccount"

    val adminDeleteRecord = urlAdmin + "/deleteRecord"

    val adminbanOnAnchor = urlAdmin + "/banOnAnchor"

    val loginDataByHour = urlStat + "/loginDataByHour"

    val loginDataByDay = urlStat + "/loginDataByDay"

    val getLoginData = urlStat + "/getLoginData"

    val watchObserve = urlStat + "/watchObserve"

    val watchObserveByHour = urlStat + "/watchObserveByHour"

    val watchObserveByDay = urlStat + "/watchObserveByDay"

    val getRecordDataByAdmin = urlStat + "/getRecordDataByAdmin"
  }

  val getToken = base + "/rtmp/getToken"


//  def handleAuthError(errorCode:Int, msg:String)(func:() => Unit) ={
//    if(errorCode == 1000202){
//      MainPage.gotoPage("/login")
//    } else {
//      func()
//    }
//  }


}
