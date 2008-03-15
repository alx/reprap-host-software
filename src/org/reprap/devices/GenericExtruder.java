package org.reprap.devices;

import java.io.IOException;

import org.reprap.Device;
import org.reprap.Preferences;
import org.reprap.utilities.Debug;
import org.reprap.Extruder;
import org.reprap.comms.Address;
import org.reprap.comms.Communicator;
import org.reprap.comms.IncomingMessage;
import org.reprap.comms.OutgoingMessage;
import org.reprap.comms.IncomingMessage.InvalidPayloadException;
import org.reprap.comms.messages.OutgoingBlankMessage;
import org.reprap.comms.messages.OutgoingByteMessage;
import javax.media.j3d.Appearance;
import javax.vecmath.Color3f;
import javax.media.j3d.Material;

/**
 * @author jwiel
 *
 */
public class GenericExtruder extends Device implements Extruder{

	
	/**
	 * API for firmware
	 * Activate the extruder motor in forward direction 
	 */
	public static final byte MSG_SetActive = 1;
	
	/**
	 *  Activate the extruder motor in reverse direction
	 */
	public static final byte MSG_SetActiveReverse = 2;
	
	/**
	 * There is no material left to extrude 
	 */
	public static final byte MSG_IsEmpty = 8;
	
	/**
	 * Set the temperature of the extruder
	 */
	public static final byte MSG_SetHeat = 9;
	
	/**
	 * Get the temperature of the extruder 
	 */
	public static final byte MSG_GetTemp = 10;
		
	/**
	 * Turn the cooler/fan on 
	 */
	public static final byte MSG_SetCooler = 11;
	
	/**
	 * Set Vref 
	 */
	public static final byte MSG_SetVRef = 52;
	
	/**
	 * Set the Tempscaler 
	 */
	public static final byte MSG_SetTempScaler = 53;
	 
	/**
	 * Offset of 0 degrees centigrade from absolute zero
	 */
	private static final double absZero = 273.15;
	
	/**
	 * The temperature to maintain 
	 */
	private double requestedTemperature = 0;
	
	/**
	 * The temperature most recently read from the device 
	 */
	private double currentTemperature = 0;
	
	/**
	 * Temprature history
	 */
	private double[] tH;
	private int tHi;
	
	/**
	 * Is a material-out sensor connected to the exteruder or not. 
	 * If this is the case, TODO: impact?
	 */
	private boolean currentMaterialOutSensor = false;
	
	
	/**
	 * Indicates when polled values are first ready 
	 */
	private boolean sensorsInitialised = false;
	
	/**
	 * 
	 */
	private Thread pollThread = null;
	
	/**
	 * 
	 */
	private boolean pollThreadExiting = false;
	
	/**
	 * 
	 */
	private int vRefFactor = 7;
	
	/**
	 * 
	 */
	private int tempScaler = 4;
	
	
	/**
	 * Thermistor beta
	 */
	private double beta; 
	
	/**
	 * Thermistor resistance at 0C
	 */
	private double rz;  
	
	/**
	 * Thermistor timing capacitor in farads
	 */
	private double cap;    

	/**
	 * Heater power gradient
	 */
	private double hm;   

	/**
	 * Heater power intercept
	 * TODO: hb should probably be ambient temperature measured at this point
	 */
	private double hb;    

	/**
	 * Maximum motor speed (value between 0-255)
	 */
	private int maxExtruderSpeed; 
	
	/**
	 * The actual extrusion speed
	 */
	private int extrusionSpeed;
	
	/**
	 * The extrusion temperature
	 */
	private double extrusionTemp; 
	
	/**
	 * The extrusion width in XY
	 */
	private double extrusionSize;
	
	/**
	 * The extrusion height in Z
	 * TODO: Should this be a machine-wide constant? - AB
	 */
	private double extrusionHeight; 
	                                
	/**
	 * The step between infill tracks
	 */
	private double extrusionInfillWidth; 
	
	/**
	 * The number of mm to stop extruding before the end of a track
	 */
	private double extrusionOverRun; 
	
	/**
	 * The number of seconds to cool between layers
	 */
	private int coolingPeriod;
	
	/**
	 * The speed of movement in XY when depositing
	 */
	private int xySpeed; 
	
	/**
	 * Zero torque speed 
	 */
	private int t0;
	
	/**
	 * Infill speed [0,1]*maxSpeed
	 */
	private double iSpeed;
	
	/**
	 * Outline speed [0,1]*maxSpeed
	 */
	private double oSpeed;
	
	/**
	 * Length (mm) to speed up round corners
	 */
	private double asLength;
	
	/**
	 * Factor by which to speed up round corners
	 */
	private double asFactor;
	
	/**
	 * Line length below which to plot faster
	 */
	private double shortLength;
	
	/**
	 *Factor for short line speeds
	 */
	private double shortSpeed;
	
	/**
	 * The name of this extruder's material
	 */
	private String material;
	
	/**
	 * Number of mm to overlap the hatching infill with the outline.  0 gives none; -ve will 
     * leave a gap between the two
	 */
	private double infillOverlap = 0;
	
	/**
	 * Where to put the nozzle
	 */
	private double offsetX, offsetY, offsetZ; 
	
	/**
	 * 
	 */
	private long lastTemperatureUpdate = 0;
	
	/**
	 * Identifier of and extruder
	 * TODO: which values mean what? 
	 */
	private int myExtruderID;
	
	/**
	 * Start polygons at random perimiter points 
	 */
	private boolean randSt = false;

	/**
	 * Start polygons at incremented perimiter points 
	 */
	private boolean incrementedSt = false;
	
	/**
	 * Flag indicating if initialisation succeeded.  Usually this
	 * indicates if the extruder is present in the network. 
	 */
	private boolean isCommsAvailable = false;
	
	/**
	 *  The colour black
	 */	
	protected static final Color3f black = new Color3f(0, 0, 0);
	
	/**
	 *  The colour of the material to use in the simulation windows 
	 */	
	private Appearance materialColour;
	
	/**
	 * @param communicator
	 * @param address
	 * @param prefs
	 * @param extruderId
	 */
	
	/**
	 * Enable wiping procedure for nozzle
	 */
	private boolean nozzleWipeEnabled;
	
	/**
	 * Co-ordinates for the nozzle wiper
	 */
	private double nozzleWipeDatumX;
	private double nozzleWipeDatumY;
	
	/**
	 * X Distance to move nozzle over wiper
	 */
	private double nozzleWipeStrokeX;
	
	/**
	 * Y Distance to move nozzle over wiper
	 */
	private double nozzleWipeStrokeY;
	
	/**
	 * Number of wipe cycles per method call
	 */
	private int nozzleWipeFreq;
	
	/**
	 * Number of seconds to run to re-start the nozzle before a wipe
	 */
	private double nozzleClearTime;

	/**
	 * Number of seconds to wait after restarting the nozzle
	 */
	private double nozzleWaitTime;
	
	
	/**
	 * The number of milliseconds to wait before starting a border track
	 */
	private int extrusionDelayForBorder = 0;
	
	/**
	 * The number of milliseconds to wait before starting a hatch track
	 */
	private int extrusionDelayForHatch = 0;

	
	public GenericExtruder(Communicator communicator, Address address, Preferences prefs, int extruderId) {
		
		super(communicator, address);
		
		tH = new double[] {20, 20, 20}; // Bit of a hack - room temp
		tHi = 0;
		
		myExtruderID = extruderId;
		String prefName = "Extruder" + extruderId + "_";
		
		beta = prefs.loadDouble(prefName + "Beta(K)");
		rz = prefs.loadDouble(prefName + "Rz(ohms)");
		cap = prefs.loadDouble(prefName + "Capacitor(F)");
		hm = prefs.loadDouble(prefName + "hm(C/pwr)");
		hb = prefs.loadDouble(prefName + "hb(C)");
		maxExtruderSpeed = prefs.loadInt(prefName + "MaxSpeed(0..255)");
		extrusionSpeed = prefs.loadInt(prefName + "ExtrusionSpeed(0..255)");
		extrusionTemp = prefs.loadDouble(prefName + "ExtrusionTemp(C)");
		extrusionSize = prefs.loadDouble(prefName + "ExtrusionSize(mm)");
		extrusionHeight = prefs.loadDouble(prefName + "ExtrusionHeight(mm)");
		extrusionInfillWidth = prefs.loadDouble(prefName + "ExtrusionInfillWidth(mm)");
		extrusionOverRun = prefs.loadDouble(prefName + "ExtrusionOverRun(mm)");
		coolingPeriod = prefs.loadInt(prefName + "CoolingPeriod(s)");
		xySpeed = prefs.loadInt(prefName + "XYSpeed(0..255)");
		t0 = prefs.loadInt(prefName + "t0(0..255)");
		iSpeed = prefs.loadDouble(prefName + "InfillSpeed(0..1)");
		oSpeed = prefs.loadDouble(prefName + "OutlineSpeed(0..1)");
		asLength = prefs.loadDouble(prefName + "AngleSpeedLength(mm)");
		asFactor = prefs.loadDouble(prefName + "AngleSpeedFactor(0..1)");
		material = prefs.loadString(prefName + "MaterialType(name)");
		offsetX = prefs.loadDouble(prefName + "OffsetX(mm)");
		offsetY = prefs.loadDouble(prefName + "OffsetY(mm)");
		offsetZ = prefs.loadDouble(prefName + "OffsetZ(mm)");
		nozzleWipeEnabled = prefs.loadBool(prefName + "NozzleWipeEnabled");
		nozzleWipeDatumX = prefs.loadDouble(prefName + "NozzleWipeDatumX(mm)");
		nozzleWipeDatumY = prefs.loadDouble(prefName + "NozzleWipeDatumY(mm)");
		nozzleWipeStrokeX = prefs.loadDouble(prefName + "NozzleWipeStrokeX(mm)");
		nozzleWipeStrokeY = prefs.loadDouble(prefName + "NozzleWipeStrokeY(mm)");
		nozzleWipeFreq = prefs.loadInt(prefName + "NozzleWipeFreq");
		nozzleClearTime = prefs.loadDouble(prefName + "NozzleClearTime(s)");
		nozzleWaitTime = prefs.loadDouble(prefName + "NozzleWaitTime(s)");
		randSt = prefs.loadBool(prefName + "RandomStart");
		incrementedSt = prefs.loadBool(prefName + "IncrementedStart");
		shortLength = prefs.loadDouble(prefName + "ShortLength(mm)");
		shortSpeed = prefs.loadDouble(prefName + "ShortSpeed(0..1)");
		infillOverlap = prefs.loadDouble(prefName + "InfillOverlap(mm)");
		extrusionDelayForBorder = prefs.loadInt(prefName + "ExtrusionDelayForBorder(ms)");
		extrusionDelayForHatch = prefs.loadInt(prefName + "ExtrusionDelayForHatch(ms)");
		
		Color3f col = new Color3f((float)prefs.loadDouble(prefName + "ColourR(0..1)"), 
				(float)prefs.loadDouble(prefName + "ColourG(0..1)"), 
				(float)prefs.loadDouble(prefName + "ColourB(0..1)"));
		materialColour = new Appearance();
		materialColour.setMaterial(new Material(col, black, col, black, 101f));
		
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
	
	/* (non-Javadoc)
	 * @see org.reprap.Extruder#dispose()
	 */
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
	 * Turn the extruder on using the extrusionTemp property
	 * @throws Exception
	 */
	
	public void heatOn() throws Exception 
	{
		setTemperature(extrusionTemp);
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Extruder#setTemperature(double)
	 */
	public void setTemperature(double temperature) throws Exception {
		setTemperature(temperature, true);
	}
	
	/**
	 * @param temperature
	 * @param lock
	 * @throws Exception
	 */
	private void setTemperature(double temperature, boolean lock) throws Exception {
		requestedTemperature = temperature;
		if(Math.abs(requestedTemperature - extrusionTemp) > 5)
		{
			Debug.d(material + " extruder temperature set to " + requestedTemperature +
				"C, which is not the standard temperature (" + extrusionTemp + "C).");
		}
		// Aim for 10% above our target to ensure we reach it.  It doesn't matter
		// if we go over because the power will be adjusted when we get there.  At
		// the same time, if we aim too high, we'll overshoot a bit before we
		// can react.
		
		// Tighter temp constraints under test 10% -> 3% (10-1-8)
		double temperature0 = temperature * 1.03;
		
		// A safety cutoff will be set at 20% above requested setting
		// Tighter temp constraints added by eD 20% -> 6% (10-1-8)
		
		double temperatureSafety = temperature * 1.06;
		
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
		//System.out.println(material + " extruder heater set to " + heat + " limit " + safetyCutoff);
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
		Debug.d(material + " extruder heater set to " + heat0 + "/" + heat1 + " limit " + t0 + "/" + t1);
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
	
	/**
	 * 
	 */
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
		Debug.d(material + " extruder vRefFactor set to " + vRefFactor);
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
			//System.out.println(material + " extruder refreshing sensor");
			RequestIsEmptyResponse reply = new RequestIsEmptyResponse(this, new OutgoingBlankMessage(MSG_IsEmpty), 500);
			currentMaterialOutSensor = reply.getValue() == 0 ? false : true; 
		} catch (InvalidPayloadException e) {
			throw new IOException();
		} finally {
			unlock();
		}
	}

	/* (non-Javadoc)
	 * @see org.reprap.Extruder#getTemperatureTarget()
	 */
	public double getTemperatureTarget() {
		return requestedTemperature;
	}

	/* (non-Javadoc)
	 * @see org.reprap.Extruder#getDefaultTemperature()
	 */
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
				tH[tHi] = currentTemperature;
				currentTemperature = tempVote();
			} catch (Exception ex) {
				Debug.d(material + " extruder exception during temperature/material update ignored");
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * Take a vote among the last three temperatures
	 * @return
	 */
	private double tempVote()
	{
		int ip = (tHi + 1)%3;
		int ipp = (tHi + 2)%3;
		double dp = Math.abs(tH[tHi] - tH[ip]);
		double dpp = Math.abs(tH[tHi] - tH[ipp]);
		double d = Math.abs(tH[ip] - tH[ipp]);
		if(dp <= d || dpp <= d)
			d = tH[tHi];
		else
			d = 0.5*(tH[ip] + tH[ipp]);
		//Debug.d("tempVote() - t0: " + tH[0] + ", t1: " + tH[1] + ", t2: " + tH[2] +
				//", current: " + tH[tHi] + ", returning: " + d);
		tHi = (tHi + 1)%3;
		return d;
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Extruder#getTemperature()
	 */
	public double getTemperature() {
		awaitSensorsInitialised();
		TEMPpollcheck();
		//return tempVote();
		return currentTemperature;
	}

	/**
	 * The the outline speed and the infill speed [0,1]
	 */
	public double getInfillSpeed()
	{
		return iSpeed;
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Extruder#getOutlineSpeed()
	 */
	public double getOutlineSpeed()
	{
		return oSpeed;
	}
	
	/**
	 * The length in mm to speed up when going round corners
	 * (non-Javadoc)
	 * @see org.reprap.Extruder#getAngleSpeedUpLength()
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
	 * (non-Javadoc)
	 * @see org.reprap.Extruder#getAngleSpeedFactor()
	 */
	public double getAngleSpeedFactor()
	{
		return asFactor;
	}	
	
	
	/* (non-Javadoc)
	 * @see org.reprap.Extruder#setCooler(boolean)
	 */
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
	
	/**
	 * @throws Exception
	 */
	private void RefreshTemperature() throws Exception {
		//System.out.println(material + " extruder refreshing temperature");
		getDeviceTemperature();
	}
	
	/**
	 * 
	 * @param rawHeat
	 * @return
	 * @throws Exception
	 */
	private boolean rerangeTemperature(int rawHeat) throws Exception 
	{
		boolean notDone = false;
		if (rawHeat == 255 && vRefFactor > 0) {
			vRefFactor--;
			Debug.d(material + " extruder re-ranging temperature (faster): ");
			setTempRange();
		} else if (rawHeat < 64 && vRefFactor < 15) {
			vRefFactor++;
			Debug.d(material + " extruder re-ranging temperature (slower): ");
			setTempRange();
		} else
			notDone = true;
		return notDone;
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	private void getDeviceTemperature() throws Exception {
		lock();
		try {
			int rawHeat = 0;
			int calibration = 0;
			for(;;) { // Don't repeatedly re-range?
				OutgoingMessage request = new OutgoingBlankMessage(MSG_GetTemp);
				RequestTemperatureResponse reply = new RequestTemperatureResponse(this, request, 500);
				
				rawHeat = reply.getHeat();
				//System.out.println(material + " extruder raw temp " + rawHeat);

				calibration = reply.getCalibration();
				
				if(rerangeTemperature(rawHeat))
					break; // All ok
				else
					Thread.sleep(500); // Wait for PIC temp routine to settle before going again
			}
			
			double resistance = calculateResistance(rawHeat, calibration);
			
			currentTemperature = calculateTemperature(resistance);
			Debug.d(material + " extruder current temp " + currentTemperature);
			
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
	
	/**
	 * @param temperature
	 * @return
	 */
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
	
	/* (non-Javadoc)
	 * @see org.reprap.Extruder#isAvailable()
	 */
	public boolean isAvailable() {
		return isCommsAvailable;
	}

	
	/**
	 *
	 */
	protected class RequestTemperatureResponse extends IncomingMessage {
		
		/**
		 * @param device
		 * @param message
		 * @param timeout
		 * @throws IOException
		 */
		public RequestTemperatureResponse(Device device, OutgoingMessage message, 
				long timeout) throws IOException {
			super(device, message, timeout);
		}
		
		/* (non-Javadoc)
		 * @see org.reprap.comms.IncomingMessage#isExpectedPacketType(byte)
		 */
		protected boolean isExpectedPacketType(byte packetType) {
			return packetType == MSG_GetTemp; 
		}
		
		/**
		 * @return
		 * @throws InvalidPayloadException
		 */
		public int getHeat() throws InvalidPayloadException {
		    byte [] reply = getPayload();
		    if (reply == null || reply.length != 3)
		    		throw new InvalidPayloadException();
		    return reply[1] < 0 ? reply[1] + 256 : reply[1];
		}

		/**
		 * @return
		 * @throws InvalidPayloadException
		 */
		public int getCalibration() throws InvalidPayloadException {
		    byte [] reply = getPayload();
		    if (reply == null || reply.length != 3)
		    		throw new InvalidPayloadException();
		    return reply[2] < 0 ? reply[2] + 256 : reply[2];
		}
		
	}

	/**
	 *
	 */
	protected class RequestSetHeat extends OutgoingMessage {
		
		/**
		 * 
		 */
		byte [] message;
		
		/**
		 * @param heat
		 * @param cutoff
		 */
		RequestSetHeat(byte heat, byte cutoff) {
			message = new byte [] { MSG_SetHeat, heat, heat, cutoff, cutoff }; 
		}

		/**
		 * @param heat0
		 * @param heat1
		 * @param t0
		 * @param t1
		 */
		RequestSetHeat(byte heat0, byte heat1, byte t0, byte t1) {
			message = new byte [] { MSG_SetHeat, heat0, heat1, t0, t1}; 
		}
		
		/* (non-Javadoc)
		 * @see org.reprap.comms.OutgoingMessage#getBinary()
		 */
		public byte[] getBinary() {
			return message;
		}
		
	}
	
	/**
	 *
	 */
	protected class RequestIsEmptyResponse extends IncomingMessage {

		/**
		 * @param device
		 * @param message
		 * @param timeout
		 * @throws IOException
		 */
		public RequestIsEmptyResponse(Device device, OutgoingMessage message, long timeout) throws IOException {
			super(device, message, timeout);
		}
		
		/**
		 * @return
		 * @throws InvalidPayloadException
		 */
		public byte getValue() throws InvalidPayloadException {
			byte [] reply = getPayload();
			if (reply == null || reply.length != 2)
				throw new InvalidPayloadException();
			return reply[1];
		}
		
		/* (non-Javadoc)
		 * @see org.reprap.comms.IncomingMessage#isExpectedPacketType(byte)
		 */
		protected boolean isExpectedPacketType(byte packetType) {
			return packetType == MSG_IsEmpty;
		}
		
	}

    
    /* (non-Javadoc)
     * @see org.reprap.Extruder#getXYSpeed()
     */
    public int getXYSpeed()
    {
    	return xySpeed;
    }
    
    /* (non-Javadoc)
     * @see org.reprap.Extruder#getExtruderSpeed()
     */
    public int getExtruderSpeed()
    {
    	return extrusionSpeed;
    } 
    
    /* (non-Javadoc)
     * @see org.reprap.Extruder#getExtrusionSize()
     */
    public double getExtrusionSize()
    {
    	return extrusionSize;
    } 
    
    /* (non-Javadoc)
     * @see org.reprap.Extruder#getExtrusionHeight()
     */
    public double getExtrusionHeight()
    {
    	return extrusionHeight;
    } 
    
    /* (non-Javadoc)
     * @see org.reprap.Extruder#getExtrusionInfillWidth()
     */
    public double getExtrusionInfillWidth()
    {
    	return extrusionInfillWidth;
    } 
    
    /* (non-Javadoc)
     * @see org.reprap.Extruder#getExtrusionOverRun()
     */
    public double getExtrusionOverRun()
    {
    	return extrusionOverRun;
    } 
    
  
    /* (non-Javadoc)
     * @see org.reprap.Extruder#getCoolingPeriod()
     */
    public int getCoolingPeriod()
    {
    	return coolingPeriod;
    } 
    
    /* (non-Javadoc)
     * @see org.reprap.Extruder#getOffsetX()
     */
    public double getOffsetX()
    {
    	return offsetX;
    }    
    
    /* (non-Javadoc)
     * @see org.reprap.Extruder#getOffsetY()
     */
    public double getOffsetY()
    {
    	return offsetY;
    }
    
    /* (non-Javadoc)
     * @see org.reprap.Extruder#getOffsetZ()
     */
    public double getOffsetZ()
    {
    	return offsetZ;
    }
    
    /* (non-Javadoc)
     * @see org.reprap.Extruder#getColour()
     */    
    public Appearance getAppearance()
    {
    	return materialColour;
    }  
    
    /**
     * @return the name of the material
     */
    public String toString()
    {
    	return material;
    }
    
    /**
     * @return determine whether nozzle wipe method is enabled or not 
     */
    public boolean getNozzleWipeEnabled()
    {
    	return nozzleWipeEnabled;
    }    
    
    /**
     * @return the X-cord for the nozzle wiper
     */
    public double getNozzleWipeDatumX()
    {
    	return nozzleWipeDatumX;
    }

    /**
     * @return the Y-cord for the nozzle wiper
     */
    public double getNozzleWipeDatumY()
    {
    	return nozzleWipeDatumY;
    }
    
    /**
     * @return the length of the nozzle movement over the wiper
     */
    public double getNozzleWipeStrokeX()
    {
    	return nozzleWipeStrokeX;
    }
    
    /**
     * @return the length of the nozzle movement over the wiper
     */
    public double getNozzleWipeStrokeY()
    {
    	return nozzleWipeStrokeY;
    }
    
    /**
     * @return the number of times the nozzle moves over the wiper
     */
    public int getNozzleWipeFreq()
    {
    	return nozzleWipeFreq;
    }
    
    /**
     * @return the time to extrude before wiping the nozzle
     */
    public double getNozzleClearTime()
    {
    	return nozzleClearTime;
    }
    
    /**
     * @return the time to wait after extruding before wiping the nozzle
     */
    public double getNozzleWaitTime()
    {
    	return nozzleWaitTime;
    }
    
    
    /**
     * Start polygons at a random location round their perimiter
     * @return
     */
    public boolean randomStart()
    {
    	return randSt;
    }

    /**
     * Start polygons at an incremented location round their perimiter
     * @return
     */
    public boolean incrementedStart()
    {
    	return incrementedSt;
    }
    
    /**
     * get short lengths which need to be plotted faster
     * set -ve to turn this off.
     * @return
     */
    public double getShortLength()
    {
    	return shortLength; 
    }
    
    /**
     * Factor (between 0 and 1) to use to set the speed for
     * short lines.
     * @return
     */
    public double getShortSpeed()
    {
    	return shortSpeed; 
    }
    
    /**
     * Number of mm to overlap the hatching infill with the outline.  0 gives none; -ve will 
     * leave a gap between the two
     * @return
     */
    public double getInfillOverlap()
    {
    	return infillOverlap;
    }
    
    /**
	 * Gets the number of milliseconds to wait before starting a border track
	 * @return
     */
    public int getExtrusionDelayForBorder()
    {
    	return extrusionDelayForBorder; 
    }
    
    /**
	 * Gets the number of milliseconds to wait before starting a hatch track
	 * @return
     */
    public int getExtrusionDelayForHatch()
    {
    	return extrusionDelayForHatch; 
    }
    
}
