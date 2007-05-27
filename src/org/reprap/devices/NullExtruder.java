/**
 * 
 */
package org.reprap.devices;

import java.io.IOException;
import org.reprap.Device;
import org.reprap.Extruder;
import org.reprap.Preferences;

/**
 * @author Adrian
 *
 */
public class NullExtruder implements Extruder{
	
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
	
	public NullExtruder(Preferences prefs, int extruderId) {

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
		
		
		isCommsAvailable = true;
	
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

	}

	/**
	 * Set extruder temperature
	 * @param temperature
	 * @throws Exception
	 */
	
	public void heatOn() throws Exception 
	{
		
	}
	
	public void setTemperature(double temperature) throws Exception {
		
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

	}
	

	/**
	 * Check if the extruder is out of feedstock
	 * @return true if there is no material remaining
	 */
	public boolean isEmpty() {
		return currentMaterialOutSensor;
	}
	


	public double getTemperatureTarget() {
		return requestedTemperature;
	}

	public double getDefaultTemperature() {
		return extrusionTemp;
	}	

	
	public double getTemperature() {
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

	}

	
	public boolean isAvailable() {
		return isCommsAvailable;
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
