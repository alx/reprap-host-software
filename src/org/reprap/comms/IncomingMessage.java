package org.reprap.comms;

import java.io.IOException;

import org.reprap.ReprapException;

public abstract class IncomingMessage {
	private byte [] payload;
	
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
		Communicator comm = incomingContext.getCommunicator();
		comm.ReceiveMessage(this);
	}

	public byte[] getPayload() {
		return payload;
	}
	
}
