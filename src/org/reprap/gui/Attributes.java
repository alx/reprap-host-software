package org.reprap.gui;

import javax.media.j3d.Appearance;
import javax.media.j3d.*;
import org.reprap.devices.NullExtruder;

/**
 * Small class to hold RepRap attributes that are attached to
 * Java3D shapes as user data.
 * 
 * @author ensab
 *
 */
public class Attributes {
	private String material;
	private STLObject parent;
	private BranchGroup part;
	private Appearance app;
	
	public Attributes(String s, STLObject p, BranchGroup b, Appearance a)
	{
		material = s;
		parent = p;
		part = b;
		app = a;
	}
	
	public String getMaterial() { return material; }
	public STLObject getParent() { return parent; }
	public BranchGroup getPart() { return part; }
	public Appearance getAppearance() { return app; }
	public void setMaterial(String s) 
	{ 
		material = s;
		app = NullExtruder.getAppearanceFromNumber(
				NullExtruder.getNumberFromMaterial(material));
		if(parent != null)
			parent.restoreAppearance();
	}
	public void setParent(STLObject p) { parent = p; }
	public void setPart(BranchGroup b) { part = b; }
	public void setAppearance(Appearance a) { app = a; }
}
