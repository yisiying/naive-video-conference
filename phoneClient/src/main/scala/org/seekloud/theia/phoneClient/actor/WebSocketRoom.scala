package org.seekloud.theia.phoneClient.actor

import io.circe.generic.auto._
import io.circe.parser._
import org.scalajs.dom
import org.scalajs.dom.raw._
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJs
import org.seekloud.theia.phoneClient.util.Shortcut
import org.seekloud.theia.protocol.ptcl.client2Manager.websocket.AuthProtocol._

import scala.scalajs.js.typedarray.ArrayBuffer

/**
  * create by zhaoyin
  * 2019/7/18  11:26 AM
  *
  * 与roomManager建立websocket连接
  */
case class WebSocketRoom(
  messageHandler: WsMsgRm => Unit,
  startHandler: () => Unit
) {

  private var webSocketStreamOpt: Option[WebSocket] = None
  var wsFlag = false
  var pingTimer = 0
  def setup(wsUrl: String) = {
    if (wsFlag) {
      println(s"room_websocket已经启动")
    } else{
      val websocketStream = new WebSocket(wsUrl)
      webSocketStreamOpt = Some(websocketStream)

      websocketStream.onopen = {event: Event=>
        println(s"room_websocket已经建立")
        wsFlag = true
        pingTimer = Shortcut.schedule(() => sendMsg(PingPackage), 10000)
        startHandler()
      }

      websocketStream.onerror = {event: Event =>
        println("room_error", event.eventPhase)
//        wsFlag = false
//        webSocketStreamOpt = None
//        Shortcut.cancelSchedule(pingTimer)
      }

      websocketStream.onmessage ={event: MessageEvent =>
        event.data match {
          case blobMsg:Blob =>
            val fr = new FileReader()
            fr.readAsArrayBuffer(blobMsg)
            fr.onloadend = {_: Event =>
              val buf = fr.result.asInstanceOf[ArrayBuffer]
              val middleDataInJs = new MiddleBufferInJs(buf)
              val data = bytesDecode[WsMsgRm](middleDataInJs) match{
                case Right(msg) => msg
              }
              messageHandler(data)
            }
          case jsonStringMsg: String =>
            val data = decode[WsMsgRm](jsonStringMsg).right.get
            messageHandler(data)
          case unknow => println(s"room_recv unknow msg:${unknow}")
        }
      }

      websocketStream.onclose ={event: CloseEvent =>
        println("room_close", event.eventPhase)
//        println(webSocketStreamOpt.get.readyState)
//        wsFlag = false
//        webSocketStreamOpt = None
//        Shortcut.cancelSchedule(pingTimer)
      }
    }
  }

  /**
    * created by byf for the test,could be deleted
    * */
  @deprecated
  def testWs() = {
    var testTimer = 0
    testTimer = Shortcut.schedule(() => webSocketStreamOpt.foreach(r => println("ready state:"+r.readyState)), 10000)
  }
//  testWs()

  def closeWs = {
    println("room disconnect")
    wsFlag = false
    webSocketStreamOpt.foreach(_.close())
//    dom.window.setInterval(()=>{println(webSocketStreamOpt.get.readyState)},5000)
//    println(webSocketStreamOpt.get.readyState)
    webSocketStreamOpt = None
    Shortcut.cancelSchedule(pingTimer)
  }
  private val sendBuffer: MiddleBufferInJs = new MiddleBufferInJs(8192)

  def sendMsg(msg: WsMsgClient) = {
    import org.seekloud.byteobject.ByteObject._
    if(dom.window.localStorage.getItem("isTemUser") == null){
      println("sendMsg",msg)
      webSocketStreamOpt.get.send(msg.fillMiddleBuffer(sendBuffer).result())
    }
  }

}
