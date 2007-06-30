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
	 * Communicator
	 * 
	 */
	private Communicator communicator;

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
