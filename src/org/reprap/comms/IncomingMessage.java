package org.reprap.comms;

import java.io.IOException;

import org.reprap.ReprapException;

public abstract class IncomingMessage {
	private byte [] payload; // The actual content portion of a packet, not the frilly bits
	IncomingContext incomingContext;
	
	public class InvalidPayloadException extends ReprapException {
		static final long serialVersionUID = 0;
		public InvalidPayloadException() {
			super();
		}
		public InvalidPayloadException(String arg0) {
			super(arg0);
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
		comm.ReceiveMessage(this);
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

	public boolean receiveData(byte [] payload) {
		// We assume the packet was for us, etc.  But we need to
		// know it contains the correct contents
		if (isExpectedPacketType(payload[0])) {
			this.payload = payload;
			return true;
		} else {
			// That's not what we were after, so discard and wait for more
			return false;
		}
	}

}
