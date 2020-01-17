package org.seekloud.theia.faceAnalysis.grpc

import com.google.protobuf.ByteString
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import org.seekloud.theia.faceAnalysis.common.AppSettings._
import org.seekloud.theia.faceAnalysis.pb.api._
import org.seekloud.theia.faceAnalysis.pb.service.FaceServerGrpc
object GrpcAgent {

  private[this] val channel: ManagedChannel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort).usePlaintext().build
  private val faceStub:FaceServerGrpc.FaceServerStub  = FaceServerGrpc.stub(channel)
  def predict(f:Long, width:Int, height:Int, len:Int, data:Array[Byte]) = {
    println("agent predict is in =====")
    faceStub.predict(ImageReq(f, Some(ImgData(width,height,len,ByteString.copyFrom(data)))))
  }
}
