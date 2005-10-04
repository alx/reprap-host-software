package org.reprap.comms.messages;

import org.reprap.comms.OutgoingMessage;

/**
 * The VersionMessage is supported by all devices
 * 
 */

public class VersionRequestMessage extends OutgoingMessage {
	public static final int MSG_GetVersion = 0;

	public byte[] getBinary() {
		byte message[] = new byte [] {MSG_GetVersion};		
		return message;
	}
	
}
