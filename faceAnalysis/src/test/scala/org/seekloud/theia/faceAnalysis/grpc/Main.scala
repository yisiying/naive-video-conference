package org.seekloud.theia.faceAnalysis.grpc
import scala.concurrent.{ExecutionContext,Future}
import concurrent.ExecutionContext.Implicits.global
object Main {
  def main(args: Array[String]): Unit = {
    val a = new Array[Byte](1)
     GrpcAgent.predict(1,2,3,4,a).map{
       a=> println(a)
     }
    Thread.sleep(4000)
  }
}
