package org.seekloud.theia.webrtcServer.ptcl

/**
  * Created by sky
  * Date on 2019/7/17
  * Time at 17:03
  */
case class MediaInfo(
                      imageWidth: Int,
                      imageHeight: Int,
                      pixelFormat: Int,
                      frameRate: Double,
                      videoCodec: Int,
                      videoBitrate: Int,
                      audioChannels: Int,
                      audioBitrate: Int,
                      sampleFormat: Int,
                      sampleRate: Int
                    )
