package org.reprap.machines;

import org.reprap.PolarPrinter;
import org.reprap.comms.Communicator;
//import org.reprap.comms.snap.SNAPAddress;

public class Repstrap implements PolarPrinter {
	//private static final short ADDR_COORDINATOR = 1;
	//private static final short ADDR_MOTORTHETA = 2;
	//private static final short ADDR_MOTORX = 3;
	//private static final short ADDR_MOTORZ = 4;
	//private static final short ADDR_EXTRUDER1 = 5;
	
	//private SNAPAddress coordinator;
	
	public Repstrap(Communicator communicator) {
	//	coordinator = new SNAPAddress(ADDR_COORDINATOR);
	}

	public void printPolarSegment(double startTheta, double startX,
			double startZ, double endTheta, double endX, double endZ) {
		// TODO Auto-generated method stub

	}

	public void calibrate() {
		// TODO Auto-generated method stub

	}

	public void printPoint(double x, double y, double z) {
		// TODO Auto-generated method stub

	}

	public void printSegment(double startX, double startY, double startZ,
			double endX, double endY, double endZ) {
		// TODO Auto-generated method stub

	}

}
