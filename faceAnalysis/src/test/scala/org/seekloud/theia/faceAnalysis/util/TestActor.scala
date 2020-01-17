package org.seekloud.theia.faceAnalysis.util

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import org.seekloud.theia.faceAnalysis.BootJFx
import akka.actor.typed.scaladsl.adapter._
import org.slf4j.LoggerFactory
import scala.language.postfixOps

/**
  * Created by sky
  * Date on 2019/10/9
  * Time at 上午10:44
  *
  * 可用来尝试https://doc.akka.io/docs/akka/current/typed/index.html
  */

object TestActor {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  case class Test(t: Long) extends Command
  case class Test2(t:Long) extends Command

  case object Change extends Command

  def create(): Behavior[Command] = Behaviors.setup[Command] { ctx =>
    log.info("create| start..")
//    if (System.currentTimeMillis() % 2 == 0) {
      idle("idle|")
//    } else {
//      work("work|")
//    }
  }

  private def idle(
                    logPrefix: String,
                  ): Behavior[Command] = Behaviors.receive[Command] { (ctx, msg) =>
    msg match {
      case m: Test =>
        log.info(s"$logPrefix :${m.t}")
        ctx.self ! Test(123)
        ctx.self ! Test2(456)
//        ctx.self ! Test(System.nanoTime())
        work("work|")
      case m:Test2 =>
        log.info(s"$logPrefix :${m.t}")
        Behaviors.same

      case Change =>
        log.info(s"$logPrefix change")
        work("work|")
    }
  }

  private def work(
                    logPrefix: String,
                  ): Behavior[Command] = Behaviors.receive[Command] { (ctx, msg) =>
    msg match {
      case m: Test =>
        log.info(s"$logPrefix ${m.t}")

//        ctx.self ! Test(System.nanoTime())
        idle("idle|")
      case m:Test2=>
        log.info(s"$logPrefix ${m.t}")
        Behaviors.same
      case Change =>
        log.info(s"$logPrefix change")
        idle("idle|")
    }
  }


  def main(args: Array[String]): Unit = {
    import BootJFx._
    val testActor = system.spawn(create(), "testActor")
    testActor ! Test(1)
//    testActor ! Test(2)
//    testActor ! Change
//    testActor ! Test(3)
//    testActor ! Test(4)
  }
}
