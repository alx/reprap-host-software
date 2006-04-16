package org.reprap.gui;
import java.awt.BorderLayout;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Material;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

import javax.swing.WindowConstants;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.universe.SimpleUniverse;


public class PreviewWindow extends javax.swing.JFrame implements Previewer {
	private int material = 0;
	private SimpleUniverse simpleU;
	private Canvas3D canvas3D;
	
	public PreviewWindow() {
		super();
		initGUI();
	}
	
	private void initGUI() {
		try {
			canvas3D = new Canvas3D(null);
			getContentPane().add(canvas3D, BorderLayout.CENTER);
			setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			pack();
			setSize(500, 350);
			
			BranchGroup scene = createSceneGraph();
			scene.compile();
			
			// SimpleUniverse is a Convenience Utility class
			simpleU = new SimpleUniverse(canvas3D);
			
			// This moves the ViewPlatform back a bit so the
			// objects in the scene can be viewed.
			simpleU.getViewingPlatform().setNominalViewingTransform();
			
			simpleU.addBranchGraph(scene);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void dispose() {
	}

	private void AddBlock(BranchGroup root, Appearance appearance,
			double x1, double y1, double z1,
			double x2, double y2, double z2,
			float thickness) {
		
		Point3d p1 = new Point3d(x1, y1, z1);
		Point3d p2 = new Point3d(x2, y2, z2);

		Vector3d unity = new Vector3d(0, 1, 0);
		Vector3d v = new Vector3d(x2 - x1, y2 - y1, z2 - z1);
		
		Cylinder cyl = new Cylinder(thickness, (float)v.length(), appearance);
		
		Transform3D transform = new Transform3D();
		transform.setTranslation(v);
				
		double angle = v.angle(unity);
		Vector3d axis = new Vector3d();
		axis.cross(unity, v);
		AxisAngle4d rotationAngle = new AxisAngle4d(axis.x, axis.y, axis.z, angle);
		transform.setRotation(rotationAngle);
		
		TransformGroup tg = new TransformGroup(transform);
		tg.addChild(cyl);
		root.addChild(tg);
		
	}

	private Appearance flatAppearance(Color3f colour) {
		Appearance appearance = new Appearance();
		Material material = new Material(colour, colour, colour, colour, 120.0f);
		appearance.setMaterial(material);
		return appearance;
	}
	
	public BranchGroup createSceneGraph() {
		// Create the root of the branch graph
		Appearance axisAppearanceX = flatAppearance(new Color3f(0.8f, 0.2f, 0.2f));
		Appearance axisAppearanceY = flatAppearance(new Color3f(0.2f, 0.8f, 0.2f));
		Appearance axisAppearanceZ = flatAppearance(new Color3f(0.2f, 0.2f, 0.8f));
		Appearance plasticAppearance = flatAppearance(new Color3f(0.8f, 0.8f, 0.6f));

		BranchGroup objRoot = new BranchGroup();
		
		AddBlock(objRoot, axisAppearanceX,   0, 0, 0,  1, 0, 0,  0.01f);
		AddBlock(objRoot, axisAppearanceY,   0, 0, 0,  0, 1, 0,  0.01f);
		AddBlock(objRoot, axisAppearanceZ,   0, 0, 0,  0, 0, 1,  0.01f);
		
		AddBlock(objRoot, plasticAppearance, 0, 0, 0,  1, 1, 1,  0.02f);
		
		return objRoot;
	}
	
	
	
	public void setMaterial(int index) {
		material = index;
	}
	
	public void addSegment(double x1, double y1, double z1,
			double x2, double y2, double z2) {
		
	}
	
}
