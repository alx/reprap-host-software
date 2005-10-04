package org.reprap.comms.messages;

import org.reprap.comms.IncomingContext;
import org.reprap.comms.IncomingMessage;

public class VersionResponseMessage extends IncomingMessage {

	public VersionResponseMessage(IncomingContext incomingContext) {
		super(incomingContext);
	}
	
	public int getVersion() throws InvalidPayloadException {
	    byte [] reply = getPayload();
	    if (reply.length != 2)
	    	throw new InvalidPayloadException();
	    return reply[1] + reply[2] << 8;
	}


}
