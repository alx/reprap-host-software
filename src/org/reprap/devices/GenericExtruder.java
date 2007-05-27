package org.reprap.devices;

import java.io.IOException;

import org.reprap.Device;
import org.reprap.Preferences;
import org.reprap.Extruder;
import org.reprap.comms.Address;
import org.reprap.comms.Communicator;
import org.reprap.comms.IncomingMessage;
import org.reprap.comms.OutgoingMessage;
import org.reprap.comms.IncomingMessage.InvalidPayloadException;
import org.reprap.comms.messages.OutgoingBlankMessage;
import org.reprap.comms.messages.OutgoingByteMessage;

//TODO: make all extruder properties ExtruderN... like Extruder1Address; at the moment some of them
// are Extrusion... (like ExtrusionHeight).  That way we can have multiple extruders each with
// their own properties.  Also, all the rest of the code should get this information from the
// current extruder, rather than directly from the properties file.

public class GenericExtruder extends Device implements Extruder{

	public static final byte MSG_SetActive = 1;
	public static final byte MSG_SetActiveReverse = 2;
	public static final byte MSG_IsEmpty = 8;
	public static final byte MSG_SetHeat = 9;
	public static final byte MSG_GetTemp = 10;
	public static final byte MSG_SetCooler = 11;
	public static final byte MSG_SetVRef = 52;
	public static final byte MSG_SetTempScaler = 53;
	
	/// Offset of 0 degrees centigrade from absolute zero
	private static final double absZero = 273.15;
	
	/// The temperature to maintain
	private double requestedTemperature = 0;
	
	/// The temperature most recently read from the device
	private double currentTemperature = 0;
	
	private boolean currentMaterialOutSensor = false;
	
	/// Indicates when polled values are first ready
	private boolean sensorsInitialised = false;
	
	private Thread pollThread = null;
	private boolean pollThreadExiting = false;

	private int vRefFactor = 7;
	private int tempScaler = 4;
	
	private double beta;   ///< Thermistor beta
	private double rz;     ///< Thermistor resistance at 0C
	private double cap;    ///< Thermistor timing capacitor in farads
	private double hm;     ///< Heater power gradient
	private double hb;     ///< Heater power intercept
	private int maxExtruderSpeed;  ///< Maximum motor speed (0-255)
	private int extrusionSpeed; ///< The actual extrusion speed
	private double extrusionTemp; ///< The extrusion temperature
	private double extrusionSize; ///< The extrusion width in XY
	private double extrusionHeight; ///< The extrusion height in Z
	                                 // Should this be a machine-wide constant? - AB
	private double extrusionInfillWidth; ///< The step between infill tracks
	private double extrusionOverRun; ///< The number of mm to stop extruding before the end of a track
	private int extrusionDelay; ///< The number of ms to wait before starting a track
	private int coolingPeriod; ///< The number of s to cool between layers
	private int xySpeed; ///< The speed of movement in XY when depositing
	private int t0;        ///< Zero torque speed
	private double iSpeed;///< Infill speed [0,1]*maxSpeed
	private double oSpeed;///< Outline speed [0,1]*maxSpeed	
	private double asLength;///< Length (mm) to speed up round corners
	private double asFactor;///< Factor by which to speed up round corners
	private String materialType;  ///< The name of this extruder's material
	private double offsetX, offsetY, offsetZ; ///< Where to put the nozzle
	private long lastTemperatureUpdate = 0;
	
	/// TODO hb should probably be ambient temperature measured at this point
	
	/// Flag indicating if initialisation succeeded.  Usually this
	/// indicates if the extruder is present in the network.
	private boolean isCommsAvailable = false;
	
	public GenericExtruder(Communicator communicator, Address address, Preferences prefs, int extruderId) {
		
		super(communicator, address);

		String prefName = "Extruder" + extruderId;
		
		beta = prefs.loadDouble(prefName + "Beta");
		rz = prefs.loadDouble(prefName + "Rz");
		cap = prefs.loadDouble(prefName + "Capacitor");
		hm = prefs.loadDouble(prefName + "hm");
		hb = prefs.loadDouble(prefName + "hb");
		maxExtruderSpeed = prefs.loadInt(prefName + "MaxSpeed");
		extrusionSpeed = prefs.loadInt(prefName + "ExtrusionSpeed");
		extrusionTemp = prefs.loadDouble(prefName + "ExtrusionTemp");
		extrusionSize = prefs.loadDouble(prefName + "ExtrusionSize");
		extrusionHeight = prefs.loadDouble(prefName + "ExtrusionHeight");
		extrusionInfillWidth = prefs.loadDouble(prefName + "ExtrusionInfillWidth");
		extrusionOverRun = prefs.loadDouble(prefName + "ExtrusionOverRun");
		extrusionDelay = prefs.loadInt(prefName + "ExtrusionDelay");
		coolingPeriod = prefs.loadInt(prefName + "CoolingPeriod");
		xySpeed = prefs.loadInt(prefName + "XYSpeed");
		t0 = prefs.loadInt(prefName + "t0");
		iSpeed = prefs.loadDouble(prefName + "InfillSpeed");
		oSpeed = prefs.loadDouble(prefName + "OutlineSpeed");
		asLength = prefs.loadDouble(prefName + "AngleSpeedLength");
		asFactor = prefs.loadDouble(prefName + "AngleSpeedFactor");
		materialType = prefs.loadString(prefName + "MaterialType");
		offsetX = prefs.loadDouble(prefName + "OffsetX");
		offsetY = prefs.loadDouble(prefName + "OffsetY");
		offsetZ = prefs.loadDouble(prefName + "OffsetZ");
		
		// Check Extruder is available
		try {
			getVersion();
			setTempRange();
		} catch (Exception ex) {
			isCommsAvailable = false;
			return;
		}
		
		isCommsAvailable = true;
		
		/*pollThread = new Thread() {
			public void run() {
				Thread.currentThread().setName("Extruder poll");
				boolean first = true;
				while(!pollThreadExiting) {
					try {
						// Sleep is beforehand to prevent runaway on exception
						if (!first) Thread.sleep(2000);
						first = false;
						RefreshTemperature();
						RefreshEmptySensor();
						sensorsInitialised = true;
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
		pollThread.start();*/

	
	}

	public void dispose() {
		if (pollThread != null) {
			pollThreadExiting = true;
			pollThread.interrupt();
		}
	}

	/**
	 * Start the extruder motor at a given speed.  This ranges from 0
	 * to 255 but is scaled by maxSpeed and t0, so that 255 corresponds to the
	 * highest permitted speed.  It is also scaled so that 0 would correspond
	 * with the lowest extrusion speed.
	 * @param speed The speed to drive the motor at (0-255)
	 * @throws IOException
	 */
	public void setExtrusion(int speed) throws IOException {
		setExtrusion(speed, false);
	}
	
	/**
	 * Start the extruder motor at a given speed.  This ranges from 0
	 * to 255 but is scaled by maxSpeed and t0, so that 255 corresponds to the
	 * highest permitted speed.  It is also scaled so that 0 would correspond
	 * with the lowest extrusion speed.
	 * @param speed The speed to drive the motor at (0-255)
	 * @param reverse If set, run extruder in reverse
	 * @throws IOException
	 */
	public void setExtrusion(int speed, boolean reverse) throws IOException {
		// Assumption: Between t0 and maxSpeed, the speed is fairly linear
		int scaledSpeed;
		
		if (speed > 0)
			scaledSpeed = (int)Math.round((maxExtruderSpeed - t0) * speed / 255.0 + t0);
		else
			scaledSpeed = 0;
		
		lock();
		try {
			OutgoingMessage request =
				new OutgoingByteMessage(reverse ? MSG_SetActiveReverse : MSG_SetActive,
						(byte)scaledSpeed);
			sendMessage(request);
		}
		finally {
			unlock();
		}
	}

	/**
	 * Set extruder temperature
	 * @param temperature
	 * @throws Exception
	 */
	
	public void heatOn() throws Exception 
	{
		setTemperature(extrusionTemp);
	}
	
	public void setTemperature(double temperature) throws Exception {
		setTemperature(temperature, true);
	}
	
	private void setTemperature(double temperature, boolean lock) throws Exception {
		requestedTemperature = temperature;
		if(Math.abs(requestedTemperature - extrusionTemp) > 5)
		{
			System.out.println(materialType + " extruder temperature set to " + requestedTemperature +
				"C, which is not the standard temperature (" + extrusionTemp + "C).");
		}
		// Aim for 10% above our target to ensure we reach it.  It doesn't matter
		// if we go over because the power will be adjusted when we get there.  At
		// the same time, if we aim too high, we'll overshoot a bit before we
		// can react.
		double temperature0 = temperature * 1.1;
		
		// A safety cutoff will be set at 20% above requested setting
		double temperatureSafety = temperature * 1.2;
		
		// Calculate power output from hm, hb.  In general, the temperature
		// we achieve is power * hm + hb.  So to achieve a given temperature
		// we need a power of (temperature - hb) / hm
		
		// If we reach our temperature, rather than switching completely off
		// go to a reduced power level.
		int power0 = (int)Math.round(((0.9 * temperature0) - hb) / hm);
		if (power0 < 0) power0 = 0;
		if (power0 > 255) power0 = 255;

		// Otherwise, this is the normal power level we will maintain
		int power1 = (int)Math.round((temperature0 - hb) / hm);
		if (power1 < 0) power1 = 0;
		if (power1 > 255) power1 = 255;

		// Now convert temperatures to equivalent raw PIC temperature resistance value
		// Here we use the original specified temperature, not the slight overshoot
		double resistance0 = calculateResistanceForTemperature(temperature);
		double resistanceSafety = calculateResistanceForTemperature(temperatureSafety);

		// Determine equivalent raw value
		int t0 = calculatePicTempForResistance(resistance0);
		if (t0 < 0) t0 = 0;
		if (t0 > 255) t0 = 255;
		int t1 = calculatePicTempForResistance(resistanceSafety);
		if (t1 < 0) t1 = 0;
		if (t1 > 255) t1 = 255;
		
		if (temperature == 0)
			setHeater(0, 0, lock);
		else {
			setHeater(power0, power1, t0, t1, lock);
		}
	}
	
	/**
	 * Set a heat output power.  For normal production use you would
	 * normally call setTemperature, however this method may be useful
	 * for lower temperature profiling, etc.
	 * @param heat Heater power (0-255)
	 * @param maxTemp Cutoff temperature in celcius
	 * @throws IOException
	 */
	public void setHeater(int heat, double maxTemp) throws IOException {

		double safetyResistance = calculateResistanceForTemperature(maxTemp);
		// Determine equivalent raw value
		int safetyPICTemp = calculatePicTempForResistance(safetyResistance);
		if (safetyPICTemp < 0) safetyPICTemp = 0;
		if (safetyPICTemp > 255) safetyPICTemp = 255;

		if (heat == 0)
			setHeater(0, 0, true);
		else
			setHeater(heat, safetyPICTemp, true);

	}
	
	/**
	 * Set the raw heater output value and safety cutoff.  A specific
	 * temperature can be reached by setting a suitable output power.
	 * A limit temperature can also be specified.  If reached, the
	 * heater will be automatically turned off until the temperature
	 * drops below the limit.
	 * @param heat A heater power output
	 * @param safetyCutoff A temperature at which to cut off the heater
	 * @throws IOException
	 */
	private void setHeater(int heat, int safetyCutoff, boolean lock) throws IOException {
		//System.out.println(materialType + " extruder heater set to " + heat + " limit " + safetyCutoff);
		if (lock) lock();
		try {
			sendMessage(new RequestSetHeat((byte)heat, (byte)safetyCutoff));
		}
		finally {
			if (lock) unlock();
		}
	}

	/**
	 * Similar to setHeater(int, int) except this provides two different
	 * heating zones.  Below t0, it heats at h1.  From t0 to t1 it heats at h0
	 * and above t1 it shuts off.  This has the effect of still providing some
	 * power when the desired temperature is reached so it cools less quickly.   
	 * @param heat0
	 * @param heat1
	 * @param t0
	 * @param t1
	 * @throws IOException
	 */
	private void setHeater(int heat0, int heat1, int t0, int t1, boolean lock) throws IOException {
		System.out.println(materialType + " extruder heater set to " + heat0 + "/" + heat1 + " limit " + t0 + "/" + t1);
		if (lock) lock();
		try {
			sendMessage(new RequestSetHeat((byte)heat0,
										   (byte)heat1,
										   (byte)t0,
										   (byte)t1));
		}
		finally {
			if (lock) unlock();
		}
	}

	/**
	 * Check if the extruder is out of feedstock
	 * @return true if there is no material remaining
	 */
	public boolean isEmpty() {
		awaitSensorsInitialised();
		TEMPpollcheck();
		return currentMaterialOutSensor;
	}
	
	private void awaitSensorsInitialised() {
		// Simple minded wait to let sensors become valid
		//while(!sensorsInitialised) {
		//	try {
		//		Thread.sleep(100);
		//	} catch (InterruptedException e) {
		//	}
		//}
	}
	
	/**
	 * Send current vRefFactor and a suitable timer scaler
	 * to the device. 
	 *
	 */
	private void setTempRange() throws Exception
	{
		// We will send the vRefFactor to the PIC.  At the same
		// time we will send a suitable temperature scale as well.
		// To maximize the range, when vRefFactor is high (15) then
		// the scale is minimum (0).
		System.out.println(materialType + " extruder vRefFactor set to " + vRefFactor);
		tempScaler = 7 - (vRefFactor >> 1);
	    setVref(vRefFactor);
		setTempScaler(tempScaler);
		if (requestedTemperature != 0)
			setTemperature(requestedTemperature, false);
	}
	
	/**
	 * Called internally to refresh the empty sensor.  This is
	 * called periodically in the background by another thread.
	 * @throws IOException
	 */
	private void RefreshEmptySensor() throws IOException {
		// TODO in future, this should use the notification mechanism rather than polling (when fully working)
		
		lock();
		try {
			//System.out.println(materialType + " extruder refreshing sensor");
			RequestIsEmptyResponse reply = new RequestIsEmptyResponse(this, new OutgoingBlankMessage(MSG_IsEmpty), 500);
			currentMaterialOutSensor = reply.getValue() == 0 ? false : true; 
		} catch (InvalidPayloadException e) {
			throw new IOException();
		} finally {
			unlock();
		}
	}

	public double getTemperatureTarget() {
		return requestedTemperature;
	}

	public double getDefaultTemperature() {
		return extrusionTemp;
	}	
	/**
	 * TEMPORARY WORKAROUND FUNCTION
	 */
	private void TEMPpollcheck() {
		if (System.currentTimeMillis() - lastTemperatureUpdate > 10000) {
			// Polled updates are having a hard time getting through with
			// the temporary comms locking, so we'll get them through here
			try {
				RefreshEmptySensor();
				RefreshTemperature();
			} catch (Exception ex) {
				System.out.println(materialType + " extruder exception during temperature/material update ignored");
			}
		}
	}
	
	public double getTemperature() {
		awaitSensorsInitialised();
		TEMPpollcheck();
		return currentTemperature;
	}

	/**
	 * The the outline speed and the infill speed [0,1]
	 */
	public double getInfillSpeed()
	{
		return iSpeed;
	}
	public double getOutlineSpeed()
	{
		return oSpeed;
	}
	
	/**
	 * The length in mm to speed up when going round corners
	 */
	public double getAngleSpeedUpLength()
	{
		return asLength;
	}
	
	/**
	 * The factor by which to speed up when going round a corner.
	 * The formula is speed = baseSpeed*[1 - 0.5*(1 + ca)*getAngleSpeedFactor()]
	 * where ca is the cos of the angle between the lines.  So it goes fastest when
	 * the line doubles back on itself (returning 1), and slowest when it 
	 * continues straight (returning 1 - getAngleSpeedFactor()).
	 */	
	public double getAngleSpeedFactor()
	{
		return asFactor;
	}	
	
	
	public void setCooler(boolean f) throws IOException {
		lock();
		try {
			OutgoingMessage request =
				new OutgoingByteMessage(MSG_SetCooler, f?(byte)200:(byte)0); // Should set speed properly!
			sendMessage(request);
		}
		finally {
			unlock();
		}
	}
	
	private void RefreshTemperature() throws Exception {
		//System.out.println(materialType + " extruder refreshing temperature");
		getDeviceTemperature();
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	private void getDeviceTemperature() throws Exception {
		lock();
		try {
			int rawHeat;
			int calibration;
			for(;;) {
				OutgoingMessage request = new OutgoingBlankMessage(MSG_GetTemp);
				RequestTemperatureResponse reply = new RequestTemperatureResponse(this, request, 500);
				
				rawHeat = reply.getHeat();
				//System.out.println(materialType + " extruder raw temp " + rawHeat);
				calibration = reply.getCalibration();
				
				if (rawHeat == 255 && vRefFactor > 0) {
					System.out.println(materialType + " extruder re-ranging temperature (faster)");
					vRefFactor--;
					setTempRange();
				} else if (rawHeat < 64 && vRefFactor < 15) {
					System.out.println(materialType + " extruder re-ranging temperature (slower)");
					vRefFactor++;
					setTempRange();
				} else
					break;
			}
			
			double resistance = calculateResistance(rawHeat, calibration);
			
			currentTemperature = calculateTemperature(resistance);
			//System.out.println(materialType + " extruder current temp " + currentTemperature);
			
			lastTemperatureUpdate = System.currentTimeMillis();
		}
		finally {
			unlock();
		}
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
		//double c = 1e-6;                           // farads - now cap from prefs(AB)
		double scale = 1 << (tempScaler+1);
		double clock = 4000000.0 / (4.0 * scale);  // hertz		
		double vdd = 5.0;                          // volts
		
		double vRef = 0.25 * vdd + vdd * vRefFactor / 32.0;  // volts
		
		double T = picTemp / clock; // seconds
		
		double resistance =	-T / (Math.log(1 - vRef / vdd) * cap);  // ohms
		
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
		//double c = 1e-6;                           // farads - now cap from prefs(AB)
		double scale = 1 << (tempScaler+1);
		double clock = 4000000.0 / (4.0 * scale);  // hertz		
		double vdd = 5.0;                          // volts
		
		double vRef = 0.25 * vdd + vdd * vRefFactor / 32.0;  // volts
		
		double T = -resistance * (Math.log(1 - vRef / vdd) * cap);

		double picTemp = T * clock;
		return (int)Math.round(picTemp);
		
	}
	
	/**
	 * Set raw voltage reference used for analogue to digital converter
	 * @param ref Set reference voltage (0-15).  Actually this is
	 * just directly OR'd into the PIC VRCON register, so it can also
	 * set the High/Low range bit.  
	 * @throws IOException
	 */
	private void setVref(int ref) throws IOException {
		sendMessage(new OutgoingByteMessage(MSG_SetVRef, (byte)ref));		
		vRefFactor = ref;
	}

	/**
	 * Set the scale factor used on the temperature timer used
	 * for analogue to digital conversion
	 * @param scale A value from 0..7
	 * @throws IOException
	 */
	private void setTempScaler(int scale) throws IOException {
		sendMessage(new OutgoingByteMessage(MSG_SetTempScaler, (byte)scale));		
		tempScaler = scale;
	}
	
	public boolean isAvailable() {
		return isCommsAvailable;
	}

	
	protected class RequestTemperatureResponse extends IncomingMessage {
		public RequestTemperatureResponse(Device device, OutgoingMessage message, long timeout) throws IOException {
			super(device, message, timeout);
		}
		
		protected boolean isExpectedPacketType(byte packetType) {
			return packetType == MSG_GetTemp; 
		}
		
		public int getHeat() throws InvalidPayloadException {
		    byte [] reply = getPayload();
		    if (reply == null || reply.length != 3)
		    		throw new InvalidPayloadException();
		    return reply[1] < 0 ? reply[1] + 256 : reply[1];
		}

		public int getCalibration() throws InvalidPayloadException {
		    byte [] reply = getPayload();
		    if (reply == null || reply.length != 3)
		    		throw new InvalidPayloadException();
		    return reply[2] < 0 ? reply[2] + 256 : reply[2];
		}
		
	}

	protected class RequestSetHeat extends OutgoingMessage {
		byte [] message;
		
		RequestSetHeat(byte heat, byte cutoff) {
			message = new byte [] { MSG_SetHeat, heat, heat, cutoff, cutoff }; 
		}

		RequestSetHeat(byte heat0, byte heat1, byte t0, byte t1) {
			message = new byte [] { MSG_SetHeat, heat0, heat1, t0, t1}; 
		}
		
		public byte[] getBinary() {
			return message;
		}
		
	}
	
	protected class RequestIsEmptyResponse extends IncomingMessage {

		public RequestIsEmptyResponse(Device device, OutgoingMessage message, long timeout) throws IOException {
			super(device, message, timeout);
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

    
    public int getXYSpeed()
    {
    	return xySpeed;
    }
    
    public int getExtruderSpeed()
    {
    	return extrusionSpeed;
    } 
    
    public double getExtrusionSize()
    {
    	return extrusionSize;
    } 
    
    public double getExtrusionHeight()
    {
    	return extrusionHeight;
    } 
    
    public double getExtrusionInfillWidth()
    {
    	return extrusionInfillWidth;
    } 
    
    public double getExtrusionOverRun()
    {
    	return extrusionOverRun;
    } 
    
    public long getExtrusionDelay()
    {
    	return extrusionDelay;
    } 
    
    public int getCoolingPeriod()
    {
    	return coolingPeriod;
    } 
    
    public double getOffsetX()
    {
    	return offsetX;
    }    
    public double getOffsetY()
    {
    	return offsetY;
    }
    public double getOffsetZ()
    {
    	return offsetZ;
    }
}
