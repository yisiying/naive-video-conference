package org.seekloud.theia.faceAnalysis.jme3;

/**
 * Created by sky
 * Date on 2019/9/23
 * Time at 15:00
 */
import com.jme3.animation.SkeletonControl;
import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class TestBlenderLoader extends SimpleApplication {

    public static void main(String[] args){
        TestBlenderLoader app = new TestBlenderLoader();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);

        //load model with packed images
        Node ogre =(Node)  assetManager.loadModel("Blender/2.4x/Sinbad.blend");

//        SkeletonControl sc = ogre.getControl(SkeletonControl.class);
//        int c=sc.getSkeleton().getBoneCount();
//        System.out.println(c);

        rootNode.attachChild(ogre);

        //load model with referenced images
//        Spatial track = assetManager.loadModel("Blender/2.4x/MountainValley_Track.blend");
//        rootNode.attachChild(track);

        // sunset light
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f,-0.7f,1).normalizeLocal());
        dl.setColor(new ColorRGBA(0.44f, 0.30f, 0.20f, 1.0f));
        rootNode.addLight(dl);

        // skylight
        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.6f,-1,-0.6f).normalizeLocal());
        dl.setColor(new ColorRGBA(0.10f, 0.22f, 0.44f, 1.0f));
        rootNode.addLight(dl);

        // white ambient light
        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(1, -0.5f,-0.1f).normalizeLocal());
        dl.setColor(new ColorRGBA(0.80f, 0.70f, 0.80f, 1.0f));
        rootNode.addLight(dl);
    }

    @Override
    public void simpleUpdate(float tpf){
    }

}