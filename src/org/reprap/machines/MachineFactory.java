package org.reprap.machines;

import java.net.URL;
import java.util.Properties;

import org.reprap.Printer;
import org.reprap.ReprapException;

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

		String geometry = props.getProperty("Geometry");
		
		if (geometry.compareToIgnoreCase("cartesian") == 0)
		  	return new Reprap(props);
		else if (geometry.compareToIgnoreCase("nullcartesian") == 0)
		    return new NullCartesianMachine(props);		
		else
			throw new ReprapException("Invalid geometry in properties file");
		
	}
	
}
