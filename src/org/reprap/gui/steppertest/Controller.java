package org.reprap.steppertestgui;

import java.io.IOException;

import org.reprap.comms.Communicator;
import org.reprap.comms.snap.SNAPAddress;
import org.reprap.comms.snap.SNAPCommunicator;
import org.reprap.devices.GenericStepperMotor;

public class Controller {

	private final int localNodeNumber = 0;
	private final int baudRate = 19200;
	private final String commPortName = "1";  // Use "0" on linux, "COM1" on Windows, etc
	
	private GenericStepperMotor motorX; 
	private GenericStepperMotor motorY;
	
	private int speedX, speedY;
	private int requestedPositionX = 0;
	private int requestedPositionY = 0;
	private boolean movedX = false;
	private boolean movedY = false;
	private int reportedPositionX = 0;
	private int reportedPositionY = 0;
	
	private Communicator comm;
	
	private Main guiToNotify;
	
	public Controller(Main guiToNotify, int speedX, int speedY) throws Exception {
		SNAPAddress myAddress = new SNAPAddress(localNodeNumber); 
		comm = new SNAPCommunicator(commPortName, baudRate, myAddress);
			
		motorX = new GenericStepperMotor(comm, new SNAPAddress(2));
		motorY = new GenericStepperMotor(comm, new SNAPAddress(4));

		this.guiToNotify = guiToNotify;
		this.speedX = speedX;
		this.speedY = speedY;
		
		motorX.resetPosition();
		motorY.resetPosition();
	}
	
	public void updateSpeeds(int speedX, int speedY) throws IOException {
		this.speedX = speedX;
		this.speedY = speedY;
		if (movedX)
			setPositionX(requestedPositionX);
		if (movedY)
			setPositionY(requestedPositionY);
	}
	
	public boolean isMovingX() {
		return (movedX && reportedPositionX != requestedPositionX);
	}
	
	public boolean isMovingY() {
		return (movedY && reportedPositionY != requestedPositionY);
	}

	public synchronized int getPositionX() throws IOException {
		reportedPositionX = motorX.getPosition(); 
		return reportedPositionX;
	}
	
	public synchronized int getPositionY() throws IOException {
		reportedPositionY = motorY.getPosition(); 
		return reportedPositionY;
	}
	
	public synchronized void setPositionX(int position) throws IOException {
		requestedPositionX = position;
		motorX.seek(speedX, position);
		movedX = true;
	}
	
	public synchronized void setPositionY(int position) throws IOException {
		requestedPositionY = position;
		motorY.seek(speedY, position);
		movedY = true;
	}

	protected void finalize() throws Throwable {
		comm.close();
	}

}
