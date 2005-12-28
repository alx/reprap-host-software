package org.reprap.comms.messages;

import org.reprap.comms.OutgoingMessage;

public class OutgoingByteMessage extends OutgoingMessage  {
	private byte messageType;
	private byte value;
	
	public OutgoingByteMessage(byte messageType, byte value) {
		this.messageType = messageType;
		this.value = value;
	}
	
	public byte[] getBinary() {
		byte [] payload = new byte[2];
		payload[0] = messageType;
		payload[1] = value;
		return payload;
	}

}
