package org.seekloud.theia.webrtcServer

import java.io.{FileInputStream, InputStream}
import java.security.{KeyStore, SecureRandom}

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, DispatcherSelector}
import akka.dispatch.MessageDispatcher
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout

import scala.util.{Failure, Success}
import scala.language.postfixOps
import akka.actor.typed.scaladsl.adapter._
import org.kurento.client.KurentoClient
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import org.seekloud.theia.webrtcServer.common.AppSettings
import org.seekloud.theia.webrtcServer.core.udp.RtpClientActor
import org.seekloud.theia.webrtcServer.core.{LiveManager, RtpRecordActor}
import org.seekloud.theia.webrtcServer.http.HttpService

/**
  * Autheia: Tao Zhang
  * Date: 4/29/2019
  * Time: 11:28 PM
  */
object Boot extends HttpService {

  import concurrent.duration._

  implicit val system: ActorSystem = ActorSystem("neeko", AppSettings.config)

  override implicit val materializer: Materializer = ActorMaterializer()

  override implicit val scheduler = system.scheduler

  override implicit val timeout: Timeout = Timeout(10 seconds)

  private val log: LoggingAdapter = Logging(system, getClass)

  implicit val executor: MessageDispatcher = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")

  val liveManager = system.spawn(LiveManager.create(), "liveManager")

  val rtpClientActor = system.spawn(RtpClientActor.create(),"rtpClientActor")

  def main(args: Array[String]): Unit = {
    log.info("Starting.")
//    val password: Array[Char] = AppSettings.tlsInfo._1.toCharArray // do not store passwords in code, read them from somewhere safe!
//
//    val ks: KeyStore = KeyStore.getInstance("PKCS12")
//    val keystore = new FileInputStream(AppSettings.tlsInfo._2)
//    require(keystore != null, "Keystore required!")
//    ks.load(keystore, password)
//    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
//    keyManagerFactory.init(ks, password)
//
//    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
//    tmf.init(ks)
//    val sslContext: SSLContext = SSLContext.getInstance("TLS")
//    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom())
//    val https: HttpsConnectionContext = ConnectionContext.https(sslContext)

//    val httpsBinding = Http().bindAndHandle(httpsRoutes, AppSettings.httpInterface, AppSettings.httpPort, connectionContext = https)
    //remind 此处实现http请求
    val httpBinding = Http().bindAndHandle(httpRoutes, AppSettings.httpInterface, AppSettings.httpPort)

//    httpsBinding.onComplete {
//      case Success(b) ⇒
//        val localAddress = b.localAddress
//        println(s"Server is listening on https://${localAddress.getHostName}:${localAddress.getPort}")
//      case Failure(e) ⇒
//        println(s"httpsBinding failed with ${e.getMessage}")
//        system.terminate()
//        System.exit(-1)
//    }

    httpBinding.onComplete {
      case Success(b) ⇒
        val localAddress = b.localAddress
        println(s"Server is listening on http://${localAddress.getHostName}:${localAddress.getPort}")
      case Failure(e) ⇒
        println(s"httpBinding failed with ${e.getMessage}")
        system.terminate()
        System.exit(-1)
    }
  }
}
