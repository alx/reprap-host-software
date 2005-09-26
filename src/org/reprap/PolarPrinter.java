package org.reprap;

public interface PolarPrinter extends Printer {
  public void PrintPolarSegment(double startTheta, double startX,
		  double startZ, double endTheta, double endX, double endZ);
  
}
