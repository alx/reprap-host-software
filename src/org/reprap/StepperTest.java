package org.reprap;

import org.reprap.comms.Communicator;
import org.reprap.comms.snap.SNAPAddress;
import org.reprap.comms.snap.SNAPCommunicator;
import org.reprap.devices.GenericStepperMotor;

public class StepperTest {

	private final int localNodeNumber = 0;
	private final int baudRate = 19200;
	private final String commPortName = "1";  // Use "0" on linux, "COM1" on Windows, etc
	
	private void test() {
		
		try {
			SNAPAddress myAddress = new SNAPAddress(localNodeNumber); 
			Communicator comm = new SNAPCommunicator(commPortName, baudRate, myAddress);
			
			GenericStepperMotor motor = new GenericStepperMotor(comm, new SNAPAddress(2));
			
			motor.setSpeed(200);
			Thread.sleep(5000);
			motor.setIdle();
			
			comm.close();
			
		} catch(Exception ex) {
			System.out.println(ex.getClass().toString() + ": " + ex.getMessage());
		}
	}

	public static void main(String[] args) {
    	new StepperTest().test();
	}

}
