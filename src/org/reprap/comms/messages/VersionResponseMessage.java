package org.reprap.comms.messages;

import java.io.IOException;

import org.reprap.comms.IncomingContext;
import org.reprap.comms.IncomingMessage;

public class VersionResponseMessage extends IncomingMessage {

	public VersionResponseMessage(IncomingContext incomingContext) throws IOException {
		super(incomingContext);
	}
	
	public int getVersion() throws InvalidPayloadException {
	    byte [] reply = getPayload();
	    if (reply == null || reply.length != 2)
	    	throw new InvalidPayloadException();
	    return reply[0] + reply[1] << 8;
	}

	protected boolean isExpectedPacketType(byte packetType) {
		return packetType == VersionRequestMessage.MSG_GetVersion;
	}

}
