package org.seekloud.theia.webrtcServer.core.udp

import java.nio.ByteBuffer
import java.nio.channels.Pipe.SinkChannel

import org.seekloud.theia.shared.rtp.Protocol.Header
import org.seekloud.theia.shared.rtp.RtpClient
import org.seekloud.theia.webrtcServer.core.LiveActor
import org.seekloud.theia.webrtcServer.utils.RtpUtil.PT_TYPE
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * Created by sky
  * Date on 2019/7/18
  * Time at 13:37
  */
object ReceivePipeline {
//  private val log = LoggerFactory.getLogger(this.getClass)
//
//  import MediaPipeline.{channel4push, channel4pull}
//
//  private val receivePool: mutable.HashMap[Int, SinkChannel] = mutable.HashMap.empty
//  private val sinkPool: mutable.HashMap[String, SinkChannel] = mutable.HashMap.empty

//  /** rtp拉流
//    * */
//  def initRtpPull(list: List[(String, SinkChannel)]) = {
//    RtpClient.sendData(channel4pull, target4pull, Header(PT_TYPE.payloadType_111, 0, 0), list.map(_._1).mkString("#").getBytes("UTF-8"))
//    list.foreach(s => sinkPool.put(s._1, s._2))
//  }
//
//  new Thread(() => {
//    val buf = ByteBuffer.allocate(2 * 1024)
//    while (!Thread.interrupted()) {
//      buf.clear()
//      channel4push.receive(buf)
//      val rtpData = RtpClient.parseData(buf)
//      rtpData.header.payloadType match {
//        case PT_TYPE.payloadType_102 =>
//          log.info(s"push receive ${rtpData.header.ssrc} ${rtpData.header.seq}")
//          val liveId = new String(rtpData.body, "UTF-8")
//          log.info(s"push-102 $liveId")
//          SendPipeline.sourcePool.get(liveId) match {
//            case Some(s) =>
//              val sp = new Thread(new SendPipeline(s._1, liveId, rtpData.header.ssrc))
//              sp.start()
//              SendPipeline.sendPool.put(liveId, sp)
//            case None =>
//          }
//        case PT_TYPE.payloadType_103 =>
//          log.info(s"push receive ${rtpData.header.ssrc} ${rtpData.header.seq}")
//          val liveId = new String(rtpData.body, "UTF-8")
//          log.error(s"push-103 auth error...$liveId")
//          SendPipeline.sourcePool.get(liveId) match {
//            case Some(s) =>
//              s._2 ! LiveActor.Stop
//              SendPipeline.sourcePool.remove(liveId)
//            case None =>
//          }
//        case x =>
//      }
//    }
//  }).start()
//
//  new Thread(() => {
//    val buf = ByteBuffer.allocate(2 * 1024)
//    val rtpBuf = ByteBuffer.allocate(188 * 7)
//    while (!Thread.interrupted()) {
//      buf.clear()
//      channel4pull.receive(buf)
//      //      log.info(s"pull receiver got pkt size=${buf.limit()}")
//      val rtpData = RtpClient.parseData(buf)
//      rtpData.header.payloadType match {
//        case PT_TYPE.payloadType_33 =>
//          //          log.info(s"pull receive ${rtpData.header.ssrc} ${rtpData.header.seq}")
//          rtpBuf.put(rtpData.body)
//          rtpBuf.flip()
//          receivePool.get(rtpData.header.ssrc).foreach(_.write(rtpBuf))
//        case PT_TYPE.payloadType_112 =>
//          //          log.info(s"pull receive ${rtpData.header.ssrc} ${rtpData.header.seq}")
//          val l = new String(rtpData.body, "UTF-8")
//          l.split(";").foreach { s =>
//            val k = s.split("#")
//            try {
//              sinkPool.get(k(0)) match {
//                case Some(value) =>
//                  receivePool.put(k(1).toInt, value)
//                case None =>
//              }
//            } catch {
//              case exception: Exception =>
//                log.error("pull-112-" + exception.getMessage)
//            }
//          }
//        case x =>
//          log.error(s"pull error ${rtpData.header.ssrc} ${rtpData.header.seq}")
//      }
//      rtpBuf.clear()
//    }
//  }).start()

}