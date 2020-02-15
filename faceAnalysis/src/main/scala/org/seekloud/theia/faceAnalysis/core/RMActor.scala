package org.seekloud.theia.faceAnalysis.core

import java.net.InetSocketAddress

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, WebSocketRequest}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.typed.scaladsl.ActorSource
import akka.util.{ByteString, ByteStringBuilder}
import javafx.scene.canvas.GraphicsContext
import org.seekloud.theia.faceAnalysis.BootJFx
import org.seekloud.theia.protocol.ptcl.client2Manager.websocket.AuthProtocol
import org.slf4j.LoggerFactory
import org.seekloud.theia.faceAnalysis.BootJFx.{executor, materializer, system}
import org.seekloud.theia.faceAnalysis.common.{AppSettings, Routes}
import org.seekloud.theia.faceAnalysis.component.WarningDialog
import org.seekloud.theia.faceAnalysis.controller.{AnchorController, UserControllerImpl, ViewerController}
import org.seekloud.theia.faceAnalysis.utils.{NetUtil, RMClient}
import org.seekloud.theia.player.sdk.MediaPlayer
import org.seekloud.theia.protocol.ptcl.CommonInfo.{ClientType, RoomInfo, UserInfo}
import org.seekloud.theia.protocol.ptcl.client2Manager.websocket.AuthProtocol.{DecodeError, HostStopPushStream, StartLiveReq, TextMsg}
import org.seekloud.theia.rtpClient.PullStreamClient

import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
  * Created by sky
  * Date on 2019/8/21
  * Time at 下午3:02
  * connect with RoomManager
  */
object RMActor {
  private val log = LoggerFactory.getLogger(this.getClass)
  private final val InitTime = Some(5.minutes)

  case class WatchInfo(roomId: Long, gc: GraphicsContext)

  trait Command

  final case class GotoAnchor(userInfo: UserInfo, roomInfo: RoomInfo, anchorController: AnchorController) extends Command

  final case object ConnectRM extends Command

  final case object AnchorLiveReq extends Command

  final case object StopLive extends Command

  final case class GetRoomDetail(userInfo: UserInfo,roomInfo: RoomInfo,viewerController: ViewerController) extends Command

  final case class GoToWatch(userInfo: UserInfo,roomInfo: RoomInfo, viewerController: ViewerController) extends Command

  final case class EstablishWs4Viewer(roomInfo: RoomInfo) extends Command

  final case object PullerStopped extends Command

  final case object HeartBeat extends Command

  final case object PingTimeOut extends Command

  final case class SendComment(comment: AuthProtocol.Comment) extends Command

  final case object ViewerLeft extends Command

  final case object DeviceReady extends Command

  private case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  final case object ConnectTimerKey

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
    log.info(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  def create(): Behavior[Command] = Behaviors.setup[Command] { ctx =>
    log.info("start..")
    implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
    Behaviors.withTimers[Command] { implicit timer =>
      val mediaPlayer = new MediaPlayer()
      mediaPlayer.init(isDebug = true, needTimestamp = true)
      init("RMActor init|", mediaPlayer)
    }
  }

  //todo 整理本部分代码
  private def init(logPrefix: String,
    mediaPlayer: MediaPlayer)(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] = Behaviors.receive[Command] { (ctx, msg) =>
    msg match {
      case msg: GotoAnchor =>
        log.info(s"$logPrefix receive $msg")
        ctx.self ! ConnectRM
        idle("idle|", msg.userInfo, msg.roomInfo, Some(mediaPlayer),Some(msg.anchorController))

      case msg:GetRoomDetail =>
        log.info(s"$logPrefix receive a GetRoomDetail")
        RMClient.searchRoom(Some(msg.userInfo.userId), msg.roomInfo.roomId).onComplete{
          case Success(rst) =>
            rst match {
              case Right(room) =>
                if(room.errCode == 0){
                  ctx.self ! GoToWatch(msg.userInfo, room.roomInfo.get, msg.viewerController)
                } else {
                  BootJFx.addToPlatform {
                    //.get.removeLoading()
                    WarningDialog.initWarningDialog(s"processor还没准备好哦~~~")
                  }
                }
              case Left(e) =>
                log.error(s"search room rsp error: $e")
            }
          case Failure(exception) =>
            log.error(s"search room-${msg.roomInfo.roomId} future error: $exception")

        }
        Behaviors.same

      case msg: GoToWatch =>
        if(msg.roomInfo.rtmp.nonEmpty) {
          val info = WatchInfo(msg.roomInfo.roomId,msg.viewerController.gc)
          val pullChannel = new InetSocketAddress(AppSettings.rtpServerIp, AppSettings.rtpServerPullPort)
          val puller = getStreamPuller(ctx, msg.roomInfo.rtmp.get, mediaPlayer, Some(info), Some(msg.viewerController))
          val client = new PullStreamClient(AppSettings.host, NetUtil.getFreePort, pullChannel, puller, AppSettings.rtpServerDst)
          puller ! RtpPullActor.InitRtpClient(client)
          ctx.self ! EstablishWs4Viewer(msg.roomInfo)
          idle("idle|", msg.userInfo, msg.roomInfo, Some(mediaPlayer),None, Some(msg.viewerController))
        } else {
          BootJFx.addToPlatform{
            WarningDialog.initWarningDialog("processor没有给我们liveId哦~~")
          }
          Behaviors.same
        }

      case unKnown =>
        log.error(s"$logPrefix receive a unknown $unKnown")
        Behaviors.same
    }
  }

  private def idle(logPrefix: String,
                   userInfo: UserInfo,
                   roomInfo: RoomInfo,
                   mediaPlayer: Option[MediaPlayer] = None,
                   anchorController: Option[AnchorController] = None,
                   viewerController: Option[ViewerController] = None
                  )(
                    implicit stashBuffer: StashBuffer[Command],
                    timer: TimerScheduler[Command]
                  ): Behavior[Command] = Behaviors.receive[Command] { (ctx, msg) =>
    msg match {
      case ConnectRM =>
        log.info(s"$logPrefix receive ConnectRM")
        val url = Routes.linkRoomManager(userInfo.userId, userInfo.token, roomInfo.roomId)
        log.info(s"url---$url")
        val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(url))
        val source = getSource
        val sink = getSink(anchorController.get)
        val ((stream, response), closed) =
          source
            .viaMat(webSocketFlow)(Keep.both)
            .toMat(sink)(Keep.both)
            .run()

        val connected = response.flatMap { upgrade =>
          if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
            ctx.self ! SwitchBehavior("anchorBehavior", anchorBehavior("RMActor anchorWork|", userInfo, roomInfo ,anchorController.get, stream))
            Future.successful(s"$logPrefix connect success.")
          } else {
            throw new RuntimeException(s"$logPrefix connection failed: ${upgrade.response.status}")
          }
        } //链接建立时
        connected.onComplete { i => log.info(i.toString) }
        closed.onComplete { i =>
          log.info(s"${ctx.self.path} connect closed! try again 1 minutes later")
          //remind 此处存在失败重试
          ctx.self ! SwitchBehavior("idle", idle(logPrefix, userInfo, roomInfo, mediaPlayer, anchorController), InitTime)
          timer.startSingleTimer(ConnectTimerKey, msg, 1.minutes)
        } //链接断开时
        switchBehavior(ctx, "busy", busy(), InitTime)

      case msg:EstablishWs4Viewer =>
        val url = Routes.linkRoomManager(userInfo.userId, userInfo.token, roomInfo.roomId)
        val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(url))
        val source = getSource
        val sink = getSink(viewerController.get)
        val ((stream, response), closed) =
          source
            .viaMat(webSocketFlow)(Keep.both)
            .toMat(sink)(Keep.both)
            .run()
        val connected = response.flatMap { upgrade =>
          if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
            ctx.self ! SwitchBehavior("viewerBehavior", viewerBehavior("RMActor viewWork|", userInfo, roomInfo, mediaPlayer.get,viewerController.get, stream))
            Future.successful(s"$logPrefix  connect success.")
          } else {
            throw new RuntimeException(s"$logPrefix connection failed: ${upgrade.response.status}")
          }
        } //链接建立时
        connected.onComplete { i => log.info(i.toString)
          //ctx.self ! StartPull(msg.roomInfo.rtmp.get) //fixme startPull消息的顺序
        }
        closed.onComplete { i =>
          log.info(s"$logPrefix connect closed! try again 1 minutes later")
          //remind 此处存在失败重试
          ctx.self ! SwitchBehavior("idle", idle(logPrefix, userInfo, roomInfo, mediaPlayer, anchorController), InitTime)
          timer.startSingleTimer(ConnectTimerKey, msg, 1.minutes)
        } //链接断开时
        switchBehavior(ctx, "busy", busy(), InitTime)

      case PullerStopped =>
        log.info(s"rmActor got pullerStoped")
        BootJFx.addToPlatform{
          WarningDialog.initWarningDialog("已断开连接")
        }
        Behaviors.stopped

      case ChildDead(name,childRef) =>
        log.info(s"rmActor unwatch child-$childRef")
        ctx.unwatch(childRef)
        Behaviors.same

      case SwitchBehavior(name, behavior, durationOpt, timeOut) =>
        switchBehavior(ctx, name, behavior, durationOpt, timeOut)

      case unKnown =>
        log.error(s"$logPrefix receive a unknown $unKnown")
        Behaviors.same
    }
  }

  private def anchorBehavior(
                    logPrefix: String,
                    userInfo: UserInfo,
                    roomInfo: RoomInfo,
                    anchorController: AnchorController,
                    rmServer: ActorRef[AuthProtocol.WsMsgFront])(
                    implicit stashBuffer: StashBuffer[Command],
                    timer: TimerScheduler[Command]
                  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case HeartBeat =>
          //          sender.foreach(_ ! PingPackage)
          timer.cancel(PingTimeOut)
          timer.startSingleTimer(PingTimeOut, PingTimeOut, 30.seconds)
          //          timer.startSingleTimer(HeartBeat, HeartBeat, 10.second)
          Behaviors.same

        case PingTimeOut =>
          //ws断了
          //          timer.cancel(HeartBeat)
          log.error("连接断开！")
          BootJFx.addToPlatform {
            WarningDialog.initWarningDialog("连接断开！")
          }
          idle("idle|", userInfo, roomInfo, None, Some(anchorController))

        case msg: SendComment =>
          rmServer ! msg.comment
          Behaviors.same

        case AnchorLiveReq =>
          rmServer ! StartLiveReq(userInfo.userId, userInfo.token, ClientType.PC)
          Behaviors.same

        //todo 断开直播
        case StopLive =>
          rmServer ! HostStopPushStream(roomInfo.roomId)
          Behaviors.same

        case DeviceReady =>
          log.info(s"$logPrefix receive  DeviceReady")
          anchorController.deviceReadyAction()
          Behaviors.same

        case unKnown =>
          log.error(s"$logPrefix receive a unknown $unKnown")
          Behaviors.same
      }
    }

  private def viewerBehavior(
    logPrefix: String,
    userInfo: UserInfo,
    roomInfo: RoomInfo,
    mediaPlayer: MediaPlayer,
    viewerController: ViewerController,
    rmServer: ActorRef[AuthProtocol.WsMsgFront])(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] = {
    Behaviors.receive[Command]{ (ctx, msg) =>
      msg match {
        case HeartBeat =>
          timer.cancel(PingTimeOut)
          timer.startSingleTimer(PingTimeOut, PingTimeOut, 30.seconds)
          Behaviors.same

        case PingTimeOut =>
          //ws断了
          //          timer.cancel(HeartBeat)
          log.error("连接断开！")
          BootJFx.addToPlatform {
            WarningDialog.initWarningDialog("连接断开！")
          }
          idle("idle|", userInfo, roomInfo, Some(mediaPlayer),None, Some(viewerController))

        case msg: SendComment =>
          rmServer ! msg.comment
          Behaviors.same

        case ViewerLeft =>
          init("init|",  mediaPlayer)//fixme 退出后的操作

        case PullerStopped =>
          log.info(s"rmActor got puller Stopped")
          BootJFx.addToPlatform{
            WarningDialog.initWarningDialog("已断开连接")
          }
          init("init|",  mediaPlayer) //fixme 退出后的操作

        case unKnown =>
          log.error(s"$logPrefix receive a unknown $unKnown")
          Behaviors.same

      }
    }

  }



  private def busy()(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, behavior, durationOpt, timeOut) =>
          switchBehavior(ctx, name, behavior, durationOpt, timeOut)

        case TimeOut(m) =>
          log.info(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }


  import org.seekloud.byteobject.ByteObject._
  import org.seekloud.byteobject.MiddleBufferInJvm

  def getSink(control: UserControllerImpl) = {
//    import scala.language.implicitConversions

//    implicit def parseJsonString2WsMsgFront(s: String): AuthProtocol.WsMsgRm = {
//      import io.circe.generic.auto._
//      import io.circe.parser._
//      try {
//        val wsMsg = decode[AuthProtocol.WsMsgRm](s).right.get
//        wsMsg
//      } catch {
//        case e: Exception =>
//          println(s"parse front msg failed when json parse,s=${s}")
//          AuthProtocol.DecodeError
//      }
//    }


    Sink.foreach[Message] {
      case BinaryMessage.Strict(m) =>
        val buffer = new MiddleBufferInJvm(m.asByteBuffer)
        bytesDecode[AuthProtocol.WsMsgRm](buffer) match {
          case Right(req) =>
            control.wsMessageHandler(req)
          case Left(e) =>
            log.info(s"decode binaryMessage failed,error:${e.message}")
            control.wsMessageHandler(AuthProtocol.DecodeError)
        }

      //akka http 分片流
      case msg: BinaryMessage.Streamed =>
        log.info(s"${msg}")
        val f = msg.dataStream.runFold(new ByteStringBuilder().result()) {
          case (s, str) => s.++(str)
        }
        f.map { m =>
          val buffer = new MiddleBufferInJvm(m.asByteBuffer)
          bytesDecode[AuthProtocol.WsMsgRm](buffer) match {
            case Right(req) =>
              control.wsMessageHandler(req)
            case Left(e) =>
              log.info(s"decode binaryMessage failed,error:${e.message}")
              control.wsMessageHandler(AuthProtocol.DecodeError)
          }
        }

      case TextMessage.Strict(msg) =>
        control.wsMessageHandler(TextMsg(msg))

      case _ =>


    }
  }



  def getSource = ActorSource.actorRef[AuthProtocol.WsMsgFront](
    completionMatcher = {
      case AuthProtocol.CompleteMsgClient =>
    }, failureMatcher = {
      case AuthProtocol.FailMsgClient(ex) ⇒ ex
    },
    bufferSize = 128,
    overflowStrategy = OverflowStrategy.fail
  ).collect {
    case message: AuthProtocol.WsMsgClient =>
      val sendBuffer = new MiddleBufferInJvm(409600)
      BinaryMessage.Strict(ByteString(
        message.fillMiddleBuffer(sendBuffer).result()
      ))
  }

  def getStreamPuller(
    ctx: ActorContext[Command],
    liveId: String,
    mediaPlayer: MediaPlayer,
    watchInfo: Option[WatchInfo],
    viewerController: Option[ViewerController]
  ) = {
    val childName = s"StreamPuller liveId=$liveId"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(RtpPullActor.create(liveId, watchInfo, ctx.self, mediaPlayer, viewerController), "pullActor")
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[RtpPullActor.PullCommand]
  }

}
