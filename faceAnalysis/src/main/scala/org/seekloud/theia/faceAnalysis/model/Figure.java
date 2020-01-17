package org.seekloud.theia.faceAnalysis.model;

import org.bytedeco.opencv.opencv_core.Point;

import java.util.Vector;

/**
 * User: gaohan
 * Date: 2019/11/13
 * Time: 21:46
 * 面部数据特征  脸部关键点和头部姿态
 */
public class Figure {

    Vector<Point> landmarks;
    Vector<Float> headPose;

    Figure(Vector<Point> landmark, Vector<Float> headpose){
      landmarks = landmark;
      headPose = headpose;
    }

    public Vector<Point> getLandmarks() {
      return landmarks;
    }

    public void setLandmarks(Vector<Point> landmarks) {
      this.landmarks = landmarks;
    }

    public Vector<Float> getHeadPose() {
      return headPose;
    }

    public void setHeadPose(Vector<Float> headPose) {
      this.headPose = headPose;
    }
}
