package org.seekloud.theia.faceAnalysis.model;

import org.apache.commons.io.IOUtils;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_calib3d;
import org.bytedeco.opencv.opencv_core.*;
import org.opencv.core.CvType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.seekloud.theia.faceAnalysis.common.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;

import java.io.FileInputStream;
import java.nio.FloatBuffer;
import java.util.*;

import org.bytedeco.opencv.global.opencv_core;


/**
 * Created by sky
 * Date on 2019/10/29
 * Time at 下午4:02
 */
class TfModelLandmark {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private Graph graph;
    private Session session;

    private static int min_face = 20;
    private static int point_num = 68 * 2;
    private static float[] base_extend_range = {0.2f, 0.3f};
    private static Size size = new Size(160, 160);
    public static int batch = 1;
    public static int width = 160;
    public static int height = 160;
    public static int channel = 3;
    public static int length = width * height * channel;
    public static long[] shape = new long[]{batch, height, width, channel};

    private static String PB_FILE_PATH = AppSettings.path()+"keypoints/shufflenet/keypoints.pb";
    private static String INPUT_TENSOR_NAME = "tower_0/images:0";
    private static String OUTPUT_TENSOR_NAME = "tower_0/prediction:0";

    private Tensor flag = Tensor.create(false);
    //fixme 此处可以考虑公用内存空间
    private FloatBuffer floatBuffer = FloatBuffer.allocate(length);

    TfModelLandmark() {
        log.info("init with tf-version-{}", TensorFlow.version());
        try {
            graph = new Graph();
            byte[] graphBytes = IOUtils.toByteArray(new FileInputStream(PB_FILE_PATH));
            graph.importGraphDef(graphBytes);
            session = new Session(graph);
        } catch (Exception e) {
            log.error("init error {}", e.getMessage());
        } finally {
            log.info("init success");
        }
    }

    private MarkFeature simple_predict(Mat r_crop_img) {
        long t1 = System.currentTimeMillis();
        floatBuffer.clear();
        Java2DConverter.mat2FloatBuffer(floatBuffer, r_crop_img, length);
        floatBuffer.flip();
        Tensor img = Tensor.create(shape, floatBuffer);
        List<Tensor<?>> out = session.runner()
                .feed(INPUT_TENSOR_NAME, img)
                .feed("training_flag:0", flag)
                .fetch(OUTPUT_TENSOR_NAME).run();

        //fixme java->scala 在此处存在问题
//        log.debug("simple_predict use {}", System.currentTimeMillis() - t1);
        INDArray embeddings = Nd4j.create(out.get(0).copyTo(new float[1][143])).reshape(143);
        INDArray landmark = embeddings.get(NDArrayIndex.interval(0, point_num)).reshape(68,2).transpose();
        INDArray headPose = embeddings.get(NDArrayIndex.interval(point_num, point_num + 3)).mul(90);
        //fixme 内存问题
//        embeddings.close();
        return new MarkFeature(landmark, headPose);
    }

    private Figure ontShotRun(Mat mat, float[] bbox) {
        float bbox_width = bbox[2] - bbox[0];
        float bbox_height = bbox[3] - bbox[1];
        if (bbox_width <= min_face || bbox_height <= min_face) {
            return null;
        } else {
            int add = (int) Math.max(bbox_width, bbox_height);
            Mat bimg = new Mat();
            opencv_core.copyMakeBorder(mat, bimg, add, add, add, add, opencv_core.BORDER_CONSTANT, opencv_core.RGB(123, 116, 103));

            //fixme 避免矩阵运算
            bbox = Nd4j.create(bbox).add(add).toFloatVector();

            int half_edge = ((int) ((1 + 2 * base_extend_range[0]) * bbox_width)) >> 1;
            int[] center = {(int) (bbox[0] + bbox[2]) / 2, (int) (bbox[1] + bbox[3]) / 2};

            bbox[0] = center[0] - half_edge;
            bbox[1] = center[1] - half_edge;
            bbox[2] = center[0] + half_edge;
            bbox[3] = center[1] + half_edge;

            /**
             * (0,1)  (2-0,3-1)
             * */
            // 截部分图
            Mat crop_image = new Mat(bimg, new Rect((int) bbox[0], (int) bbox[1], (int) (bbox[2] - bbox[0]), (int) (bbox[3] - bbox[1])));

            int h = crop_image.rows();
            int w = crop_image.cols();
            Mat r_crop_img = new Mat();
            opencv_imgproc.resize(crop_image, r_crop_img, size);

            MarkFeature mf = simple_predict(r_crop_img);

//            System.out.println(Arrays.toString(mf.landmark_x.toFloatVector()));
            mf.landmark_x.muli(w).divi(size.width()).muli(size.height()).addi(bbox[0]).subi(add);
            mf.landmark_y.muli(h).divi(size.height()).muli(size.width()).addi(bbox[1]).subi(add);

            Vector<Point> lm = mf.landMark();
            Vector<Float> hp = mf.headpose();
            return new Figure(lm, hp);
        }
    }

    public Vector<Figure> predict(Mat mat, Vector<float[]> boxs) {
        Vector<Vector<Point>> mark = new Vector<Vector<Point>>(boxs.size());
        Vector<Vector<Float>> headPose = new Vector<Vector<Float>>(boxs.size());


        Vector<Figure> figures = new Vector<Figure>(boxs.size());

        for (float[] bbox : boxs) {
           Figure a  = ontShotRun(mat, bbox);
           if(a != null){
               figures.add(a);
           }
        }
        return figures;
    }

    //计算头部姿态,
//    public Vector<Double> getHeadpose(Vector<Point> landmark, int imgWidth, int imgHeight ) {
//        // 选取的模型的三维数据点
//        int[][] objects  = {{0, 0, 0}, {0, -330, -65}, {-225, 170, -135}, {225, 170, -135},{-150, -150, -125}, {150, -150, -125}};
//        Mat object_mat = new Mat(1,6,CvType.CV_8UC3 );
//        Java2DConverter.array2mat(objects,object_mat );
//
//        Mat rotation = new Mat(1,3,CvType.CV_8UC1);
//        Mat translation = new Mat(1,3, CvType.CV_8UC1);
//        //System.out.println("tra======="+translation.cols() + translation.rows());
//
//        Mat camera = new Mat(1,3, CvType.CV_8UC3);
//        int[][] camera_arr = {{imgWidth, 0, imgWidth / 2} ,{ 0, imgWidth, imgHeight/2}, {0,0,1}};
//        Java2DConverter.array2mat(camera_arr, camera);
//
//        Mat dist = new Mat(1,5, CvType.CV_8UC1 );
//        int[][] dist_arr = {{0},{0}, {0}, {0}, {0}};
//        Java2DConverter.arraymat(dist_arr, dist);
//
//
//      //选取二维的关键点数据
//        Mat image_point = new Mat(1,6, CvType.CV_8UC2);
//        int[][] image_arr = {{landmark.elementAt(30).x(), landmark.elementAt(30).y()},
//                             {landmark.elementAt(8).x(), landmark.elementAt(8).y()},
//                             {landmark.elementAt(36).x(), landmark.elementAt(36).y()},
//                             {landmark.elementAt(45).x(), landmark.elementAt(45).y()},
//                             {landmark.elementAt(48).x(), landmark.elementAt(48).y()},
//                             {landmark.elementAt(54).x(), landmark.elementAt(54).y()},};
//        Java2DConverter.array1mat(image_arr, image_point);
//
//
//
//      // 得到旋转向量
//        opencv_calib3d.solvePnP(object_mat, image_point, camera, dist, rotation, translation);
//
//        opencv_calib3d.projectPoints(object_mat, rotation, translation, camera, dist);
//
//
//        //根据旋转向量计算旋转角
//       double theta = opencv_core.norm(rotation);
//       double w = Math.cos(theta / 2);
//       double x = Math.sin(theta / 2) * rotation.data().get(0) / theta;
//       double y = Math.sin(theta / 2) * rotation.data().get(1) / theta;
//       double z = Math.sin(theta / 2) * rotation.data().get(2) / theta;
//
//       //pitch  x-axis rotation
//      double ysqr = y * y;
//      double t0 = 2.0 * (w * x + y * z);
//      double t1 = 1.0 - 2.0 * (x * x + ysqr);
//      double pitch = Math.atan2(t0, t1);
//
//       //yaw y-axis rotation
//      double t2 = 2.0 * (w * y - z * x);
//      if (t2 > 1.0) {
//        t2 = 1.0;
//      }
//      if (t2 < -1.0){
//        t2 = -1.0;
//      }
//      double yaw = Math.asin(t2);
//
//       //roll z-axis rotation
//      double t3 = 2.0 * (w * z + x * y);
//      double t4 = 1.0 - 2.0 * (ysqr + z * z);
//      double roll = Math.atan2(t3, t4);
//
//       //单位转换，弧度转换为角度
//      double Y =(pitch / 3.14) * 180;
//      double X =(yaw / 3.14) * 180;
//      double Z =(roll / 3.14) * 180;
//
//      Vector<Double> headPose = new Vector<Double>(3);
//      headPose.add(X);
//      headPose.add(Y);
//      headPose.add(Z);
//
//      return headPose;
//
//
//
//  }
}

class MarkFeature {
    INDArray landmark_x;
    INDArray landmark_y;
    INDArray headPose;

    MarkFeature(INDArray l, INDArray h) {
        landmark_x = l.get(NDArrayIndex.indices(0));
        landmark_y = l.get(NDArrayIndex.indices(1));
        headPose = h;
//        System.out.println(Arrays.toString(landmark_x.shape()));
//        System.out.println(Arrays.toString(landmark_y.shape()));
//        System.out.println(Arrays.toString(headPose.shape()));
    }

    public Vector<Point> landMark() {
        Vector<Point> pl = new Vector<Point>(68);
        for (int i = 0; i < 68; i++) {
            pl.add(new Point((int) landmark_x.getFloat(i), (int) landmark_y.getFloat(i)));
        }
        //fixme 内存问题
        landmark_x.close();
        landmark_y.close();
        //headPose.close();
        return pl;
    }
    public Vector<Float> headpose() {
        Vector<Float> hp = new Vector<Float>(3);
        for(int i = 0; i < 3; i ++){
            hp.add(headPose.getFloat(i));
        }
        landmark_x.close();
        landmark_y.close();
        headPose.close();
        return hp;

    }

}

