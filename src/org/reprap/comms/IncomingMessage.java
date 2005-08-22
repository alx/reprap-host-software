package org.reprap.comms;

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
	
	public IncomingMessage(IncomingContext incomingContext) {
		// TODO receive a message according to context and populate the payload
	}

	public byte[] getPayload() {
		return payload;
	}
}
