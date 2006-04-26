package org.reprap.comms.snap;

// A temporary class to allow locking of comms while waiting
// for the asynchronous context delivery mechanism to be implemented
public class CommsLock {

	private boolean locked = false;
	
	synchronized public void lock() {
		while(locked) {
			//System.out.println("Lock: waiting");
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		locked = true;
	}
	
	synchronized public void unlock() {
		if (!locked)
			System.out.println("Warning: Calling unlock from an already unlocked state!");
		locked = false;
		notifyAll();
	}
	
}
