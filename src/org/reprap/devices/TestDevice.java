package org.reprap.devices;

import org.reprap.Device;
import org.reprap.comms.Address;
import org.reprap.comms.Communicator;
import org.reprap.comms.port.TestPort;
import org.reprap.comms.snap.SNAPAddress;

public class TestDevice extends Device {
	
	private TestPort port;
	
	public TestDevice(Communicator communicator, Address address) {
		super(communicator, address);
		this.port = port;
		//port.addDevice(this, address);
	}
	
}
