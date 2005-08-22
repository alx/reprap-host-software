package org.reprap.comms.messages;

import org.reprap.comms.OutgoingMessage;

/**
 * The VersionMessage is supported by all devices
 * 
 */

public class VersionRequestMessage extends OutgoingMessage {

	public byte[] getBinary() {
		byte message[] = new byte [] {0};		
		return message;
	}
	
}
