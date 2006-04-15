package org.reprap.comms;

public interface Address {
	
	public boolean equals(Object arg);
	
	public byte [] getBinary();

	public Address getNullAddress();
	
	public String toString();
}
