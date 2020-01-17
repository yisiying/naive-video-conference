package org.seekloud.theia.faceAnalysis.scene.panel

import javafx.scene.{Group, Scene}
import org.seekloud.theia.faceAnalysis.common.Constants
import org.slf4j.LoggerFactory

/**
  * Created by sky
  * Date on 2019/8/19
  * Time at 下午2:41
  * Implement basic function
  */
//fixme 目前这种设计其实并没有减少内存和性能，可以考虑panel局部渲染而不是局部定义
trait SceneImpl {
  protected val log = LoggerFactory.getLogger(this.getClass)
  protected val width = Constants.AppWindow.width * 0.9
  protected val height = Constants.AppWindow.height * 0.75
  protected val group = new Group()
  protected val scene = new Scene(group, width, height)
  def showScene(): Unit
}
