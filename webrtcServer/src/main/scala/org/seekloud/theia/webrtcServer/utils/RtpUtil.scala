package org.seekloud.theia.webrtcServer.utils

/**
  * Created by sky
  * Date on 2019/7/19
  * Time at 13:10
  */
object RtpUtil {
  object PT_TYPE {
    val payloadType_33 = 33 //负载类型号33
    val payloadType_101 = 101
    val payloadType_102 = 102
    val payloadType_103 = 103
    val payloadType_111 = 111
    val payloadType_112 = 112
  }

  def toLong42Byte(numArr: Array[Byte]) = {
    numArr.reverse.zipWithIndex.map { rst =>
      (rst._1 << (8 * rst._2)).toLong
    }.sum
  }

  def toLong44Byte(numArr: Array[Byte]) = {
    val b0 = numArr(3) & 0xff
    val b1 = numArr(2) & 0xff
    val b2 = numArr(1) & 0xff
    val b3 = numArr(0) & 0xff
    ((b3 << 24) | (b2 << 16) | (b1 << 8) | b0).toLong & 0xFFFFFFFFL
  }

  def to16Bit(num: Long) = {
    val byte1 = (num >>> 8 & 0xFF).toByte
    val byte2 = (num & 0xFF).toByte
    List(byte1, byte2)
  }

  def to32Bit(num: Long) = {
    val byte1 = (num >>> 24 & 0xFF).toByte
    val byte2 = (num >>> 16 & 0xFF).toByte
    val byte3 = (num >>> 8 & 0xFF).toByte
    val byte4 = (num & 0xFF).toByte
    List(byte1, byte2, byte3, byte4)
  }


  def toHexFromByte(b: Byte): String = {
    val hexSymbols = Array("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f")
    val leftSymbol = ((b >>> 4) & 0x0f).toByte
    val rightSymbol = (b & 0x0f).toByte
    hexSymbols(leftSymbol) + hexSymbols(rightSymbol)
  }

  case class RtpHeader(
                        pt: Byte, // m + 7 bit
                        seq: Long, //2 byte
                        timestamp: Long, //4 byte
                        ssrc: Long, //4 byte
                        vpx: Byte = 0x80.toByte, //V P X CC
                      )

  def head2buffer(h: RtpHeader): List[Byte] = {
    List(h.vpx, h.pt) ::: to16Bit(h.seq) ::: to32Bit(h.timestamp) ::: to32Bit(h.ssrc)
  }

  def buffer2Head(h: Array[Byte]): RtpHeader = {
    RtpHeader(
      h.drop(1).head,
      toLong42Byte(h.slice(2, 4)),
      toLong44Byte(h.slice(4, 8)),
      toLong44Byte(h.takeRight(4)),
      h.head,
    )
  }
}
