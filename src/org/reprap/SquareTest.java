package org.reprap;

import org.reprap.gui.PreviewWindow;
import org.reprap.machines.MachineFactory;

public class SquareTest {
	
	public static void main(String[] args) throws Exception {
	
		Printer reprap = MachineFactory.create();

		// Comment out the following three
		// lines if you don't have java3d or don't want to preview
		PreviewWindow preview = new PreviewWindow();
		preview.setVisible(true);
		reprap.setPreviewer(preview);

		reprap.calibrate();
		reprap.selectMaterial(0);
		reprap.setSpeed(248);
		reprap.setExtruderSpeed(180);

		// Print a square, rotated 45 degrees
		reprap.moveTo(20, 5, 2);
		reprap.printTo(15, 10, 2);
		reprap.printTo(20, 15, 2);
		reprap.printTo(25, 10, 2);
		reprap.printTo(20, 5, 2);
		
		reprap.terminate();
	}
}
