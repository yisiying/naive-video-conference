package org.seekloud.theia.faceAnalysis.model

import com.jme3.animation.{Bone, Skeleton, SkeletonControl}
import com.jme3.math.{Quaternion, Vector3f}
import com.jme3.scene.Node

/**
  * Created by sky
  * Date on 2019/9/26
  * Time at 下午7:21
  */
object RenderModelGirl extends RenderModel {
  override val id: Byte = 2
  protected var model:Node = null
  private var ske: Skeleton = null

  private var head: Bone = null
  private var jaw: Bone = null
  private var lipSideR: Bone = null
  private var lipSideL: Bone = null
  private var eyeLidTopR: Bone = null
  private var eyeLidTopL: Bone = null
  private var eyeLidBottomR: Bone = null
  private var eyeLidBottomL: Bone = null

  private val LipSideChangeMax = 0.025f

  private val MouthChangeMax = 0.020f

  private val EyeTopZChangeMax = -0.030f

  private val EyeBottomZChangeMax = 0.030f

  private val EyeTopYChangeMax = 0f

  RenderEngine.enqueueToEngine({
    /**
      * 加载Jaime的模型
      */
    val scene = RenderEngine.getAssetManager.loadModel("Models/ruyue_head_2.glb").asInstanceOf[Node]
    model = scene.getChild("Armature").asInstanceOf[Node]
    // 将Jaime放大一点点，这样我们能观察得更清楚。
    model.scale(6f)
    model.setLocalTranslation(0,-2.5f,0)

    // 获得SkeletonControl
    // 骨骼控制器
    val sc = model.getControl(classOf[SkeletonControl])

    ske = sc.getSkeleton

    head = ske.getBone("head.x")
    head.setUserControl(true)
    jaw = ske.getBone("c_jawbone.x")
    jaw.setUserControl(true)
    lipSideL = ske.getBone("c_lips_smile.l")
    lipSideL.setUserControl(true)
    lipSideR = ske.getBone("c_lips_smile.r")
    lipSideR.setUserControl(true)
    eyeLidTopL = ske.getBone("c_eyelid_top.l")
    eyeLidTopL.setUserControl(true)
    eyeLidBottomL = ske.getBone("c_eyelid_bot.l")
    eyeLidBottomL.setUserControl(true)
    eyeLidTopR = ske.getBone("c_eyelid_top.r")
    eyeLidTopR.setUserControl(true)
    eyeLidBottomR = ske.getBone("c_eyelid_bot.r")
    eyeLidBottomR.setUserControl(true)

  })

  override def setModel(): Unit = {
    //remind 清除场景
    RenderEngine.getRootNode.detachAllChildren()
    RenderEngine.getRootNode.attachChild(model)
  }

  override def lipSideChange(changeSize: Float): Unit = {
    val size = changeSize * LipSideChangeMax
    lipSideR.setUserTransforms(new Vector3f(size, size, 0), Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    lipSideL.setUserTransforms(new Vector3f(-size, size, 0), Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    ske.updateWorldVectors()
  }

  override def mouthChange(changeSize: Float): Unit = {
    val vec = new Vector3f(0, changeSize * MouthChangeMax, 0)
    jaw.setUserTransforms(vec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    ske.updateWorldVectors()
  }

  override def headMove(xAngle: Float, yAngle: Float, zAngle: Float): Unit = {
    val move = new Quaternion
    move.fromAngles(-yAngle, -xAngle, zAngle)
    head.setUserTransforms(Vector3f.ZERO, move, Vector3f.UNIT_XYZ)
    ske.updateWorldVectors()
  }

  override def leftEyeLidChange(changeSize: Float): Unit = {
    val topVec = new Vector3f(0, 0, changeSize * EyeTopZChangeMax)
    val botVec = new Vector3f(0, 0, changeSize * EyeBottomZChangeMax)
    eyeLidTopL.setUserTransforms(topVec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    eyeLidBottomL.setUserTransforms(botVec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    ske.updateWorldVectors()
  }

  override def rightEyeLidChange(changeSize: Float): Unit = {
    val topVec = new Vector3f(0, 0, changeSize * EyeTopZChangeMax)
    val botVec = new Vector3f(0, 0, changeSize * EyeBottomZChangeMax)
    eyeLidTopR.setUserTransforms(topVec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    eyeLidBottomR.setUserTransforms(botVec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    ske.updateWorldVectors()
  }
}
