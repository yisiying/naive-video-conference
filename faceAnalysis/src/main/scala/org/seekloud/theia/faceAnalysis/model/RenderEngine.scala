package org.seekloud.theia.faceAnalysis.model

import java.awt.Toolkit

import com.jme3.app.SimpleApplication
import com.jme3.asset.plugins.FileLocator
import com.jme3.light.{AmbientLight, DirectionalLight}
import com.jme3.math.{ColorRGBA, Quaternion, Vector3f}
import com.jme3.post.SceneProcessor
import com.jme3.profile.AppProfiler
import com.jme3.renderer.{Camera, RenderManager, ViewPort}
import com.jme3.renderer.queue.RenderQueue
import com.jme3.system.AppSettings
import com.jme3.system.JmeContext.Type
import com.jme3.texture.FrameBuffer
import com.jme3.texture.Image.Format
import com.jme3.util.BufferUtils
import org.bytedeco.opencv.opencv_core.Mat
import org.opencv.core.CvType
import org.seekloud.theia.faceAnalysis.common.Constants
import org.seekloud.theia.faceAnalysis.common.AppSettings.path

/**
  * Created by sky
  * Date on 2019/9/25
  * Time at 下午5:03
  * render2memory
  */
object RenderEngine extends SimpleApplication with SceneProcessor {
  //fixme 此处未使用，但需要用来初始化LwjglContext？？取消会导致engine卡死
  val tk: Toolkit = Toolkit.getDefaultToolkit
  //构造app
  setPauseOnLostFocus(false)
  val offSettings = new AppSettings(true)
  offSettings.setResolution(1, 1)
  setSettings(offSettings)
  start(Type.OffscreenSurface)
  paused = true //初始化完成后暂停游戏

  protected val width = Constants.DefaultPlayer.width

  protected val height = Constants.DefaultPlayer.height

  // create offscreen framebuffer
  protected val offBuffer: FrameBuffer = new FrameBuffer(width, height, 1)
  //setup framebuffer to use renderbuffer
  // this is faster for gpu -> cpu copies
  offBuffer.setDepthBuffer(Format.Depth)
  offBuffer.setColorBuffer(Format.RGBA8)

  private val cpuBuf = BufferUtils.createByteBuffer(width * height * 4)

  private val mat: Mat = new Mat(height, width, CvType.CV_8UC3)

  protected def setupOffscreenView(): Unit = {
    val offCamera = new Camera(width, height)
    // create a pre-view. a view that is rendered before the main view
    val offView = renderManager.createPreView("Offscreen View", offCamera)
    offView.setBackgroundColor(ColorRGBA.Black)  //remind 保证黑底便于抠图
    offView.setClearFlags(true, true, true)
    // this will let us know when the scene has been rendered to the frame buffer
    offView.addProcessor(this)

    /**
      * 摄像机
      * 调到崩溃。。。
      */
    offCamera.setFrustumPerspective(50f, 1f, 1f, 1000f)
    offCamera.setLocation(new Vector3f(-0.15730041f, 6.891322f, 9.460022f))
    offCamera.setRotation(new Quaternion(0.0018579672f, 0.98827016f, -0.1522275f, 0.012063393f))
    offCamera.lookAt(new Vector3f(0f, 3.5f, 0f), new Vector3f(0f, -1f, 0f))

    //set viewport to render to offscreen framebuffer
    offView.setOutputFrameBuffer(offBuffer)
    /**
      * 要有光
      */
    rootNode.addLight(new AmbientLight(new ColorRGBA(0.2f, 0.2f, 0.2f, 1f)))
    rootNode.addLight(new DirectionalLight(new Vector3f(-1, -2, -3), new ColorRGBA(0.8f, 0.8f, 0.8f, 1f).mult(1.5f)))

    //场景添加模型
    offView.attachScene(rootNode)
    assetManager.registerLocator(path, classOf[FileLocator])
  }

  def gamePause(): Unit = enqueueToEngine({
    paused = true
  })

  def gameGoOn(): Unit = enqueueToEngine({
    paused = false
  })

  def gameStop(): Unit = enqueueToEngine(stop())

  override def simpleInitApp(): Unit = {
    setupOffscreenView()
  }

  def getRenderMat(): Option[Mat] = {
    if (mat.empty()) None else Some(mat)
  }

  /**
    * @param fun function push into engine thread
    **/
  def enqueueToEngine(fun: => Unit) = enqueue(new Runnable {
    override def run(): Unit = fun
  })

  private def updateImageContents(): Unit = {
    cpuBuf.clear
    renderer.readFrameBuffer(offBuffer, cpuBuf)
    synchronized {
      Java2DConverter.buffer2mat(cpuBuf, mat)
    }
  }

  override def simpleUpdate(tpf: Float): Unit = {
  }

  override def initialize(rm: RenderManager, vp: ViewPort): Unit = {
  }

  override def reshape(vp: ViewPort, w: Int, h: Int): Unit = {
  }

  override def isInitialized = true

  override def preFrame(tpf: Float): Unit = {
  }

  override def postQueue(rq: RenderQueue): Unit = {
  }

  /**
    * Update the CPU image's contents after the scene has
    * been rendered to the framebuffer.
    */
  override def postFrame(out: FrameBuffer): Unit = {
    updateImageContents()
  }

  override def cleanup(): Unit = {
  }

  override def setProfiler(profiler: AppProfiler): Unit = {
  }
}
