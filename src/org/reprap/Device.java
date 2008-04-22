package org.reprap;

import java.io.IOException;

import org.reprap.comms.Address;
import org.reprap.comms.Communicator;
import org.reprap.comms.IncomingContext;
import org.reprap.comms.OutgoingMessage;
import org.reprap.comms.IncomingMessage.InvalidPayloadException;
import org.reprap.comms.messages.VersionRequestMessage;
import org.reprap.comms.messages.VersionResponseMessage;

/**
 * Class implements an abstract device containing the basic properties and methods.
 * An "implemented" device refers to for example a UCB/Stepper motor combination, 
 * extruder or other. 
 */
public abstract class Device {

	/**
	 * Adress of the device. Identifier returned by the firmware in the device
	 */
	private Address address;
	
	/**
	 * Result of the last call to isAvailable (which we don't want to call in loops
	 * etc as it does real comms)
	 */
	private boolean wasAlive = false;
	
	/**
	 * Communicator
	 * 
	 */
	private Communicator communicator = org.reprap.Main.getCommunicator();

	/**
	 * Basic constructor for a device.
	 * @param communicator communicator used by the device
	 * @param address address of the device
	 */
	public Device(Communicator communicator, Address address) {
		this.communicator = communicator;
		this.address = address;
	}

	/**
	 * @return the adress of the device
	 */
	public Address getAddress() {
		return address;
	}

	/**
	 * @return the communicator
	 */
	public Communicator getCommunicator() {
		return communicator;
	}

	/**
	 * @return Version ID of the firmware the device is running  
	 * @throws IOException
	 * @throws InvalidPayloadException
	 */
	public int getVersion() throws IOException, InvalidPayloadException {
		VersionRequestMessage request = new VersionRequestMessage();
		IncomingContext replyContext = sendMessage(request);
		VersionResponseMessage reply = new VersionResponseMessage(replyContext);
		return reply.getVersion(); 
	}
	
	/**
	 * Check if the device is alive
	 * @return
	 */
	public boolean isAvailable()
	{
	       try {
	            getVersion();
	        } catch (Exception ex) {
	        	wasAlive = false;
	            return false;
	        }
	        wasAlive = true;
	        return true;
	}
	
	/**
	 * Result of last call to isAvailable(), which we don't want to
	 * call repeatedly as each call polls the device.
	 * @return
	 */
	public boolean wasAvailable()
	{
		return wasAlive;
	}
	
	/**
	 * @param message 
	 * @return incoming context
	 * @throws IOException
	 */
	public IncomingContext sendMessage(OutgoingMessage message) throws IOException {
		return communicator.sendMessage(this, message);
	}
	
	/**
	 * Method to lock communication to this device. 
	 * <p>TODO: when called?</P> 
	 */
	protected void lock() {
		communicator.lock();
	}
	
	/**
	 * Method to unlock communication to this device
	 * <p>TODO: when called?</P> 
	 *  
	 */
	protected void unlock() {
		communicator.unlock();
	}
	
}
