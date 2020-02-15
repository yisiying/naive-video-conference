package org.seekloud.theia.faceAnalysis.core

import java.nio.ByteBuffer
import java.nio.channels.{Channels, Pipe}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.scaladsl.Behaviors
import org.seekloud.theia.faceAnalysis.controller.ViewerController
import org.seekloud.theia.faceAnalysis.core.RMActor.WatchInfo
import org.seekloud.theia.player.core.PlayerManager.MediaSettings
import org.seekloud.theia.player.sdk.MediaPlayer
import org.seekloud.theia.rtpClient.{Protocol, PullStreamClient}
import org.seekloud.theia.rtpClient.Protocol._
import org.slf4j.LoggerFactory

import concurrent.duration._


/**
  * User: gaohan
  * Date: 2019/10/15
  * Time: 16:52
  */
object RtpPullActor {

  private val log = LoggerFactory.getLogger(this.getClass)


  type PullCommand = Protocol.Command

  final case class InitRtpClient(pullClient: PullStreamClient) extends PullCommand

  final case object PullStartTimeOut extends PullCommand

  final case object PullStream extends PullCommand

  final case object PullTimeOut extends PullCommand

  case object StopPull extends PullCommand

  private case class TimeOut(msg: String) extends PullCommand

  private final case object BehaviorChangeKey


  private final case class SwitchBehavior(
    name: String,
    behavior: Behavior[PullCommand],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error")
  ) extends PullCommand

  private[this] def switchBehavior(ctx: ActorContext[PullCommand],
    behaviorName: String, behavior: Behavior[PullCommand], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
    (implicit stashBuffer: StashBuffer[PullCommand],
      timer: TimerScheduler[PullCommand]) = {
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  def create(
    liveId: String,
    watchInfo: Option[WatchInfo],
    rmActor: ActorRef[RMActor.Command],
    mediaPlayer: MediaPlayer,
    viewerController: Option[ViewerController]
  ): Behavior[PullCommand] = {
    Behaviors.setup[PullCommand] { ctx =>
      log.info(s"PullActor is starting...")
      implicit val stashBuffer: StashBuffer[PullCommand] = StashBuffer[PullCommand](Int.MaxValue)
      Behaviors.withTimers[PullCommand]{ implicit timer =>
        init(liveId, mediaPlayer, rmActor,watchInfo, None, viewerController)
      }

    }
  }



  def init(
    liveId: String,
    mediaPlayer: MediaPlayer,
    rmActor: ActorRef[RMActor.Command],
    watchInfo: Option[WatchInfo],
    client: Option[PullStreamClient]= None,
    viewerController: Option[ViewerController] = None,
  )(implicit stashBuffer: StashBuffer[PullCommand],
    timer: TimerScheduler[PullCommand]
  ): Behavior[PullCommand] = {
    Behaviors.receive[PullCommand]{ (ctx, msg) =>
      msg match {
        case msg: InitRtpClient =>
          log.info(s"PullerActor-$liveId init rtpClint")
          msg.pullClient.pullStreamStart()
          timer.startSingleTimer(PullStartTimeOut, PullStartTimeOut, 5.seconds)
          init(liveId, mediaPlayer, rmActor,watchInfo, Some(msg.pullClient), viewerController)

        case PullStreamReady =>
          log.info(s"PullerActor-$liveId ready for pull.")
          timer.cancel(PullStartTimeOut)
          ctx.self ! PullStream
          Behaviors.same

        case PullStartTimeOut =>
          client.foreach(_.getClientId())
          timer.startSingleTimer(PullStartTimeOut, PullStartTimeOut, 5.seconds)
          Behaviors.same

        case PullStream =>
          log.info(s"PullActor-$liveId pullStream")
          client.foreach(_.pullStreamData(List(liveId)))
          timer.startSingleTimer(PullTimeOut, PullTimeOut, 5.seconds)
          Behaviors.same

        case msg: PullStreamReqSuccess =>
          log.info(s"PullActor-$liveId pullStream -${msg.liveIds} success")
          timer.cancel(PullTimeOut)
          val mediaPipe = Pipe.open() //ts流
          val sink = mediaPipe.sink()
          val source = mediaPipe.source()
          sink.configureBlocking(false)
          val inputStream = Channels.newInputStream(source)
          if(watchInfo.nonEmpty) {
            //viewerController.foreach(_.)//todo

            val playId = s"roomId${watchInfo.get.roomId}"
            mediaPlayer.setTimeGetter(playId, client.get.getServerTimestamp)
            val videoPlayer = ctx.spawn(PlayerActor.create(playId, viewerController, None, None), s"playerActoe${playId}")
            mediaPlayer.start(playId, videoPlayer, Right(inputStream), Some(watchInfo.get.gc), None)
          }
          stashBuffer.unstashAll(ctx, work(liveId, rmActor, mediaPlayer, sink, client.get, viewerController))

        case PullStreamPacketLoss =>
          log.info(s"pullActor-$liveId PullStreamPacketLoss")
          timer.startSingleTimer(PullStream, PullStream, 30.seconds)
          Behaviors.same

        case msg: NoStream =>
          log.info(s"No stream ids: ${msg.liveIds}")
          if (msg.liveIds.contains(liveId)) {
            log.info(s"Stream-$liveId unavailable now, try later.")
            timer.startSingleTimer(PullStream, PullStream, 30.seconds)
          }
          Behaviors.same

        case PullTimeOut =>
          log.info(s"StreamPuller-$liveId pull timeout, try again.")
          ctx.self ! PullStream
          Behaviors.same

        case StopPull =>
          log.info(s"StreamPuller-$liveId stopped in init.")
          rmActor ! RMActor.PullerStopped
          Behaviors.stopped

        case x =>
          log.warn(s"unhandled msg in init: $x")
          stashBuffer.stash(x)
          Behaviors.same


      }
    }

  }

  def work(
    liveId: String,
    rmActor: ActorRef[RMActor.Command],
    mediaPlayer: MediaPlayer,
    mediaSink: Pipe.SinkChannel,
    client: PullStreamClient,
    viewerController: Option[ViewerController],
  )(
    implicit stashBuffer: StashBuffer[PullCommand],
    timer: TimerScheduler[PullCommand]
  ): Behavior[PullCommand] = {
    Behaviors.receive[PullCommand]{(ctx,msg) =>
      msg match {
        case msg: PullStreamData =>
          if(msg.data.nonEmpty){
            try{
              mediaSink.write(ByteBuffer.wrap(msg.data))
              ctx.self ! SwitchBehavior("work", work(liveId, rmActor, mediaPlayer,mediaSink, client, viewerController))
            } catch {
              case e: Exception =>
                log.warn(s"sink write pulled data error: $e. Stop Puller-$liveId")
                ctx.self ! StopPull
            }
          } else {
            log.debug(s"pullerActor -$liveId pull null")
            ctx.self ! SwitchBehavior("work", work(liveId, rmActor, mediaPlayer,mediaSink, client, viewerController))
          }
          busy(liveId, rmActor, client)

        case msg: PullStreamReqSuccess =>
          Behaviors.same

        case CloseSuccess =>
          log.info(s"pullActor -$liveId is stopping")
          rmActor ! RMActor.PullerStopped
          Behaviors.stopped

        case msg: StreamStop =>
          log.info(s"pillActor-${msg.liveId} thread has been closed.")
          rmActor ! RMActor.PullerStopped
          Behaviors.stopped

        case StopPull =>
          log.info(s"PullerActor-$liveId is stopping.")
          try client.close()
          catch {
            case  e: Exception =>
              log.info(s"PullerActor-$liveId close error: $e")
          }
          Behaviors.same

        case PullStream =>
          Behaviors.same
      }

      }

    }

  def busy(
    liveId: String,
    rmActor: ActorRef[RMActor.Command],
    client: PullStreamClient
  )(
    implicit stashBuffer: StashBuffer[PullCommand],
    timer: TimerScheduler[PullCommand]
  ): Behavior[PullCommand] = {
    Behaviors.receive[PullCommand]{ (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, b, durationOpt, timeOut) =>
          switchBehavior(ctx, name, b, durationOpt, timeOut)

        case TimeOut(msg) =>
          log.debug(s"${ctx.self.path} is time out when busy, msg=$msg")
          Behaviors.stopped

        case StopPull =>
          log.info(s"PullerActor-$liveId is stopping.")
          try client.close()
          catch {
            case  e: Exception =>
              log.info(s"PullerActor-$liveId close error: $e")
          }
          Behaviors.same

        case CloseSuccess =>
          log.info(s"StreamPuller-$liveId stopped.")
          rmActor ! RMActor.PullerStopped
          Behaviors.stopped

        case x =>
          stashBuffer.stash(x)
          Behavior.same
      }

    }
  }


}
