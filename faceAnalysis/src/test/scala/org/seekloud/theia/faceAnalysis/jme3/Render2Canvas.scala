package org.seekloud.theia.faceAnalysis.jme3

import java.awt.Canvas
import java.util.concurrent.Callable

import com.jme3.animation.{Bone, Skeleton, SkeletonControl}
import com.jme3.app.SimpleApplication
import com.jme3.light.{AmbientLight, DirectionalLight}
import com.jme3.math.{ColorRGBA, Quaternion, Vector3f}
import com.jme3.scene.Node
import com.jme3.system.{AppSettings, JmeCanvasContext}
import org.seekloud.theia.faceAnalysis.common.Constants

/**
  * Created by sky
  * Date on 2019/9/18
  * Time at 12:20
  * render2canvas
  * 暂时弃用
  */
class Render2Canvas extends SimpleApplication{
  private var offCanvas:Canvas = null  //游戏引擎渲染canvas

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
  private val iKEyeTargetR: Bone = null
  private val iKEyeTargetL: Bone = null
  private val eyeL: Bone = null
  private val eyeR: Bone = null

  private val LipSideChangeMax = 0.025f

  private val MouthChangeMax = -0.030f

  private val EyeTopYChangeMax = -0.038f

  private val EyeBottomYChangeMax = 0.027f

  private val EyeZChangeMax = 0.015f

  private def createOffCanvas(): Unit = {
    val settings = new AppSettings(true)
    settings.setWidth(Constants.DefaultPlayer.width)
    settings.setHeight(Constants.DefaultPlayer.height)
    setPauseOnLostFocus(false)
    setSettings(settings)
    createCanvas()
    offCanvas = getContext.asInstanceOf[JmeCanvasContext].getCanvas
    offCanvas.setSize(settings.getWidth, settings.getHeight)
  }

  private def startApp(): Unit = {
    startCanvas()
    enqueue(new Callable[Void]() {
      override def call = {
        getFlyByCamera.setDragToRotate(true)
        null
      }
    })
  }

  //构造app
  createOffCanvas()
  startApp()

  override def simpleInitApp(): Unit = {
    /**
      * 摄像机
      */
    cam.setLocation(new Vector3f(-0.15730041f, 6.891322f, 9.460022f))
    cam.setRotation(new Quaternion(0.0018579672f, 0.98827016f, -0.1522275f, 0.012063393f))
    //        flyCam.setMoveSpeed(10f)
    flyCam.setEnabled(false)

    /**
      * 要有光
      */
    rootNode.addLight(new AmbientLight(new ColorRGBA(0.2f, 0.2f, 0.2f, 1f)))
    rootNode.addLight(new DirectionalLight(new Vector3f(-1, -2, -3), new ColorRGBA(0.8f, 0.8f, 0.8f, 1f)))
    /**
      * 加载Jaime的模型
      */
    // 我们的模特：Jaime
    val jaime = assetManager.loadModel("Models/Jaime/Jaime.j3o").asInstanceOf[Node]
    // 将Jaime放大一点点，这样我们能观察得更清楚。
    jaime.scale(5f)
    rootNode.attachChild(jaime)
    // 获得SkeletonControl
    // 骨骼控制器
    val sc = jaime.getControl(classOf[SkeletonControl])
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
  }

  def lipSideChange(changeSize: Float): Unit = {
    enqueue(new Runnable() {
      override def run(): Unit = {
        val size = changeSize * LipSideChangeMax
        lipSideR.setUserTransforms(new Vector3f(size, size, 0), Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
        lipSideL.setUserTransforms(new Vector3f(-size, size, 0), Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
        ske.updateWorldVectors()
      }
    })
  }

  def mouthChange(changeSize: Float): Unit = {
    enqueue(new Runnable {
      override def run(): Unit = {
        val vec = new Vector3f(0, changeSize * MouthChangeMax, 0)
        lipBottomL.setUserTransforms(vec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
        lipBottomR.setUserTransforms(vec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
        jaw.setUserTransforms(vec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
        ske.updateWorldVectors()
      }
    })
  }

  def headMove(xAngle: Float, yAngle: Float, zAngle: Float): Unit = {
    enqueue(new Runnable {
      override def run(): Unit = {
        val move = new Quaternion
        move.fromAngles(-yAngle, -xAngle, zAngle)
        head.setUserTransforms(Vector3f.ZERO, move, Vector3f.UNIT_XYZ)
        ske.updateWorldVectors()
      }
    })
  }

  def leftEyeLidChange(changeSize: Float): Unit = {
    enqueue(new Runnable {
      override def run(): Unit = {
        val topVec = new Vector3f(0, changeSize * EyeTopYChangeMax, changeSize * EyeZChangeMax)
        val botVec = new Vector3f(0, changeSize * EyeBottomYChangeMax, changeSize * EyeZChangeMax)
        eyeLidTopL.setUserTransforms(topVec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
        eyeLidBottomL.setUserTransforms(botVec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
        ske.updateWorldVectors()
      }
    })
  }

  def rightEyeLidChange(changeSize: Float): Unit = {
    enqueue(new Runnable {
      override def run(): Unit = {
        val topVec = new Vector3f(0, changeSize * EyeTopYChangeMax, changeSize * EyeZChangeMax)
        val botVec = new Vector3f(0, changeSize * EyeBottomYChangeMax, changeSize * EyeZChangeMax)
        eyeLidTopR.setUserTransforms(topVec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
        eyeLidBottomR.setUserTransforms(botVec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ)
        ske.updateWorldVectors()
      }
    })
  }
}
