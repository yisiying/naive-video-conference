package org.seekloud.theia.faceAnalysis.jme3;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * 演示Jaime的骨骼
 * @author yanmaoyuan
 *
 */

// sdk测试
public class HelloSkeleton3 extends SimpleApplication {

    private final static String SmileUp = "smileUp";

    private final static String SmileDown = "smileDown";

    private final static String MouthOpen = "mouthOpen";

    private final static String MouthClose = "mouthClose";

    private final static  String HeadMove = "headMove";

    private final static String DebugInfo = "debugInfo";

    private final static String EyeOpen = "eyeOpen";

    private final static String EyeClose = "eyeClose";

    private final static float LipSideChangeMax = 0.025f;

    private final static float MouthChangeMax = 0.020f;

    private final static float EyeTopZChangeMax = -0.030f;

    private final static float EyeBottomZChangeMax = 0.030f;

    private final static float EyeTopYChangeMax = 0f;

    private Skeleton ske;

    private Node model;

    private Bone neck;
    private Bone head;
    private Bone jaw;
    private Bone lipBottomR;
    private Bone lipTopR;
    private Bone lipSideR;
    private Bone lipBottomL;
    private Bone lipTopL;
    private Bone lipSideL;
    private Bone eyeLidTopR;
    private Bone eyeLidTopL;
    private Bone eyeLidBottomR;
    private Bone eyeLidBottomL;

    private float smileSize = 0;
    private float mouthSize = 0;
    private float eyelidSize = 0;

    private int headXyzTest = 0;

    @Override
    public void simpleInitApp() {
        /**
         * 摄像机
         */
        cam.setLocation(new Vector3f(0f, 1.5f, 1f));
        cam.setRotation(new Quaternion(3.6091544E-5f, 0.99936426f, 0.03563703f, -0.0010121169f));
        flyCam.setEnabled(false);

        /**
         * 要有光
         */
        rootNode.addLight(new AmbientLight(new ColorRGBA(0.2f, 0.2f, 0.2f, 1f)));
        rootNode.addLight(new DirectionalLight(new Vector3f(0, -2, -3), new ColorRGBA(0.8f, 0.8f, 0.8f, 1f)));

        /**
         * 加载Jaime的模型
         */
        // 我们的模特：Jaime
        assetManager.registerLocator("faceAnalysis/model", FileLocator.class);
        Node scene = (Node) assetManager.loadModel("Models/ruyue_head.glb");

//        scene.getChildren().forEach((spa)-> System.out.println(spa.getName()));

        Node model = (Node) scene.getChild("root");
        // 将Jaime放大一点点，这样我们能观察得更清楚。
        model.scale(7f);
        rootNode.attachChild(model);

        // 获得SkeletonControl
        // 骨骼控制器
        SkeletonControl sc = model.getControl(SkeletonControl.class);

        ske = sc.getSkeleton();

        head = ske.getBone("head.x");
        head.setUserControl(true);
        jaw = ske.getBone("c_jawbone.x");
        jaw.setUserControl(true);
        lipSideL = ske.getBone("c_lips_smile.l");
        lipSideL.setUserControl(true);
        lipSideR = ske.getBone("c_lips_smile.r");
        lipSideR.setUserControl(true);
//        lipBottomL = ske.getBone("LipBottom.L");
//        lipBottomL.setUserControl(true);
//        lipBottomR = ske.getBone("LipBottom.R");
//        lipBottomR.setUserControl(true);
        eyeLidTopL = ske.getBone("c_eyelid_top.l");
        eyeLidTopL.setUserControl(true);
        eyeLidBottomL = ske.getBone("c_eyelid_bot.l");
        eyeLidBottomL.setUserControl(true);
        eyeLidTopR = ske.getBone("c_eyelid_top.r");
        eyeLidTopR.setUserControl(true);
        eyeLidBottomR = ske.getBone("c_eyelid_bot.r");
        eyeLidBottomR.setUserControl(true);

        inputManager.addMapping(SmileUp, new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping(SmileDown, new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping(MouthOpen, new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping(MouthClose, new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping(HeadMove, new KeyTrigger(KeyInput.KEY_B));
        inputManager.addMapping(EyeOpen, new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping(EyeClose, new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping(DebugInfo, new KeyTrigger(KeyInput.KEY_V));

        inputManager.addListener(actionListener,
                SmileUp, SmileDown, MouthOpen, MouthClose, EyeOpen, EyeClose, DebugInfo, HeadMove);
    }

    /**
     * 动作监听器
     */
    private ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (isPressed) {
                switch (name) {
                    case SmileUp:
                        smileSize += 0.1;
//                        lipSideChange(smileSize);
                        model.setLocalTranslation(0,smileSize*10,0);
                        break;
                    case SmileDown:
                        smileSize -= 0.1;
//                        lipSideChange(smileSize);
                        model.setLocalTranslation(0,smileSize*10,0);
                        break;
                    case MouthOpen:
                        mouthSize += 0.1;
                        mouthChange(mouthSize);
                        break;
                    case MouthClose:
                        mouthSize -= 0.1;
                        mouthChange(mouthSize);
                        break;

                    case EyeOpen:
                        eyelidSize += 0.1;
                        eyeLidChange(eyelidSize);
                        break;

                    case EyeClose:
                        eyelidSize -= 0.1;
                        eyeLidChange(eyelidSize);
                        break;

                    case HeadMove:
                        if(headXyzTest == 0){
                            headMove(FastMath.PI/4,0,0);
                            headXyzTest = 1;
                        } else if (headXyzTest == 1){
                            headMove(0, FastMath.PI/4, 0);
                            headXyzTest = 2;
                        } else {
                            headMove(0 , 0, FastMath.PI/4);
                            headXyzTest = 0;
                        }
                        break;

                    case DebugInfo:
                        Vector3f location = cam.getLocation();
                        Quaternion rotation = cam.getRotation();
                        System.out.println(location + "," + rotation);
                        break;

                    default:
                        break;

                }
            }

        }
    };

    public void lipSideChange(float changeSize) {
        float size = changeSize * LipSideChangeMax;
        lipSideR.setUserTransforms(new Vector3f(size, size, 0), Quaternion.IDENTITY, Vector3f.UNIT_XYZ);
        lipSideL.setUserTransforms(new Vector3f(-size, size, 0), Quaternion.IDENTITY, Vector3f.UNIT_XYZ);
        ske.updateWorldVectors();
    }

    public void mouthChange(float changeSize) {
        System.out.println(changeSize);

        Vector3f vec = new Vector3f(0, changeSize * MouthChangeMax, 0);
        jaw.setUserTransforms(vec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ);
        ske.updateWorldVectors();
    }

    public void headMove(float xAngle, float yAngle, float zAngle) {
        Quaternion move = new Quaternion();
        move.fromAngles(xAngle, yAngle, zAngle);
        head.setUserTransforms(Vector3f.ZERO, move, Vector3f.UNIT_XYZ);
        ske.updateWorldVectors();
    }

    private void eyeLidChange(float changeSize) {
        Vector3f topVec = new Vector3f(0, changeSize * EyeTopYChangeMax, changeSize * EyeTopZChangeMax);
        Vector3f botVec = new Vector3f(0, 0, changeSize * EyeBottomZChangeMax);

        System.out.println(changeSize);

        eyeLidTopL.setUserTransforms(topVec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ);
        eyeLidBottomL.setUserTransforms(botVec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ);

        eyeLidTopR.setUserTransforms(topVec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ);
        eyeLidBottomR.setUserTransforms(botVec, Quaternion.IDENTITY, Vector3f.UNIT_XYZ);

        ske.updateWorldVectors();
    }

    public static void main(String[] args) {
        HelloSkeleton3 app = new HelloSkeleton3();
        app.start();
    }

}
