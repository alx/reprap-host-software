package org.reprap.comms.snap;

import java.io.IOException;

public class SNAPPacket {
	
	private final int offset_sync = 0;
	private final int offset_hdb2 = 1;
	private final int offset_hdb1 = 2;
	private final int offset_dab = 3;
	private final int offset_sab = 4;
	private final int offset_payload = 5;
	
	private final byte syncMarker = 0x54;
	
	private final int maxSize = 64;
	
	// Full raw packet contents including all headers
	private byte [] buffer;

	private int receiveLength = 0;
	
	private boolean complete = false;
	
	SNAPPacket() {
		buffer = new byte[maxSize];
	}
	
	SNAPPacket(SNAPAddress srcAddress, SNAPAddress destAddress, byte [] payload) {
		buffer = new byte[payload.length + offset_payload + 1];
		buffer[offset_sync] = syncMarker;
		buffer[offset_hdb2] = 0x31;  // Always fixed for now
		buffer[offset_hdb1] = 0x30;
		buffer[offset_dab] = (byte)destAddress.getAddress();
		buffer[offset_sab] = (byte)srcAddress.getAddress();
		setLength(payload.length);
		SNAPChecksum crc = new SNAPChecksum();
		for(int i = 0; i < payload.length; i++)
			buffer[i + offset_payload] = crc.addData(payload[i]);
		buffer[offset_payload + payload.length] = crc.getResult();
		complete = true;
	}
	
	public byte getPacketType() {
		return buffer[0]; // TODO fix offset
	}
	
	public byte [] getPayload() {
		return buffer;  // TODO return correct part
	}
	
	/**
	 * 
	 * @param data
	 * @return true is the packet is now complete, otherwise false
	 * @throws IOException 
	 */
	public boolean receiveByte(byte data) throws IOException {
		if (complete)
			throw new IOException("Received data beyond end of packet");
		
		if (receiveLength < maxSize)
			buffer[receiveLength++] = data;
		// TODO determine if packet is complete
		return false;
	}
	
	public boolean validate() {
		SNAPChecksum crc = new SNAPChecksum();
		
		return crc.getResult() == 0;
	}
	
	public SNAPAddress getSourceAddress() {
		return new SNAPAddress((short)buffer[offset_sab]);
	}
	
	public SNAPAddress getDestinationAddress() {
		return new SNAPAddress((short)buffer[offset_dab]);
	}

	private void setLength(int length) {
		buffer[offset_hdb1] = (byte)((buffer[offset_hdb1] & 0xf0) |
				(length > 7 ? 8 : length));
	}
	
	public int getLength() {
		int l = buffer[offset_hdb1] & 0x0f;
		if ((l & 8) != 0)
			return 8 << (l & 7);
		return l;
	}

	
}
