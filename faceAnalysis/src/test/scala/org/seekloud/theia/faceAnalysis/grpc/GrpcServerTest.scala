package org.seekloud.theia.faceAnalysis.grpc

import io.grpc.{Server, ServerBuilder}
import org.seekloud.theia.faceAnalysis.pb.api._
import org.seekloud.theia.faceAnalysis.pb.service.FaceServerGrpc
import org.seekloud.theia.faceAnalysis.pb.service.FaceServerGrpc.FaceServer
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

object GrpcServerTest {
  private[this] val log = LoggerFactory.getLogger(this.getClass)

  def build( port: Int,executionContext: ExecutionContext): Server = {
    log.info("FaceAnalysis gRPC Sever is building..")
    val service = new GrpcServerTest()
    ServerBuilder.forPort(port).addService(
      FaceServerGrpc.bindService(service, executionContext)
    ).build
  }
}

class GrpcServerTest extends FaceServer{
  override def predict(request: ImageReq): Future[MarkRsp] = {
    println("================")
    Future.successful(MarkRsp(1,Seq(Mask(Seq(Point(1,2))))))
  }
}

