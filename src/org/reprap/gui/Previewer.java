package org.reprap.gui;

import javax.media.j3d.*;

public interface Previewer {

	public void setMaterial(int index, double extrusionSize, double extrusionHeight);
	public void addSegment(double x1, double y1, double z1,
			double x2, double y2, double z2);
	public void setMessage(String message);
	public void reset();
	public boolean isCancelled();
	public void setCancelled(boolean isCancelled);
	public void setLowerShell(Shape3D ls);
}
