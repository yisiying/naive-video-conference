package org.seekloud.theia.faceAnalysis.controller

import org.seekloud.theia.protocol.ptcl.client2Manager.websocket.AuthProtocol

/**
  * Created by sky
  * Date on 2019/10/21
  * Time at 下午5:31
  * 主播观众共有操作
  */
trait UserControllerImpl {
  def wsMessageHandler(msg: AuthProtocol.WsMsgRm): Unit
}
