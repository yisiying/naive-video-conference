package org.seekloud.theia.webrtcServer.ptcl

import akka.actor.typed.ActorRef
import org.seekloud.theia.webrtcMessage.ptcl.BrowserJson
import org.seekloud.theia.webrtcServer.core.LiveActor

/**
  * Created by sky
  * Date on 2019/6/17
  * Time at 下午1:16
  * 保存browser-socket连接信息
  */
case class WebSocketSession(
                             id:String,
                             actor:ActorRef[LiveActor.Command],
                             session:ActorRef[BrowserJson.WsMsg]
                           )
