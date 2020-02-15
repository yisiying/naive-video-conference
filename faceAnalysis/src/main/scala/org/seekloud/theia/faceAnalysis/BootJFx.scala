package org.seekloud.theia.faceAnalysis

import akka.actor.typed.{ActorRef, DispatcherSelector}
import akka.actor.{ActorSystem, Scheduler}
import akka.dispatch.MessageDispatcher
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.actor.typed.scaladsl.adapter._
import javafx.application.Platform
import javafx.scene.text.Font
import javafx.stage.Stage
import org.seekloud.theia.faceAnalysis.common.StageContext
import org.seekloud.theia.faceAnalysis.controller.HomeController
import org.seekloud.theia.faceAnalysis.controller.AnchorController
import org.seekloud.theia.faceAnalysis.core.{CaptureActor, RMActor, RtpPushActor}
import org.seekloud.theia.faceAnalysis.model.FaceAnalysis
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * User: Arrow
  * Date: 2019/7/16
  * Time: 11:28
  */
object BootJFx {

  import org.seekloud.theia.faceAnalysis.common.AppSettings._

  implicit val system: ActorSystem = ActorSystem("theia", config)
  implicit val executor: MessageDispatcher = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  val blockingDispatcher: DispatcherSelector = DispatcherSelector.fromConfig("akka.actor.my-blocking-dispatcher")

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val scheduler: Scheduler = system.scheduler
  implicit val timeout: Timeout = Timeout(20 seconds)

  val captureActor = system.spawn(CaptureActor.create(), "captureActor")
  val rmActor = system.spawn(RMActor.create(), "rmActor")
  val rtpPushActor = system.spawn(RtpPushActor.create(), "rtpPushActor")

  def addToPlatform(fun: => Unit): Unit = {
    Platform.runLater(() => fun)
  }

}

class BootJFx extends javafx.application.Application {
  //fixme 测试内存溢出
//  System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", "0")
//  System.setProperty("org.bytedeco.javacpp.maxbytes", "0")

  import BootJFx._

  private[this] val log = LoggerFactory.getLogger(this.getClass)
  FaceAnalysis

  override def start(primaryStage: Stage): Unit = {
    Font.loadFont(getClass.getResourceAsStream("/img/seguiemj.ttf"), 12)
    val context = new StageContext(primaryStage)


    val homeController = new HomeController(context)
    homeController.showScene()
    //    val anchorScene = new AnchorController(context)
    //    AnchorController.anchorControllerOpt = Some(anchorScene)
    //    anchorScene.showScene()

    addToPlatform {
      homeController.loginByTemp()
    }

    primaryStage.setOnCloseRequest(event => {
      log.info("OnCloseRequest...")
      System.exit(0)
    })
  }


}
