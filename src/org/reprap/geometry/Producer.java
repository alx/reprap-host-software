package org.reprap.geometry;

import org.reprap.Printer;
import org.reprap.gui.PreviewPanel;
import org.reprap.gui.RepRapBuild;
import org.reprap.machines.MachineFactory;

public class Producer {

	private Printer reprap;
	
	public Producer(PreviewPanel preview, RepRapBuild builder) throws Exception {
		reprap = MachineFactory.create();
		reprap.setPreviewer(preview);
	}
	
	public void produce() throws Exception {
	
		reprap.initialise();
		reprap.selectMaterial(0);
		reprap.setSpeed(248);
		reprap.setExtruderSpeed(180);
		reprap.setTemperature(40);

		// TODO This should be a loop over all segments as long as reprap.isCancelled() is false.
		// For now, print a square, rotated 45 degrees
		if (!reprap.isCancelled()) reprap.moveTo(20, 5, 0);
		if (!reprap.isCancelled()) reprap.printTo(15, 10, 0);
		if (!reprap.isCancelled()) reprap.printTo(20, 15, 0);
		if (!reprap.isCancelled()) reprap.printTo(25, 10, 0);
		if (!reprap.isCancelled()) reprap.printTo(20, 5, 0);
		
		reprap.terminate();

	}
	
	public void dispose() {
		reprap.dispose();
	}
	
}
