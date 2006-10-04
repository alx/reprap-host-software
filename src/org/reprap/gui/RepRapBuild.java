/*
 
 RepRap
 ------
 
 The Replicating Rapid Prototyper Project
 
 
 Copyright (C) 2006
 Adrian Bowyer & The University of Bath
 
 http://reprap.org
 
 Principal author:
 
 Adrian Bowyer
 Department of Mechanical Engineering
 Faculty of Engineering and Design
 University of Bath
 Bath BA2 7AY
 U.K.
 
 e-mail: A.Bowyer@bath.ac.uk
 
 RepRap is free; you can redistribute it and/or
 modify it under the terms of the GNU Library General Public
 Licence as published by the Free Software Foundation; either
 version 2 of the Licence, or (at your option) any later version.
 
 RepRap is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Library General Public Licence for more details.
 
 For this purpose the words "software" and "library" in the GNU Library
 General Public Licence are taken to mean any and all computer programs
 computer files data results documents and other copyright information
 available from the RepRap project.
 
 You should have received a copy of the GNU Library General Public
 Licence along with RepRap; if not, write to the Free
 Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA,
 or see
 
 http://www.gnu.org/
 
 =====================================================================
 
 This program loads STL files of objects, orients them, and builds them
 in the RepRap machine.
 
 It is based on one of the open-source examples in Daniel Selman's excellent
 Java3D book, and his notice is immediately below.
 
 First version 2 April 2006
 This version: 16 April 2006
 
 */

/*******************************************************************************
 * VrmlPickingTest.java Copyright (C) 2001 Daniel Selman
 * 
 * First distributed with the book "Java 3D Programming" by Daniel Selman and
 * published by Manning Publications. http://manning.com/selman
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * The license can be found on the WWW at: http://www.fsf.org/copyleft/gpl.html
 * 
 * Or by writing to: Free Software Foundation, Inc., 59 Temple Place - Suite
 * 330, Boston, MA 02111-1307, USA.
 * 
 * Authors can be contacted at: Daniel Selman: daniel@selman.org
 * 
 * If you make changes you think others would like, please contact one of the
 * authors or someone at the www.j3d.org web site.
 ******************************************************************************/

package org.reprap.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Background;
import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.ViewPlatform;
import javax.vecmath.Color3f;

import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.picking.PickTool;

//************************************************************************

// This is the main public class that creates a virtual world of the RepRap
// working volume, allows you to put STL-file objects in it, move them about
// to arrange them, (and builds them in the machine - one day soon).

public class RepRapBuild extends Panel3D implements MouseListener {
	private MouseObject mouse = null;
	private PickCanvas pickCanvas = null; // The thing picked by a mouse click
	private STLObject lastPicked = null; // The last thing picked
	private java.util.List stls = new ArrayList(); // All the STLs to be built
	private int objectIndex = 0; // Counter for STLs as they are loaded

	// Constructors
	public RepRapBuild() throws Exception {
		initialise();
	}
	
	public java.util.List getSTLs()
	{
		return stls;
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

	// Set up the RepRap working volume

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

		mouse = new MouseObject(getApplicationBounds(), mouse_tf, mouse_zf);

		wv_and_stls.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		wv_and_stls.setCapability(Group.ALLOW_CHILDREN_WRITE);
		wv_and_stls.setCapability(Group.ALLOW_CHILDREN_READ);

		// Load the STL file for the working volume

		world = new STLObject(wv_and_stls, worldName);

		String stlFile = getStlBackground();

		workingVolume = new STLObject(stlFile, wv_offset, objectIndex, wv_app);
		wv_and_stls.addChild(workingVolume.top);

		// Set the mouse to move everything

		mouse.move(world, false);
		objRoot.addChild(world.top);

		return objRoot;
	}

	// Action on mouse click

	public void mouseClicked(MouseEvent e) {
		pickCanvas.setShapeLocation(e);

		PickResult pickResult = pickCanvas.pickClosest();
		STLObject picked = null;

		if (pickResult != null) // Got anything?
		{
			Node actualNode = pickResult.getObject();
			String name = (String) actualNode.getUserData();
			if (name != null) // Really got something?
			{
				if (name != workingVolume.name) // STL object picked?
				{
					picked = findSTL(name);
					if (picked != null) {
						picked.setAppearance(picked_app); // Highlight it
						if (lastPicked != null)
							lastPicked.setAppearance(default_app); // lowlight
						// the last
						// one
						mouse.move(picked, true); // Set the mouse to move it
						lastPicked = picked; // Remember it
					}
				} else { // Picked the working volume - deselect all and...
					mouseToWorld();
				}
			}
		}
	}
	
	public void mouseToWorld()
	{
		if (lastPicked != null)
			lastPicked.setAppearance(default_app);
		mouse.move(world, false); // ...switch the mouse to moving the world
		lastPicked = null;
	}

	// Find the stl object in the scene with the given name

	protected STLObject findSTL(String name) {
		STLObject stl;
		for (int i = 0; i < stls.size(); i++) {
			stl = (STLObject) stls.get(i);
			if (stl.name == name)
				return stl;
		}
		return null;
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	// Callback for when the user selects an STL file to load

	public void anotherSTLFile(String s) {
		if (s == null)
			return;
		objectIndex++;
		STLObject stl = new STLObject(s, null, objectIndex, default_app);
		if (stl != null) {
			wv_and_stls.addChild(stl.top);
			stls.add(stl);
		}
	}

	public void start() throws Exception {
		if (pickCanvas == null)
			initialise();
	}

	protected void addCanvas3D(Canvas3D c3d) {
		setLayout(new BorderLayout());
		add(c3d, BorderLayout.CENTER);
		doLayout();

		if (sceneBranchGroup != null) {
			c3d.addMouseListener(this);

			pickCanvas = new PickCanvas(c3d, sceneBranchGroup);
			pickCanvas.setMode(PickTool.GEOMETRY_INTERSECT_INFO);
			pickCanvas.setTolerance(4.0f);
		}

		c3d.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	// Callbacks for when the user rotates the selected object

	public void xRotate() {
		if (lastPicked != null)
			lastPicked.xClick();
	}

	public void yRotate() {
		if (lastPicked != null)
			lastPicked.yClick();
	}

	public void zRotate() {
		if (lastPicked != null)
			lastPicked.zClick();
	}
	
	public void inToMM() {
		if (lastPicked != null)
			lastPicked.inToMM();
	}
	
	public void deleteSTL()
	{
		if (lastPicked == null)
			return;
		int index = -1;
		for(int i = 0; i < stls.size(); i++)
		{
			if((STLObject)stls.get(i) == lastPicked)
			{
				index = i;
				break;
			}
		}
		if (index >= 0) 
		{
			stls.remove(index);
			index = wv_and_stls.indexOfChild(lastPicked.top);
			wv_and_stls.removeChild(index);
		}
	}

}