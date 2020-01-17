package org.seekloud.theia.phoneClient.common

import org.scalajs.dom

/**
  * create by zhaoyin
  * 2019/9/2  2:44 PM
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

    val temporaryUser = urlbase + "/temporaryUser"

    val searchRoom = urlbase + "/searchRoom"
    val getCommentInfo = "/theia/roomManager/recordComment/getRecordCommentList"
    val sendCommentInfo = "/theia/roomManager/recordComment/addRecordComment"

    def uploadImg(imgType:Int, userId:String) = base+s"/file/uploadFile?imgType=$imgType&userId=$userId"

    def nickNameChange(userId:Long,userName:String) = urlbase + s"/nickNameChange?userId=$userId&newName=$userName"

    def getRecordList(sortBy:String,pageNum:Int,pageSize:Int) = urlRecord + s"/getRecordList?sortBy=$sortBy&pageNum=$pageNum&pageSize=$pageSize"

    val getOneRecord = urlRecord + "/searchRecord"
    val watchRecordOver = urlRecord + "/watchRecordOver"
  }

  def getWsSocketUri(liveId:String,liveCode:String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://10.1.29.246:41650/webrtcServer/userJoin?liveId=$liveId&liveCode=$liveCode"
  }

  def rmWebScocketUri(userId:Long, token:String,roomId:Long) = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/theia/roomManager/user/setupWebSocket?userId=$userId&token=$token&roomId=$roomId"
  }

}
