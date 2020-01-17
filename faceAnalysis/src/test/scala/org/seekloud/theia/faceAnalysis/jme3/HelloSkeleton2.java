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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import static com.jme3.math.FastMath.PI;

/**
 * 演示Jaime的骨骼
 *
 * @author yanmaoyuan
 */

//各部位 max 调试
public class HelloSkeleton2 extends SimpleApplication {

    private final static String DebugInfo = "debugInfo";

    private final static String KeyR = "keyR";

    private final static String KeyF= "keyF";

    private final static String KeyT = "keyT";

    private final static String KeyG = "keyG";

    private final static String KeyY = "keyY";

    private final static String KeyH = "keyH";

    private final static String KeyU = "keyU";

    private final static String KeyJ = "keyJ";

    private final static String KeyI = "keyI";

    private final static String KeyK = "keyK";

    private final static String KeyO = "keyO";

    private final static String KeyL = "keyL";

    private Skeleton ske;

    private Bone neck;
    private Bone head;
    private Bone iKJawTarget;
    private Bone jaw;
    private Bone lipBottomR;
    private Bone lipTopR;
    private Bone lipSideR;
    private Bone lipBottomL;
    private Bone lipTopL;
    private Bone lipSideL;
    private Bone eyebrowR;
    private Bone eyebrowL;
    private Bone cheekR;
    private Bone cheekL;
    private Bone eyeLidTopR;
    private Bone eyeLidTopL;
    private Bone eyeLidBottomR;
    private Bone eyeLidBottomL;
    private Bone sightTarget;
    private Bone iKEyeTargetR;
    private Bone iKEyeTargetL;
    private Bone eyeL;
    private Bone eyeR;

    private Vector3f neckV = new Vector3f(); //颈
    private Vector3f headV = new Vector3f(); //头

    private Vector3f ijtV = new Vector3f(); //下巴指向
    private Vector3f jawV = new Vector3f(); //下巴

    private Vector3f lsrV = new Vector3f(); //右嘴角
    private Vector3f lslV = new Vector3f(); //左嘴角
    private Vector3f lbrV = new Vector3f(); //右下唇
    private Vector3f lblV = new Vector3f(); //左下唇

    private Vector3f eb1rV = new Vector3f(); //右眉毛
    private Vector3f eb2rV = new Vector3f();
    private Vector3f eb3rV = new Vector3f();
    private Vector3f eb1lV = new Vector3f(); //左眉毛
    private Vector3f eb2lV = new Vector3f();
    private Vector3f eb3lV = new Vector3f();

    private Vector3f eltrV = new Vector3f(); //左上眼皮
    private Vector3f elbrV = new Vector3f(); //左下眼皮
    private Vector3f eltlV = new Vector3f(); //右上眼皮
    private Vector3f elblV = new Vector3f(); //右下眼皮

    private Vector3f stV = new Vector3f(); //视线

    private Vector3f erV = new Vector3f(); //左眼
    private Vector3f elV = new Vector3f(); //右眼

    private Quaternion rotation = new Quaternion();

    @Override
    public void simpleInitApp() {
        /**
         * 摄像机
         */
//        cam.setLocation(new Vector3f(8.896082f, 12.328749f, 13.69658f));
//        cam.setRotation(new Quaternion(-0.09457599f, 0.9038204f, -0.26543108f, -0.32204098f));
//        flyCam.setMoveSpeed(10f);
        cam.setLocation(new Vector3f(0f, 1.5f, 1f));
        cam.setRotation(new Quaternion(0f, 1f, 0f, 0.0068f));
        flyCam.setMoveSpeed(10f);
//        flyCam.setEnabled(false);

        /**
         * 要有光
         */
        rootNode.addLight(new AmbientLight(new ColorRGBA(0.2f, 0.2f, 0.2f, 1f)));
        rootNode.addLight(new DirectionalLight(new Vector3f(0f, -2f, -3f), new ColorRGBA(0.8f, 0.8f, 0.8f, 1f)));

        /**
         * 加载Jaime的模型
         */
        // 我们的模特：Jaime
        assetManager.registerLocator("faceAnalysis/model", FileLocator.class);
        Node scene = (Node) assetManager.loadModel("Models/ruyue_head.glb");

//        scene.getChildren().forEach((spa)-> System.out.println(spa.getName()));

        Node model = (Node) scene.getChild("root");
        // 将Jaime放大一点点，这样我们能观察得更清楚。
        model.scale(3f);
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
        eyebrowL = ske.getBone("c_eyebrow_full.l");
        eyebrowL.setUserControl(true);
        eyebrowR = ske.getBone("c_eyebrow_full.r");
        eyebrowR.setUserControl(true);
        eyeLidTopL = ske.getBone("c_eyelid_top.l");
        eyeLidTopL.setUserControl(true);
        eyeLidBottomL = ske.getBone("c_eyelid_bot.l");
        eyeLidBottomL.setUserControl(true);
        eyeLidTopR = ske.getBone("c_eyelid_top.r");
        eyeLidTopR.setUserControl(true);
        eyeLidBottomR = ske.getBone("c_eyelid_bot.r");
        eyeLidBottomR.setUserControl(true);

        inputManager.addMapping(KeyR, new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping(KeyF, new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping(KeyT, new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping(KeyG, new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping(KeyY, new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping(KeyH, new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping(KeyU, new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping(KeyJ, new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping(KeyI, new KeyTrigger(KeyInput.KEY_I));
        inputManager.addMapping(KeyK, new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping(KeyO, new KeyTrigger(KeyInput.KEY_O));
        inputManager.addMapping(KeyL, new KeyTrigger(KeyInput.KEY_L));
        inputManager.addMapping(DebugInfo, new KeyTrigger(KeyInput.KEY_V));

        inputManager.addListener(actionListener,
                KeyR, KeyF, KeyT, KeyG, KeyY, KeyH, KeyU, KeyJ, KeyI, KeyK, KeyO, KeyL, DebugInfo);
    }

    /**
     * 动作监听器
     */
    private ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (isPressed) {
                switch (name) {
                    case KeyR:
                        eyeLidChange(new Vector3f(0.001f, 0.0f, 0.0f), Vector3f.ZERO);
//                        smileChange(new Vector3f(0.001f, 0.0f, 0.0f));
                        break;
                    case KeyF:
                        eyeLidChange(new Vector3f(-0.001f, 0.0f, 0.0f), Vector3f.ZERO);
//                        smileChange(new Vector3f(-0.001f, 0.0f, 0.0f));

                        break;
                    case KeyT:
//                        eyeLidChange(new Vector3f(0.0f, 0.001f, 0.0f), Vector3f.ZERO);
//                        smileChange(new Vector3f(0.0f, 0.001f, 0.0f));

                        mouthChange(new Vector3f(0.0f, 0.001f, 0f));
                        break;
                    case KeyG:
//                        eyeLidChange(new Vector3f(0.0f, -0.001f, 0.0f), Vector3f.ZERO);
//                        smileChange(new Vector3f(0.0f, -0.001f, 0.0f));

                        mouthChange(new Vector3f(0.0f, -0.001f, 0f));
                        break;
                    case KeyY:
                        eyeLidChange(new Vector3f(0.0f, 0.0f, 0.001f), Vector3f.ZERO);
//                        smileChange(new Vector3f(0.0f, 0.0f, 0.001f));
                        break;
                    case KeyH:
                        eyeLidChange(new Vector3f(0.0f, 0.0f, -0.001f), Vector3f.ZERO);
//                        smileChange(new Vector3f(0.0f, 0.0f, -0.001f));

                        break;
                    case KeyU:
                        eyeLidChange(Vector3f.ZERO, new Vector3f(0.001f, 0.0f, 0.0f));
                        break;
                    case KeyJ:
                        eyeLidChange(Vector3f.ZERO, new Vector3f(-0.001f, 0.0f, 0.0f));
                        break;
                    case KeyI:
                        eyeLidChange(Vector3f.ZERO, new Vector3f(0.0f, 0.001f, 0.0f));
                        break;
                    case KeyK:
                        eyeLidChange(Vector3f.ZERO, new Vector3f(0.0f, -0.001f, 0.0f));
                        break;
                    case KeyO:
                        eyeLidChange(Vector3f.ZERO, new Vector3f(0.0f, 0.0f, 0.001f));
                        break;
                    case KeyL:
                        eyeLidChange(Vector3f.ZERO, new Vector3f(0.0f, 0.0f, -0.001f));
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

    private void smileUp() {
        lsrV.addLocal(0.000f, -0.001f, 0.001f);
        lslV.addLocal(0.000f, 0.001f, 0.001f);
        System.out.println(lsrV.y);
        lipSideR.setUserTransforms(lsrV, Quaternion.IDENTITY, Vector3f.UNIT_XYZ);
        lipSideL.setUserTransforms(lslV, Quaternion.IDENTITY, Vector3f.UNIT_XYZ);
        ske.updateWorldVectors();
    }

    private void smileDown() {
        lsrV.addLocal(0.000f, -0.001f, 0.0f);
        lslV.addLocal(0.000f, -0.001f, 0.0f);
        System.out.println(lsrV.y);
        lipSideR.setUserTransforms(lsrV, Quaternion.IDENTITY, Vector3f.UNIT_XYZ);
        lipSideL.setUserTransforms(lslV, Quaternion.IDENTITY, Vector3f.UNIT_XYZ);
        ske.updateWorldVectors();
    }

    private void smileChange(Vector3f changeSize) {
        lsrV.addLocal(changeSize);
        System.out.println(lsrV);
        lipSideL.setUserTransforms(lslV, Quaternion.IDENTITY, Vector3f.UNIT_XYZ);
        ske.updateWorldVectors();
    }

    private void mouthChange(Vector3f changeSize) {
        jawV.addLocal(changeSize);
        System.out.println(jawV);
        jaw.setUserTransforms(jawV, Quaternion.IDENTITY, Vector3f.UNIT_XYZ);

        ske.updateWorldVectors();
    }

    private void eyeLidChange(Vector3f topChangeSize, Vector3f bottomChangeSize) {

        eltlV.addLocal(topChangeSize);
        eltrV.addLocal(topChangeSize);
        elbrV.addLocal(bottomChangeSize);
        elblV.addLocal(bottomChangeSize);
        System.out.println(eltlV.toString() + "" + elblV.toString());

        eyeLidTopL.setUserTransforms(eltlV, Quaternion.IDENTITY, Vector3f.UNIT_XYZ);
        eyeLidBottomL.setUserTransforms(elblV, Quaternion.IDENTITY, Vector3f.UNIT_XYZ);

        eyeLidTopR.setUserTransforms(eltrV, Quaternion.IDENTITY, Vector3f.UNIT_XYZ);
        eyeLidBottomR.setUserTransforms(elbrV, Quaternion.IDENTITY, Vector3f.UNIT_XYZ);

        ske.updateWorldVectors();
    }

    public static void main(String[] args) {
        HelloSkeleton2 app = new HelloSkeleton2();
        app.start();
    }

}
