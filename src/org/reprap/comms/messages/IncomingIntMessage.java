package org.reprap.comms.messages;

import java.io.IOException;

import org.reprap.comms.IncomingContext;
import org.reprap.comms.IncomingMessage;

public abstract class IncomingIntMessage extends IncomingMessage {

	public IncomingIntMessage(IncomingContext incomingContext)
			throws IOException {
		super(incomingContext);
	}

	public static int ConvertBytesToInt(byte b1, byte b2) {
		int low = b1;
		int high = b2;
	    if (low < 0) low += 256;
	    if (high < 0) high += 256;
	    return low + (high << 8);
	}
	
	public int getValue() throws InvalidPayloadException {
	    byte [] reply = getPayload();
	    if (reply == null || reply.length != 3)
	    	throw new InvalidPayloadException();
	    return ConvertBytesToInt(reply[1], reply[2]);
	}
	
	abstract protected boolean isExpectedPacketType(byte packetType);

}
