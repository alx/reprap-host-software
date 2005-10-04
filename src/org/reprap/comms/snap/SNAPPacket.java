package org.reprap.comms.snap;

public class SNAPPacket {

	private final int maxSize = 64;
	
	private byte [] buffer = new byte[maxSize];
	private int length = 0;
	
	SNAPPacket() {
		
	}

	/**
	 * 
	 * @param data
	 * @return true is the packet is now complete, otherwise false
	 */
	public boolean receiveByte(byte data) {
		if (length < maxSize)
  		  buffer[length++] = data;
		// TODO determine if packet is complete
		return false;
	}
	
	public boolean validate() {
		SNAPChecksum crc = new SNAPChecksum();
		
		return crc.getResult() == 0;
	}

	public byte getPacketType() {
	    return buffer[0]; // TODO fix offset
	}
	
	public byte [] getPayload() {
		return buffer;  // TODO return correct part
	}
		
}
