package org.reprap.machines;

import java.util.Properties;

import org.reprap.Printer;
import org.testng.Assert;

public class ReprapTest {

	Printer reprap;
	
	public void setup() throws Exception {
		Properties props = new Properties();
		props.setProperty("Geometry", "cartesian");
		reprap = MachineFactory.create(props);
	}
	
	public void teardown() {
		
	}
	/**
	 * @testng.test groups = "geometry,comms,all,all-offline"
	 */
	public void testXXX() {
		
	}

}
