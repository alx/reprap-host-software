package org.reprap.devices;

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

	public void setExtrusion(boolean active) {
		OutgoingMessage request =
			new OutgoingByteMessage(MSG_SetActive, (byte)(active?1:0));
	}
	
}
