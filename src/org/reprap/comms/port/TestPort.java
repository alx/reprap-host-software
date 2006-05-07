package org.reprap.comms.port;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.reprap.comms.snap.SNAPAddress;
import org.reprap.devices.TestDevice;

public class TestPort implements Port {
	
	private HashMap deviceMap;
	
	public TestPort() {
		deviceMap = new HashMap();
	}
	
	public OutputStream getOutputStream() throws IOException {
		return System.out;
	}

	public InputStream getInputStream() throws IOException {
		return System.in;
	}

	public void close() {
	}
	
	/**
	 * Called by a device to register itself on the TestPort
	 * @param device
	 * @param address
	 */
	public void addDevice(TestDevice device, SNAPAddress address) {
	  deviceMap.put(address, device);	
	}

}
