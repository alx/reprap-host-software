package org.reprap.geometry;

import java.io.IOException;

import org.reprap.Printer;
import org.reprap.ReprapException;
import org.reprap.gui.PreviewPanel;
import org.reprap.gui.RepRapBuild;
import org.reprap.machines.MachineFactory;

public class Producer {

	private Printer reprap;
	
	public Producer(PreviewPanel preview, RepRapBuild builder) throws Exception {
		reprap = MachineFactory.create();
		reprap.setPreviewer(preview);
	}
	
	public void Produce() throws IOException, ReprapException {
	
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
	
}
