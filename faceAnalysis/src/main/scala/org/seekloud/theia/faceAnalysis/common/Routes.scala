package org.seekloud.theia.faceAnalysis.common

import java.awt.image.BufferedImage

import javax.imageio.ImageIO

object Routes {
  /*roomManager*/
  val baseUrl = AppSettings.roomManagerHttp + s"${AppSettings.roomManagerDomain}" + "/" + "theia/roomManager"
  //  val baseUrl = rmProtocol + "://" + rmHostName + ":" +  rmPort + "/" + rmUrl


  val userUrl = baseUrl + "/user"
  val signUp = userUrl + "/signUp"
  val signIn = userUrl + "/signIn"
  val signInByMail = userUrl + "/signInByMail"
  val getRoomInfo: String = userUrl + "/getRoomInfo"
  val getRoomList: String = userUrl + "/getRoomList"
  val searchRoom: String = userUrl + "/searchRoom"

  val wsBase = AppSettings.roomManagerHttp.replace("http", "ws") + s"${AppSettings.roomManagerDomain}" + "/" + "theia/roomManager" + "/user"

  def linkRoomManager(userId: Long, token: String, roomId: Long) = wsBase + "/setupWebSocket" + s"?userId=$userId&token=$token&roomId=$roomId"

  def getPic(picUrl:String):BufferedImage = {
    ImageIO.read(getClass.getResourceAsStream(picUrl))
  }
}
