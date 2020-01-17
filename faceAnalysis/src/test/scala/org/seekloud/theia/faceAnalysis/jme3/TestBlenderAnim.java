package org.seekloud.theia.faceAnalysis.jme3;

/**
 * Created by sky
 * Date on 2019/9/23
 * Time at 下午4:22
 */
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.BlenderKey;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class TestBlenderAnim extends SimpleApplication {

    private AnimChannel channel;
    private AnimControl control;

    public static void main(String[] args) {
        TestBlenderAnim app = new TestBlenderAnim();
        app.start();
    }

    @Override
    public void simpleInitApp() {
//        flyCam.setMoveSpeed(10f);
        flyCam.setEnabled(false);
        cam.setLocation(new Vector3f(6.4013605f, 7.488437f, 12.843031f));
        cam.setRotation(new Quaternion(-0.060740203f, 0.93925786f, -0.2398315f, -0.2378785f));

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f, -0.7f, -1).normalizeLocal());
        dl.setColor(new ColorRGBA(1f, 1f, 1f, 1.0f));
        rootNode.addLight(dl);

        BlenderKey blenderKey = new BlenderKey("Blender/2.4x/BaseMesh_249.blend");

        Spatial scene =  assetManager.loadModel(blenderKey);
        rootNode.attachChild(scene);

        SkeletonControl sc = scene.getControl(SkeletonControl.class);

//        ske = sc.getSkeleton();
//        System.out.println("sc.getSkeleton() = " );

        Spatial model = this.findNode(rootNode, "BaseMesh_01");
        model.center();

        control = model.getControl(AnimControl.class);
        channel = control.createChannel();

        channel.setAnim("run_01");
    }

    /**
     * This method finds a node of a given name.
     * @param rootNode the root node to search
     * @param name the name of the searched node
     * @return the found node or null
     */
    private Spatial findNode(Node rootNode, String name) {
        if (name.equals(rootNode.getName())) {
            return rootNode;
        }
        return rootNode.getChild(name);
    }
}

