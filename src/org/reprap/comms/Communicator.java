package org.reprap.comms;

import java.io.IOException;

import org.reprap.Device;
import org.reprap.comms.OutgoingMessage;

public interface Communicator {
	
	public IncomingContext sendMessage(Device device,
			OutgoingMessage messageToSend) throws IOException;

	public void receiveMessage(IncomingMessage message) throws IOException;
	
	public void close();
	
	public void dispose();
	
	public Address getAddress();
	
	// These methods will be removed when the asynchronous contexts are completed
	public void lock();
	public void unlock();
}
