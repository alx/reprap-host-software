package org.reprap.devices;

import java.io.IOException;

import org.reprap.Device;
import org.reprap.comms.Address;
import org.reprap.comms.Communicator;
import org.reprap.comms.OutgoingMessage;
import org.reprap.comms.messages.OutgoingByteMessage;

public class GenericExtruder extends Device {

	public static final byte MSG_SetActive = 1;		
	
	public GenericExtruder(Communicator communicator, Address address) {
		super(communicator, address);
	}

	public void setExtrusion(int speed) throws IOException {
		OutgoingMessage request =
			new OutgoingByteMessage(MSG_SetActive, (byte)speed);
		sendMessage(request);
	}
	
}
