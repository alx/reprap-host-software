package org.reprap.gui;

import java.net.URL;
import java.util.ArrayList;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Group;
import javax.media.j3d.Material;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.ViewPlatform;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;

public class PreviewPanel extends Panel3D implements Previewer {
	private int material = 0;

	// ------------------------
	// Most of the stuff that follows will be read from
	// a configuration file ultimately.

	// The relative location of the STL model of the working volume
	// And the offset of the origin in it.

	private static final String wv_location = "RepRap-data/RepRap-wv.stl";
	private static final String worldName = "RepRap World";
	private static final Vector3d wv_offset = new Vector3d(-17.3, -24.85, -2);

	// Black, the background, and other colours
	private static final Color3f black = new Color3f(0, 0, 0);
	private static final Color3f bgColour = new Color3f(0.6f, 0.6f, 0.6f);
	private static final Color3f rrGreen = new Color3f(0.3f, 0.4f, 0.3f);
	private static final Color3f plastic = new Color3f(0.8f, 0.8f, 0.8f);

	//---- End of stuff to be loaded from config file

	private Appearance extrusion_app = null; // Colour for extrused material
	private Appearance wv_app = null; // Colour for the working volume
	private BranchGroup wv_and_stls = new BranchGroup(); // Where in the scene

	// the
	// working volume and STLs
	// are joined on.

	private STLObject world = null; // Everything
	private STLObject workingVolume = null; // The RepRap machine itself.
	private STLObject lastPicked = null; // The last thing picked
	private java.util.List stls = new ArrayList(); // All the STLs to be built
	private int objectIndex = 0; // Counter for STLs as they are loaded

	// Constructors
	public PreviewPanel() {
		initialise();
	}

	// Set bg light grey
	protected Background createBackground() {
		Background back = new Background(bgColour);
		back.setApplicationBounds(createApplicationBounds());
		return back;
	}

	protected BranchGroup createViewBranchGroup(TransformGroup[] tgArray,
			ViewPlatform vp) {
		BranchGroup vpBranchGroup = new BranchGroup();

		if (tgArray != null && tgArray.length > 0) {
			Group parentGroup = vpBranchGroup;
			TransformGroup curTg = null;

			for (int n = 0; n < tgArray.length; n++) {
				curTg = tgArray[n];
				parentGroup.addChild(curTg);
				parentGroup = curTg;
			}

			tgArray[tgArray.length - 1].addChild(vp);
		} else
			vpBranchGroup.addChild(vp);

		return vpBranchGroup;
	}

	// Set stuff up for the constructors - called by all of them that actually
	// do anything.

	private void initialise() {
		wv_app = new Appearance();
		wv_app.setMaterial(new Material(rrGreen, black, rrGreen, black, 0f));
		
		extrusion_app = new Appearance();
		extrusion_app.setMaterial(new Material(plastic, black, plastic, black, 101f));
		
		initJava3d();

	}

	// Set up the RepRap working volume

	protected BranchGroup createSceneBranchGroup() {
		sceneBranchGroup = new BranchGroup();

		BranchGroup objRoot = sceneBranchGroup;

		Bounds lightBounds = getApplicationBounds();

		AmbientLight ambLight = new AmbientLight(true, new Color3f(1.0f, 1.0f,
				1.0f));
		ambLight.setInfluencingBounds(lightBounds);
		objRoot.addChild(ambLight);

		DirectionalLight headLight = new DirectionalLight();
		headLight.setInfluencingBounds(lightBounds);
		objRoot.addChild(headLight);

		wv_and_stls.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);

		// Load the STL file for the working volume

		world = new STLObject(wv_and_stls, worldName);

		URL codebase = null;

		String stlFile = null;

		try {
			codebase = RepRapBuild.getWorkingDirectory();
			stlFile = codebase.toExternalForm() + wv_location;
		} catch (Exception e) {
			System.err
					.println("createSceneBranchGroup(): Exception finding working directory: "
							+ codebase.toExternalForm());
			e.printStackTrace();
		}

		workingVolume = new STLObject(stlFile, wv_offset, objectIndex, wv_app);
		wv_and_stls.addChild(workingVolume.top);

		// Set the mouse to move everything
		MouseObject mouse = new MouseObject(getApplicationBounds(), mouse_tf, mouse_zf);
		mouse.move(world, false);
		
		objRoot.addChild(world.top);

		return objRoot;
	}

	public void setMaterial(int index) {
		material = index;
	}

	public void addSegment(double x1, double y1, double z1, double x2, double y2, double z2) {
		final double extrusionSize = 0.3;
		BranchGroup group = new BranchGroup();
		addBlock(group, extrusion_app,
				x1, y1, z1,
				x2, y2, z2,
				(float)(extrusionSize * 0.5));
		wv_and_stls.addChild(group);
	}

}