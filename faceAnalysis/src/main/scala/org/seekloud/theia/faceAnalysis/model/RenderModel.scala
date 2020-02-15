package org.seekloud.theia.faceAnalysis.model

import com.jme3.asset.AssetManager
import com.jme3.scene.Node

/**
  * Created by sky
  * Date on 2019/9/26
  * Time at 下午7:16
  * 模型控制抽象类
  */
object RenderModel{
  def apply(id: Byte): RenderModel = {
    val model= id match {
      case 1=> RenderModelBoy
      case 2=> RenderModelGirl
      case 3=> RenderModelJaime
      case _=> RenderModelBoy
    }
    RenderEngine.gamePause()
    RenderEngine.enqueueToEngine(model.setModel())
    RenderEngine.gameGoOn()
    model
  }
}
trait RenderModel {
  val id:Byte

  protected var model:Node

  def setModel():Unit
  
  def headMove(xAngle: Float, yAngle: Float, zAngle: Float): Unit

  def lipSideChange(changeSize: Float): Unit

  def mouthChange(changeSize: Float): Unit

  def leftEyeLidChange(changeSize: Float): Unit

  def rightEyeLidChange(changeSize: Float): Unit
}
