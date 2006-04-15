package org.reprap;

public interface Printer {

	public void calibrate();
	
	public void printSegment(double startX, double startY,
			double startZ, double endX, double endY, double endZ);
	
	
}
