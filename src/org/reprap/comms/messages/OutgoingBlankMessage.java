package org.reprap.comms.messages;

import org.reprap.comms.OutgoingMessage;

public class OutgoingBlankMessage extends OutgoingMessage {
	
	private byte messageType;
	
	public OutgoingBlankMessage(byte messageType) {
		this.messageType = messageType;
	}

	public byte[] getBinary() {
		return new byte [] { messageType };
	}

}
