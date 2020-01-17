package org.seekloud.theia.rtmpServer.protocol

/**
  * User: yuwei
  * Date: 2019/5/27
  * Time: 9:29
  */
object AuthProtocol {
  trait CommonRsp {
    val errCode: Int
    val msg: String
  }


  final case class ErrorRsp(
                             errCode: Int,
                             msg: String
                           ) extends CommonRsp

  final case class SuccessRsp(
                               errCode: Int = 0,
                               msg: String = "ok"
                             ) extends CommonRsp

  var token = Set("null")
}
