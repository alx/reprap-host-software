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
import org.reprap.comms.Address;
import org.reprap.comms.Communicator;
import org.reprap.comms.IncomingContext;
import org.reprap.comms.IncomingMessage;
import org.reprap.comms.OutgoingMessage;

public class SNAPCommunicator implements Communicator {
	
	private Address localAddress;
	
	private SerialPort port;
	private OutputStream writeStream;
	private InputStream readStream;
	
	public SNAPCommunicator(String portName, int baudRate, Address localAddress)
	throws NoSuchPortException, PortInUseException, IOException, UnsupportedCommOperationException {
		this.localAddress = localAddress;
		CommPortIdentifier commId = CommPortIdentifier.getPortIdentifier(portName);
		port = (SerialPort)commId.open(portName, 30000);
		
		port.setSerialPortParams(baudRate,
				SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE );
		port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
		
		writeStream = port.getOutputStream();
		readStream = port.getInputStream();
	}
	
	public void close()
	{
		port.close();
	}
	
	public IncomingContext sendMessage(Device device,
			OutgoingMessage messageToSend) throws IOException {
		
		SNAPPacket packet = new SNAPPacket((SNAPAddress)localAddress,
				(SNAPAddress)device.getAddress(),
				messageToSend.getBinary());
	
		sendRawMessage(packet);
		
		IncomingContext replyContext = messageToSend.getReplyContext(this,
				device);
		return replyContext;
	}

	private void sendRawMessage(SNAPPacket packet) throws IOException {
		writeStream.write(packet.getRawData());
	}
	
	public void ReceiveMessage(IncomingMessage message) throws IOException {
		// Here we collect one packet and notify the message
		// of its contents.  The message will respond
		// to indicate if it wants the message.  If not,
		// it will be discarded and we will wait for another
		// message.
		
		// Since this is a SNAP ring, we have to pass on
		// any packets that are not destined for us.
		
		// We will also only pass packed to the message if they are for
		// the local address.
		SNAPPacket packet = null;
		for(;;) {
			if (packet == null)
				packet = new SNAPPacket();
			int c = readStream.read();
			if (c == -1) throw new IOException();
			// TODO loop over data and multiple packets
			if (packet.receiveByte((byte)c)) {
				// Packet is complete
				if (packet.validate()) {
					// Packet is complete and valid, so process it
					if (processPacket(message, packet))
						return;
				} else
					sendRawMessage(packet.generateNAK());
				packet = null;
			}
		}
	}

	private boolean processPacket(IncomingMessage message, SNAPPacket packet) throws IOException {
		// First ACK the message
		sendRawMessage(packet.generateACK());
		
		if (!packet.getDestinationAddress().equals(localAddress)) {
			// Not for us, so forward it on
			sendRawMessage(packet);
			return false;
		} else if (message.receiveData(packet.getPayload())) {
			// All received as expected
			return true;
		} else {
			// Not interested, wait for more
			return false;
		}
	}
	
	// TODO Make a generic message receiver.  Use reflection to get correct class. 
	
}
