package org.reprap;

import org.reprap.machines.MachineFactory;

public class SquareTest {
	
	public static void main(String[] args) {
		try {
			Printer reprap = MachineFactory.create();
			reprap.calibrate();
			reprap.selectMaterial(0);
			reprap.setSpeed(248);
			reprap.setExtruderSpeed(180);

			// Print a square, rotated 45 degrees
			reprap.moveTo(20, 5, 0);
			reprap.printTo(15, 10, 0);
			reprap.printTo(20, 15, 0);
			reprap.printTo(25, 10, 0);
			reprap.printTo(20, 5, 0);
			
			reprap.terminate();
		}
		catch (Exception ex) {
			System.out.print(ex);
			ex.printStackTrace();
		}
	}
}
