package org.reprap.devices.pseudo;

import java.io.IOException;

import org.reprap.devices.GenericExtruder;
import org.reprap.devices.GenericStepperMotor;

/**
 * This is pseudo device that provides an apparent single device
 * for plotting lines.
 */
public class LinePrinter {

	private GenericStepperMotor motorX;
	private GenericStepperMotor motorY;
	private GenericExtruder extruder;

	public LinePrinter(GenericStepperMotor motorX, GenericStepperMotor motorY, GenericExtruder extruder) {
		this.motorX = motorX;
		this.motorY = motorY;
		this.extruder = extruder;
	}

	/*
	 * Print a line.  At the moment this is just the pure 2D Bresenham algorithm.
	 * It would be good to generalise this to a 3D DDA.
	 */
	public void PrintLine(int startX, int startY, int endX, int endY, int movementSpeed, int extruderSpeed) throws IOException {
		GenericStepperMotor master, slave;
		
		int x0, x1, y0, y1;
		
		// Whichever is the greater distance will be the master
		// From an algorithmic point of view, we'll just consider
		// the master to be X and the slave to be Y, which eliminates
		// the need for mapping quadrants.
		if (Math.abs(endX - startX) > Math.abs(endY - startY)) {
			master = motorX;
			slave = motorY;
			x0 = startX;
			x1 = endX;
			y0 = startY;
			y1 = endY;
		} else {
			master = motorY;
			slave = motorX;
			x0 = startY;
			x1 = endY;
			y0 = startX;
			y1 = endX;
		}
				
		master.setSync(GenericStepperMotor.SYNC_NONE);
		if (y0 < y1)
			slave.setSync(GenericStepperMotor.SYNC_INC);
		else
			slave.setSync(GenericStepperMotor.SYNC_DEC);

		int deltaY = Math.abs(y1 - y0); 
		int deltaX = Math.abs(x1 - x0); 
		
		master.seekBlocking(movementSpeed, x0);
		slave.seekBlocking(movementSpeed, y0);
		
		/// TODO Both should seek independently and just wait for both to complete
		
		/// TODO Start extruding
		master.dda(movementSpeed, x1, deltaY);
		
		/// TODO Stop extruding
		
		slave.setSync(GenericStepperMotor.SYNC_NONE);
		
	}

}
