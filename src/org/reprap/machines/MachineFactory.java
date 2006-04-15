package org.reprap.machines;

import org.reprap.Printer;

/**
 * 
 * Returns an appropriate Printer object based on the current properties
 * 
 */
public class MachineFactory {

	private MachineFactory() {
	}
	
	Printer create() {
		// Currently this just always assumes we're building
		// a 3-axis cartesian printer
		return new Reprap();
	}
	
}
