package org.reprap.gui;
import java.awt.BorderLayout;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Group;
import javax.media.j3d.Material;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;

import javax.swing.WindowConstants;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.universe.SimpleUniverse;


public class PreviewWindow extends javax.swing.JFrame implements Previewer {
	private int material = 0;
	private SimpleUniverse simpleU;
	private Canvas3D canvas3D;
	private TransformGroup world;
	private Appearance plasticAppearance;
	
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

			plasticAppearance = flatAppearance(new Color3f(0.8f, 0.8f, 0.6f));
			
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

	private void addBlock(Group root, Appearance appearance,
			double x1, double y1, double z1,
			double x2, double y2, double z2,
			float thickness) {
		root.addChild(addBlock(appearance, x1, y1, z1, x2, y2, z2, thickness));
	}

	private void addBlock(BranchGroup root, Appearance appearance,
			double x1, double y1, double z1,
			double x2, double y2, double z2,
			float thickness) {
		root.addChild(addBlock(appearance, x1, y1, z1, x2, y2, z2, thickness));
	}

	private void addBlock(TransformGroup root, Appearance appearance,
			double x1, double y1, double z1,
			double x2, double y2, double z2,
			float thickness) {
		root.addChild(addBlock(appearance, x1, y1, z1, x2, y2, z2, thickness));
	}
	
	private TransformGroup addBlock(Appearance appearance,
			double x1, double y1, double z1,
			double x2, double y2, double z2,
			float thickness) {
		
		Point3d p1 = new Point3d(x1, y1, z1);
		Point3d p2 = new Point3d(x2, y2, z2);

		Vector3d unity = new Vector3d(0, 1, 0);
		Vector3d v = new Vector3d(x2 - x1, y2 - y1, z2 - z1);
		
		Cylinder cyl = new Cylinder(thickness, (float)v.length(), appearance);
		
		Transform3D transform = new Transform3D();
		
		Vector3d translate = new Vector3d(p1);
		v.scale(0.5);
		translate.add(v);
		transform.setTranslation(translate);
				
		double angle = v.angle(unity);
		Vector3d axis = new Vector3d();
		axis.cross(unity, v);
		AxisAngle4d rotationAngle = new AxisAngle4d(axis.x, axis.y, axis.z, angle);
		transform.setRotation(rotationAngle);
		
		TransformGroup tg = new TransformGroup(transform);
		tg.addChild(cyl);
		return tg;
	}

	private Appearance flatAppearance(Color3f colour) {
		Appearance appearance = new Appearance();
		Material material = new Material();
		material.setAmbientColor(colour);
		material.setDiffuseColor(colour);
		material.setEmissiveColor(colour);
		material.setShininess(101.0f);
		appearance.setMaterial(material);
		return appearance;
	}
	
	public BranchGroup createSceneGraph() {
		// Create the root of the branch graph
		Appearance axisAppearanceX = flatAppearance(new Color3f(0.8f, 0.2f, 0.2f));
		Appearance axisAppearanceY = flatAppearance(new Color3f(0.2f, 0.8f, 0.2f));
		Appearance axisAppearanceZ = flatAppearance(new Color3f(0.2f, 0.2f, 0.8f));

		BranchGroup objRoot = new BranchGroup();
		
		world = new TransformGroup();
		world.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		
		world.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		world.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		
		addBlock(world, axisAppearanceX,   0, 0, 0,  1, 0, 0,  0.01f);
		addBlock(world, axisAppearanceY,   0, 0, 0,  0, 1, 0,  0.01f);
		addBlock(world, axisAppearanceZ,   0, 0, 0,  0, 0, 1,  0.01f);
	
		Transform3D baseTransform = new Transform3D();
		baseTransform.setTranslation(new Vector3d(0.5, 0.5, 0.0));
		TransformGroup baseTransformGroup = new TransformGroup(baseTransform);
		Color3f baseColour = new Color3f(0.2f, 0.6f, 0.6f);
		Appearance baseAppearance = new Appearance();
		Material baseMaterial = new Material();
		baseMaterial.setAmbientColor(baseColour);
		baseMaterial.setDiffuseColor(baseColour);
		baseMaterial.setEmissiveColor(baseColour);
		baseAppearance.setMaterial(baseMaterial);
		TransparencyAttributes ta = new TransparencyAttributes();
		ta.setTransparencyMode(TransparencyAttributes.BLENDED);
		baseAppearance.setTransparencyAttributes(ta);
		ta.setTransparency(0.66f);
		Box base = new Box(0.5f, 0.5f, 0.001f, baseAppearance);
		baseTransformGroup.addChild(base);
		world.addChild(baseTransformGroup);
		
		MouseRotate mouseRotate = new MouseRotate();
		mouseRotate.setTransformGroup(world);
		mouseRotate.setSchedulingBounds(new BoundingSphere());
		objRoot.addChild(mouseRotate);
		
		MouseTranslate mouseTranslate = new MouseTranslate();
		mouseTranslate.setTransformGroup(world);
		mouseTranslate.setSchedulingBounds(new BoundingSphere());
		objRoot.addChild(mouseTranslate);

		MouseZoom mouseZoom = new MouseZoom();
		mouseZoom.setTransformGroup(world);
		mouseZoom.setSchedulingBounds(new BoundingSphere());
		objRoot.addChild(mouseZoom);
		
		objRoot.addChild(world);
		
		return objRoot;
	}
	
	public void setMaterial(int index) {
		material = index;
	}
	
	public void addSegment(double x1, double y1, double z1,
			double x2, double y2, double z2) {
		final double scale = 0.01;  // We're displaying in meters
		final double extrusionSize = 0.3;
		BranchGroup group = new BranchGroup();
		addBlock(group, plasticAppearance,
				x1 * scale, y1 * scale, z1 * scale,
				x2 * scale, y2 * scale, z2 * scale,
				(float)(extrusionSize * 0.5 * scale));
		world.addChild(group);
		
	}
	
}
