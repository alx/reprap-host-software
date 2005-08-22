package org.reprap;

import org.reprap.comms.Address;
import org.reprap.comms.Communicator;
import org.reprap.comms.IncomingContext;
import org.reprap.comms.OutgoingMessage;
import org.reprap.comms.messages.VersionRequestMessage;

public abstract class Device {

	private Address address;
	private Communicator communicator;

	public Device(Communicator communicator, Address address) {
		this.communicator = communicator;
		this.address = address;
	}

	public Address getAddress() {
		return address;
	}

	public Communicator getCommunicator() {
		return communicator;
	}

	public int getVersion() {
		VersionRequestMessage vm = new VersionRequestMessage();
		sendMessage(vm);
		return 1; 
	}
	
	public IncomingContext sendMessage(OutgoingMessage message) {
		return communicator.sendMessage(this, message);
	}
	
}
