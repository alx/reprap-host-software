package org.reprap.gui;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Background;
import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Group;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.ViewPlatform;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.vecmath.Color3f;
//import javax.vecmath.Vector3d;

public class PreviewPanel extends Panel3D implements Previewer {
	private int material = 0;
	private double previousZ = Double.NaN;
	private JCheckBoxMenuItem layerPauseCheckbox = null, segmentPauseCheckbox = null;
	private BranchGroup extrusions;

	private StatusMessage statusWindow;
		
	/**
	 * Constructor
	 */
	public PreviewPanel() throws Exception {
		initialise();
		statusWindow = new StatusMessage(new JFrame());
	}

	/**
	 * Set bg light grey
	 */
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

	/**
	 * Set stuff up for the constructors - called by all of them that actually
	 * do anything.
	 */


	/**
	 * Set up the RepRap working volume
	 */
	protected BranchGroup createSceneBranchGroup() throws Exception {
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

		wv_and_stls.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		extrusions = new BranchGroup();
		extrusions.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		extrusions.setCapability(Group.ALLOW_CHILDREN_WRITE);
		wv_and_stls.addChild(extrusions);
		
		// Load the STL file for the working volume

		world = new STLObject(wv_and_stls, worldName);

		String stlFile = getStlBackground();

		workingVolume = new STLObject(stlFile, wv_offset, 0, wv_app);
		wv_and_stls.addChild(workingVolume.top);

		// Set the mouse to move everything
		MouseObject mouse = new MouseObject(getApplicationBounds(), mouse_tf, mouse_zf);
		mouse.move(world, false);
		
		objRoot.addChild(world.top);

		return objRoot;
	}

	/**
	 * Set the current extrusion material (or equivalently, the extruder head)
	 */
	public void setMaterial(int index) {
		material = index;
	}
	
	/**
	 * Get the current extrusion material
	 * @return The current extrusion material as an integer index
	 */
	public int getMaterial() {
		return material;
	}

	/**
	 * Called to add a new segment of extruded material to the preview
	 */
	public void addSegment(double x1, double y1, double z1, double x2, double y2, double z2) {
		if (layerPauseCheckbox != null && layerPauseCheckbox.isSelected() &&
				z2 != previousZ)
			layerPause();
		
		if (segmentPauseCheckbox != null && segmentPauseCheckbox.isSelected())
			segmentPause();
		
		if (isCancelled()) return;
		
		final double extrusionSize = 0.3;
		BranchGroup group = new BranchGroup();
		group.setCapability(BranchGroup.ALLOW_DETACH);
		addBlock(group, extrusion_app,
				x1, y1, z1,
				x2, y2, z2,
				(float)(extrusionSize * 0.5));
		extrusions.addChild(group);
		previousZ = z2;
	}

	/**
	 * Clear and prepare for a new preview
	 *
	 */
	public void reset() {
		extrusions.removeAllChildren();
		previousZ = Double.NaN;
		setCancelled(false);
	}
	
	/**
	 * Display a message indicating a segment is about to be
	 * printed and wait for the user to acknowledge
	 */
	private void segmentPause() {
		ContinuationMesage msg =
			new ContinuationMesage(null, "A new segment is about to be produced",
					segmentPauseCheckbox, layerPauseCheckbox);
		msg.setVisible(true);
		try {
			synchronized(msg) {
				msg.wait();
			}
		} catch (Exception ex) {
		}
		if (msg.getResult() == false)
			setCancelled(true);
		msg.dispose();
	}

	/**
	 * Display a message indicating a layer is about to be
	 * printed and wait for the user to acknowledge
	 */
	private void layerPause() {
		ContinuationMesage msg =
			new ContinuationMesage(null, "A new layer is about to be produced",
					segmentPauseCheckbox, layerPauseCheckbox);
		msg.setVisible(true);
		try {
			synchronized(msg) {
				msg.wait();
			}
		} catch (Exception ex) {
		}
		if (msg.getResult() == false)
			setCancelled(true);
		msg.dispose();
	}

	/**
	 * Set the source checkbox used to determine if there should
	 * be a pause between segments.
	 * 
	 * @param segmentPause The source checkbox used to determine
	 * if there should be a pause.  This is a checkbox rather than
	 * a boolean so it can be changed on the fly. 
	 */
	public void setSegmentPause(JCheckBoxMenuItem segmentPause) {
		segmentPauseCheckbox = segmentPause;
	}

	/**
	 * Set the source checkbox used to determine if there should
	 * be a pause between layers.
	 * 
	 * @param layerPause The source checkbox used to determine
	 * if there should be a pause.  This is a checkbox rather than
	 * a boolean so it can be changed on the fly.
	 */
	public void setLayerPause(JCheckBoxMenuItem layerPause) {
		layerPauseCheckbox = layerPause;
	}

	public void setMessage(String message) {
		if (message == null)
			statusWindow.setVisible(false);
		else {
			statusWindow.setMessage(message);
			statusWindow.setVisible(true);
		}
	}
	
	public boolean isCancelled() {
		return statusWindow.isCancelled();
	}

	public void setCancelled(boolean isCancelled) {
		statusWindow.setCancelled(isCancelled);
	}

}