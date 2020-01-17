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

package org.seekloud.theia.faceAnalysis.utils

import java.net.{InetAddress, NetworkInterface, ServerSocket, SocketException}
import java.util
import java.util.Enumeration

/**
  * Created by sky
  * Date on 2019/5/6
  * Time at 上午11:55
  * get ip from the machine which the app run on
  * edit from http://www.voidcn.com/article/p-rludpgmk-yb.html
  */
object NetUtil {
  def getNetId():(Int,String) = {
    val a = NetworkInterface.getNetworkInterfaces
    var netState = 0 // 是否找到外网IP
    var netIp = ""
    while (a.hasMoreElements && netState!=1) {
      val ni = a.nextElement
      val address = ni.getInetAddresses
      while (address.hasMoreElements&& netState!=1) {
        val ip = address.nextElement
        if (!ip.isSiteLocalAddress && !ip.isLoopbackAddress && ip.getHostAddress.indexOf(":") == -1) { // 外网IP
          netIp = ip.getHostAddress
          netState = 3
        } else if (ip.isSiteLocalAddress && !ip.isLoopbackAddress && ip.getHostAddress.indexOf(":") == -1) { // 内网IP
          netIp = ip.getHostAddress
          if(netIp.startsWith("10.")) netState=1 else netState = 2
        }
      }
    }
    (netState,netIp)
  }

  def getFreePort: Int = {
    val serverSocket =  new ServerSocket(0) //读取空闲的可用端口
    val port = serverSocket.getLocalPort
    serverSocket.close()
    port
  }
}
