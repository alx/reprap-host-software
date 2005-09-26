package org.reprap;

import java.applet.Applet;
import java.awt.BorderLayout;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3d;

import org.reprap.machines.VirtualPolarPrinter;

import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.universe.SimpleUniverse;

public class PrinterUI extends Applet {

	private static final long serialVersionUID = -5342204698208702886L;
	VirtualPolarPrinter printer;
	
	public void Test() {
		setLayout(new BorderLayout());
		Canvas3D canvas3D = new Canvas3D(null);
		add("Center", canvas3D);

		BranchGroup scene = createSceneGraph();
		scene.compile();

		// SimpleUniverse is a Convenience Utility class
		SimpleUniverse simpleU = new SimpleUniverse(canvas3D);

		// This moves the ViewPlatform back a bit so the
		// objects in the scene can be viewed.
		simpleU.getViewingPlatform().setNominalViewingTransform();

		simpleU.addBranchGraph(scene);
	}
	
	public BranchGroup createSceneGraph() {
		// Create the root of the branch graph
		BranchGroup objRoot = new BranchGroup();
		
		Transform3D rotate = new Transform3D();
		Transform3D transform = new Transform3D();
		rotate.rotY(Math.PI / 4.0d);
		transform.rotX(Math.PI / 4.0d);
		rotate.mul(transform);
		transform.rotZ(Math.PI / 4.0d);
		rotate.mul(transform);
		transform.setTranslation(new Vector3d(0.0, 0.5, 0.0));
		rotate.mul(transform);
		
		TransformGroup objRotate = new TransformGroup(rotate);
		// Create a simple shape leaf node, add it to the scene graph.
		// ColorCube is a Convenience Utility class
		objRotate.addChild(new ColorCube(0.2));
		objRoot.addChild(objRotate);

		transform.setTranslation(new Vector3d(0.0, -1.0, 0.0));
		rotate.mul(transform);
		TransformGroup cube2 = new TransformGroup(rotate);
		cube2.addChild(new ColorCube(0.2));
		objRoot.addChild(cube2);

		transform.setTranslation(new Vector3d(0.0, -1.0, 0.0));
		rotate.mul(transform);
		cube2 = new TransformGroup(rotate);
		Cylinder cyl = new Cylinder();
		cube2.addChild(cyl);
		objRoot.addChild(cube2);

		
		return objRoot;
	}
	
	public PrinterUI() {
		printer = new VirtualPolarPrinter();
		Test();
	}

	public void Begin() {
		System.out.println("Print....");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//PrinterUI ui = new PrinterUI();
		//ui.Begin();
		new MainFrame(new PrinterUI(), 256, 256);
	}

}
