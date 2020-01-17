import java.io.OutputStream
import java.nio.channels.{Channel, Channels}
import java.util.concurrent.{ExecutorService, Executors}

import org.bytedeco.javacv.{FFmpegFrameGrabber, FFmpegFrameGrabber1}
import org.seekloud.theia.processor.stream.PipeStream


object TestPipe {
  class TestPipe(out:OutputStream) extends Runnable {
    override def run(): Unit = {
        while (true){
          Thread.sleep(2)
          out.write("hellocq".getBytes())
        }
    }
  }
  def main(args: Array[String]): Unit = {
    val threadPool:ExecutorService = Executors.newFixedThreadPool(60)

    val pipe = new PipeStream
    val source = pipe.getSource
    val sink = pipe.getSink
    val out = Channels.newOutputStream(sink)
    val in = Channels.newInputStream(source)

    val str = "hello"
    val bufw = str.getBytes()
    out.write(bufw)
    val bufr = new Array[Byte](188 * 5)
//    in.read(bufr)
    val outstr = new String(bufr)

//    try {
//      Thread.sleep(3000)
//      threadPool.execute(new TestPipe(out))
//    }finally {
//      threadPool.shutdown()
//    }
    val grabber = new FFmpegFrameGrabber1(in)
    grabber.setFormat("mp4")
    grabber.start()
    println("start over")
    grabber.grab()
    println(outstr)
  }
}
