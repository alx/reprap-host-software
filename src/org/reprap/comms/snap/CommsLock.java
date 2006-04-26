package org.reprap.comms.snap;

// A temporary class to allow locking of comms while waiting
// for the asynchronous context delivery mechanism to be implemented
public class CommsLock {

	private boolean locked = false;
	
	synchronized public void lock() {
		System.out.println("Lock: entered");
		while(locked) {
			System.out.println("Lock: waiting");
			try {
				wait();
				System.out.println("Lock: wait complete");
			} catch (InterruptedException e) {
			}
		}
		locked = true;
		System.out.println("Lock: finished");
	}
	
	synchronized public void unlock() {
		System.out.println("Unlock: entered");
		if (!locked)
			System.out.println("Warning: Calling unlock from an already unlocked state!");
		locked = false;
		notifyAll();
		System.out.println("Unlock: finished");
	}
	
}
