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
 * Send the axis back and forth to check repeatability. 
 * Delay in between incorporated to allow for driver chip cooling.
 * To be used in conjunction with calipers.
 *
 */
public class AxisRepeatabilityTest {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		
		// Test parameters
		int motorId = 2;
		int motorSpeed = 200;
		int axisRepeatabilityRuns = 20;
		int axisRepeatabilityStepsPerStroke = 400;
		int axisRepeatabilityDelay = 3000; 
		
		
		final int localNodeNumber = 0;
		final int baudRate = 19200;


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
		
	
		
//		//Find home
//		try
//		{		
//			motor.homeReset(motorSpeed);
//		}catch(Exception ex)
//		{
//			System.err.println("Argh 4!");
//			return;
//		}
		
		System.out.println("Motor: " + motorId);
		System.out.println("Motor speed: " + motorSpeed);
		System.out.println("Runs: " + axisRepeatabilityRuns);
		System.out.println("Steps per stroke: " + axisRepeatabilityStepsPerStroke);
		System.out.println("Pause between strokes (s): " + (axisRepeatabilityDelay/1000));
		
		System.out.println("\nReset your calipers, and then push return to start...");
		try {
			String trigger = console.readLine();}
		catch(Exception ex)
		{
			System.err.println("Argh 5!");
			return;
		}
				
		for (int i = 1; i <= axisRepeatabilityRuns; i++)
		{
		
			try {		
				motor.seekBlocking(motorSpeed, axisRepeatabilityStepsPerStroke);
			}catch(Exception ex)
			{
				System.err.println("Argh 6!");
				return;
			}
			
			try { 
				Thread.sleep(axisRepeatabilityDelay);
			} catch (InterruptedException e) { System.out.println(e);
			}

			try {		
				motor.seekBlocking(motorSpeed, 0);
			}catch(Exception ex)
			{
				System.err.println("Argh 7!");
				return;
			}
			
			try {
				Thread.sleep(axisRepeatabilityDelay);
			} catch (InterruptedException e) {
			}
			
			System.out.println("Run complete: " + i);
						
		}
		System.out.println("All Done");
		communicator.close();
	}

}
