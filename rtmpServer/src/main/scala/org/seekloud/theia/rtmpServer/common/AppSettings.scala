/*
 * Copyright 2018 seekloud (https://github.com/seekloud)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seekloud.theia.rtmpServer.common

import java.util.concurrent.TimeUnit

import org.seekloud.theia.rtmpServer.utils.SessionSupport.SessionConfig
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import collection.JavaConverters._

/**
  * User: Taoz
  * Date: 9/4/2015
  * Time: 4:29 PM
  */
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
  val projectVersion = appConfig.getString("projectVersion")
  val srsHost = appConfig.getString("srsHost")
  val httpInterface = appConfig.getString("http.interface")
  val httpPort = appConfig.getInt("http.port")
  val serverHost = appConfig.getString("server.host")
  val isRecord = appConfig.getBoolean("isRecord")

  val dependenceConfig = config.getConfig("dependence")
  private val rmConfig = dependenceConfig.getConfig("roomManager.config")
  val rmProtocol = rmConfig.getString("protocol")
  val rmWsProtocol = rmConfig.getString("wsProtocol")
  val rmDomain = rmConfig.getString("domain")
  val rmHostName = rmConfig.getString("hostName")
  val rmPort = rmConfig.getInt("port")
  val rmUrl = rmConfig.getString("url")


  val slickConfig = config.getConfig("slick.db")
  val slickUrl = slickConfig.getString("url")
  val slickUser = slickConfig.getString("user")
  val slickPassword = slickConfig.getString("password")
  val slickMaximumPoolSize = slickConfig.getInt("maximumPoolSize")
  val slickConnectTimeout = slickConfig.getInt("connectTimeout")
  val slickIdleTimeout = slickConfig.getInt("idleTimeout")
  val slickMaxLifetime = slickConfig.getInt("maxLifetime")
  
  val sessionConfig = {
    val sConf = config.getConfig("session")
    SessionConfig(
      cookieName = sConf.getString("cookie.name"),
      serverSecret = sConf.getString("serverSecret"),
      domain = sConf.getOptionalString("cookie.domain"),
      path = sConf.getOptionalString("cookie.path"),
      secure = sConf.getBoolean("cookie.secure"),
      httpOnly = sConf.getBoolean("cookie.httpOnly"),
      maxAge = sConf.getOptionalDurationSeconds("cookie.maxAge"),
      sessionEncryptData = sConf.getBoolean("encryptData")
    )
  }


  val magicIp = appConfig.getString("magic.ip")
  val magicPushPort = appConfig.getInt("magic.pushPort")
  val magicPullPort = appConfig.getInt("magic.pullPort")

  val rtpServerIp = appConfig.getString("rtp.ip")
  val rtpServerPushPort = appConfig.getInt("rtp.pushPort")
  val rtpServerPullPort = appConfig.getInt("rtp.pullPort")

}
