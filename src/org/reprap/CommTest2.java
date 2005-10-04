package org.reprap;

import org.reprap.comms.Communicator;
import org.reprap.comms.snap.SNAPAddress;
import org.reprap.comms.snap.SNAPCommunicator;
import org.reprap.devices.GenericThermalSensor;

public class CommTest2 {
	
	private final int localNodeNumber = 0;
	private final int baudRate = 2400;
	private final String commPortName = "1";
	
	
	private void test() {
		
		try {
			SNAPAddress myAddress = new SNAPAddress(localNodeNumber); 
			Communicator comm = new SNAPCommunicator(commPortName, baudRate, myAddress);
			
			GenericThermalSensor sensor = new GenericThermalSensor(comm, new SNAPAddress(2));
			
			int version = sensor.getVersion();
			
			System.out.println("Sensor version is " + version);
			
			comm.close();
			
		} catch(Exception ex) {
			System.out.println("Exception: " + ex.getMessage());
		}
	}
	
    public static void main(String[] args) {
    	new CommTest2().test();
    }

}
