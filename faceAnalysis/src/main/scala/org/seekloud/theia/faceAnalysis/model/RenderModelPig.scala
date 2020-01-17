package org.seekloud.theia.faceAnalysis.model

import com.jme3.animation.{Bone, Skeleton, SkeletonControl}
import com.jme3.light.DirectionalLight
import com.jme3.math.{ColorRGBA, Quaternion, Vector3f}
import com.jme3.scene.Node

object RenderModelPig extends RenderModel {
  override val id: Byte = 3
  protected var model:Node = null
  private var ske: Skeleton = null

  private var head: Bone = null
  private var jaw: Bone = null
  private var lipSideR: Bone = null
  private var lipSideL: Bone = null
  private var eyeballL: Bone = null
  private var eyeballR: Bone = null

  private var lowerlip1L: Bone = null
  private var lowerlip2L: Bone = null
  private var lowerlip3L: Bone = null
  private var lowerlip: Bone = null
  private var lowerlip1R: Bone = null
  private var lowerlip2R: Bone = null
  private var lowerlip3R: Bone = null

  private val LipSideChangeMax = 0.025f

  private val MouthChangeMax = 0.040f

  private var CurrentFaceWidth=135f

  private var CurrentFaceHeight=144f

  private val StandardFaceWidth=135f

  private val MaxFaceWidth=220f

  private val MinFaceWidth=120f

  private val MaxFaceHeight = 200f

  private val MinFaceHeight = 140f

  RenderEngine.enqueueToEngine({
    /**
      * 加载Jaime的模型
      */
    val scene = RenderEngine.getAssetManager.loadModel("Models/pig_head.glb").asInstanceOf[Node]
    model = scene.getChild("Armature").asInstanceOf[Node]
    // 将Jaime放大一点点，这样我们能观察得更清楚。
//    model.scale(1f)
    model.setLocalTranslation(0, 1.5f, 0)
    model.setLocalRotation(new Quaternion(-0.4f, 0, 0, 1))

//    val sun = RenderEngine.getRootNode.getLocalLightList.get(1).asInstanceOf[DirectionalLight]
//    sun.setColor(new ColorRGBA(0.8f, 0.8f, 0.8f, 1f))

    // 获得SkeletonControl
    // 骨骼控制器
    val sc = model.getControl(classOf[SkeletonControl])

    ske = sc.getSkeleton

    head = ske.getBone("head")
    head.setUserControl(true)
    jaw = ske.getBone("jaw")
    jaw.setUserControl(true)
    lipSideL = ske.getBone("corner.l")
    lipSideL.setUserControl(true)
    lipSideR = ske.getBone("corner.r")
    lipSideR.setUserControl(true)
    eyeballL = ske.getBone("eyeball.l")
    eyeballL.setUserControl(true)
    eyeballR = ske.getBone("eyeball.r")
    eyeballR.setUserControl(true)

    lowerlip1L = ske.getBone("lowerlip1.l")
    lowerlip1L.setUserControl(true)
    lowerlip2L = ske.getBone("lowerlip2.l")
    lowerlip2L.setUserControl(true)
    lowerlip3L = ske.getBone("lowerlip3.l")
    lowerlip3L.setUserControl(true)
    lowerlip = ske.getBone("lowerlip")
    lowerlip.setUserControl(true)
    lowerlip1R = ske.getBone("lowerlip1.r")
    lowerlip1R.setUserControl(true)
    lowerlip2R = ske.getBone("lowerlip2.r")
    lowerlip2R.setUserControl(true)
    lowerlip3R = ske.getBone("lowerlip3.r")
    lowerlip3R.setUserControl(true)
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
    val vec = new Vector3f(0, changeSize * 10 * MouthChangeMax, 0)
//    jaw.setUserTransforms(vec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    lowerlip1L.setUserTransforms(vec.mult(0.5f), Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    lowerlip2L.setUserTransforms(vec.mult(0.8f), Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    lowerlip3L.setUserTransforms(vec.mult(0.9f), Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    lowerlip.setUserTransforms(vec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    lowerlip1R.setUserTransforms(vec.mult(0.6f), Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    lowerlip2R.setUserTransforms(vec.mult(0.8f), Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    lowerlip3R.setUserTransforms(vec.mult(0.9f), Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    ske.updateWorldVectors()
  }

  override def headMove(xAngle: Float, yAngle: Float, zAngle: Float): Unit = {
    val move = new Quaternion
    move.fromAngles(-xAngle, -yAngle, zAngle)
    head.setUserTransforms(Vector3f.ZERO, move, Vector3f.UNIT_XYZ)
    ske.updateWorldVectors()
  }
  override def modelScale(rate:Float,fw:Float,fh:Float):Unit={
    if(Math.abs(CurrentFaceWidth-fw)>2.0f && Math.abs(CurrentFaceHeight-fh)>2.0f && fw <= MaxFaceWidth && fw>=MinFaceWidth && fh <= MaxFaceHeight && fh >= MinFaceHeight){
      model.setLocalScale(7f + (fw - StandardFaceWidth ) * rate)
      model.setLocalTranslation(0f,-2.5f + (StandardFaceWidth-fw)*rate * 0.9f,0f)
      CurrentFaceWidth=fw
      CurrentFaceHeight=fh
    }
  }


  override def leftEyeLidChange(changeSize: Float): Unit = {
    val size = 1 - changeSize
    eyeballL.setLocalScale(new Vector3f(1, size, 1))
    ske.updateWorldVectors()
  }

  override def rightEyeLidChange(changeSize: Float): Unit = {
    val size = 1 - changeSize
    eyeballR.setLocalScale(new Vector3f(1, size, 1))
    ske.updateWorldVectors()
  }
}
