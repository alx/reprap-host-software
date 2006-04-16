package org.reprap.machines;

import java.net.URL;
import java.util.Properties;

import org.reprap.Printer;

/**
 * 
 * Returns an appropriate Printer object based on the current properties
 * 
 */
public class MachineFactory {

	private MachineFactory() {
	}
	
	static public Printer create() throws Exception {
		// Currently this just always assumes we're building
		// a 3-axis cartesian printer.  It should build an
		// appropriate type based on the local configuration.
		
		Properties props = new Properties();
		URL url = ClassLoader.getSystemResource("reprap.properties");
		props.load(url.openStream());

		return new Reprap(props);
	}
	
}
