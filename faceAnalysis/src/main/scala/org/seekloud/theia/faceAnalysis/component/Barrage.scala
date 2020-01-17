package org.seekloud.theia.faceAnalysis.component

import javafx.animation.AnimationTimer
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.text.Font
import org.seekloud.theia.faceAnalysis.common.Constants
import org.seekloud.theia.protocol.ptcl.client2Manager.websocket.AuthProtocol.RcvComment
import org.seekloud.theia.faceAnalysis.component.Emoji.emojiFont

import scala.util.Random

//TODO 有时间的话考虑HTML-canvas兼容，主要是将定时函数交付各平台(javaFx,html)，
object Barrage {

  trait Comment {
    val userId: Long
    val userName: String
    val receiveTime: Long
    val yPos:Double
  }
  case class TextComment(
                          override val userId: Long,
                          override val userName: String,
                          override val receiveTime: Long,
                          override val yPos: Double,
                          text:String,
                          color:Color,
                          effect:Int //特效
                        ) extends Comment
  case class ImageComment(
                           override val userId: Long,
                           override val userName: String,
                           override val receiveTime: Long,
                           override val yPos: Double,
                           xPos:Double,
                           ySpeed:Double,
                           xSpeed:Double,
                           font:Double,
                           url:String,
                           effect:Int //特效
                         )extends Comment

  var commentsList = List.empty[TextComment]
  var giftsList = List.empty[ImageComment]
  val barrageView = new Canvas(Constants.DefaultPlayer.width * 0.9, Constants.DefaultPlayer.height * 0.9)
  val barrageGc: GraphicsContext = barrageView.getGraphicsContext2D
  var animationTimerStart = false
//  val emojiFont="Segoe UI Emoji"

  private val animationTimer = new AnimationTimer() {
    override def handle(now: Long): Unit = {
      drawBarrages()
    }
  }

  def drawBarrages(): Unit = {
    barrageGc.clearRect(0, 0, barrageView.getWidth, barrageView.getHeight)
    commentsList = commentsList.filter(comment => System.currentTimeMillis() - comment.receiveTime < 20000)
    commentsList.foreach {
      case TextComment(userId, userName, receiveTime, y, text, color, effect) =>
        val speed = userId match {
          case -1 => 0.05
          case _  => 0.1
        }
        val font = effect match {
          case  1 => barrageView.getWidth * 0.04 + (System.currentTimeMillis() - receiveTime) * speed * 0.05
          case -1 => barrageView.getWidth * 0.1 - (System.currentTimeMillis() - receiveTime) * speed * 0.1
          case  0 => 25
        }
        val xPos = effect match  {
          case  1 => barrageView.getWidth * 0.4 - (System.currentTimeMillis() - receiveTime) * speed
          case -1 => barrageView.getWidth * 0.4 - (System.currentTimeMillis() - receiveTime) * speed
          case  0 => barrageView.getWidth - (System.currentTimeMillis() - receiveTime) * speed
        }
        val yPos = userId match {
          case -1 =>
            20
          case _ =>
            y
        }

        userId match{
          case -1 =>
            barrageGc.setFont(Font.font(emojiFont, 25))
            barrageGc.setFill(Color.RED)
            barrageGc.fillText(s"系统消息：$text", xPos, yPos)

          case _ =>
            barrageGc.setFont(Font.font(emojiFont, font))
            barrageGc.setFill(color)
            if(System.currentTimeMillis() - receiveTime < 8000){
              barrageGc.fillText(s"$userName：$text",xPos, yPos)
            }
        }
    }

    giftsList.foreach{
      case ImageComment(userId, userName, receiveTime, yPos, xPos, ySpeed, xSpeed,font, url,  effect) =>
        val x = xPos + xSpeed*(System.currentTimeMillis() - receiveTime)
        val y = yPos + ySpeed*(System.currentTimeMillis() - receiveTime)

        val size = effect match {
          case  1 => font + (System.currentTimeMillis() - receiveTime) * 0.03
          case -1 => 5 * font - (System.currentTimeMillis() - receiveTime) * 0.01
          case  0 => 50
        }
        barrageGc.drawImage(new Image(url),x, y,size,size)
    }

  }

  val random = new Random(System.nanoTime())

  def updateBarrage(comment: RcvComment): Unit = {
    if (!animationTimerStart) {
      animationTimer.start()
      animationTimerStart = true
    }

    val effect =
      if(comment.comment.contains("【放大特效】"))
        1
      else if (comment.comment.contains("【缩小特效】"))
        -1
      else
        0

    //文字弹幕
    commentsList = commentsList.filter(comment => System.currentTimeMillis() - comment.receiveTime < 30000)
    val yPos = random.nextInt(barrageView.getHeight.toInt - 50) + 25
    val color = Constants.barrageColors(Random.nextInt(Constants.barrageColors.size))

    val comments =
      if(comment.comment.contains("【放大特效】"))
        comment.comment.replaceAll("【放大特效】","")
      else if (comment.comment.contains("【缩小特效】"))
        comment.comment.replaceAll("【缩小特效】","")
      else
        comment.comment

    val oneComment = TextComment(comment.userId, comment.userName, System.currentTimeMillis(), yPos, comments, color, effect)
    commentsList ::= oneComment

    //图片弹幕
    giftsList = giftsList.filter(comment => System.currentTimeMillis() - comment.receiveTime < 20000)
    if(comment.comment.contains("【礼物】")){
      val ySpeed = -0.1
      if(comment.comment.contains("掌声") ){
        val yPos = random.nextInt(barrageView.getHeight.toInt - 50) + 25
        val oneGift = ImageComment(comment.userId,comment.userName,System.currentTimeMillis(),yPos,barrageView.getWidth,0,-0.1,20,"img/clap.png",effect)
        giftsList ::= oneGift
      }
      if(comment.comment.contains("蛋糕") ){
        val xPos = random.nextInt(barrageView.getWidth.toInt - 50) + 25
        val oneGift = ImageComment(comment.userId,comment.userName,System.currentTimeMillis(),barrageView.getHeight,xPos,ySpeed,-0.15,20,"img/cake.png",effect)
        giftsList ::= oneGift
      }
      if(comment.comment.contains("鲜花") ){
        val xPos = random.nextInt(barrageView.getWidth.toInt - 50) + 25
        val oneGift = ImageComment(comment.userId,comment.userName,System.currentTimeMillis(),barrageView.getHeight,xPos,ySpeed,0.15,20,"img/flower.png",effect)
        giftsList ::= oneGift
      }
      if(comment.comment.contains("汽车") ){
        val oneGift = ImageComment(comment.userId,comment.userName,System.currentTimeMillis(),barrageView.getHeight,barrageView.getWidth,ySpeed,-0.15,25,"img/car.png",effect)
        giftsList ::= oneGift
      }
      if(comment.comment.contains("轮船") ){
        val oneGift = ImageComment(comment.userId,comment.userName,System.currentTimeMillis(),barrageView.getHeight,barrageView.getWidth,ySpeed,-0.15,25,"img/boat.png",effect)
        giftsList ::= oneGift
      }
      if(comment.comment.contains("飞机") ){
        val oneGift = ImageComment(comment.userId,comment.userName,System.currentTimeMillis(),barrageView.getHeight,0,ySpeed,0.15,30,"img/plane.png",effect)
        giftsList ::= oneGift
      }
      if(comment.comment.contains("火箭") ){
        val oneGift = ImageComment(comment.userId,comment.userName,System.currentTimeMillis(),barrageView.getHeight,0,ySpeed,0.15,30,"img/rocket.png",effect)
        giftsList ::= oneGift
      }

    }
  }
}
