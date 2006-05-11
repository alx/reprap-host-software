package org.reprap.machines;

import java.util.Properties;

import org.reprap.Printer;
import org.reprap.comms.port.TestPort;
import org.reprap.comms.port.testhandlers.TestExtruder;
import org.reprap.comms.port.testhandlers.TestStepper;
import org.reprap.comms.snap.SNAPAddress;
import org.testng.Assert;

/**
 * 
 * @testng.configuration groups = "comms,all,all-offline"
 *
 */
public class ReprapTest {
	private Printer printer;
	
	/**
	 * @testng.configuration beforeSuite = "true"
	 */
	public void setUp() throws Exception {
		// Set up a configuration for testing
		Properties props = new Properties();
		props.setProperty("PortType", "test");  // Don't use a real port!
		props.setProperty("Geometry", "cartesian");
		props.setProperty("AxisCount", "3");
		props.setProperty("ExtruderCount", "1");
		props.setProperty("Axis1Address", "2");
		props.setProperty("Axis2Address", "3");
		props.setProperty("Axis3Address", "4");
		props.setProperty("Axis1Torque", "100");
		props.setProperty("Axis2Torque", "100");
		props.setProperty("Axis3Torque", "100");
		props.setProperty("Extruder1Address", "8");
		props.setProperty("Extruder1Beta", "5000");
		props.setProperty("Extruder1Rz", "100000");
		
		TestPort port = new TestPort();
		port.addDevice(new TestStepper(), new SNAPAddress(2));
		port.addDevice(new TestStepper(), new SNAPAddress(3));
		port.addDevice(new TestStepper(), new SNAPAddress(4));
		port.addDevice(new TestExtruder(), new SNAPAddress(8));
		printer = MachineFactory.create(props, port);
	}
	
	/**
	 * @testng.configuration afterSuite = "true"
	 */
	public void tearDown() {
		printer.dispose();
	}
	
	/**
	 * @testng.test groups = "comms,all,all-offline"
	 */
	public void testReprapBasic() throws Exception {
		printer.moveTo(0, 5, 0);
		printer.printTo(10, 10, 0);
		Assert.assertEquals(printer.getX(), 10.0, 0.001);
	}
	
}
