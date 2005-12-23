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
	private GenericStepperMotor motorZ;
	
	private int speedX, speedY, speedZ;
	private int requestedPositionX = 0;
	private int requestedPositionY = 0;
	private int requestedPositionZ = 0;
	private boolean movedX = false;
	private boolean movedY = false;
	private boolean movedZ = false;
	private int reportedPositionX = 0;
	private int reportedPositionY = 0;
	private int reportedPositionZ = 0;
	
	private Communicator comm;
	
	private Main guiToNotify;
	
	public Controller(Main guiToNotify, int speedX, int speedY, int speedZ) throws Exception {
		SNAPAddress myAddress = new SNAPAddress(localNodeNumber); 
		comm = new SNAPCommunicator(commPortName, baudRate, myAddress);
			
		motorX = new GenericStepperMotor(comm, new SNAPAddress(2));
		motorY = new GenericStepperMotor(comm, new SNAPAddress(3));
		motorZ = new GenericStepperMotor(comm, new SNAPAddress(4));

		this.guiToNotify = guiToNotify;
		this.speedX = speedX;
		this.speedY = speedY;
		this.speedZ = speedZ;

		try {  motorX.resetPosition(); } catch (Exception ex) { }
		try {  motorY.resetPosition(); } catch (Exception ex) { }
		try {  motorZ.resetPosition(); } catch (Exception ex) { }
	}
	
	public void updateSpeeds(int speedX, int speedY, int speedZ) throws IOException {
		this.speedX = speedX;
		this.speedY = speedY;
		this.speedZ = speedZ;
		if (movedX)
			setPositionX(requestedPositionX);
		if (movedY)
			setPositionY(requestedPositionY);
		if (movedZ)
			setPositionZ(requestedPositionZ);
	}
	
	public boolean isMovingX() {
		return (movedX && reportedPositionX != requestedPositionX);
	}
	
	public boolean isMovingY() {
		return (movedY && reportedPositionY != requestedPositionY);
	}

	public boolean isMovingZ() {
		return (movedZ && reportedPositionZ != requestedPositionZ);
	}

	public synchronized int getPositionX() throws IOException {
		reportedPositionX = motorX.getPosition(); 
		return reportedPositionX;
	}
	
	public synchronized int getPositionY() throws IOException {
		reportedPositionY = motorY.getPosition(); 
		return reportedPositionY;
	}
	
	public synchronized int getPositionZ() throws IOException {
		reportedPositionZ = motorZ.getPosition(); 
		return reportedPositionZ;
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

	public synchronized void setPositionZ(int position) throws IOException {
		requestedPositionZ = position;
		motorZ.seek(speedZ, position);
		movedZ = true;
	}

	protected void finalize() throws Throwable {
		comm.close();
	}

}
