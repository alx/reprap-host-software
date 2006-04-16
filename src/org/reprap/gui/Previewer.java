package org.reprap.gui;

public interface Previewer {

	public void setMaterial(int index);
	public void addSegment(double x1, double y1, double z1,
			double x2, double y2, double z2);
}
