package org.reprap.devices;

import java.io.IOException;

import org.reprap.Device;
import org.reprap.comms.Address;
import org.reprap.comms.Communicator;
import org.reprap.comms.IncomingContext;
import org.reprap.comms.IncomingMessage;
import org.reprap.comms.OutgoingMessage;
import org.reprap.comms.IncomingMessage.InvalidPayloadException;
import org.reprap.comms.messages.OutgoingBlankMessage;
import org.reprap.comms.messages.OutgoingByteMessage;

public class GenericExtruder extends Device {

	public static final byte MSG_SetActive = 1;
	public static final byte MSG_IsEmpty = 8;
	public static final byte MSG_SetHeat = 9;
	public static final byte MSG_GetTemp = 10;
	public static final byte MSG_SetVRef = 52;
	public static final byte MSG_SetTempScaler = 53;
	
	private static final double absZero = 273.15;
	
	private double requestedTemperature = 0;
	private double currentTemperature = 0;
	
	private boolean currentMaterialOutSensor = false;
	
	// Indicates when polled values are first ready
	private boolean sensorsInitialised = false;
	
	private Thread pollThread;
	private boolean pollThreadExiting = false;

	private int vRefFactor = 3;  // Default firmware value
	private int tempScaler = 7;  // Default firmware value
	
	private double beta, rz;
	
	public GenericExtruder(Communicator communicator, Address address, double beta, double rz) throws IOException {
		super(communicator, address);

		this.beta = beta;
		this.rz = rz;

		//setVref(3);
		//setTempScaler(7);
		
		pollThread = new Thread() {
			public void run() {
				boolean first = true;
				while(!pollThreadExiting) {
					try {
						// Sleep is beforehand to prevent runaway on exception
						if (!first) Thread.sleep(2000);
						RefreshTemperature();
						RefreshEmptySensor();
						sensorsInitialised = true;
						first = false;
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

	public void setTemperature(int temperature) throws Exception {
		// Currently just implemented as a chop-chop heater by
		// setting safety cutoff temperature to desired
		// temperature.  This can be improved by modelling
		// the thermal characteristics and directly setting
		// the appropriate heat output, rather than full-on/full off
		
		requestedTemperature = temperature;
		
		// safety margin
		double safetyTemperature = (double)temperature;
		
		// Now convert safety level to equivalent raw PIC temperature value
		double safetyResistance = calculateResistanceForTemperature(safetyTemperature);
		// Determine equivalent raw value
		int safetyPICTemp = calculatePicTempForResistance(safetyResistance);
		if (safetyPICTemp < 0) safetyPICTemp = 0;
		if (safetyPICTemp > 255) safetyPICTemp = 255;
		
		double a = calculateTemperature(calculateResistance(safetyPICTemp, safetyPICTemp));
		
		if (temperature == 0)
			setHeater(0, 0);
		else
			setHeater(255, safetyPICTemp);
			
	}
	
	private synchronized void setHeater(int heat, int safetyCutoff) throws IOException {
		//System.out.println("Set heater to " + heat + " limit " + safetyCutoff);
		sendMessage(new RequestSetHeat((byte)heat, (byte)safetyCutoff));
	}

	public boolean isEmpty() {
		awaitSensorsInitialised();
		return currentMaterialOutSensor;
	}
	
	private void awaitSensorsInitialised() {
		// Simple minded wait to let sensors become valid
		while(!sensorsInitialised) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}
	
	private synchronized void RefreshEmptySensor() throws IOException {
		// TODO in future, this should use the notification mechanism rather than polling (when fully working)
		//System.out.println("Refreshing sensor");
		try {
			IncomingContext replyContext = sendMessage(new OutgoingBlankMessage(MSG_IsEmpty));
			RequestIsEmptyResponse reply = new RequestIsEmptyResponse(replyContext);
		
			currentMaterialOutSensor = reply.getValue() == 0 ? false : true; 
		} catch (InvalidPayloadException e) {
			throw new IOException();
		}
	}

	public double getTemperatureTarget() {
		return requestedTemperature;
	}

	public double getTemperature() {
		awaitSensorsInitialised();
		return currentTemperature;
	}
	
	private void RefreshTemperature() throws Exception {
		//System.out.println("Refreshing temperature");
		getDeviceTemperature();
	}
	
	private synchronized void getDeviceTemperature() throws Exception {
		OutgoingMessage request = new OutgoingBlankMessage(MSG_GetTemp);
		IncomingContext replyContext = sendMessage(request);
		RequestTemperatureResponse reply = new RequestTemperatureResponse(replyContext);

		//System.out.println("Raw temp " + reply.getHeat());

		double resistance = calculateResistance(reply.getHeat(), reply.getCalibration());
		
		currentTemperature = calculateTemperature(resistance);
	}

	/**
	 * Calculates the actual resistance of the thermistor
	 * from the raw timer values received from the PIC. 
	 * @param picTemp
	 * @param calibrationPicTemp
	 * @return
	 */
	private double calculateResistance(int picTemp, int calibrationPicTemp) {
		// TODO remove hard coded constants
		// TODO should use calibration value instead of first principles
		
		//double resistor = 10000;                   // ohms
		double c = 1e-6;                           // farads
		double scale = 1 << (tempScaler+1);
		double clock = 4000000.0 / (4.0 * scale);  // hertz		
		double vdd = 5.0;                          // volts
		
		double vRef = 0.25 * vdd + vdd * vRefFactor / 32.0;  // volts
		
		double T = picTemp / clock; // seconds
		
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
	
	private double calculateResistanceForTemperature(double temperature) {
		return rz * Math.exp(beta * (1/(temperature + absZero) - 1/absZero));
	}
	
	/**
	 * Calculates an expected PIC Temperature expected for a
	 * given resistance 
	 * @param resistance
	 * @return
	 */
	private int calculatePicTempForResistance(double resistance) {
		double c = 1e-6;                           // farads
		double scale = 1 << (tempScaler+1);
		double clock = 4000000.0 / (4.0 * scale);  // hertz		
		double vdd = 5.0;                          // volts
		
		double vRef = 0.25 * vdd + vdd * vRefFactor / 32.0;  // volts
		
		double T = -resistance * (Math.log(1 - vRef / vdd) * c);

		double picTemp = T * clock;
		return (int)Math.round(picTemp);
		
	}
	
	private synchronized void setVref(int ref) throws IOException {
		sendMessage(new OutgoingByteMessage(MSG_SetVRef, (byte)ref));		
		vRefFactor = ref;
	}

	private synchronized void setTempScaler(int scale) throws IOException {
		sendMessage(new OutgoingByteMessage(MSG_SetTempScaler, (byte)scale));		
		tempScaler = scale;
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

	protected class RequestSetHeat extends OutgoingMessage {
		byte [] message;
		
		RequestSetHeat(byte heat, byte cutoff) {
			message = new byte [] { MSG_SetHeat, heat, cutoff }; 
		}
		
		public byte[] getBinary() {
			return message;
		}
		
	}
	
	protected class RequestIsEmptyResponse extends IncomingMessage {

		public RequestIsEmptyResponse(IncomingContext incomingContext)
		throws IOException {
			super(incomingContext);
		}
		
		public byte getValue() throws InvalidPayloadException {
			byte [] reply = getPayload();
			if (reply == null || reply.length != 2)
				throw new InvalidPayloadException();
			return reply[1];
		}
		
		protected boolean isExpectedPacketType(byte packetType) {
			return packetType == MSG_IsEmpty;
		}
		
	}


}
