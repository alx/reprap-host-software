package org.reprap.comms;

import java.io.IOException;

import org.reprap.Device;
import org.reprap.ReprapException;

public abstract class IncomingMessage {
	private byte [] payload; // The actual content portion of a packet, not the frilly bits
	IncomingContext incomingContext;
	
	public class InvalidPayloadException extends ReprapException {
		private static final long serialVersionUID = -5403970405132990115L;
		public InvalidPayloadException() {
			super();
		}
		public InvalidPayloadException(String arg) {
			super(arg);
		}
	}
	
	/**
	 * Receive a message matching context criteria
	 * @param incomingContext the context in which to receive messages
	 * @throws IOException 
	 */
	public IncomingMessage(IncomingContext incomingContext) throws IOException {
		this.incomingContext = incomingContext;
		Communicator comm = incomingContext.getCommunicator();
		comm.receiveMessage(this);
	}

	/**
	 * Send a given message and return the incoming response.  Re-try
	 * if there is a comms problem.
	 * @param message
	 * @throws IOException
	 */
	public IncomingMessage(Device device, OutgoingMessage message, long timeout) {
		Communicator comm = device.getCommunicator();
		for(;;) {
			try {
				incomingContext = comm.sendMessage(device, message);
				comm.receiveMessage(this, timeout);
			} catch (IOException e) {
				System.out.println("IO error/timeout, resending");
				// Just to prevent any unexpected spinning
				try {
					Thread.sleep(1);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				continue;
			}
			return;
		}
	}

	
	/**
	 * Implemented by subclasses to allow them to indicate if they
	 * understand or expect a given packetType.  This is used to
	 * decide if a received packet should be accepted or possibly discarded. 
	 * @param packetType the type of packet to receive
	 * @return
	 */
	protected abstract boolean isExpectedPacketType(byte packetType);
	
	public byte[] getPayload() {
		return payload;
	}

	/**
	 * Called by the framework to provide data to the IncomingMessage.
	 * This should not normally be called by a user. 
	 * @param payload The completed message to insert into the IncomingMessage
	 * @return true is the data was accepted, otherwise false.
	 */
	public boolean receiveData(byte [] payload) {
		// We assume the packet was for us, etc.  But we need to
		// know it contains the correct contents
		if (payload == null || payload.length == 0)
			return false;
		if (isExpectedPacketType(payload[0])) {
			this.payload = (byte[])payload.clone();
			return true;
		} else {
			// That's not what we were after, so discard and wait for more
			return false;
		}
	}
	
}
