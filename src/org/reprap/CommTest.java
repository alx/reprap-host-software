package org.reprap;

import javax.comm.*;
import java.io.*;

public class CommTest {

    private CommPortIdentifier mPortId = null;
    private SerialPort mPort = null;

    private void test() {
	try {
	    openPort("1");
	    initPort(2400);
	    OutputStream os = getOutputStream();
	    String msg = "Testing\r\n";
	    os.write(msg.getBytes());
	    closePort();
	} catch (Exception ex) {
	    System.out.println(ex.getMessage());
	    System.exit(1);
	}
    }

    public void openPort(String portName) throws NoSuchPortException, PortInUseException
    {
	System.out.println("Opening port " + portName);
	mPortId = CommPortIdentifier.getPortIdentifier(portName);
	mPort = (SerialPort)mPortId.open(portName, 30000);
    }
    
    public void closePort()
    {
	mPort.close();
    }
    
    public void initPort(int baud) throws UnsupportedCommOperationException
    {
	mPort.setSerialPortParams(baud,
				  SerialPort.DATABITS_8,
				  SerialPort.STOPBITS_1,
				  SerialPort.PARITY_NONE );
	mPort.setFlowControlMode(javax.comm.SerialPort.FLOWCONTROL_NONE);
    }
    
    OutputStream getOutputStream() throws IOException
    {
	return mPort.getOutputStream();
    }

    public static void main(String[] args) {
	new CommTest().test();
    }
}
