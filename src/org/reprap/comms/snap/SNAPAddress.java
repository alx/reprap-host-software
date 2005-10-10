package org.reprap.comms.snap;

import org.reprap.comms.Address;

public class SNAPAddress implements Address {	
	private short address;

	public SNAPAddress(int address) {
		this((short)address);
	}
	
	public SNAPAddress(short address) {
		this.address = address;
	}
	
	public short getAddress() {
		return address;
	}

	public void setAddress(short address) {
		this.address = address;
	}
	
	public boolean equals(Object arg) {
		if (arg == this)
			return true;
		if (arg == null)
			return false;
		if (!(arg instanceof SNAPAddress))
			return false;
		return address == ((SNAPAddress)arg).address;
	}
}
