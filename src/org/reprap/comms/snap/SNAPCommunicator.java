package org.reprap.comms.snap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.comm.CommPortIdentifier;
import javax.comm.NoSuchPortException;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.UnsupportedCommOperationException;

import org.reprap.Device;
import org.reprap.Preferences;
import org.reprap.comms.Address;
import org.reprap.comms.Communicator;
import org.reprap.comms.IncomingContext;
import org.reprap.comms.IncomingMessage;
import org.reprap.comms.OutgoingMessage;

public class SNAPCommunicator implements Communicator {

	private final static int ackTimeout = 300;
	private final static int messageTimeout = 300;
    
	private Address localAddress;
	
	private SerialPort port;
	private OutputStream writeStream;
	private InputStream readStream;
	
	//private ReceiveThread receiveThread = null;
	
	private boolean debugMode;
	
	private CommsLock lock = new CommsLock();
		
	public SNAPCommunicator(String portName, int baudRate, Address localAddress)
			throws NoSuchPortException, PortInUseException, IOException, UnsupportedCommOperationException {
		this.localAddress = localAddress;
		System.out.println("Opening port "+portName);
		CommPortIdentifier commId = CommPortIdentifier.getPortIdentifier(portName);
		port = (SerialPort)commId.open(portName, 30000);
		
		
		// Workround for javax.comm bug.
		// See http://forum.java.sun.com/thread.jspa?threadID=673793
		
		try {
			port.setSerialPortParams(baudRate,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
		}
		catch (Exception e) {
			
		}
			 
		port.setSerialPortParams(baudRate,
				SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE);
		
		// End of workround
		
		try {
			port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
		} catch (Exception e) {
			// Um, Linux USB ports don't do this. What can I do about it?
		}

		writeStream = port.getOutputStream();
		readStream = port.getInputStream();
		
		try {
			// Try to load debug setting from properties file
			debugMode = Preferences.loadGlobalBool("CommsDebug");
		} catch (Exception ex) {
			// Fall back to non-debug mode if no setting is available
			debugMode = false;
		}
	}
	
	public void close()
	{
		if (port != null)
			port.close();
		port = null;
	}
	
	private void dumpPacket(Device device, OutgoingMessage messageToSend) {
		byte [] binaryMessage = messageToSend.getBinary(); 
		System.out.print(localAddress.toString());
		System.out.print("->");
		System.out.print(device.getAddress().toString());
		System.out.print(": ");
		for(int i = 0; i < binaryMessage.length; i++)
			System.out.print(Integer.toHexString(binaryMessage[i]>=0?binaryMessage[i]:binaryMessage[i]+256) + " ");
		System.out.println("");
	}
	
	public IncomingContext sendMessage(Device device,
			OutgoingMessage messageToSend) throws IOException {
		
		byte [] binaryMessage = messageToSend.getBinary(); 
		SNAPPacket packet = new SNAPPacket((SNAPAddress)localAddress,
				(SNAPAddress)device.getAddress(),
				binaryMessage);

		for(;;) {
			if (debugMode) {
				System.out.print("TX ");
				dumpPacket(device, messageToSend);
			}
			sendRawMessage(packet);

			SNAPPacket ackPacket;
			try {
				ackPacket = receivePacket(ackTimeout);	
			} catch (IOException ex) {
				// An error occurred during receive, so send and try again
				//if (debugMode) {
					System.out.println("Receive error, re-sending");
					dumpPacket(device, messageToSend);
				//}
				continue;
			}
			if (ackPacket.isAck())
				break;
			if (ackPacket.getSourceAddress().equals(localAddress)) {
				// Packet was from us, so assume no node present
				System.out.println("Device at address " + device.getAddress() + " not present");
				throw new IOException("Device at address " + device.getAddress() + " not present");
			}
			if (!ackPacket.isNak()) {
				System.out.println("Received data packet when expecting ACK");
			}
		}
		
		IncomingContext replyContext = messageToSend.getReplyContext(this,
				device);
		return replyContext;
	}
	
	private synchronized void sendRawMessage(SNAPPacket packet) throws IOException {
		writeStream.write(packet.getRawData());
	}

	private int readByte(long timeout) throws IOException {
		long t0 = System.currentTimeMillis();
		int c = -1;

		// Sometimes javacomm seems to freak out and say something
		// timed out when it didn't, so double check and try again
		// if it really didn't time out
		for(;;) {
			c = readStream.read();
			if (c != -1)
				return c;
			if (System.currentTimeMillis() - t0 >= timeout)
				return -1;
			
			try {
				// Just to avoid a deadly spin if something unexpected happens
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
		} 
	}
	
	protected synchronized SNAPPacket receivePacket(long timeout) throws IOException {
		SNAPPacket packet = null;
		if (debugMode) System.out.print("RX ");
		try {
			port.enableReceiveTimeout(messageTimeout);
		} catch (UnsupportedCommOperationException e) {
			System.out.println("Read timeouts unsupported on this platform");
		}
		for(;;) {
			int c = readByte(timeout);
			if (debugMode) System.out.print(Integer.toHexString(c) + " ");
			if (c == -1)
				throw new IOException();
			if (packet == null) {
				if (c != 0x54)  // Always wait for a sync byte before doing anything
					continue;
				packet = new SNAPPacket();
			}
			if (packet.receiveByte((byte)c)) {
				// Packet is complete
				if (packet.validate()) {
					if (debugMode) System.out.println("");
					return packet;
				} else {
					System.out.println("CRC error");
					throw new IOException("CRC error");
				}
			}
		}	
	}
	
	public void receiveMessage(IncomingMessage message) throws IOException {
		receiveMessage(message, messageTimeout);
	}
	
	public void receiveMessage(IncomingMessage message, long timeout) throws IOException {
		// Here we collect one packet and notify the message
		// of its contents.  The message will respond
		// to indicate if it wants the message.  If not,
		// it will be discarded and we will wait for another
		// message.
		
		// Since this is a SNAP ring, we have to pass on
		// any packets that are not destined for us.
		
		// We will also only pass packets to the message if they are for
		// the local address.
		for(;;) {
			SNAPPacket packet = receivePacket(timeout);
			if (processPacket(message, packet))
				return;
		}
	}
	
	private boolean processPacket(IncomingMessage message, SNAPPacket packet) throws IOException {
		// First ACK the message
		if (packet.isAck()) {
			System.out.println("Unexpected ACK received instead of message, not supported yet");
	  	  	return false;
		}
		/// TODO send ACKs
		//sendRawMessage(packet.generateACK());
		
		if (!packet.getDestinationAddress().equals(localAddress)) {
			// Not for us, so forward it on
			sendRawMessage(packet);
			return false;
		} else if (message.receiveData(packet.getPayload())) {
			// All received as expected
			return true;
		} else {
			// Not interested, wait for more
			System.out.println("Ignored and dropped packet");
			return false;
		}
	}

	public Address getAddress() {
		return localAddress;
	}

	public void dispose() {
		close();
	}

	public void lock() {
		lock.lock();
	}

	public void unlock() {
		lock.unlock();
	}
	
	// TODO make a background receiver thread.  It can keep a pool of async receive contexts and
	// fire them off if anything matching arrives.
	
	// TODO Make a generic message receiver.  Use reflection to get correct class. 

}
