package org.reprap;

public interface Printer {

	public void Calibrate();
	
	public void PrintPoint(double x, double y, double z);
	
	public void PrintSegment(double startX, double startY,
			double startZ, double endX, double endY, double endZ);
	
	
}
