package org.reprap;

import org.reprap.gui.PreviewWindow;
import org.reprap.machines.MachineFactory;

/**
 * Class to execute a test where a square is printed
 */
public class SquareTest {
	
	public static void main(String[] args) throws Exception {
	
		Printer reprap = MachineFactory.create();

		// Comment out the following three
		// lines if you don't have java3d or don't want to preview
		PreviewWindow preview = new PreviewWindow(reprap);
		preview.setVisible(true);
		reprap.setPreviewer(preview);

		reprap.calibrate();
		reprap.selectExtruder(0);
		reprap.setSpeed(reprap.getExtruder().getXYSpeed());  
		//reprap.getExtruder().setExtruderSpeed(180);
		reprap.getExtruder().heatOn();

		// Print a square, rotated 45 degrees
		reprap.moveTo(20, 5, 2, true, false);
		reprap.printTo(15, 10, 2, false);
		reprap.printTo(20, 15, 2, false);
		reprap.printTo(25, 10, 2, false);
		reprap.printTo(20, 5, 2, true);
		
		reprap.terminate();
	}
}
