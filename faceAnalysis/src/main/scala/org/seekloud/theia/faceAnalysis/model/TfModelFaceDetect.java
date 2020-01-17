package org.seekloud.theia.faceAnalysis.model;

import org.apache.commons.io.IOUtils;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.global.opencv_imgproc;
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
import java.util.List;
import java.util.Vector;

/**
 * Created by sky
 * Date on 2019/10/29
 * Time at 下午1:40
 */
public class TfModelFaceDetect {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    Graph graph;
    Session session;

    private static String PB_FILE_PATH = AppSettings.path() +"faceboxes/detector.pb";
    private static String INPUT_TENSOR_NAME = "tower_0/images:0";
    private static String OUTPUT_TENSOR_NAME1 = "tower_0/boxes:0";
    private static String OUTPUT_TENSOR_NAME2 = "tower_0/scores:0";
    private static String OUTPUT_TENSOR_NAME3 = "tower_0/num_detections:0";
    private Tensor flag = Tensor.create(false);
    private static float thres = 0.5f;
    private static int batch = 1;
    public static int width = 512;
    public static int height = 512;
    public static int channel = 3;
    public static Size size = new Size(width, height);
    private static int length = width * height * channel;
    private static long[] shape = new long[]{batch, height, width, channel};
    private FloatBuffer floatBuffer = FloatBuffer.allocate(length);

    public TfModelFaceDetect() {
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

    private Vector<float[]> simple_predict(ReShapeImg reShapeImg) {
        long t1 = System.currentTimeMillis();

        floatBuffer.clear();
        Java2DConverter.mat2FloatBuffer(floatBuffer, reShapeImg.rs_mat, length);
        floatBuffer.flip();

        Tensor data = Tensor.create(shape, floatBuffer);
        List<Tensor<?>> out = session.runner()
                .feed(INPUT_TENSOR_NAME, data)
                .feed("training_flag:0", flag)
                .fetch(OUTPUT_TENSOR_NAME1).fetch(OUTPUT_TENSOR_NAME2).fetch(OUTPUT_TENSOR_NAME3).run();

//        System.out.print("face detect use:");
//        System.out.println(System.currentTimeMillis() - t1);
        INDArray layer1 = Nd4j.create(out.get(0).copyTo(new float[batch][100][4])).reshape(100, 4);
        INDArray layer2 = Nd4j.create(out.get(1).copyTo(new float[batch][100])).reshape(100);
//        System.out.println(Arrays.toString(Nd4j.create(out.get(0).copyTo(new float[batch][100][4])).reshape(100,4).toFloatMatrix()));
//        System.out.println(Arrays.toString(Nd4j.create(out.get(1).copyTo(new float[batch][100])).reshape(100).toFloatVector()));
//        System.out.println(Arrays.toString(Nd4j.create(out.get(2).copyTo(new int[batch])).toIntVector()));

        int layer3 = out.get(2).copyTo(new int[batch])[0];
//        System.out.print("box_size:");
//        System.out.println(layer3);

//        System.out.println(Arrays.toString(layer1.shape()));
        layer1 = layer1.get(NDArrayIndex.interval(0, layer3));
//        System.out.println(Arrays.toString(layer1.shape()));

//        System.out.println(Arrays.toString(layer2.shape()));
        layer2 = layer2.get(NDArrayIndex.interval(0, layer3));
//        System.out.println(Arrays.toString(layer2.shape()));
        float[] s = {height / reShapeImg.scale_y, width / reShapeImg.scale_x, height / reShapeImg.scale_y, width / reShapeImg.scale_x};
        INDArray scaler = Nd4j.create(s);

//        System.out.println(Arrays.toString(s));

        Vector<float[]> boxs = new Vector<>(1);
        for (int i = 0; i < 100; i++) {
            if (layer2.getFloat(i) > thres) {
                float[] box = new float[4];
//                System.out.println(Arrays.toString(layer1.getRow(i).toFloatVector()));
                layer1.getRow(i).muli(scaler);
                box[0] = layer1.getRow(i).getFloat(1);
                box[1] = layer1.getRow(i).getFloat(0);
                box[2] = layer1.getRow(i).getFloat(3);
                box[3] = layer1.getRow(i).getFloat(2);
//                System.out.println(Arrays.toString(box));
                boxs.add(box);
            }
        }

        //fixme 内存问题
        layer1.close();
        layer2.close();
        scaler.close();
        return boxs;
    }

    private Mat previous_image;
    private Vector<float[]> trick_boxes;
    private Vector<Vector<Point>> preLandmark;
    private Vector<float[]> tmp_box;

    private TfModelLandmark tfModelLandmark = new TfModelLandmark();

    public Vector<Figure> predict(Mat mat) {
        Vector<float[]> boxes;
        if (Lk4Box.diff_frames(previous_image, mat)) {
            boxes = simple_predict(new ReShapeImg(mat));
            boxes = Lk4Box.judge_boxes(trick_boxes, boxes);
        } else {
            boxes = trick_boxes;
        }
        if (previous_image==null){
            previous_image = mat.clone();  //fixme 减少内存消耗
        }else {
            mat.copyTo(previous_image);
        }

        boxes = Lk4Box.sort(boxes);


        Vector<Figure> figures = tfModelLandmark.predict(mat,boxes);

//        landmarks.forEach(lm-> {
//            System.out.print("landmark-point.size:");
//            System.out.println(lm.size());
//        });
//
        Vector<Vector<Point>> landmarks = new Vector<Vector<Point>>(figures.size());

        for (Figure figure : figures) {
          landmarks.add(figure.getLandmarks());
        }

        landmarks = Lk4Point.calculate(preLandmark,landmarks);

        for (int i = 0; i < landmarks.size(); i++) {
            figures.get(i).setLandmarks(landmarks.get(i));
        }
        preLandmark = landmarks;

       //todo 根据返回调整检测框
        Vector<float[]> tmp_boxs = new Vector<float[]>(landmarks.size());
        landmarks.forEach(l-> {
          tmp_box =  Lk4Point.getBox(l,tmp_boxs);
        });

        boxes = Lk4Box.judge_boxes(boxes, tmp_box);

        trick_boxes = boxes;

        return figures;
    }



    public static void main(String[] args) {
        Mat mat = opencv_imgcodecs.imread("D:/pythonWorkSpace/Peppa_Pig_Face_Engine-master/img/test_0.jpg");
        TfModelFaceDetect tfModelFaceDetect = new TfModelFaceDetect();
//        tfModelFaceDetect.predict(mat).forEach(vp->vp.forEach(p->{
//            opencv_imgproc.circle(mat, p, 2, opencv_core.RGB(255, 255, 0));
//        }));
        opencv_imgcodecs.imwrite("test_0.jpg",mat);
    }
}

class ReShapeImg {
    Mat rs_mat;
    float scale_x;
    float scale_y;

    ReShapeImg(Mat src) {
        int h = src.rows();
        int w = src.cols();
        int long_side = Math.max(h, w);
        scale_y = (float) TfModelFaceDetect.height / long_side;
        scale_x = scale_y;
        rs_mat = new Mat(TfModelFaceDetect.height, TfModelFaceDetect.width, opencv_core.CV_8UC3, opencv_core.RGB(123, 116, 103));
        Mat rm = new Mat();
        opencv_imgproc.resize(src, rm, new Size((int) (w * scale_x), (int) (h * scale_y)));

//        System.out.println(rm.cols());
//        System.out.println(rm.rows());
//        System.out.println(rs_mat.cols());
//        System.out.println(rs_mat.rows());

        rm.copyTo(rs_mat.apply(new Rect(0, 0, rm.cols(), rm.rows())));

//        opencv_imgcodecs.imwrite("box_r1.jpg",rs_mat);
    }
}
