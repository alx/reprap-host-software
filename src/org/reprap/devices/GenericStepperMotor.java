package org.reprap.devices;

import java.io.IOException;

import org.reprap.Device;
import org.reprap.ReprapException;
import org.reprap.comms.Address;
import org.reprap.comms.Communicator;
import org.reprap.comms.IncomingContext;
import org.reprap.comms.OutgoingMessage;
import org.reprap.comms.IncomingMessage;
import org.reprap.comms.messages.IncomingIntMessage;
import org.reprap.comms.messages.OutgoingBlankMessage;
import org.reprap.comms.messages.OutgoingIntMessage;

public class GenericStepperMotor extends Device {

	public static final byte MSG_SetForward = 1;		
	public static final byte MSG_SetReverse = 2;
	public static final byte MSG_SetPosition = 3;
	public static final byte MSG_GetPosition = 4;
	public static final byte MSG_Seek = 5;	
	public static final byte MSG_SetIdle = 6;		

	
	public GenericStepperMotor(Communicator communicator, Address address) {
		super(communicator, address);
	}

	/**
	 * Set the motor speed (or turn it off) 
	 * @param speed A value between -255 and 255.
	 * @throws ReprapException
	 * @throws IOException
	 */
	public void setSpeed(int speed) throws ReprapException, IOException {
		OutgoingMessage request = new RequestSetSpeed(speed);
		sendMessage(request);
	}

	public void setIdle() throws IOException {
		OutgoingMessage request = new RequestSetSpeed();
		sendMessage(request);
	}
	
	public void resetPosition() throws IOException {
		setPosition(0);
	}
	
	public void setPosition(int position) throws IOException {
		sendMessage(new RequestSetPosition(position));
	}
	
	public int getPosition() throws IOException {
		IncomingContext replyContext = sendMessage(
				new OutgoingBlankMessage(MSG_GetPosition));
		
		IncomingIntMessage reply = new RequestPositionResponse(replyContext);
		try {
			int value = reply.getValue();
			return value;
		}
		catch (IncomingMessage.InvalidPayloadException ex) {
			throw new IOException(ex.getMessage());
		}
	}
	
	public void seek(int speed, int position) throws IOException {
		sendMessage(new RequestSeekPosition(speed, position));		
	}
	
	protected class RequestPositionResponse extends IncomingIntMessage {
		public RequestPositionResponse(IncomingContext incomingContext) throws IOException {
			super(incomingContext);
		}
		
		protected boolean isExpectedPacketType(byte packetType) {
			return packetType == MSG_GetPosition; 
		}
	}
	
	protected class RequestSetPosition extends OutgoingIntMessage {
		public RequestSetPosition(int position) {
			super(MSG_SetPosition, position);
		}
	}
	
	protected class RequestSetSpeed extends OutgoingMessage {

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
		
		public byte[] getBinary() {
			return message;
		}
		
	}
	
	protected class RequestSeekPosition extends OutgoingMessage {
		byte [] message;
		RequestSeekPosition(int speed, int position) {
			message = new byte[] { MSG_Seek,
					(byte)speed,
					(byte)(position & 0xff),
					(byte)((position >> 8) & 0xff)};
		}
		
		public byte[] getBinary() {
			return message;
		}
		
	}

	
}
