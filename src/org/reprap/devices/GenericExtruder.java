package org.reprap.devices;

import java.io.IOException;

import org.reprap.Device;
import org.reprap.comms.Address;
import org.reprap.comms.Communicator;
import org.reprap.comms.IncomingContext;
import org.reprap.comms.IncomingMessage;
import org.reprap.comms.OutgoingMessage;
import org.reprap.comms.messages.OutgoingBlankMessage;
import org.reprap.comms.messages.OutgoingByteMessage;

public class GenericExtruder extends Device {

	public static final byte MSG_SetActive = 1;
	public static final byte MSG_SetHeat = 9;
	public static final byte MSG_GetTemp = 10;
	
	private static final double absZero = 273.15;
	
	private double requestedTemperature = 0;
	private double currentTemperature = 0;
	
	private Thread pollThread;
	private boolean pollThreadExiting = false;

	private int vRefFactor = 3;
	
	private double beta, rz;
	
	public GenericExtruder(Communicator communicator, Address address, double beta, double rz) {
		super(communicator, address);

		this.beta = beta;
		this.rz = rz;
		
		pollThread = new Thread() {
			public void run() {
				while(!pollThreadExiting) {
					try {
						RefreshTemperature();
						Thread.sleep(2000);
					}
					catch (InterruptedException ex) {
						// This is normal when shutting down, so ignore
					}
					catch (Exception ex) {
						System.out.println("Exception during temperature poll");
						ex.printStackTrace();
					}
				}
			}
		};
		pollThread.start();

	
	}

	public void dispose() {
		pollThreadExiting = true;
		pollThread.interrupt();
	}
	
	public synchronized void setExtrusion(int speed) throws IOException {
		OutgoingMessage request =
			new OutgoingByteMessage(MSG_SetActive, (byte)speed);
		sendMessage(request);
	}

	public void setTemperature(int temperature) {
		requestedTemperature = temperature;
		if (temperature == 0)
			setHeater(0, 0);
		else
			setHeater(20, 30);
			
	}
	
	private synchronized void setHeater(int heat, int safetyCutoff) {
		
	}

	public double getTemperature() {
		return currentTemperature;
	}
	
	private synchronized void RefreshTemperature() throws Exception {
		//System.out.println("Refreshing temperature");
		getDeviceTemperature();
	}
	
	private void getDeviceTemperature() throws Exception {
		OutgoingMessage request = new OutgoingBlankMessage(MSG_GetTemp);
		IncomingContext replyContext = sendMessage(request);
		RequestTemperatureResponse reply = new RequestTemperatureResponse(replyContext);
		
		double resistance = calculateResistance(reply.getHeat(), reply.getCalibration());
		
		currentTemperature = calculateTemperature(resistance);
	}

	/**
	 * @param heat
	 * @param calibration
	 * @return
	 */
	private double calculateResistance(int heat, int calibration) {
		// TODO remove hard coded constants
		// TODO should use calibration value instead of first principles
		
		double resistor = 10000;                   // ohms
		double c = 1e-6;                           // farads
		double clock = 4000000.0 / (4.0 * 256.0);  // hertz		
		double vdd = 5.0;                          // volts
		
		double vRef = 0.25 * vdd + vdd * vRefFactor / 32.0;  // volts
		
		double T = heat / clock; // seconds
		
		double resistance =	-T / (Math.log(1 - vRef / vdd) * c);  // ohms
		
		return resistance;
	}

	/**
	 * Calculate temperature in celsius given resistance in ohms
	 * @param resistance
	 * @return
	 */
	private double calculateTemperature(double resistance) {
		return (1.0 / (1.0 / absZero + Math.log(resistance/rz) / beta)) - absZero;
	}
	
	protected class RequestTemperatureResponse extends IncomingMessage {
		public RequestTemperatureResponse(IncomingContext incomingContext) throws IOException {
			super(incomingContext);
		}
		
		protected boolean isExpectedPacketType(byte packetType) {
			return packetType == MSG_GetTemp; 
		}
		
		public int getHeat() throws InvalidPayloadException {
		    byte [] reply = getPayload();
		    if (reply == null || reply.length != 3)
		    		throw new InvalidPayloadException();
		    return ((int)reply[1]) < 0 ? (int)reply[1] + 256 : reply[1];
		}

		public int getCalibration() throws InvalidPayloadException {
		    byte [] reply = getPayload();
		    if (reply == null || reply.length != 3)
		    		throw new InvalidPayloadException();
		    return ((int)reply[2]) < 0 ? (int)reply[2] + 256 : reply[2];
		}
		
	}


	
}
