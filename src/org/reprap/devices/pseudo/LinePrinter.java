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

	private boolean initialisedXY = false;
	private int currentX, currentY;
	
	public LinePrinter(GenericStepperMotor motorX, GenericStepperMotor motorY, GenericExtruder extruder) {
		this.motorX = motorX;
		this.motorY = motorY;
		this.extruder = extruder;
	}
	
	public void initialiseXY() throws IOException {
		if (!initialisedXY) {
			currentX = motorX.getPosition();
			currentY = motorY.getPosition();
			initialisedXY = true;
		}
	}

	
	public void moveTo(int endX, int endY, int movementSpeed) throws IOException {
		initialiseXY();

		GenericStepperMotor master, slave;

		int x0, x1, y0, y1;
		
		// Whichever is the greater distance will be the master
		// From an algorithmic point of view, we'll just consider
		// the master to be X and the slave to be Y, which eliminates
		// the need for mapping quadrants.
		if (Math.abs(endX - currentX) > Math.abs(endY - currentY)) {
			master = motorX;
			slave = motorY;
			x0 = currentX;
			x1 = endX;
			y0 = currentY;
			y1 = endY;
		} else {
			master = motorY;
			slave = motorX;
			x0 = currentY;
			x1 = endY;
			y0 = currentX;
			y1 = endX;
		}
				
		master.setSync(GenericStepperMotor.SYNC_NONE);
		if (y0 < y1)
			slave.setSync(GenericStepperMotor.SYNC_INC);
		else
			slave.setSync(GenericStepperMotor.SYNC_DEC);

		int deltaY = Math.abs(y1 - y0); 
		int deltaX = Math.abs(x1 - x0); 
				
		master.dda(movementSpeed, x1, deltaY);
		
		slave.setSync(GenericStepperMotor.SYNC_NONE);

		currentX = endX;
		currentY = endY;
	}
	
	/*
	 * Print a line.  At the moment this is just the pure 2D Bresenham algorithm.
	 * It would be good to generalise this to a 3D DDA.
	 */
	public void printLine(int startX, int startY, int endX, int endY, int movementSpeed, int extruderSpeed) throws IOException {
		moveTo(startX, startY, movementSpeed);
		extruder.setExtrusion(extruderSpeed);
		moveTo(endX, endX, movementSpeed);
		extruder.setExtrusion(0);
	}

}
