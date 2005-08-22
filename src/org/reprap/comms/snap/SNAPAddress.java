package org.reprap.comms.snap;

import org.reprap.comms.Address;

public class SNAPAddress implements Address {

	public SNAPAddress(short address) {
		this.address = address;
	}
	
	public short getAddress() {
		return address;
	}

	public void setAddress(short address) {
		this.address = address;
	}
	
	//////////////////////////////////////////////////////////
	
	private short address;

}
