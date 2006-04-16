package org.reprap;

import java.io.IOException;

import org.reprap.gui.Previewer;

public interface Printer {

	public void calibrate();

	public void printSegment(double startX, double startY,
			double startZ, double endX, double endY, double endZ) throws ReprapException, IOException;
	
	public void moveTo(double x, double y, double z) throws ReprapException, IOException;
	public void printTo(double x, double y, double z) throws ReprapException, IOException;

	public void selectMaterial(int materialIndex);
	
	/**
	 * Indicates end of job, homes extruder, powers down etc
	 *
	 */
	public void terminate() throws IOException;
	
	
	public int getSpeed();
	public void setSpeed(int speed);
	public int getExtruderSpeed();
	public void setExtruderSpeed(int speed);
	
	public void setPreviewer(Previewer previewer);

	
}
