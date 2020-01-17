package org.seekloud.theia.faceAnalysis.model

import com.jme3.animation._
import com.jme3.asset.AssetManager
import com.jme3.asset.plugins.FileLocator
import com.jme3.light.{AmbientLight, DirectionalLight}
import com.jme3.math.{ColorRGBA, Quaternion, Vector3f}
import com.jme3.scene.Node

/**
  * Created by sky
  * Date on 2019/9/26
  * Time at 下午7:28
  */
object RenderModelJaime extends RenderModel {
  override val id: Byte = 3
  protected var model:Node = null
  private var ske: Skeleton = null
  private var neck: Bone = null
  private var head: Bone = null
  private var iKJawTarget: Bone = null
  private var jaw: Bone = null
  private var lipBottomR: Bone = null
  private val lipTopR = null
  private var lipSideR: Bone = null
  private var lipBottomL: Bone = null
  private val lipTopL = null
  private var lipSideL: Bone = null
  private var eyebrow01R: Bone = null
  private var eyebrow02R: Bone = null
  private var eyebrow03R: Bone = null
  private var eyebrow01L: Bone = null
  private var eyebrow02L: Bone = null
  private var eyebrow03L: Bone = null
  private val cheekR: Bone = null
  private val cheekL: Bone = null
  private var eyeLidTopR: Bone = null
  private var eyeLidTopL: Bone = null
  private var eyeLidBottomR: Bone = null
  private var eyeLidBottomL: Bone = null
  private var sightTarget: Bone = null

  private val LipSideChangeMax = 0.025f

  private val MouthChangeMax = -0.030f

  private val EyeTopYChangeMax = -0.038f

  private val EyeBottomYChangeMax = 0.027f

  private val EyeZChangeMax = 0.015f

  private var CurrentFaceWidth=135f

  private var CurrentFaceHeight=144f

  private val StandardFaceWidth=135f

  private val MaxFaceWidth=220f

  private val MinFaceWidth=120f

  private val MaxFaceHeight = 220f

  private val MinFaceHeight = 120f

  private var animControl: AnimControl = null
  private var animChannel: AnimChannel = null

  RenderEngine.enqueueToEngine({

    /**
      * 加载Jaime的模型
      */
    // 我们的模特：Jaime
    model = RenderEngine.getAssetManager.loadModel("Models/Jaime/Jaime.j3o").asInstanceOf[Node]
    // 将Jaime放大一点点，这样我们能观察得更清楚。
    model.scale(5f)

    // 动画控制器
    animControl = model.getControl(classOf[AnimControl])
    animControl.addListener(animEventListener)

    animChannel = animControl.createChannel
    // 播放“闲置”动画
    animChannel.setAnim("Idle")

    // 获得SkeletonControl
    // 骨骼控制器
    val sc = model.getControl(classOf[SkeletonControl])
    ske = sc.getSkeleton
    ske.getBone("")
    neck = ske.getBone("neck")
    neck.setUserControl(true)
    head = ske.getBone("head")
    head.setUserControl(true)
    iKJawTarget = ske.getBone("IKjawTarget")
    iKJawTarget.setUserControl(true)
    jaw = ske.getBone("jaw")
    jaw.setUserControl(true)
    lipSideL = ske.getBone("LipSide.L")
    lipSideL.setUserControl(true)
    lipSideR = ske.getBone("LipSide.R")
    lipSideR.setUserControl(true)
    lipBottomL = ske.getBone("LipBottom.L")
    lipBottomL.setUserControl(true)
    lipBottomR = ske.getBone("LipBottom.R")
    lipBottomR.setUserControl(true)
    eyebrow01L = ske.getBone("eyebrow.01.L")
    eyebrow01L.setUserControl(true)
    eyebrow02L = ske.getBone("eyebrow.02.L")
    eyebrow02L.setUserControl(true)
    eyebrow03L = ske.getBone("eyebrow.03.L")
    eyebrow03L.setUserControl(true)
    eyebrow01R = ske.getBone("eyebrow.01.R")
    eyebrow01R.setUserControl(true)
    eyebrow02R = ske.getBone("eyebrow.02.R")
    eyebrow02R.setUserControl(true)
    eyebrow03R = ske.getBone("eyebrow.03.R")
    eyebrow03R.setUserControl(true)
    eyeLidTopL = ske.getBone("EyeLidTop.L")
    eyeLidTopL.setUserControl(true)
    eyeLidBottomL = ske.getBone("EyeLidBottom.L")
    eyeLidBottomL.setUserControl(true)
    eyeLidTopR = ske.getBone("EyeLidTop.R")
    eyeLidTopR.setUserControl(true)
    eyeLidBottomR = ske.getBone("EyeLidBottom.R")
    eyeLidBottomR.setUserControl(true)
    sightTarget = ske.getBone("SightTarget")
    sightTarget.setUserControl(true)
  })

  /**
    * 动画事件监听器
    */
  private val animEventListener = new AnimEventListener() {
    override def onAnimCycleDone(control: AnimControl, channel: AnimChannel, animName: String): Unit = {
      if ("JumpStart" == animName) { // “起跳”动作结束后，紧接着播放“着地”动画。
        channel.setAnim("JumpEnd")
        channel.setLoopMode(LoopMode.DontLoop)
        channel.setSpeed(1.5f)
      } else if ("JumpEnd" == animName) { // “着地”后，根据按键状态来播放“行走”或“闲置”动画。
        channel.setAnim("Wave")
        channel.setLoopMode(LoopMode.DontLoop)
      }else if ("Wave" == animName) { // “着地”后，根据按键状态来播放“行走”或“闲置”动画。
        channel.setAnim("Idle")
        channel.setLoopMode(LoopMode.Loop)
      }
    }

    override def onAnimChange(control: AnimControl, channel: AnimChannel, animName: String): Unit = {
    }
  }

  override def setModel(): Unit = {
    RenderEngine.getRootNode.detachAllChildren()

    RenderEngine.getRootNode.attachChild(model)

    animChannel.setAnim("JumpStart")
    animChannel.setLoopMode(LoopMode.DontLoop)
    animChannel.setSpeed(1.5f)
  }

  override def lipSideChange(changeSize: Float): Unit = {
    val size = changeSize * LipSideChangeMax
    lipSideR.setUserTransforms(new Vector3f(size, size, 0), Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    lipSideL.setUserTransforms(new Vector3f(-size, size, 0), Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    ske.updateWorldVectors()
  }

  override def mouthChange(changeSize: Float): Unit = {
    val vec = new Vector3f(0, changeSize * MouthChangeMax, 0)
    lipBottomL.setUserTransforms(vec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    lipBottomR.setUserTransforms(vec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    jaw.setUserTransforms(vec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    ske.updateWorldVectors()
  }

  override def headMove(xAngle: Float, yAngle: Float, zAngle: Float): Unit = {
    val move = new Quaternion
    move.fromAngles( -xAngle,-yAngle, zAngle)
    head.setUserTransforms(Vector3f.ZERO, move, Vector3f.UNIT_XYZ)
    ske.updateWorldVectors()
  }

  override def modelScale(rate:Float,fw:Float,fh:Float):Unit={
    if(Math.abs(CurrentFaceWidth-fw)>2.0f && Math.abs(CurrentFaceHeight-fh)>2.0f && fw<=MaxFaceWidth && fw>=MinFaceWidth && fh <= MaxFaceHeight && fh >= MinFaceHeight){
      model.setLocalScale(7f+(fw-StandardFaceWidth)*rate)
      model.setLocalTranslation(0f,-2.5f+(StandardFaceWidth-fw)*rate*0.9f,0f)
      CurrentFaceWidth=fw
      CurrentFaceHeight=fh
    }
  }


  override def leftEyeLidChange(changeSize: Float): Unit = {
    val topVec = new Vector3f(0, changeSize * EyeTopYChangeMax, changeSize * EyeZChangeMax)
    val botVec = new Vector3f(0, changeSize * EyeBottomYChangeMax, changeSize * EyeZChangeMax)
    eyeLidTopL.setUserTransforms(topVec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    eyeLidBottomL.setUserTransforms(botVec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    ske.updateWorldVectors()
  }

  override def rightEyeLidChange(changeSize: Float): Unit = {
    val topVec = new Vector3f(0, changeSize * EyeTopYChangeMax, changeSize * EyeZChangeMax)
    val botVec = new Vector3f(0, changeSize * EyeBottomYChangeMax, changeSize * EyeZChangeMax)
    eyeLidTopR.setUserTransforms(topVec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    eyeLidBottomR.setUserTransforms(botVec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
    ske.updateWorldVectors()
  }
}
