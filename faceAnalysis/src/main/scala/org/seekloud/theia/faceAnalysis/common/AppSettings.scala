package org.seekloud.theia.faceAnalysis.common

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}
import org.seekloud.theia.faceAnalysis.utils.NetUtil
import org.slf4j.LoggerFactory

object AppSettings {

  private implicit class RichConfig(config: Config) {
    val noneValue = "none"

    def getOptionalString(path: String): Option[String] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getString(path))

    def getOptionalLong(path: String): Option[Long] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getLong(path))

    def getOptionalDurationSeconds(path: String): Option[Long] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getDuration(path, TimeUnit.SECONDS))
  }


  val log = LoggerFactory.getLogger(this.getClass)
  val config = ConfigFactory.parseResources("product.conf").withFallback(ConfigFactory.load())

  val appConfig = config.getConfig("app")

  val path = appConfig.getString("path")

  //remind 用于打包
  //  val path = System.getProperty("user.dir") + "/model/"

  val host = appConfig.getString("host")

  val autoNet = NetUtil.getNetId()

  val magicIp = if (autoNet._2.startsWith("10")) autoNet._2 else appConfig.getString("magic.ip")

  log.info(s"set localIp=$magicIp")

  val magicPushPort = appConfig.getInt("magic.pushPort")
  val magicPullPort = appConfig.getInt("magic.pullPort")

  val roomManagerHttp = appConfig.getString("roomManager.http")
  val roomManagerDomain = appConfig.getString("roomManager.domain")

  val rtpServerIp = appConfig.getString("rtp.ip")
  val rtpServerPushPort = appConfig.getInt("rtp.pushPort")
  val rtpServerPullPort = appConfig.getInt("rtp.pullPort")
  val rtpServerDst = appConfig.getString("rtp.rtpServerDst")
  val grpcHost = appConfig.getString("grpc.host")
  val grpcPort = appConfig.getInt("grpc.port")

  val alpha = appConfig.getDouble("trace.smooth_landmark")
  val iou_thres = appConfig.getDouble("trace.iou_thres")
  val thres = appConfig.getInt("trace.pixel_thres")

}
