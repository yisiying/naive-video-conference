package com.jame3learn.cq;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;

public class HelloJME3 extends SimpleApplication {

  /**
   * 初始化3D场景，显示一个方块。
   */
  @Override
  public void simpleInitApp() {

    // #1 创建一个方块形状的网格
    Mesh box = new Box(1, 1, 1);

    // #2 加材载一个感光质
    Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");

    // #3 创建一个几何体，应用刚才和网格和材质。
    Geometry geom = new Geometry("Box");
    geom.setMesh(box);
    geom.setMaterial(mat);

    // #4 创建一束定向光，并让它斜向下照射，好使我们能够看清那个方块。
    DirectionalLight sun = new DirectionalLight();
    sun.setDirection(new Vector3f(-1, -2, -3));
    for(int i=0;i<2;i++){
      geom.scale(2f,2f,2f);
    }
    // #5 将方块和光源都添加到场景图中
    rootNode.attachChild(geom);
    rootNode.addLight(sun);
  }

  public static void main(String[] args) {
    // 启动jME3程序
    HelloJME3 app = new HelloJME3();
    app.start();
  }
}
