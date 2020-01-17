package org.seekloud.theia.faceAnalysis.jme3;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.SkeletonDebugger;
import com.jme3.scene.plugins.fbx.SceneLoader;
import com.sun.javaws.IconUtil;
import org.seekloud.theia.faceAnalysis.common.AppSettings;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 演示Jaime的骨骼
 * @author yanmaoyuan
 */
public class JmeImporterTest extends SimpleApplication {

    @Override
    public void simpleInitApp() {
        /**
         * 摄像机
         */
        assetManager.registerLocator("faceAnalysis/model", FileLocator.class);
        cam.setLocation(new Vector3f(0f, 2f, 3f));
        cam.setRotation(new Quaternion(7.584799E-4f, 0.99156f, -0.12951611f, 0.0058092116f));
        flyCam.setMoveSpeed(10f);

        /**
         * 要有光
         */
        rootNode.addLight(new AmbientLight(new ColorRGBA(0.2f, 0.2f, 0.2f, 1f)));
        rootNode.addLight(new DirectionalLight(new Vector3f(0f, -2f, -3f), new ColorRGBA(0.8f, 0.8f, 0.8f, 1f)));

        // #1 导入模型
        long t1 = System.currentTimeMillis();
        Node scene = (Node) assetManager.loadModel("Models/ruyue_head.glb");

        scene.getChildren().forEach((spa)-> System.out.println(spa.getName()));

        Node model = (Node) scene.getChild("root");

        model.getChildren().forEach((spa)-> System.out.println(spa.getName()));


        model.scale(2f);// 按比例缩小


        rootNode.attachChild(model);

        System.out.println(System.currentTimeMillis()-t1);

        // 获得SkeletonControl
        // 骨骼控制器
        SkeletonControl sc = model.getControl(SkeletonControl.class);



        if (sc != null){
            // 打印骨骼的名称和层次关系
            System.out.println(sc.getSkeleton());

            /**
             * 创建一个SkeletonDebugger，用于显示骨骼的形状。
             */
            // 骨骼调试器
            SkeletonDebugger sd = new SkeletonDebugger("debugger", sc.getSkeleton());
            model.attachChild(sd);

            // 创建材质
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Green);
            mat.getAdditionalRenderState().setDepthTest(false);// 禁用深度测试，实现透视效果。
            sd.setMaterial(mat);

        } else {
            System.out.println("get skeleton failed");
        }

    }

    public static void main(String[] args) {
        JmeImporterTest app = new JmeImporterTest();
        app.start();
    }

}
