package org.seekloud.theia.webrtcServer.core

import java.nio.channels.{Channels, Pipe}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.scaladsl.AskPattern._
import akka.stream.OverflowStrategy
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.stream.scaladsl.Flow
import org.seekloud.theia.webrtcMessage.ptcl.BrowserJson
import org.seekloud.theia.webrtcMessage.ptcl.BrowserJson.{EventId, WsMsg}
import org.slf4j.LoggerFactory
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe._
import io.circe.parser._
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.kurento.client.KurentoClient
import org.seekloud.theia.protocol.ptcl.CommonInfo.LiveInfo
import org.seekloud.theia.webrtcServer.common.AppSettings
import org.seekloud.theia.webrtcServer.kurento.control.{Handler, LiveHandler}
import org.seekloud.theia.webrtcServer.ptcl.{MediaInfo, WebSocketSession}
import org.seekloud.theia.webrtcServer.utils.FileUtil

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import org.seekloud.theia.webrtcServer.core.udp.RtpClientActor
import org.seekloud.theia.webrtcServer.Boot.{rtpClientActor, executor, liveManager, scheduler, timeout}
import org.seekloud.theia.webrtcServer.core.RtpRecordActor.{AudioCommand, VideoCommand}

/**
  * Created by sky
  * Date on 2019/7/16
  * Time at 上午11:42
  */
object LiveActor {
  private val log = LoggerFactory.getLogger(this.getClass)

  private val handlerMap: mutable.HashMap[String, Handler] = mutable.HashMap[String, Handler]()

  private val kurento = KurentoClient.create(s"ws://${AppSettings.kmsIp}:${AppSettings.kmsPort}/kurento")

  trait Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  case class WebSocketMsg(msg: String) extends Command

  case object CompleteMsgFront extends Command

  case class FailMsgFront(ex: Throwable) extends Command

  case class UserFrontActor(actor: ActorRef[WsMsg]) extends Command

  case class UserLeft[U](actorRef: ActorRef[U]) extends Command

  case class InitEncode(video: Int, audio: Int) extends Command

  case class InitRecord() extends Command

  case class Handler4Pc(handler: Handler) extends Command

  case object Stop extends Command

  private final case object BehaviorChangeKey

  final case class SwitchBehavior(
                                   name: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  case class TimeOut(msg: String) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer: TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  private def sink(actor: ActorRef[Command]) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = CompleteMsgFront,
    onFailureMessage = FailMsgFront.apply
  )

  def flow(actor: ActorRef[LiveActor.Command]): Flow[WebSocketMsg, WsMsg, Any] = {
    val in = Flow[WebSocketMsg].to(sink(actor))
    val out =
      ActorSource.actorRef[WsMsg](
        completionMatcher = {
          case BrowserJson.Complete =>
        },
        failureMatcher = {
          case BrowserJson.Fail(e) => e
        },
        bufferSize = 128,
        overflowStrategy = OverflowStrategy.dropHead
      ).mapMaterializedValue(outActor => actor ! UserFrontActor(outActor))
    Flow.fromSinkAndSource(in, out)
  }

  def create(liveInfo: LiveInfo): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      log.debug(s"${ctx.self.path} is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        if (liveInfo.liveId.contains(AppSettings.webUserPref)) {
          init4web(liveInfo)
        } else {
          init4pc(liveInfo)
        }
      }
    }
  }

  private def init4pc(liveInfo: LiveInfo)(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg: Handler4Pc =>
          work4pc(liveInfo, msg.handler)

        case Stop =>
          log.info(s"${ctx.self.path} stop")
          Behaviors.stopped

        case unKnow =>
          stashBuffer.stash(unKnow)
          Behavior.same
      }
    }

  private def init4web(liveInfo: LiveInfo)(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case UserFrontActor(f) =>
          log.info(s"${liveInfo.liveId} Ws connect success")
          wait4web(liveInfo, f)

        case Stop =>
          log.info(s"${ctx.self.path} stop")
          Behaviors.stopped

        case TimeOut(m) =>
          log.info(s"${ctx.self.path} is time out when busy,msg=$m")
          Behaviors.stopped

        case unKnow =>
          stashBuffer.stash(unKnow)
          Behavior.same
      }
    }

  private def wait4web(liveInfo: LiveInfo, frontActor: ActorRef[BrowserJson.WsMsg])(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg: WebSocketMsg =>
          parse(msg.msg) match {
            case Right(jsonMessage) =>
              val jsonCursor = jsonMessage.hcursor
              jsonCursor.get[String]("id") match {
                case Right(id) =>
                  id match {
                    case EventId.Anchor_SDP_OFFER =>
                      //先收到Anchor_Sdp_Offer,web 为主播
                      val handler = LiveHandler(kurento)
                      handlerMap.put(liveInfo.liveId, handler)
                      handler.handleJsonCursor(WebSocketSession(liveInfo.liveId, ctx.self, frontActor), jsonCursor)
                      work4web(liveInfo, WebSocketSession(liveInfo.liveId, ctx.self, frontActor), handler)

                    case EventId.CONNECT =>
                      //先收到CONNECT,web 为连线者
                      jsonCursor.get[String]("anchor") match {
                        case Right(anchorLiveId) =>
                          if (anchorLiveId.contains(AppSettings.webUserPref)) {
                            //web为主播
                            val handlerOpt = handlerMap.get(anchorLiveId)
                            if (handlerOpt.nonEmpty) {
                              log.info("switch into work4webAudience")
                              work4web(liveInfo, WebSocketSession(liveInfo.liveId, ctx.self, frontActor), handlerOpt.get)
                            } else {
                              log.error("the anchor liveId in CONNECT msg does not exist in handler map")
                              Behaviors.same
                            }
                          } else {
                            //todo:  处理pc为主播的情况,为pc创建liveActor
                            val handler = LiveHandler(kurento)
                            liveManager ! LiveManager.PcInit(anchorLiveId, handler)
                            handlerMap.put(anchorLiveId, handler)
                            work4web(liveInfo, WebSocketSession(liveInfo.liveId, ctx.self, frontActor), handler)
                          }
                        case Left(err) =>
                          log.error(s"CONNECT msg has right msg id , but there is no anchor field:${err.getMessage()}")
                          Behaviors.same
                      }
                    case _ =>
                      stashBuffer.stash(msg)
                      Behaviors.same
                  }
                case Left(err) =>
                  log.error(s"wait4web state : get WebSocketMsg id decode error:${err.getMessage()}")
                  Behaviors.same
              }
            case Left(err) =>
              log.error(s"wait4web state : parse WebSocketMsg to json error:${err.getMessage()}")
              Behaviors.same
          }

        case CompleteMsgFront =>
          log.info(s"${liveInfo.liveId} ws disconnect")
          init4web(liveInfo)

        case Stop =>
          log.info(s"${ctx.self.path} stop")
          Behaviors.stopped

        case TimeOut(m) =>
          log.info(s"${ctx.self.path} is time out when busy,msg=$m")
          Behaviors.stopped

        case unKnow =>
          stashBuffer.stash(unKnow)
          Behavior.same
      }
    }

  private def work4web(liveInfo: LiveInfo, session: WebSocketSession, handler: Handler)(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg: WebSocketMsg =>
          parse(msg.msg) match {
            case Right(jsonMessage) =>
              val jsonCursor = jsonMessage.hcursor
              jsonCursor.get[String]("id") match {
                case Right(id) =>
                  handler.handleJsonCursor(session, jsonCursor)
                case Left(err) =>
                  log.error(s"work4webAnchor state : get WebSocketMsg id decode error:${err.getMessage()}")
              }
            case Left(err) =>
              log.error(s"work4webAnchor state : parse WebSocketMsg to json error:${err.getMessage()}")
          }
          Behaviors.same

        case CompleteMsgFront =>
          log.info(s"${liveInfo.liveId} ws disconnect")
          handler.handleDisconnect(liveInfo.liveId)
          init4web(liveInfo)

        case msg: InitEncode =>
          val pipe = Pipe.open() //ts流通道
          println("==========", liveInfo)
          rtpClientActor ! RtpClientActor.AuthInit(liveInfo, pipe.source(), ctx.self)
          val recordActor = getRecordActor(ctx)
          val grabActor = getGrabActor(ctx)
          val url = s"${liveInfo.liveId}.sdp"
          val sdp = AppSettings.generateRtpSdp(msg.audio, msg.video)
          FileUtil.saveFile(sdp, AppSettings.sdpPath + url)
          val rspFuture1: Future[(MediaInfo, FFmpegFrameGrabber)] = grabActor ? (RtpGrabActor.StartGrab(url, _))
          rspFuture1.map { r1 =>
            val rspFuture: Future[(ActorRef[VideoCommand], ActorRef[AudioCommand])] = recordActor ? (RtpRecordActor.InitRtc2Rtp(r1._1, Channels.newOutputStream(pipe.sink()), _))
            rspFuture.map { r =>
              grabActor ! RtpGrabActor.InitEncode(r1._2, r._1, r._2)
            }
          }
          Behaviors.same

        case Stop =>
          log.info(s"${ctx.self.path} stop")
          Behaviors.stopped

        case TimeOut(m) =>
          log.info(s"${ctx.self.path} is time out when busy,msg=$m")
          Behaviors.stopped

        case unKnow =>
          stashBuffer.stash(unKnow)
          Behavior.same
      }
    }

  private def work4pc(liveInfo: LiveInfo, handler: Handler)(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        //todo: grabActor通过liveId去rtpServer拉pc的rtp流 ,分成两路recorder record到rtpEp端口
        case Stop =>
          log.info(s"${ctx.self.path} stop")
          Behaviors.stopped

        case TimeOut(m) =>
          log.info(s"${ctx.self.path} is time out when busy,msg=$m")
          Behaviors.stopped

        case unKnow =>
          stashBuffer.stash(unKnow)
          Behavior.same
      }
    }

  private def getGrabActor(ctx: ActorContext[Command]): ActorRef[RtpGrabActor.Command] = {
    val childName = s"GrabActor"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(RtpGrabActor.create(), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[RtpGrabActor.Command]
  }

  private def getRecordActor(ctx: ActorContext[Command]): ActorRef[RtpRecordActor.Command] = {
    val childName = s"RecordActor"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(RtpRecordActor.create(), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[RtpRecordActor.Command]
  }
}
