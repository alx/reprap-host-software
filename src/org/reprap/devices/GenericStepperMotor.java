package org.reprap.devices;

import java.io.IOException;

import org.reprap.Device;
import org.reprap.ReprapException;
import org.reprap.comms.Address;
import org.reprap.comms.Communicator;
import org.reprap.comms.OutgoingMessage;

public class GenericStepperMotor extends Device {

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
	
	public class RequestSetSpeed extends OutgoingMessage {
		public static final int MSG_SetForward = 1;		
		public static final int MSG_SetReverse = 2;		
		public static final int MSG_SetIdle = 6;		

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
	
}
