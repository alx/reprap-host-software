package org.reprap;

import org.reprap.machines.MachineFactory;

public class SquareTest {
	
	public static void main(String[] args) {
		try {
			Printer reprap = MachineFactory.create();
			reprap.calibrate();
			reprap.selectMaterial(0);
			reprap.setSpeed(240);
			reprap.setExtruderSpeed(200);

			// Print a square, rotated 45 degrees
			reprap.moveTo(20, 5, 0);
			reprap.printTo(10, 15, 0);
			reprap.printTo(20, 25, 0);
			reprap.printTo(30, 15, 0);
			reprap.printTo(20, 5, 0);
			
			reprap.terminate();
		}
		catch (Exception ex) {
			System.out.print(ex);
			ex.printStackTrace();
		}
	}
}
