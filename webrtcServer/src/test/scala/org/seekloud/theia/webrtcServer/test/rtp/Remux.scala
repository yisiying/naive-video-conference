package org.seekloud.theia.webrtcServer.test.rtp

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.{AVFormatContext, AVStream}
import org.bytedeco.ffmpeg.avutil.AVRational
import org.bytedeco.ffmpeg.global.{avcodec, avformat, avutil}
import org.bytedeco.javacv.{FFmpegFrameGrabber, FFmpegFrameRecorder}

/**
  * Created by sky
  * Date on 2019/7/21
  * Time at 17:50
  * copy from hellomedia
  */
class Remux(recorder: FFmpegFrameRecorder, format: String) {

  val ifmt_ctx = new AVFormatContext(null)

  /* allocate the output media context */
  if (avformat.avformat_alloc_output_context2(ifmt_ctx, null, format, null) < 0) {
    throw new Exception("avformat_alloc_context2() error:\tCould not allocate format context")
  }

  private var videoStreamIndex = -1
  private var audioStreamIndex = -1
  private var video_time_base: AVRational = _
  private var audio_time_base: AVRational = _

  def setInputVideoStream(
                           index: Int,
                           frameRate: Double,
                           videoCodec: Int
                         ): AVStream = {
    videoStreamIndex = index
    val video_codec = avcodec.avcodec_find_encoder(videoCodec)
    val frame_rate = avutil.av_d2q(frameRate, 1001000)
    val vStream = avformat.avformat_new_stream(ifmt_ctx, video_codec)
    vStream.index(index)
    video_time_base = avutil.av_inv_q(frame_rate)
    vStream.time_base(video_time_base)
    vStream.avg_frame_rate(frame_rate)
    vStream.codec().time_base(video_time_base) // "deprecated", but this is actually required
    vStream
  }

  def setInputAudioStream(
                           index: Int,
                           sampleRate: Int,
                           audioChannels: Int,
                           audioCodec: Int
                         ): AVStream = {
    audioStreamIndex = index
    val audio_codec = avcodec.avcodec_find_encoder(audioCodec)
    val aStream = avformat.avformat_new_stream(ifmt_ctx, audio_codec)
    aStream.index(index)

    val sample_rate = avutil.av_d2q(sampleRate, 1001000)
    audio_time_base = avutil.av_inv_q(sample_rate)

    aStream.time_base(audio_time_base)
    aStream.codec().time_base(audio_time_base); // "deprecated", but this is actually required
    aStream.codec().sample_rate(sampleRate)
    aStream.codec().channels(audioChannels)
    aStream
  }


  def start(): Unit = {
    recorder.start(ifmt_ctx)
  }

  private var curVideoPts = -1
  private var audioPacketCount = 0

  def recordVideoPacket(pkt: AVPacket, timestamp: Long): Boolean = {
    pkt.stream_index(videoStreamIndex)
    val pts = Math.round(timestamp * video_time_base.den() / (video_time_base.num() * 1000000))
    if (pts > curVideoPts) {
      pkt.pts(pts)
      pkt.dts(pts)
      curVideoPts = pts
      recorder.recordPacket(pkt)
    } else if (pts == curVideoPts) {
      pkt.pts(pts + 1)
      pkt.dts(pts + 1)
      curVideoPts = pts + 1
      recorder.recordPacket(pkt)
    } else {
      throw new RuntimeException(s"video pts[$pts] < curVideoPts[$curVideoPts]")
    }
  }

  def recordAudioPacket(pkt: AVPacket, nbSamples: Int): Boolean = {
    pkt.stream_index(audioStreamIndex)
    val pts = nbSamples * audioPacketCount
    pkt.pts(pts)
    pkt.dts(pts)
    audioPacketCount += 1
    recorder.recordPacket(pkt)
  }

  def recordPacket(pkt: AVPacket): Boolean = {
    recorder.recordPacket(pkt)
  }

  def close(): Unit = {
    recorder.close()
  }

}

object Remux {

  case class VPkt(p: AVPacket)

  case class APkt(p: AVPacket)

  class Grabber(s: Boolean, grabber: FFmpegFrameGrabber, recorder: ActorRef) extends Actor {
    override def receive: Receive = {
      case "next" =>
        if (s) recorder ! VPkt(grabber.grabPacket()) else recorder ! APkt(grabber.grabPacket())
        context.self ! "next"
    }
  }

  class Recorder(frameRate: Double, remux: Remux) extends Actor {
    var vCount = 0l

    override def receive: Receive = {
      case m: VPkt =>
        val ts = vCount * 1000000.toDouble / frameRate
        val l= m.p.stream_index()
        println(m.p.stream_index(), vCount)
        //        remux.recordVideoPacket(m.p, ts.toLong)
        remux.recordPacket(m.p)
        vCount += 1

      case m: APkt =>
        remux.recordAudioPacket(m.p, 1024)
        //        remux.recordPacket(m.p)
        println("audio", m.p.stream_index(), vCount)
    }
  }


  def main(args: Array[String]): Unit = {
    val outFile = "remux_av_" + System.currentTimeMillis() + ".ts"
    val output = "D:\\test\\out\\" + outFile


    val system = ActorSystem("test")
    //    val vGrabber = new FFmpegFrameGrabber("D:\\ideaProject\\theia\\webrtcServer\\src\\main\\data\\1000008_100008.sdp")
    //    vGrabber.setOption("protocol_whitelist", "file,rtp,udp")
    //    vGrabber.setOption("fflags", "nobuffer")
    //    vGrabber.startUnsafe()
    //
    //    val aGrabber = new FFmpegFrameGrabber("D:\\ideaProject\\theia\\webrtcServer\\src\\main\\data\\1000007_100007.sdp")
    //    aGrabber.setOption("protocol_whitelist", "file,rtp,udp")
    //    aGrabber.setOption("fflags", "nobuffer")
    //    aGrabber.startUnsafe()

    val grabber = new FFmpegFrameGrabber("D:\\ideaProject\\theia\\webrtcServer\\src\\main\\data\\WEB_1000007_100007.sdp")
    grabber.setOption("protocol_whitelist", "file,rtp,udp")
    grabber.setOption("fflags", "nobuffer")
    grabber.startUnsafe()

    val recorder = new FFmpegFrameRecorder(output, grabber.getImageWidth, grabber.getImageHeight, grabber.getAudioChannels)
    recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC)

    val remux = new Remux(recorder, "mpegts")
    remux.setInputVideoStream(0, grabber.getFrameRate, grabber.getVideoCodec)
    remux.setInputAudioStream(1, grabber.getSampleRate, grabber.getAudioChannels, grabber.getAudioCodec)

    remux.start()
    val rActor = system.actorOf(Props(new Recorder(grabber.getFrameRate, remux)), "rActor")

    //    val vActor = system.actorOf(Props(new Grabber(true, vGrabber, rActor)), "vActor")

    //    val aActor = system.actorOf(Props(new Grabber(false, aGrabber, rActor)), "aActor")

    val gActor = system.actorOf(Props(new Grabber(true, grabber, rActor)), "vActor")
    //    vActor ! "next"
    //    aActor ! "next"

    gActor ! "next"
  }


}