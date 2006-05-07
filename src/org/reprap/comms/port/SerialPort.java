package org.reprap.comms.port;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.comm.CommPortIdentifier;
import javax.comm.NoSuchPortException;

public class SerialPort implements Port {
	
	private javax.comm.SerialPort port;
	
	public SerialPort(String portName, int baudRate) throws Exception {
		try {
			CommPortIdentifier commId = CommPortIdentifier.getPortIdentifier(portName);
			port = (javax.comm.SerialPort)commId.open(portName, 30000);
		}
		catch (NoSuchPortException ex) {
		}
		
		// Workround for javax.comm bug.
		// See http://forum.java.sun.com/thread.jspa?threadID=673793
		try {
			port.setSerialPortParams(baudRate,
					javax.comm.SerialPort.DATABITS_8,
					javax.comm.SerialPort.STOPBITS_1,
					javax.comm.SerialPort.PARITY_NONE);
		}
		catch (Exception e) {
			port.setSerialPortParams(baudRate,
					javax.comm.SerialPort.DATABITS_8,
					javax.comm.SerialPort.STOPBITS_1,
					javax.comm.SerialPort.PARITY_NONE);			 
		}
		// End of workround
		
		try {
			port.setFlowControlMode(javax.comm.SerialPort.FLOWCONTROL_NONE);
		} catch (Exception e) {
			// Um, Linux USB ports don't do this. What can I do about it?
		}
		
	}
	
	public OutputStream getOutputStream() throws IOException {
		return port.getOutputStream();
	}
	
	public InputStream getInputStream() throws IOException {
		return port.getInputStream();
	}
	
	public void close() {
		port.close();
	}
}
