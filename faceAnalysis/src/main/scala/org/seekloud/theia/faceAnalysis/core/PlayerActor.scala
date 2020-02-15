package org.seekloud.theia.faceAnalysis.core

import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.seekloud.theia.faceAnalysis.controller.ViewerController
import org.seekloud.theia.player.core.PlayerGrabber
import org.seekloud.theia.player.protocol.Messages
import org.seekloud.theia.player.protocol.Messages._
import org.slf4j.LoggerFactory

import scala.collection.immutable
import scala.concurrent.duration._
/**
  * User: gaohan
  * Date: 2019/10/17
  * Time: 10:23
  */
object PlayerActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  type PlayCommand = Messages.RTCommand

  //videoPlayer to self
  final case class StartTimers(grabActor: ActorRef[PlayerGrabber.MonitorCmd]) extends PlayCommand

  final case object TryAskPicture extends PlayCommand

  final case object TryAskSamples extends PlayCommand

  // keys
  private object IMAGE_TIMER_KEY

  private object SOUND_TIMER_KEY

  //init
  var frameRate: Double = _
  var hasAudio: Boolean = _
  var hasVideo: Boolean = _
  var needImage: Boolean = _
  var needSound: Boolean = _



  def create(
    id: String,
    viewerController: Option[ViewerController],
    imageQueue: Option[immutable.Queue[AddPicture]] = None,
    samplesQueue: Option[immutable.Queue[Array[Byte]]] = None
  ):Behavior[PlayCommand] = {
    Behaviors.setup[PlayCommand]{ ctx =>
      log.info(s"playerActor is starting")
      implicit val stashBuffer: StashBuffer[PlayCommand] = StashBuffer[PlayCommand](Int.MaxValue)
      Behaviors.withTimers[PlayCommand] { implicit timer =>
        idle(id, viewerController, imageQueue, samplesQueue)
      }
    }
  }

  def idle(
    id: String,
    viewerController: Option[ViewerController],
    imageQueue: Option[immutable.Queue[AddPicture]] = None,
    samplesQueue: Option[immutable.Queue[Array[Byte]]] = None
  ): Behavior[PlayCommand] = {
    Behaviors.receive[PlayCommand]{ (ctx, msg) =>
      msg match {
        case msg: GrabberInitialed =>
          log.info(s"playerActor =$id got GrabberInitialed")
          frameRate = msg.mediaInfo.frameRate
          hasAudio = msg.mediaInfo.hasAudio
          hasVideo = msg.mediaInfo.hasVideo
          needImage = msg.mediaSettings.needImage
          needSound = msg.mediaSettings.needSound
          val grabberActor = msg.playerGrabber
          if(msg.gc.isEmpty){
            ctx.self ! StartTimers(grabberActor)
          } else {
            log.info(s"Grabber-$id has initialed and player automatically")
          }
          Behaviors.same

        case StartTimers(grabActor) =>
          Behaviors.withTimers[PlayCommand]{ implicit  timer =>
            var pF = true
            var sF = true
            if(needImage && hasVideo){
              log.info(s"playerActor -$id start ImageTimer")
              timer.startPeriodicTimer(IMAGE_TIMER_KEY, TryAskPicture, (1000 / frameRate)millis)
              pF = false
            }
            if(needSound && hasAudio){
              log.info(s"PlayerActor-$id start SoundTimer.")
              timer.startPeriodicTimer(
                SOUND_TIMER_KEY,
                TryAskSamples,
                (1000 / frameRate) millis
              )
              sF = false
            }
            working(id, grabActor, imageQueue, samplesQueue, pF, sF, viewerController)
          }

          case msg:GrabberInitFailed =>
            log.warn(s"PlayerActor -$id got GrabberInitFailed:${msg.ex}")
            Behaviors.stopped

        case StopVideoPlayer =>
          log.info(s"VideoPlayer is stopped.")
          Behaviors.stopped

        case x =>
          log.warn(s"unknown msg in idle: $x")
          Behaviors.unhandled

      }

    }
  }

  def working(
    id: String,
    grabberActor: ActorRef[PlayerGrabber.MonitorCmd],
    imageQueue: Option[immutable.Queue[AddPicture]] = None,
    samplesQueue: Option[immutable.Queue[Array[Byte]]] = None,
    pictureFinish: Boolean = true,
    soundFinish: Boolean = true,
    viewerController: Option[ViewerController]
  )(
    implicit timer: TimerScheduler[PlayCommand]
  ): Behavior[PlayCommand] = {
    Behaviors.receive[PlayCommand]{ (ctx, msg) =>
      msg match {
        case TryAskPicture =>
          grabberActor ! PlayerGrabber.AskPicture(Right(ctx.self))
          log.info(s"playerActor - $id-AskPicture to GrabberActor")
          Behaviors.same

        case TryAskSamples =>
          grabberActor ! PlayerGrabber.AskSamples(Right(ctx.self))
          log.info(s"playerActor - $id-AskSamples to GrabberActor")
          Behaviors.same

        case msg: AddPicture =>
          log.debug(s"PlayerActor-$id got AddPicture.")
          imageQueue.map(_.enqueue(msg))
          Behaviors.same

        case AddSamples(samples, ts) =>
          log.debug(s"PlayerActor-$id got AddSample.")
          samplesQueue.map(_.enqueue(samples))
          Behaviors.same

        case msg: PictureFinish =>
          log.debug(s"PlayerActor-$id got PictureFinish.")
          //        msg.resetFunc.foreach(f => f())
          timer.cancel(IMAGE_TIMER_KEY)
         // viewerController.foreach(_.autoReset()) todo
          log.info(s"PlayerActor-$id cancel ImageTimer.")
          if (soundFinish) {
            Behaviors.stopped
          } else {
            working(id, grabberActor, imageQueue, samplesQueue, pictureFinish = true, soundFinish = soundFinish, viewerController)
          }


        case SoundFinish =>
          log.debug(s"PlayerActor-$id got SoundFinish.")
          timer.cancel(SOUND_TIMER_KEY)
          log.info(s"PlayerActor-$id cancel SoundTimer.")
          if (pictureFinish) {
            Behaviors.stopped
          } else {
            working(id, grabberActor, imageQueue, samplesQueue, pictureFinish, soundFinish = true, viewerController)
          }

        case PauseAsk =>
          log.info(s"PlayerActor-$id got PauseAsk.")
          timer.cancelAll()
          log.info(s"cancel all Timers in PlayerActor-$id.")
          Behaviors.same

        case ContinueAsk =>
          log.info(s"PlayerActor-$id got ContinueAsk.")
          if (needImage && hasVideo) {
            log.info(s"PlayerActor-$id start ImageTimer again.")
            timer.startPeriodicTimer(
              IMAGE_TIMER_KEY,
              TryAskPicture,
              (1000 / frameRate) millis
            )
          }
          if (needSound && hasAudio) {
            log.info(s"PlayerActor-$id start SoundTimer again.")
            timer.startPeriodicTimer(
              SOUND_TIMER_KEY,
              TryAskSamples,
              (1000 / frameRate) millis
            )
          }
          Behaviors.same

        case StopVideoPlayer =>
          log.info(s"PlayerActor-$id got StopSelf.")
          timer.cancelAll()
          log.info(s"PlayerActor-$id cancel all Timers.")
          Behaviors.stopped

        case x =>
          log.warn(s"unknown msg in working: $x")
          Behaviors.unhandled

      }

    }
  }

}
