package org.reprap.comms.snap;

import org.reprap.comms.port.Port;
import org.reprap.comms.port.TestPort;
import org.reprap.devices.TestDevice;
import org.testng.Assert;

public class SNAPTest {
	
	private Port port;
	
	public void setup() {
		TestPort testport;
		port = testport = new TestPort();
		
		//new TestDevice(testport, new SNAPAddress(2));
	}
	
	public void teardown() {
		
	}

	/**
	 * @testng.test groups = "comms,all,all-offline"
	 */
	public void testXXX() {
		
	}
	
	
}
