/**
 * 
 */
package org.reprap.gui.steppertest;

import org.reprap.Preferences;
import org.reprap.comms.snap.SNAPAddress;
import org.reprap.comms.Communicator;
import org.reprap.comms.snap.SNAPCommunicator;
import org.reprap.devices.GenericStepperMotor;

/**
 * @author eD Sells
 *
 */
public class StepperExerciser {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		final int localNodeNumber = 0;
		final int baudRate = 19200;
		int motorId = 3;
		int address;
		try
		{
			address = Preferences.loadGlobalInt("Axis" + motorId + "Address");
		}catch(Exception ex)
		{
			System.err.println("Argh 1!");
			return;
		}
		// Request Stepper speed
		// Request Step number
		// Request delay between strokes (for cooling)
		// Request number of runs (includes return stroke)
		
		int stepperExcerciserRepeatabilityRuns = 2;
		int stepperExcerciserRepeatabilityStepsPerStroke = 2000;
		
		SNAPAddress myAddress = new SNAPAddress(localNodeNumber);
		Communicator communicator;
		try
		{		
			communicator = new SNAPCommunicator(Preferences.loadGlobalString("Port"),
				baudRate, myAddress);
		}catch(Exception ex)
		{
			System.err.println("Argh 2!");
			return;
		}
		GenericStepperMotor motor;
		try
		{			
			motor = new GenericStepperMotor(communicator, 
				new SNAPAddress(address), Preferences.getGlobalPreferences(), motorId);
		}catch(Exception ex)
		{
			System.err.println("Argh 3!");
			return;
		}
		try
		{		
			motor.seek(100, 300);
		}catch(Exception ex)
		{
			System.err.println("Argh 4!");
			return;
		}
		
//		for (int i = 0; i <= stepperExcerciserRepeatabilityRuns; i++)
//		{
//			for (int j = 1; j <= stepperExcerciserRepeatabilityStepsPerStroke; j++)
//			{
//				// Go out
//			}
//			 
//			for (int k = 1; k <= stepperExcerciserRepeatabilityStepsPerStroke; k++)
//			{
//				// Come back
//			}
//			
//		}
		
	}

}
