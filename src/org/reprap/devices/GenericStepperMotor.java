package org.reprap.devices;

import java.io.IOException;
import org.reprap.utilities.Debug;
import org.reprap.Device;
import org.reprap.Preferences;
import org.reprap.ReprapException;
import org.reprap.comms.Address;
import org.reprap.comms.Communicator;
import org.reprap.comms.IncomingContext;
import org.reprap.comms.OutgoingMessage;
import org.reprap.comms.IncomingMessage;
import org.reprap.comms.IncomingMessage.InvalidPayloadException;
import org.reprap.comms.messages.IncomingIntMessage;
import org.reprap.comms.messages.OutgoingAddressMessage;
import org.reprap.comms.messages.OutgoingBlankMessage;
import org.reprap.comms.messages.OutgoingByteMessage;
import org.reprap.comms.messages.OutgoingIntMessage;

/**
 *
 */
public class GenericStepperMotor extends Device {
	
	/**
	 * API for firmware
	 * Activate the stepper motor in forward direction 
	 */
	public static final byte MSG_SetForward = 1;
	
	/**
	 * Activate the stepper motor in reverse direction 
	 */
	public static final byte MSG_SetReverse = 2;
	
	/**
	 *  Set the stepper motor position (how?)
	 */
	public static final byte MSG_SetPosition = 3;
	
	/**
	 * Get the current stepper motor position
	 */
	public static final byte MSG_GetPosition = 4;
	
	/**
	 * 
	 */
	public static final byte MSG_Seek = 5;	
	
	/**
	 * Set the motor to idle; this turns the torque off whereas speed = 0 keeps torque on 
	 */
	public static final byte MSG_SetIdle = 6;		
	
	/**
	 * Set notification (?) 
	 */
	public static final byte MSG_SetNotification = 7;
	
	/**
	 * Set the sync mode (?)
	 */
	public static final byte MSG_SetSyncMode = 8;
	
	/**
	 * Calibrate (?) 
	 */
	public static final byte MSG_Calibrate = 9;
	
	/**
	 * Get the range (?)  
	 */
	public static final byte MSG_GetRange = 10;
	
	/**
	 * DDAMaster ?
	 */
	public static final byte MSG_DDAMaster = 11;
	
	/**
	 * Move on step in forward direction 
	 */
	public static final byte MSG_StepForward = 12;
	
	/**
	 * Move on step in backward direction
	 */
	public static final byte MSG_StepBackward = 13;	
	
	/**
	 * Set the power to the stepper motor 
	 */
	public static final byte MSG_SetPower = 14;
	
	/**
	 * Homereset(?? 
	 */
	public static final byte MSG_HomeReset = 16;


	/**
	 * 
	 */
	public static final byte SYNC_NONE = 0;
	public static final byte SYNC_SEEK = 1;
	public static final byte SYNC_INC = 2;
	public static final byte SYNC_DEC = 3;
	
	/**
	 * 
	 */
	private boolean haveInitialised = false;
	private boolean haveSetNotification = false;
	private boolean haveCalibrated = false;
	
	
	/**
	 * Useful to know what we're called
	 */
	private String axis;
	

	
	/**
	 * Power output limiting (0-100 percent)
	 */
	private int maxTorque;
	
	/**
	 * @param communicator
	 * @param address
	 * @param prefs
	 * @param motorId
	 */
	public GenericStepperMotor(Communicator communicator, Address address, Preferences prefs, int motorId) {
		
		//Address address, int maxTorque
		super(communicator, address);
			
		switch(motorId)
		{
		case 1:
			axis = "X";
			break;
		case 2:
			axis = "Y";
			break;
		case 3:
			axis = "Z";
			break;
		default:
			axis = "X";
			System.err.println("GenericStepperMotor - dud axis id: " + motorId);
				
		}
		this.maxTorque = prefs.loadInt(axis + "Axis" + "Torque(%)");
	}

	/**
	 * @throws IOException
	 */
	private void initialiseIfNeeded() throws IOException {
		if (!haveInitialised) {
			haveInitialised = true;
			setMaxTorque(maxTorque);
		}
	}
	
	/**
	 * Dispose of this object
	 */
	public void dispose() {
	}
	
	/**
	 * Set the motor speed (or turn it off) 
	 * @param speed A value between -255 and 255.
	 * @throws ReprapException
	 * @throws IOException
	 */
	public void setSpeed(int speed) throws IOException {
		initialiseIfNeeded();
		lock();
		Debug.d(axis + " axis - setting speed: " + speed);
		try {
			OutgoingMessage request = new RequestSetSpeed(speed);
			sendMessage(request);
		}
		finally {
			unlock();
		}
	}

	/**
	 * @throws IOException
	 */
	public void setIdle() throws IOException {
		initialiseIfNeeded();
		lock();
		Debug.d(axis + " axis - going idle.");
		try {
			OutgoingMessage request = new RequestSetSpeed();
			sendMessage(request);
		}
		finally {
			unlock();
		}
	}
	
	/**
	 * @throws IOException
	 */
	public void stepForward() throws IOException {
		initialiseIfNeeded();
		lock();
		Debug.d(axis + " axis - stepping forward.");		
		try {
			OutgoingMessage request = new RequestOneStep(true);
			sendMessage(request);
		}
		finally {
			unlock();
		}
	}
	
	/**
	 * @throws IOException
	 */
	public void stepBackward() throws IOException {
		initialiseIfNeeded();
		lock();
		Debug.d(axis + " axis - stepping backward.");	
		try {
			OutgoingMessage request = new RequestOneStep(false);
			sendMessage(request);
		}
		finally {
			unlock();
		}
	}
	
	/**
	 * @throws IOException
	 */
	public void resetPosition() throws IOException {
		setPosition(0);
	}
	
	/**
	 * @param position
	 * @throws IOException
	 */
	public void setPosition(int position) throws IOException {
		initialiseIfNeeded();
		lock();
		Debug.d(axis + " axis - setting position to: " + position);	
		try {
			sendMessage(new RequestSetPosition(position));
		}
		finally {
			unlock();
		}
	}
	
	/**
	 * @return current position of the motor
	 * @throws IOException
	 */
	public int getPosition() throws IOException {
		int value;
		initialiseIfNeeded();
		lock();
		Debug.d(axis + " axis - getting position.  It is... ");
		try {
			IncomingContext replyContext = sendMessage(
					new OutgoingBlankMessage(MSG_GetPosition));
			
			IncomingIntMessage reply = new RequestPositionResponse(replyContext);
			try {
				value = reply.getValue();
			}
			catch (IncomingMessage.InvalidPayloadException ex) {
				throw new IOException(ex.getMessage());
			}
		}
		finally {
			unlock();
		}
		Debug.d("..." + value);		
		return value;
	}
	
	/**
	 * @param speed
	 * @param position
	 * @throws IOException
	 */
	public void seek(int speed, int position) throws IOException {
		initialiseIfNeeded()	;
		lock();
		Debug.d(axis + " axis - seeking position " + position + " at speed " + speed);
		try {
			sendMessage(new RequestSeekPosition(speed, position));
		}
		finally {
			unlock();
		}
	}

	/**
	 * @param speed
	 * @param position
	 * @throws IOException
	 */
	public void seekBlocking(int speed, int position) throws IOException {
		initialiseIfNeeded();
		lock();
		Debug.d(axis + " axis - seeking-blocking position " + position + " at speed " + speed);
		try {
			setNotification();
			new RequestSeekResponse(this, new RequestSeekPosition(speed, position), 50000);
			setNotificationOff();
		} catch (Exception e) {
			// TODO: Nasty error. But WTF do we do about it?
			e.printStackTrace();
		} finally {
			unlock();
		}
	}

	/**
	 * @param speed
	 * @return range of the motor
	 * @throws IOException
	 * @throws InvalidPayloadException
	 */
	public Range getRange(int speed) throws IOException, InvalidPayloadException {
		initialiseIfNeeded()	;
		lock();
		Debug.d(axis + " axis - getting range.");
		try {
			if (haveCalibrated) {
				IncomingContext replyContext = sendMessage(
						new OutgoingBlankMessage(MSG_GetRange));
				RequestRangeResponse response = new RequestRangeResponse(replyContext);
				return response.getRange();
			} else {
				setNotification();
				IncomingContext replyContext = sendMessage(
						new OutgoingByteMessage(MSG_Calibrate, (byte)speed));
				RequestRangeResponse response = new RequestRangeResponse(replyContext);
				setNotificationOff();
				return response.getRange();
			}
		}
		finally {
			unlock();
		}
	}
	
	/**
	 * @param speed
	 * @throws IOException
	 * @throws InvalidPayloadException
	 */
	public void homeReset(int speed) throws IOException, InvalidPayloadException {
		initialiseIfNeeded()	;
		lock();
		Debug.d(axis + " axis - home reset at speed " + speed);
		try {
			setNotification();
			new RequestHomeResetResponse(this, new OutgoingByteMessage(MSG_HomeReset, (byte)speed), 60000);
			setNotificationOff();
		} finally {
			unlock();
		}
	}
	
	/**
	 * @param syncType
	 * @throws IOException
	 */
	public void setSync(byte syncType) throws IOException {
		initialiseIfNeeded()	;
		lock();
		Debug.d(axis + " axis - setting sync to " + syncType);
		try {
			sendMessage(
					new OutgoingByteMessage(MSG_SetSyncMode, syncType));
		}
		finally {
			unlock();
		}
		
	}
	
	/**
	 * @param speed
	 * @param x1
	 * @param deltaY
	 * @throws IOException
	 */
	public void dda(int speed, int x1, int deltaY) throws IOException {
		initialiseIfNeeded()	;
		lock();
		Debug.d(axis + " axis - dda at speed " + speed + ". x1 = " + x1 + ", deltaY = " + deltaY);
		try {
			setNotification();
			
			new RequestDDAMasterResponse(this, new RequestDDAMaster(speed, x1, deltaY), 60000);
			
			setNotificationOff();
		}
		finally {
			unlock();
		}
	}
	
	/**
	 * @throws IOException
	 */
	private void setNotification() throws IOException {
		initialiseIfNeeded()	;
		Debug.d(axis + " axis - setting notification on.");
		if (!haveSetNotification) {
			sendMessage(new OutgoingAddressMessage(MSG_SetNotification,
					getCommunicator().getAddress()));
			haveSetNotification = true;
		}
	}

	/**
	 * @throws IOException
	 */
	private void setNotificationOff() throws IOException {
		initialiseIfNeeded()	;
		Debug.d(axis + " axis - setting notification off.");
		if (haveSetNotification) {
			sendMessage(new OutgoingAddressMessage(MSG_SetNotification, getAddress().getNullAddress()));
			haveSetNotification = false;
		}
	}

	/**
	 * 
	 * @param maxTorque An integer value 0 to 100 representing the maximum torque percentage
	 * @throws IOException
	 */
	public void setMaxTorque(int maxTorque) throws IOException {
		initialiseIfNeeded()	;
		if (maxTorque > 100) maxTorque = 100;
		double power = maxTorque * 68.0 / 100.0;
		byte scaledPower = (byte)power;
		lock();
		Debug.d(axis + " axis - setting maximum torque to: " + maxTorque);
		try {
			sendMessage(
					new OutgoingByteMessage(MSG_SetPower, scaledPower));
		}
		finally {
			unlock();
		}
		
	}
	
	
	/**
	 *
	 */
	protected class RequestPositionResponse extends IncomingIntMessage {
		
		/**
		 * @param incomingContext
		 * @throws IOException
		 */
		public RequestPositionResponse(IncomingContext incomingContext) throws IOException {
			super(incomingContext);
		}
		
		/* (non-Javadoc)
		 * @see org.reprap.comms.messages.IncomingIntMessage#isExpectedPacketType(byte)
		 */
		protected boolean isExpectedPacketType(byte packetType) {
			return packetType == MSG_GetPosition; 
		}
	}

	/**
	 *
	 */
	protected class RequestSeekResponse extends IncomingIntMessage {
		
		/**
		 * @param device
		 * @param message
		 * @param timeout
		 * @throws IOException
		 */
		public RequestSeekResponse(Device device, OutgoingMessage message, long timeout) throws IOException {
			super(device, message, timeout);
		}
		
		/* (non-Javadoc)
		 * @see org.reprap.comms.messages.IncomingIntMessage#isExpectedPacketType(byte)
		 */
		protected boolean isExpectedPacketType(byte packetType) {
			return packetType == MSG_Seek; 
		}
	}
	
	/**
	 *
	 */
	protected class RequestSetPosition extends OutgoingIntMessage {
		/**
		 * @param position
		 */
		public RequestSetPosition(int position) {
			super(MSG_SetPosition, position);
		}
	}

	protected class RequestSetSpeed extends OutgoingMessage {

		/**
		 * 
		 */
		byte [] message;
		
		/**
		 * The empty constructor will create a message to idle the motor
		 */
		RequestSetSpeed() {
			message = new byte [] { MSG_SetIdle }; 
		}
		
		/**
		 * Create a message for setting the motor speed.
		 * @param speed The speed to set the motor to.  Note that specifying
		 * 0 will stop the motor and hold it at 0 and thus still draws
		 * high current.  To idle the motor, use the message created by the
		 * empty constructor.
		 */
		RequestSetSpeed(int speed) {
			byte command;
			if (speed >= 0) {
				command = MSG_SetForward;
			} else {
				command = MSG_SetReverse;
				speed = -speed;
			}
			if (speed > 255) speed = 255;
			message = new byte[] { command, (byte)speed };
				
		}
		
		/* (non-Javadoc)
		 * @see org.reprap.comms.OutgoingMessage#getBinary()
		 */
		public byte[] getBinary() {
			return message;
		}
		
	}
	
	/**
	 *
	 */
	protected class RequestOneStep extends OutgoingMessage
	{
		/**
		 * 
		 */
		byte [] message;
		
		/**
		 * @param forward
		 */
		RequestOneStep(boolean forward) {
			byte command;
			if (forward)
				command = MSG_StepForward;
			else
				command = MSG_StepBackward;
			message = new byte[] { command };
				
		}
		
		/* (non-Javadoc)
		 * @see org.reprap.comms.OutgoingMessage#getBinary()
		 */
		public byte[] getBinary() {
			return message;
		}
	}
	
	/**
	 *
	 */
	protected class RequestSeekPosition extends OutgoingMessage {
		/**
		 * 
		 */
		byte [] message;
		
		/**
		 * @param speed
		 * @param position
		 */
		RequestSeekPosition(int speed, int position) {
			message = new byte[] { MSG_Seek,
					(byte)speed,
					(byte)(position & 0xff),
					(byte)((position >> 8) & 0xff)};
		}
		
		/* (non-Javadoc)
		 * @see org.reprap.comms.OutgoingMessage#getBinary()
		 */
		public byte[] getBinary() {
			return message;
		}
		
	}

	/**
	 *
	 */
	protected class RequestDDAMaster extends OutgoingMessage {
		byte [] message;
		
		/**
		 * @param speed
		 * @param x1
		 * @param deltaY
		 */
		RequestDDAMaster(int speed, int x1, int deltaY) {
			message = new byte[] { MSG_DDAMaster,
					(byte)speed,
					(byte)(x1 & 0xff),
					(byte)((x1 >> 8) & 0xff),
					(byte)(deltaY & 0xff),
					(byte)((deltaY >> 8) & 0xff)
				};
		}
		
		/* (non-Javadoc)
		 * @see org.reprap.comms.OutgoingMessage#getBinary()
		 */
		public byte[] getBinary() {
			return message;
		}
		
	}

	/**
	 *
	 */
	protected class RequestDDAMasterResponse extends IncomingIntMessage {
		
		/**
		 * @param device
		 * @param message
		 * @param timeout
		 * @throws IOException
		 */
		public RequestDDAMasterResponse(Device device, OutgoingMessage message, long timeout) throws IOException {
			super(device, message, timeout);
		}
		
		/* (non-Javadoc)
		 * @see org.reprap.comms.messages.IncomingIntMessage#isExpectedPacketType(byte)
		 */
		protected boolean isExpectedPacketType(byte packetType) {
			return packetType == MSG_DDAMaster;
		}
	}
	
	/**
	 *
	 */
	protected class RequestRangeResponse extends IncomingIntMessage {
		
		/**
		 * @param incomingContext
		 * @throws IOException
		 */
		public RequestRangeResponse(IncomingContext incomingContext) throws IOException {
			super(incomingContext);
		}
		
		/* (non-Javadoc)
		 * @see org.reprap.comms.messages.IncomingIntMessage#isExpectedPacketType(byte)
		 */
		protected boolean isExpectedPacketType(byte packetType) {
			// We could get this either as an asynchronous notification
			// from calibration or by explicit request
			return packetType == MSG_GetRange || packetType == MSG_Calibrate; 
		}
		
		/**
		 * @return
		 * @throws InvalidPayloadException
		 */
		public Range getRange() throws InvalidPayloadException {
		    byte [] reply = getPayload();
		    if (reply == null || reply.length != 3)
		    	throw new InvalidPayloadException("Unexpected payload getting range");
		    Range r = new Range();
		    r.minimum = 0;
		    r.maximum = IncomingIntMessage.ConvertBytesToInt(reply[1], reply[2]);
		    return r;
		}

	}
	
	/**
	 *
	 */
	protected class RequestHomeResetResponse extends IncomingMessage {
		
		/**
		 * @param device
		 * @param message
		 * @param timeout
		 * @throws IOException
		 */
		public RequestHomeResetResponse(Device device, OutgoingMessage message, long timeout) throws IOException {
			super(device, message, timeout);
		}
		
		/* (non-Javadoc)
		 * @see org.reprap.comms.IncomingMessage#isExpectedPacketType(byte)
		 */
		protected boolean isExpectedPacketType(byte packetType) {
			return packetType == MSG_HomeReset; 
		}
	}
	
	/**
	 *
	 */
	public class Range {
		
		/**
		 * 
		 */
		public int minimum;
		
		/**
		 * 
		 */
		public int maximum;
	}

}
