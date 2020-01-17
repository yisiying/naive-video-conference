package org.seekloud.theia.faceAnalysis.model

import org.bytedeco.javacv.{Frame, OpenCVFrameConverter}
import org.bytedeco.opencv.global.opencv_imgproc.{COLOR_BGR2GRAY, equalizeHist}
import org.bytedeco.opencv.global.{opencv_core, opencv_imgcodecs, opencv_imgproc, opencv_photo}
import org.bytedeco.opencv.helper.opencv_core.RGB
import org.bytedeco.opencv.opencv_core._
import org.bytedeco.opencv.opencv_face.FacemarkLBF
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier

import org.seekloud.theia.faceAnalysis.common.{AppSettings, Constants}
import org.seekloud.theia.faceAnalysis.pb.api.Mask
import org.seekloud.theia.faceAnalysis.pb.api
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by sky
  * Date on 2019/8/20
  * Time at 上午10:44
  * implement ai part
  */
object FaceAnalysis {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait AiInfo {
    //fixme cause null error
    @deprecated
    def release(): Unit
  }

  case class AiImageCopy(
                          src: Mat,
                          position: (Int, Int)
                        ) extends AiInfo {
    override def release(): Unit = {
      src.release()
    }
  }

  /**
    * SeamlessClone waste too much
    **/
  @deprecated
  case class AiImageSeamless(
                              src: Mat,
                              srcMask: Mat,
                              point: Point
                            ) extends AiInfo {
    override def release(): Unit = {
      src.release()
      srcMask.release()
    }
  }

  case class AiDot(
                    point: Point
                  ) extends AiInfo {
    override def release(): Unit = {
    }
  }

  case class Ai3dModel(
                        srcMask: Mat,
                        src: Mat
                      ) extends AiInfo {
    override def release(): Unit = {
      srcMask.release()
      src.release()
    }
  }

  val t1 = System.currentTimeMillis()
  private val faceDetector = new CascadeClassifier(AppSettings.path + "haarcascade_frontalface_alt2.xml")
  // Create an instance of Facemark
  private val facemark = FacemarkLBF.create
  facemark.loadModel(AppSettings.path + "lbfmodel.yaml")
  // Load landmark detector
  private val matConverter = new OpenCVFrameConverter.ToMat
  log.info(s"init FaceAnalysis finish with ${System.currentTimeMillis() - t1}")

  private val StandardFaceWidth = 135f

  def matToFrame(mat: Mat): Frame = {
    matConverter.convert(mat)
  }

  /**
    * edit from https://github.com/bytedeco/javacv/blob/master/samples/LBFFacemarkExampleWithVideo.java
    *
    * @param mat capture video matrix
    * @return list-64-Point
    *         * 脸轮廓：1~17
    *         * 眉毛：18~22, 23~27
    *         * 鼻梁：28~31
    *         * 鼻子：31~36
    *         * 眼睛：37~42, 43~48
    *         * 嘴唇：49~68
    **/
  def find64(mat: Mat): Option[Point2fVectorVector] = {
    val gray = new UMat
    mat.copyTo(gray)
    opencv_imgproc.cvtColor(gray, gray, COLOR_BGR2GRAY)
    equalizeHist(gray, gray)
    // Find faces on the image
    val faces = new RectVector
    //    val t2 = System.currentTimeMillis()
    faceDetector.detectMultiScale(gray, faces, 1.1, 3, 0, new Size(160, 120), new Size(320, 240))

    gray.release()
    gray.close()
    //    log.info(s"Faces detected: + ${faces.size()}")
    //    log.info(s"Faces detected: + ${faces.size()}")
    if (!faces.empty()) {
      // Landmarks for one face is a vector of points
      // There can be more than one face in the image.
      val landmarks = new Point2fVectorVector
      //resize inputMat
      //  val resize = new Mat()
      //  opencv_imgproc.resize(mat, resize, new Size(240, 320))
      // Run landmark detector
      val success = facemark.fit(mat, faces, landmarks)
      // val success = facemark.fit(resize, faces, landmarks)
      if (success) Some(landmarks) else None
    } else None
  }


  /**
    * detect 64 position
    **/
  def detectPoint(landmarks: Point2fVectorVector, l: ArrayBuffer[AiInfo]) = {
    for (i <- 0 until landmarks.size().toInt) {
      val v = landmarks.get(i)
      for (j <- 0 until v.size.toInt) {
        val p = new Point()
        p.x(v.get(j).x.toInt)
        p.y(v.get(j).y().toInt)
        l.append(AiDot(p))
      }
    }
  }

  def detectPoint(landmarks: Seq[Mask], l: ArrayBuffer[AiInfo]) = {
    for (v <- landmarks) {
      for (pos <- v.p) {
        val p = new Point()
        p.x(pos.x.toInt)
        p.y(pos.y.toInt)
        l.append(AiDot(p))
      }
    }
  }

  def detectPoint(landmarks: mutable.Buffer[mutable.Buffer[Point]], l: ArrayBuffer[AiInfo]) = {
    for (v <- landmarks) {
      for (pos <- v) {
        l.append(AiDot(pos))
      }
    }
  }


  private var glassMat: (Byte, Mat) = null
  private var beardMat: (Byte, Mat) = null

  private def setGlassMat(id: Byte) = {
    if (glassMat == null || id != glassMat._1) {
      glassMat = (id, opencv_imgcodecs.imread(AppSettings.path + s"glass$id.png"))
    }
  }

  private def setBeardMat(id: Byte) = {
    if (beardMat == null || id != beardMat._1) {
      beardMat = (id, opencv_imgcodecs.imread(AppSettings.path + s"beard$id.png"))
    }
  }

  /**
    * detect glass position
    **/
  @deprecated
  def detectGlass4Seamless(landmarks: Point2fVectorVector, l: ArrayBuffer[AiInfo]) = {
    for (i <- 0 until landmarks.size().toInt) {
      val v = landmarks.get(i)
      /**
        * 添加眼镜
        **/
      val pos_left = (v.get(0).x, v.get(36).y())
      val pos_right = (v.get(16).x, v.get(45).y())
      val face_center = (v.get(27).x, v.get(27).y())

      val width = pos_right._1 - pos_left._1
      try {
        val height = (glassMat._2.rows / (glassMat._2.cols() / width)).toInt
        val resize = new Mat()

        opencv_imgproc.resize(glassMat._2, resize, new Size(width.toInt, height))
        // 角度旋转，通过计算两个眼角和水平方向的夹角来旋转眼镜
        //        val sx = v.get(36).x - v.get(45).x
        //        val sy = v.get(36).y() - v.get(45).y()
        //夹角正切值
        try {
          //          val r = sx / sy
          //求正切角,弧度转为度
          //          val degree = math.toDegrees(math.atan(r))
          //        .rotatedRectangleIntersection()
          // 定义感兴趣区域(位置，logo图像大小)
          //      println(pos_left._1.toInt, pos_left._2.toInt, glassMat.cols(), glassMat.rows(), resize.cols(), resize.rows(), width, height, degree)

          val p = new Point()
          p.x(face_center._1.toInt)
          p.y(face_center._2.toInt)

          val src_mask = Mat.zeros(resize.size(), resize.`type`()).asMat()
          opencv_core.bitwise_or(resize, src_mask, src_mask)
          opencv_core.bitwise_not(src_mask, src_mask)
          l.append(AiImageSeamless(resize, src_mask, p))
        } catch {
          case e: Exception =>
            log.error("夹角正切值 /0")
        }
      } catch {
        case e: Exception =>
          log.error("height /0")
      }
    }
  }

  def detectGlass4Seamless(landmarks: Seq[Mask], l: ArrayBuffer[AiInfo]) = {
    for (v <- landmarks) {
      /**
        * 添加眼镜
        **/
      val pos_left = (v.p(0).x, v.p(36).y)
      val pos_right = (v.p(16).x, v.p(45).y)
      val face_center = (v.p(27).x, v.p(27).y)

      val width = pos_right._1 - pos_left._1
      try {
        val height = (glassMat._2.rows / (glassMat._2.cols() / width)).toInt
        val resize = new Mat()

        opencv_imgproc.resize(glassMat._2, resize, new Size(width.toInt, height))
        // 角度旋转，通过计算两个眼角和水平方向的夹角来旋转眼镜
        //        val sx = v.get(36).x - v.get(45).x
        //        val sy = v.get(36).y() - v.get(45).y()
        //夹角正切值
        try {
          //          val r = sx / sy
          //求正切角,弧度转为度
          //          val degree = math.toDegrees(math.atan(r))
          //        .rotatedRectangleIntersection()
          // 定义感兴趣区域(位置，logo图像大小)
          //      println(pos_left._1.toInt, pos_left._2.toInt, glassMat.cols(), glassMat.rows(), resize.cols(), resize.rows(), width, height, degree)

          val p = new Point()
          p.x(face_center._1.toInt)
          p.y(face_center._2.toInt)

          val src_mask = Mat.zeros(resize.size(), resize.`type`()).asMat()
          opencv_core.bitwise_or(resize, src_mask, src_mask)
          opencv_core.bitwise_not(src_mask, src_mask)
          l.append(AiImageSeamless(resize, src_mask, p))
        } catch {
          case e: Exception =>
            log.error("夹角正切值 /0")
        }
      } catch {
        case e: Exception =>
          log.error("height /0")
      }
    }
  }

  def detectGlass4Seamless(landmarks: mutable.Buffer[mutable.Buffer[Point]], l: ArrayBuffer[AiInfo]) = {
    for (v <- landmarks) {
      /**
        * 添加眼镜
        **/
      val pos_left = (v(0).x, v(36).y)
      val pos_right = (v(16).x, v(45).y)
      val face_center = (v(27).x, v(27).y)

      val width = pos_right._1 - pos_left._1
      try {
        val height = (glassMat._2.rows / (glassMat._2.cols() / width)).toInt
        val resize = new Mat()

        opencv_imgproc.resize(glassMat._2, resize, new Size(width.toInt, height))
        // 角度旋转，通过计算两个眼角和水平方向的夹角来旋转眼镜
        //        val sx = v.get(36).x - v.get(45).x
        //        val sy = v.get(36).y() - v.get(45).y()
        //夹角正切值
        try {
          //          val r = sx / sy
          //求正切角,弧度转为度
          //          val degree = math.toDegrees(math.atan(r))
          //        .rotatedRectangleIntersection()
          // 定义感兴趣区域(位置，logo图像大小)
          //      println(pos_left._1.toInt, pos_left._2.toInt, glassMat.cols(), glassMat.rows(), resize.cols(), resize.rows(), width, height, degree)

          val p = new Point()
          p.x(face_center._1.toInt)
          p.y(face_center._2.toInt)

          val src_mask = Mat.zeros(resize.size(), resize.`type`()).asMat()
          opencv_core.bitwise_or(resize, src_mask, src_mask)
          opencv_core.bitwise_not(src_mask, src_mask)
          l.append(AiImageSeamless(resize, src_mask, p))
        } catch {
          case e: Exception =>
            log.error("夹角正切值 /0")
        }
      } catch {
        case e: Exception =>
          log.error("height /0")
      }
    }
  }


  /**
    * detect glass position
    **/
  def detectGlass(landmarks: Point2fVectorVector, l: ArrayBuffer[AiInfo]) = {
    for (i <- 0 until landmarks.size().toInt) {
      val v = landmarks.get(i)
      /**
        * 添加眼镜
        **/
      val pos_left = (v.get(0).x, v.get(36).y())
      val pos_right = (v.get(16).x, v.get(45).y())
      val face_center = (v.get(27).x.toInt, v.get(27).y().toInt)

      val width = pos_right._1 - pos_left._1
      try {
        val height = (glassMat._2.rows / (glassMat._2.cols() / width)).toInt
        val resize = new Mat()

        opencv_imgproc.resize(glassMat._2, resize, new Size(width.toInt, height))
        l.append(AiImageCopy(resize, (face_center._1 - width.toInt / 2, face_center._2 - height / 2)))
      } catch {
        case e: Exception =>
          log.error("height /0")
      }
    }
  }

  def detectGlass(landmarks: Seq[Mask], l: ArrayBuffer[AiInfo]) = {
    for (v <- landmarks) {
      /**
        * 添加眼镜
        **/
      val pos_left = (v.p(0).x, v.p(36).y)
      val pos_right = (v.p(16).x, v.p(45).y)
      val face_center = (v.p(27).x.toInt, v.p(27).y.toInt)

      val width = pos_right._1 - pos_left._1
      try {
        val height = (glassMat._2.rows / (glassMat._2.cols() / width)).toInt
        val resize = new Mat()

        opencv_imgproc.resize(glassMat._2, resize, new Size(width.toInt, height))
        l.append(AiImageCopy(resize, (face_center._1 - width.toInt / 2, face_center._2 - height / 2)))
      } catch {
        case e: Exception =>
          log.error("height /0")
      }
    }
  }

  def detectGlass(landmarks: mutable.Buffer[mutable.Buffer[Point]], l: ArrayBuffer[AiInfo]) = {
    for (v <- landmarks) {
      /**
        * 添加眼镜
        **/
      val pos_left = (v.head.x, v(36).y)
      val pos_right = (v(16).x, v(45).y)
      val face_center = (v(27).x, v(27).y)

      val width = pos_right._1 - pos_left._1
      try {
        val height = glassMat._2.rows / (glassMat._2.cols() / width)
        val resize = new Mat()
        opencv_imgproc.resize(glassMat._2, resize, new Size(width.toInt, height))
        l.append(AiImageCopy(resize, (face_center._1 - width.toInt / 2, face_center._2 - height / 2)))
      } catch {
        case e: Exception =>
          log.error(s"height ${e.getMessage}  ${glassMat._1}")
      }
    }
  }


  /**
    * detect beard position
    **/
  @deprecated
  def detectBeard4Seamless(landmarks: Point2fVectorVector, l: ArrayBuffer[AiInfo]) = {
    for (i <- 0 until landmarks.size().toInt) {
      val v = landmarks.get(i)
      val pos_left = (v.get(48).x, v.get(34).y())
      val pos_right = (v.get(54).x, v.get(34).y())
      val face_center = (v.get(33).x, v.get(33).y())
      val width = pos_right._1 - pos_left._1
      try {
        val height = (beardMat._2.rows / (beardMat._2.cols() / width)).toInt
        val resize = new Mat()
        opencv_imgproc.resize(beardMat._2, resize, new Size(width.toInt, height))
        val p = new Point()
        p.x(face_center._1.toInt)
        p.y(face_center._2.toInt)

        val src_mask = Mat.zeros(resize.size(), resize.`type`()).asMat()
        opencv_core.bitwise_or(resize, src_mask, src_mask)
        opencv_core.bitwise_not(src_mask, src_mask)
        l.append(AiImageSeamless(resize, src_mask, p))
      } catch {
        case e: Exception =>
          log.error("height /0")
      }
    }
  }

  def detectBeard4Seamless(landmarks: Seq[Mask], l: ArrayBuffer[AiInfo]) = {
    for (v <- landmarks) {
      val pos_left = (v.p(48).x, v.p(34).y)
      val pos_right = (v.p(54).x, v.p(34).y)
      val face_center = (v.p(33).x, v.p(33).y)
      val width = pos_right._1 - pos_left._1
      try {
        val height = (beardMat._2.rows / (beardMat._2.cols() / width)).toInt
        val resize = new Mat()
        opencv_imgproc.resize(beardMat._2, resize, new Size(width.toInt, height))
        val p = new Point()
        p.x(face_center._1.toInt)
        p.y(face_center._2.toInt)

        val src_mask = Mat.zeros(resize.size(), resize.`type`()).asMat()
        opencv_core.bitwise_or(resize, src_mask, src_mask)
        opencv_core.bitwise_not(src_mask, src_mask)
        l.append(AiImageSeamless(resize, src_mask, p))
      } catch {
        case e: Exception =>
          log.error("height /0")
      }
    }
  }

  def detectBeard4Seamless(landmarks: mutable.Buffer[mutable.Buffer[Point]], l: ArrayBuffer[AiInfo]) = {
    for (v <- landmarks) {
      val pos_left = (v(48).x, v(34).y)
      val pos_right = (v(54).x, v(34).y)
      val face_center = (v(33).x, v(33).y)
      val width = pos_right._1 - pos_left._1
      try {
        val height = (beardMat._2.rows / (beardMat._2.cols() / width)).toInt
        val resize = new Mat()
        opencv_imgproc.resize(beardMat._2, resize, new Size(width.toInt, height))
        val p = new Point()
        p.x(face_center._1.toInt)
        p.y(face_center._2.toInt)

        val src_mask = Mat.zeros(resize.size(), resize.`type`()).asMat()
        opencv_core.bitwise_or(resize, src_mask, src_mask)
        opencv_core.bitwise_not(src_mask, src_mask)
        l.append(AiImageSeamless(resize, src_mask, p))
      } catch {
        case e: Exception =>
          log.error("height /0")
      }
    }
  }


  /**
    * detect beard position
    **/
  def detectBeard(landmarks: Point2fVectorVector, l: ArrayBuffer[AiInfo]) = {
    for (i <- 0 until landmarks.size().toInt) {
      val v = landmarks.get(i)
      val pos_left = (v.get(48).x, v.get(34).y())
      val pos_right = (v.get(54).x, v.get(34).y())
      val face_center = (v.get(33).x.toInt, v.get(33).y().toInt)
      val width = pos_right._1 - pos_left._1
      try {
        val height = (beardMat._2.rows / (beardMat._2.cols() / width)).toInt
        val resize = new Mat()
        opencv_imgproc.resize(beardMat._2, resize, new Size(width.toInt, height))
        l.append(AiImageCopy(resize, (face_center._1 - width.toInt / 2, face_center._2 - height / 2)))
      } catch {
        case e: Exception =>
          log.error("height /0")
      }
    }
  }

  def detectBeard(landmarks: Seq[Mask], l: ArrayBuffer[AiInfo]) = {
    for (v <- landmarks) {
      val pos_left = (v.p(48).x, v.p(34).y)
      val pos_right = (v.p(54).x, v.p(34).y)
      val face_center = (v.p(33).x.toInt, v.p(33).y.toInt)
      val width = pos_right._1 - pos_left._1
      try {
        val height = (beardMat._2.rows / (beardMat._2.cols() / width)).toInt
        val resize = new Mat()
        opencv_imgproc.resize(beardMat._2, resize, new Size(width.toInt, height))
        l.append(AiImageCopy(resize, (face_center._1 - width.toInt / 2, face_center._2 - height / 2)))
      } catch {
        case e: Exception =>
          log.error("height /0")
      }
    }
  }

  def detectBeard(landmarks: mutable.Buffer[mutable.Buffer[Point]], l: ArrayBuffer[AiInfo]) = {
    for (v <- landmarks) {
      val pos_left = (v(48).x, v(34).y)
      val pos_right = (v(54).x, v(34).y)
      val face_center = (v(33).x, v(33).y)
      val width = pos_right._1 - pos_left._1
      try {
        val height = (beardMat._2.rows / (beardMat._2.cols() / width)).toInt
        val resize = new Mat()
        opencv_imgproc.resize(beardMat._2, resize, new Size(width.toInt, height))
        l.append(AiImageCopy(resize, (face_center._1 - width.toInt / 2, face_center._2 - height / 2)))
      } catch {
        case e: Exception =>
          log.error("height /0")
      }
    }
  }

  //fixme
//  val hand_mask = opencv_imgcodecs.imread("/Users/sky/Downloads/hand_over_face/masks/24.png")
//  val size = new Size(640, 480)
//  val r_crop_img = new Mat()
//  opencv_imgproc.resize(hand_mask, r_crop_img, size)
//  opencv_core.bitwise_not(r_crop_img, r_crop_img)

  /**
    * @param mat        capture video matrix
    * @param aiInfoList detected position
    * @return javacv.Frame
    **/
  def draw(mat: Mat, aiInfoList: ArrayBuffer[AiInfo], controlState: Boolean = false): Frame = {
    //fixme  此处存在内存问题
    if (aiInfoList.isEmpty) {
      val m = RenderEngine.getRenderMat()
      if (controlState && m.nonEmpty) {
        val dstMat = mat.clone()
        //fixme 此处需要考虑安全
        //        m.get.copyTo(dstMat, m.get)
        matConverter.convert(dstMat)
      } else {
        matConverter.convert(mat)
      }
    } else {
      val dstMat = mat.clone()
      aiInfoList.foreach {
        case aiImageCopy: AiImageCopy =>
          try {
            val ROI = dstMat.apply(new Rect(aiImageCopy.position._1, aiImageCopy.position._2, aiImageCopy.src.cols, aiImageCopy.src.rows))
            opencv_core.addWeighted(ROI, 1.0, aiImageCopy.src, 0.3, 0.0, ROI)
          } catch {
            case e: Exception =>
              log.error(s"draw aiImageCopy with error ${e.getMessage}")
          }
          aiImageCopy.src.release()

        case aiDot: AiDot =>
          opencv_imgproc.circle(dstMat, aiDot.point, 2, RGB(255, 255, 0))

        case aiImage: AiImageSeamless =>
          try opencv_photo.seamlessClone(aiImage.src, dstMat, aiImage.srcMask, aiImage.point, dstMat, opencv_photo.MIXED_CLONE) catch {
            case e: Exception =>
              log.error(s"draw aiInfo with error ${e.getMessage}")
          }

        case ai3dModel: Ai3dModel =>
          //          ai3dModel.srcMask.copyTo(ai3dModel.srcMask, r_crop_img)
//          opencv_core.bitwise_and(r_crop_img, ai3dModel.srcMask, ai3dModel.srcMask)
          ai3dModel.src.copyTo(dstMat, ai3dModel.srcMask)
      }
      matConverter.convert(dstMat)
    }
  }

  var model: RenderModel = null

  def setDrawMat(changeIndex: Byte, changeValue: Byte) = {
    changeIndex match {
      case 0 =>
        setGlassMat(changeValue)
      case 1 =>
        setBeardMat(changeValue)
      case 3 =>
        if (model == null || model.id != changeValue) {
          model = RenderModel(changeValue)
        }
      case _ =>
    }
  }

  def modelControlCallBack() = {
    if (model != null) {
      RenderEngine.enqueueToEngine({
        model.mouthChange(FaceAnalysis.mouth_open.toFloat)
        model.headMove(x_angle.toFloat, -y.toFloat, z.toFloat)
        model.leftEyeLidChange(1 - FaceAnalysis.left_eye.toFloat)
        model.rightEyeLidChange(1 - FaceAnalysis.right_eye.toFloat)
        //直接对模型进行放大缩小，
        // model.modelScale(0.05f,face_width,face_height)
      })
    }
  }

  private def meter(A: Double, B: Double, C: Double, x: Double, y: Double) = {
    val diversion = A * x + B * y + C
    diversion / math.sqrt(math.pow(A, 2) + math.pow(B, 2))
  }

  private def eyebrow_move(p1: (Float, Float), p2: (Float, Float), slope: Double, last: Double, rate: Double) = {
    val bias = p1._2 - slope * p1._1
    val distance = math.sqrt(math.pow(p1._1 - p2._1, 2) + math.pow(p1._2 - p2._2, 2))
    val diversion = meter(slope, -1, bias, p2._1, p2._2)
    val result = (diversion / distance - 0.45) * 6
    last * (1 - rate) + result * rate
  }

  private def eye_open(p1: (Float, Float), p2: (Float, Float), v11: (Float, Float), v12: (Float, Float), v21: (Float, Float), v22: (Float, Float), last: Double, rate: Double) = {
    val distance = math.sqrt(math.pow(p1._1 - p2._1, 2) + math.pow(p1._2 - p2._2, 2))
    val slope = (p2._2 - p1._2).toDouble / (p2._1 - p1._1).toDouble
    val bias = p2._2 - slope * p2._1
    var d1 = math.abs(meter(slope, -1, bias, v11._1, v11._2))
    var d2 = math.abs(meter(slope, -1, bias, v12._1, v12._2))
    var diversion = if (d1 > d2) d1 else d2
    d1 = math.abs(meter(slope, -1, bias, v21._1, v21._2))
    d2 = math.abs(meter(slope, -1, bias, v22._1, v22._2))
    diversion += (if (d1 > d2) d1 else d2)
    var ratio = (diversion / distance - 0.18) * 8
    ratio = (ratio * 10.0).toInt / 10.0
    ratio = last * (1 - rate) + ratio * rate
    ratio
  }


  var x = 0.0d //从模型中得到的头部数据。
  var y = 0.0d
  var z = 0.0d
  var x_angle = 0.0d
  var y_angle = 0.0d
  var z_angle = 0.0d
  var left_eye = 1.0d
  var right_eye = 1.0d
  var eyebrow_left = 0.0d
  var eyebrow_right = 0.0d
  var mouth_open = 0.0d
  var face_height = 0.0f
  var face_width = 0.0f

  def detectModel(face: Point2fVector, aiInfoList: ArrayBuffer[AiInfo]) = {
    val div_x = face.get(16).x - face.get(0).x
    // 眼角横向距离
    val div_y = face.get(16).y - face.get(0).y
    val center_x = face.get(0).x + div_x / 2.0
    val center_y = face.get(0).y + div_y / 2.0
    val slope = div_y.toDouble / div_x.toDouble
    val bias = center_y - slope * center_x
    val x_proj = (slope * (face.get(30).y - bias) + face.get(30).x) / (1 + math.pow(slope, 2))
    val y_proj = slope * x_proj + bias
    var diversion = math.sqrt(math.pow(x_proj - face.get(0).x, 2) + math.pow(y_proj - face.get(0).y, 2))
    var distance = math.sqrt(math.pow(face.get(16).x - face.get(0).x, 2) + math.pow(face.get(16).y - face.get(0).y, 2))

    val rate = 0.5
    // Ax+By+C/sqrt(A^2+B^2)
    y_angle = y_angle * (1 - rate) + math.asin(diversion / distance - 0.5) * 3.14 * rate

    // nose to eye around 1/6 head
    diversion = meter(slope, -1, bias, face.get(30).x, face.get(30).y)
    diversion = diversion + 1.0 / 6 * distance
    x_angle = x_angle * (1 - rate) + math.asin(diversion / distance) * 3.14 * rate

    z_angle = z_angle * (1 - rate) + math.atan(slope) * 3.14 * rate

    // eye
    left_eye = eye_open((face.get(36).x, face.get(36).y()), (face.get(39).x, face.get(39).y()), (face.get(37).x, face.get(37).y()), (face.get(38).x, face.get(38).y()), (face.get(40).x, face.get(40).y()), (face.get(41).x, face.get(41).y()), left_eye, rate)
    right_eye = eye_open((face.get(42).x, face.get(42).y()), (face.get(45).x, face.get(45).y()), (face.get(43).x, face.get(43).y()), (face.get(44).x, face.get(44).y()), (face.get(46).x, face.get(46).y()), (face.get(47).x, face.get(47).y()), right_eye, rate)

    // eyebrow
    eyebrow_left = eyebrow_move((face.get(17).x, face.get(17).y()), (face.get(19).x, face.get(19).y()), slope, eyebrow_left, rate)
    eyebrow_right = eyebrow_move((face.get(26).x, face.get(26).y()), (face.get(24).x, face.get(24).y()), slope, eyebrow_right, rate)

    // mouth
    diversion = math.sqrt(math.pow(face.get(62).x - face.get(66).x, 2) + math.pow(face.get(62).y - face.get(66).y, 2))
    distance = math.sqrt(math.pow(face.get(60).x - face.get(64).x, 2) + math.pow(face.get(60).y - face.get(64).y, 2))
    mouth_open = (diversion / distance - 0.15) * 2

    modelControlCallBack()
    //移动3d模型
    //    val face_center = ((face.get(2).x+(face.get(14).x-face.get(2).x)/2).toInt, (face.get(2).y()+(face.get(14).y()-face.get(2).y())/2).toInt)
    val face_center = (face.get(33).x.toInt, face.get(33).y().toInt)

    RenderEngine.getRenderMat().foreach { m =>
      val sm = shiftMat(m, Constants.DefaultPlayer.width / 2 - face_center._1, Constants.DefaultPlayer.height / 2 - face_center._2)
      aiInfoList.append(Ai3dModel(sm, sm))
    }
  }

  def detectModel(face: Mask, aiInfoList: ArrayBuffer[AiInfo]) = {
    val div_x = face.p(16).x - face.p(0).x
    // 眼角横向距离
    val div_y = face.p(16).y - face.p(0).y
    val center_x = face.p(0).x + div_x / 2.0
    val center_y = face.p(0).y + div_y / 2.0
    val slope = div_y.toDouble / div_x.toDouble
    val bias = center_y - slope * center_x
    val x_proj = (slope * (face.p(30).y - bias) + face.p(30).x) / (1 + math.pow(slope, 2))
    val y_proj = slope * x_proj + bias
    var diversion = math.sqrt(math.pow(x_proj - face.p(0).x, 2) + math.pow(y_proj - face.p(0).y, 2))
    var distance = math.sqrt(math.pow(face.p(16).x - face.p(0).x, 2) + math.pow(face.p(16).y - face.p(0).y, 2))

    val rate = 0.5
    // Ax+By+C/sqrt(A^2+B^2)
    y_angle = y_angle * (1 - rate) + math.asin(diversion / distance - 0.5) * 3.14 * rate

    // nose to eye around 1/6 head
    diversion = meter(slope, -1, bias, face.p(30).x, face.p(30).y)
    diversion = diversion + 1.0 / 6 * distance
    x_angle = x_angle * (1 - rate) + math.asin(diversion / distance) * 3.14 * rate

    z_angle = z_angle * (1 - rate) + math.atan(slope) * 3.14 * rate

    // eye
    left_eye = eye_open((face.p(36).x, face.p(36).y), (face.p(39).x, face.p(39).y), (face.p(37).x, face.p(37).y), (face.p(38).x, face.p(38).y), (face.p(40).x, face.p(40).y), (face.p(41).x, face.p(41).y), left_eye, rate)
    right_eye = eye_open((face.p(42).x, face.p(42).y), (face.p(45).x, face.p(45).y), (face.p(43).x, face.p(43).y), (face.p(44).x, face.p(44).y), (face.p(46).x, face.p(46).y), (face.p(47).x, face.p(47).y), right_eye, rate)

    // eyebrow
    eyebrow_left = eyebrow_move((face.p(17).x, face.p(17).y), (face.p(19).x, face.p(19).y), slope, eyebrow_left, rate)
    eyebrow_right = eyebrow_move((face.p(26).x, face.p(26).y), (face.p(24).x, face.p(24).y), slope, eyebrow_right, rate)

    // mouth
    diversion = math.sqrt(math.pow(face.p(62).x - face.p(66).x, 2) + math.pow(face.p(62).y - face.p(66).y, 2))
    distance = math.sqrt(math.pow(face.p(60).x - face.p(64).x, 2) + math.pow(face.p(60).y - face.p(64).y, 2))
    mouth_open = (diversion / distance - 0.15) * 2

    modelControlCallBack()
    //移动3d模型
    //    val face_center = ((face.get(2).x+(face.get(14).x-face.get(2).x)/2).toInt, (face.get(2).y()+(face.get(14).y()-face.get(2).y())/2).toInt)
    val face_center = (face.p(33).x.toInt, face.p(33).y.toInt)

    RenderEngine.getRenderMat().foreach { m =>
      val sm = shiftMat(m, Constants.DefaultPlayer.width / 2 - face_center._1, Constants.DefaultPlayer.height / 2 - face_center._2)
      aiInfoList.append(Ai3dModel(sm, sm))
    }
  }

  def detectModel(face: mutable.Buffer[Point], headPose: mutable.Buffer[java.lang.Float], aiInfoList: ArrayBuffer[AiInfo]) = {
    val div_x = face(16).x - face(0).x
    // 眼角横向距离
    val div_y = face(16).y - face(0).y
    val center_x = face(0).x + div_x / 2.0
    val center_y = face(0).y + div_y / 2.0
    val slope = div_y.toDouble / div_x.toDouble
    val bias = center_y - slope * center_x
    val x_proj = (slope * (face(30).y - bias) + face(30).x) / (1 + math.pow(slope, 2))
    val y_proj = slope * x_proj + bias
    var diversion = math.sqrt(math.pow(x_proj - face(0).x, 2) + math.pow(y_proj - face(0).y, 2))
    var distance = math.sqrt(math.pow(face(16).x - face(0).x, 2) + math.pow(face(16).y - face(0).y, 2))

    val rate = 0.5

    x = (headPose(0) / 180) * 3.14 * rate + x * (1 - rate)
    y = (headPose(1) / 180) * 3.14 * rate + y * (1 - rate)
    z = (headPose(2) / 180) * 3.14 * rate + z * (1 - rate)


    // Ax+By+C/sqrt(A^2+B^2)
    y_angle = y_angle * (1 - rate) + math.asin(diversion / distance - 0.5) * 3.14 * rate

    // nose to eye around 1/6 head
    diversion = meter(slope, -1, bias, face(30).x, face(30).y)
    diversion = diversion + 1.0 / 6 * distance
    x_angle = x_angle * (1 - rate) + math.asin(diversion / distance) * 3.14 * rate

    z_angle = z_angle * (1 - rate) + math.atan(slope) * 3.14 * rate

    // eye
    left_eye = eye_open((face(36).x, face(36).y), (face(39).x, face(39).y), (face(37).x, face(37).y), (face(38).x, face(38).y), (face(40).x, face(40).y), (face(41).x, face(41).y), left_eye, rate)
    right_eye = eye_open((face(42).x, face(42).y), (face(45).x, face(45).y), (face(43).x, face(43).y), (face(44).x, face(44).y), (face(46).x, face(46).y), (face(47).x, face(47).y), right_eye, rate)

    // eyebrow
    eyebrow_left = eyebrow_move((face(17).x, face(17).y), (face(19).x, face(19).y), slope, eyebrow_left, rate)
    eyebrow_right = eyebrow_move((face(26).x, face(26).y), (face(24).x, face(24).y), slope, eyebrow_right, rate)

    // mouth
    diversion = math.sqrt(math.pow(face(62).x - face(66).x, 2) + math.pow(face(62).y - face(66).y, 2))
    distance = math.sqrt(math.pow(face(60).x - face(64).x, 2) + math.pow(face(60).y - face(64).y, 2))
    mouth_open = (diversion / distance - 0.15) * 2

    //移动3d模型
    //    val face_center = ((face.get(2).x+(face.get(14).x-face.get(2).x)/2).toInt, (face.get(2).y()+(face.get(14).y()-face.get(2).y())/2).toInt)

    //    import collection.JavaConverters._
    //    face.asJavaCollection
    //fixme 此处寻找中心可以寻找68关键点的框的中心
    //    val face_center = (face(33).x.toInt, face(33).y.toInt)
    var minx = 999999
    var miny = 999999
    var maxx = 0
    var maxy = 0
    for (p <- face) {
      if (p.x < minx) minx = p.x
      if (p.y < miny) miny = p.y
      if (p.x > maxx) maxx = p.x
      if (p.y > maxy) maxy = p.y
    }
    face_height = maxy - miny
    face_width = maxx - minx

    var face_y = 0
    val rbrate = 2
    //抬头低头，对模型中心点进行一些微小平移
    if (x > 0) {
      face_y = ((maxy + miny) / 2.0 - (face_height / 2.0 / Math.cos(x_angle) - face_height / 2.0) * rbrate).toInt
    } else {
      face_y = ((maxy + miny) / 2.0 + (face_height / 2.0 / Math.cos(x) - face_height / 2.0) * rbrate).toInt
    }
    //    face_y = ((maxy + miny) / 2.0 + (face_height / 2.0 / Math.cos(y) - face_height / 2.0) * rbrate).toInt
    val face_center = ((maxx + minx) / 2, face_y)
    //    log.info("w:"+face_width+",h:"+face_height)

    modelControlCallBack()


    RenderEngine.getRenderMat().foreach { m =>
      val fm = resizeMat(m, face_center._1, face_center._2)
      try {
        val sm = shiftMat(fm, Constants.DefaultPlayer.width / 2 - face_center._1, Constants.DefaultPlayer.height / 2 - face_center._2)
        aiInfoList.append(Ai3dModel(sm, sm))
      } catch {
        case e: Exception =>
          log.error(s"some error happened when shifMat" + e.getMessage)
      }

    }

  }

  private def shiftMat(src: Mat, col: Int, row: Int): Mat = {
    val temp = Mat.zeros(src.size(), src.`type`()).asMat()
    if (col > 0) {
      if (row > 0) {
        src.apply(new Rect(col, row, src.cols - col, src.rows() - row)).copyTo(temp.apply(new Rect(0, 0, src.cols() - col, src.rows() - row)))
      } else {
        src.apply(new Rect(col, 0, src.cols - col, src.rows() + row)).copyTo(temp.apply(new Rect(0, -row, src.cols() - col, src.rows() + row)))
      }
    } else {
      if (row > 0) {
        src.apply(new Rect(0, row, src.cols + col, src.rows() - row)).copyTo(temp.apply(new Rect(-col, 0, src.cols() + col, src.rows() - row)))
      } else {
        src.apply(new Rect(0, 0, src.cols + col, src.rows() + row)).copyTo(temp.apply(new Rect(-col, -row, src.cols() + col, src.rows() + row)))
      }
    }
    temp
  }

  private def resizeMat(src: Mat, face_xCenter: Int, face_yCenter: Int): Mat = {
    val temp = Mat.zeros(new Size(Constants.DefaultPlayer.width, Constants.DefaultPlayer.height), src.`type`()).asMat()
    val dm = src.clone()
    opencv_imgproc.resize(src, dm, new Size(0, 0), face_width / StandardFaceWidth, face_width / StandardFaceWidth, opencv_imgproc.INTER_AREA)
    if (face_width > StandardFaceWidth) {
      // 图片放大时 需要进行裁剪，
      dm.apply(new Rect((dm.cols() - Constants.DefaultPlayer.width) / 2, (dm.rows() - Constants.DefaultPlayer.height) / 2, Constants.DefaultPlayer.width, Constants.DefaultPlayer.height)).copyTo(temp.apply(new Rect(0, 0, Constants.DefaultPlayer.width, Constants.DefaultPlayer.height)))
    } else {
      //图片缩小时需要进行填充
      opencv_core.copyMakeBorder(dm, temp, (temp.rows() - dm.rows()) / 2, temp.rows() - dm.rows - (temp.rows() - dm.rows()) / 2, (temp.cols - dm.cols) / 2, temp.cols() - dm.cols() - (temp.cols - dm.cols) / 2, opencv_core.BORDER_CONSTANT)
    }
    temp
  }


}
