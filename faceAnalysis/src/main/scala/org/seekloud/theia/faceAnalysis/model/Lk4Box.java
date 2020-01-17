package org.seekloud.theia.faceAnalysis.model;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.util.ArrayUtil;
import java.util.Arrays;
import java.util.Vector;

/**
 * Created by sky
 * Date on 2019/10/31
 * Time at 下午4:14
 */
public class Lk4Box {
    private static float diff_thres = 5;
    private static float iou_thres = 0.5f;
    private static float smooth_box = 0.3f;
    private static int top_k = 1;

    public static boolean diff_frames(Mat previous_image, Mat image) {
        if (previous_image == null) {
            return true;
        } else {
            Mat dst = new Mat();
            opencv_core.absdiff(previous_image, image, dst);
            Scalar sl = opencv_core.sumElems(dst);
            double dif = (sl.blue() + sl.green() + sl.red()) / TfModelFaceDetect.height / TfModelFaceDetect.width / TfModelFaceDetect.channel;
            if (dif > diff_thres) {
                return true;
            } else {
                return false;
            }
        }
    }

    private static float iou(float[] rec1, float[] rec2) {
        // computing area of each rectangles
        float S_rec1 = (rec1[2] - rec1[0]) * (rec1[3] - rec1[1]);
        float S_rec2 = (rec2[2] - rec2[0]) * (rec2[3] - rec2[1]);
        // computing the sum_area
        float sum_area = S_rec1 + S_rec2;
        // find the each edge of intersect rectangle
        float x1 = Math.max(rec1[0], rec2[0]);
        float y1 = Math.max(rec1[1], rec2[1]);
        float x2 = Math.min(rec1[2], rec2[2]);
        float y2 = Math.min(rec1[3], rec2[3]);
        // judge if there is an intersect
        float intersect = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        return intersect / (sum_area - intersect);
    }

    private static float[] do_moving_average(float[] p_now,float[] p_previous){
        //fixme 是否要使用矩阵
        return Nd4j.create(p_now).muli(smooth_box).addi(Nd4j.create(p_previous).muli(1-smooth_box)).toFloatVector();
    }

    public static Vector<float[]> judge_boxes(Vector<float[]> previous_boxes, Vector<float[]> boxes) {
        if (previous_boxes == null || previous_boxes.size() < 1) {
            return boxes;
        } else {
            Vector<float[]> result = new Vector<float[]>(100);
            for (float[] now_box:boxes){
                boolean contain = false;
                for(float[] previous_box:previous_boxes){
                    if(iou(now_box,previous_box)>iou_thres){
                        result.add(do_moving_average(now_box,previous_box));
                        contain = true;
                        break;
                    }
                }
                if(!contain){
                    result.add(now_box);
                }
            }
            return result;
        }
    }

    public static Vector<float[]> sort(Vector<float[]> boxes){
        if(boxes.size()<top_k){
            return boxes;
        }else {
            int[] ar = new int[boxes.size()];
            for(int i=0;i<boxes.size();i++){
                float[] box = boxes.get(i);
                float box_width = box[2] - box[0];
                float box_height = box[3] - box[1];
                ar[i] =(int) -(box_height*box_width);
            }
//        System.out.println(Arrays.toString(ar));
            int[] arg=Arrays.copyOfRange(ArrayUtil.argsort(ar),0,top_k);
//        System.out.println(Arrays.toString(arg));
            Vector<float[]> result = new Vector<float[]>(top_k);
            for(int i:arg){
                result.add(boxes.get(i));
            }
//        result.forEach(l-> System.out.println(Arrays.toString(l)));
            return result;
        }
    }

    public static void main(String[] args) {
        Vector<float[]> boxes = new Vector<float[]>(3);
        float[] x1= {1000,2000,3000,4000};
        boxes.add(x1);
        float[] x3= {2000,3000,4000,5000};
        boxes.add(x3);
        float[] x2= {2000,3000,300,4000};
        boxes.add(x2);
        float[] x4= {2000,3000,3000,4000};
        boxes.add(x4);

        sort(boxes);
    }
}
