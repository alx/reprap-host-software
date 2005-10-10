package org.reprap;

public interface Printer {

	public void calibrate();
	
	public void printPoint(double x, double y, double z);
	
	public void printSegment(double startX, double startY,
			double startZ, double endX, double endY, double endZ);
	
	
}
