package org.reprap.comms.snap;

// A temporary class to allow locking of comms while waiting
// for the asynchronous context delivery mechanism to be implemented
public class CommsLock {

	private boolean locked = false;
	
	synchronized public void lock() {
		while(locked) {
			//System.out.println("Waiting on other user for lock");
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		locked = true;
		notifyAll();
	}
	
	synchronized public void unlock() {
		while(!locked) {
			//System.out.println("Waiting on other user for unlock");
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		locked = false;
		notifyAll();
	}
	
}
