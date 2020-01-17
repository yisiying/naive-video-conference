package org.seekloud.theia.faceAnalysis.grpc

import io.grpc.{Server, ServerBuilder}
import io.grpc.netty.NettyServerBuilder
import org.seekloud.theia.faceAnalysis.pb.api._
import org.seekloud.theia.faceAnalysis.pb.service.FaceServerGrpc.FaceServer
import org.seekloud.theia.faceAnalysis.pb.service.FaceServerGrpc
import org.slf4j.LoggerFactory
import scala.concurrent.{ExecutionContext,Future}

object GrpcServerTest {
  private[this] val log = LoggerFactory.getLogger(this.getClass)
  def build( port: Int,executionContext: ExecutionContext): Server = {
    log.info("FaceAnalysis gRPC Sever is building..")
    val service = new GrpcServerTest()
    ServerBuilder.forPort(port).addService(
      FaceServerGrpc.bindService(service, executionContext)
    ).build
  }


  def main(args: Array[String]): Unit = {
    val excecutor = concurrent.ExecutionContext.Implicits.global
    val server = GrpcServerTest.build(40440, excecutor)
    server.start()
    sys.addShutdownHook {
      println("JVM SHUT DOWN.")
      server.shutdown()
      println("SHUT DOWN.")
    }

    server.awaitTermination()
    println("DONE.")
  }
}

class GrpcServerTest extends FaceServer{
  override def predict(request: ImageReq): Future[MarkRsp] = {
    println("================")
    Future.successful(MarkRsp(1,Seq(Mask(Seq(Point(1,2))))))
  }
}
