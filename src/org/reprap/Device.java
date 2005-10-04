package org.reprap;

import java.io.IOException;

import org.reprap.comms.Address;
import org.reprap.comms.Communicator;
import org.reprap.comms.IncomingContext;
import org.reprap.comms.OutgoingMessage;
import org.reprap.comms.IncomingMessage.InvalidPayloadException;
import org.reprap.comms.messages.VersionRequestMessage;
import org.reprap.comms.messages.VersionResponseMessage;

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

	public int getVersion() throws IOException, InvalidPayloadException {
		VersionRequestMessage request = new VersionRequestMessage();
		IncomingContext replyContext = sendMessage(request);
		VersionResponseMessage reply = new VersionResponseMessage(replyContext);
		return reply.getVersion(); 
	}
	
	public IncomingContext sendMessage(OutgoingMessage message) throws IOException {
		return communicator.sendMessage(this, message);
	}
	
}
