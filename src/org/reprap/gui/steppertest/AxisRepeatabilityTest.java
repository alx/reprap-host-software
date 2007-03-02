/**
 * 
 */
package org.reprap.gui.steppertest;

import org.reprap.Preferences;
import org.reprap.comms.snap.SNAPAddress;
import org.reprap.comms.Communicator;
import org.reprap.comms.snap.SNAPCommunicator;
import org.reprap.devices.GenericStepperMotor;
import java.io.*;

/**
 * @author eD Sells
 *
 */
public class AxisRepeatabilityTest {
	
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
	
		// Console Reader
		BufferedReader console = new BufferedReader (new InputStreamReader(System.in));		
		
		// Parameters
		int motorSpeed = 200;
		int stepperExcerciserRepeatabilityRuns = 2;
		int stepperExcerciserRepeatabilityStepsPerStroke = 200;
		int stepperExcerciserRepeatabilityDelay = 10000; 
		
		//Find home
		try
		{		
			motor.seek(motorSpeed, 0);
		}catch(Exception ex)
		{
			System.err.println("Argh 4!");
			return;
		}
		
		System.out.println("Reset your calipers, and then push a return to start...");
		try {
			String trigger = console.readLine();}
		catch(Exception ex)
		{
			System.err.println("Argh 5!");
			return;
		}
				
		for (int i = 0; i <= stepperExcerciserRepeatabilityRuns; i++)
		{
		
			try {		
				motor.seek(motorSpeed, stepperExcerciserRepeatabilityStepsPerStroke);
			}catch(Exception ex)
			{
				System.err.println("Argh 6!");
				return;
			}
			
			try { 
				Thread.sleep(stepperExcerciserRepeatabilityDelay);
			} catch (InterruptedException e) { System.out.println(e);
			}

			try {		
				motor.seek(motorSpeed, 0);
			}catch(Exception ex)
			{
				System.err.println("Argh 7!");
				return;
			}
			
			try {
				Thread.sleep(stepperExcerciserRepeatabilityDelay);
			} catch (InterruptedException e) {
			}
			
		}
		System.out.println("Done");
		communicator.close();
	}

}
