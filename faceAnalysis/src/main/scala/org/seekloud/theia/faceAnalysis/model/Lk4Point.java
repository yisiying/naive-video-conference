package org.seekloud.theia.faceAnalysis.model;

import org.bytedeco.opencv.opencv_core.Point;
import org.seekloud.theia.faceAnalysis.common.AppSettings;

import java.util.Vector;

public class Lk4Point {

    public static Vector<Vector<Point>> calculate(Vector<Vector<Point>> preLandmark, Vector<Vector<Point>> nowLandmark) {
        if (preLandmark==null || preLandmark.size() == 0){
            return nowLandmark;
        }else{
            Vector<Vector<Point>> result = new Vector<Vector<Point>>(10);
            for (int i = 0; i < nowLandmark.size(); i++) {
                boolean not_in_flag = true;
                for (int j = 0; j < preLandmark.size(); j++) {
                    double res = iou(preLandmark.get(j), nowLandmark.get(i));
                    if ( res > AppSettings.iou_thres()) {
                        result.add(smooth(preLandmark.get(j), nowLandmark.get(i), res));
                        not_in_flag = false;
                        break;
                    }
                }
                if (not_in_flag){
                    result.add(nowLandmark.get(i));
                }
            }
            return result;
        }
    }

    private static double iou(Vector<Point> preLandmark, Vector<Point> nowLandmark) {
        double[] rec4now = { getMin(nowLandmark).x(), getMin(nowLandmark).y(), getMax(nowLandmark).x(), getMax(nowLandmark).y() };
        double[] rec4pre = { getMin(preLandmark).x(), getMin(preLandmark).y(), getMax(preLandmark).x(), getMax(preLandmark).y() };

        double rec4nowS = (rec4now[2] - rec4now[0]) * (rec4now[3] - rec4now[1]);
        double rec4preS = (rec4pre[2] - rec4pre[0]) * (rec4pre[3] - rec4pre[1]);

        double sumArea = rec4nowS + rec4preS;

        double x1 = Math.max(rec4now[0], rec4pre[0]);
        double y1 = Math.max(rec4now[1], rec4pre[1]);
        double x2 = Math.min(rec4now[2], rec4pre[2]);
        double y2 = Math.min(rec4now[3], rec4pre[3]);

        double intersect = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);

        double result = intersect / (sumArea - intersect);

        return result;
    }

    private static Vector<Point> smooth(Vector<Point> preLandmark, Vector<Point> nowLandmark, double alpha) { //pre和now的array长度都是68
        Vector<Point> lk = new Vector<>();
        for (int i = 0; i < nowLandmark.size(); i++) {
            double dis = Math.sqrt((nowLandmark.get(i).x() - preLandmark.get(i).x()) * (nowLandmark.get(i).x() - preLandmark.get(i).x()) + (nowLandmark.get(i).y() - preLandmark.get(i).y()) * (nowLandmark.get(i).y() - preLandmark.get(i).y()));
            if (dis < AppSettings.thres()) {
                lk.add(preLandmark.get(i));
            } else {
                lk.add(doMovingAverage(nowLandmark.get(i), preLandmark.get(i), alpha));
            }
        }
        return lk;
    }

    private static Point doMovingAverage(Point nowPoint, Point prePoint, double alpha) {
        float x = (float)(alpha * nowPoint.x() + (1 - alpha) * prePoint.x());
        float y = (float)(alpha * nowPoint.y() + (1 - alpha) * prePoint.y());
        return new Point((int) x,(int) y);
    }

    public static Vector<float[]> getBox(Vector<Point> landmark, Vector<float[]> boxs) {

        double x_min = getMin(landmark).x();
        double x_max = getMax(landmark).x();
        double y_min = getMin(landmark).y();
        double y_max = getMax(landmark).y();

        double[] rect = {x_min, y_min, x_max, y_max};

        double center_X = (rect[2] - rect[0]) / 2 + x_min ;
        double center_Y = (rect[3] - rect[1]) / 2 + y_min;

        Point center = new Point((int) center_X, (int) center_Y );  //得到框的中心点

        float[] box = new float[4];

        box[0] = convertToFloat(rect[0]);
        box[1] = convertToFloat(rect[1]);
        box[2] = convertToFloat(rect[2]);
        box[3] = convertToFloat(rect[3]);

        boxs.add(box);
        return boxs;
    }

    private static Float convertToFloat(Double doubleValue) {
        return doubleValue == null ? null : doubleValue.floatValue();
    }


    private static Point getMax(Vector<Point> array) {
        float x = 0f;
        float y = 0f;
        for (int i = 0; i < array.size(); i++) {
            float xnow = array.get(i).x();
            if (x < xnow)
                x = xnow;
        }
        for (int i = 0; i < array.size(); i++) {
            float ynow = array.get(i).y();
            if (y < ynow)
                y = ynow;
        }
        return new Point((int) x,(int) y);
    }

    private static Point getMin(Vector<Point> array) {
        float x = 999999f;
        float y = 999999f;
        for (int i = 0; i < array.size(); i++) {
            float xnow = array.get(i).x();
            if (x > xnow)
                x = xnow;
        }
        for (int i = 0; i < array.size(); i++) {
            float ynow = array.get(i).y();
            if (y > ynow)
                y = ynow;
        }
        return new Point((int) x,(int) y);
    }
}
