package org.reprap;
import java.io.IOException;

import org.reprap.Device;
public interface Extruder {

	public void dispose(); 

	/**
	 * Start the extruder motor at a given speed.  This ranges from 0
	 * to 255 but is scaled by maxSpeed and t0, so that 255 corresponds to the
	 * highest permitted speed.  It is also scaled so that 0 would correspond
	 * with the lowest extrusion speed.
	 * @param speed The speed to drive the motor at (0-255)
	 * @throws IOException
	 */
	public void setExtrusion(int speed) throws IOException; 
	
	/**
	 * Start the extruder motor at a given speed.  This ranges from 0
	 * to 255 but is scaled by maxSpeed and t0, so that 255 corresponds to the
	 * highest permitted speed.  It is also scaled so that 0 would correspond
	 * with the lowest extrusion speed.
	 * @param speed The speed to drive the motor at (0-255)
	 * @param reverse If set, run extruder in reverse
	 * @throws IOException
	 */
	public void setExtrusion(int speed, boolean reverse) throws IOException; 

	public void heatOn() throws Exception; 

	public void setTemperature(double temperature) throws Exception; 
	
	/**
	 * Set a heat output power.  For normal production use you would
	 * normally call setTemperature, however this method may be useful
	 * for lower temperature profiling, etc.
	 * @param heat Heater power (0-255)
	 * @param maxTemp Cutoff temperature in celcius
	 * @throws IOException
	 */
	public void setHeater(int heat, double maxTemp) throws IOException; 
	
	/**
	 * Check if the extruder is out of feedstock
	 * @return true if there is no material remaining
	 */
	public boolean isEmpty(); 
	
	public double getTemperatureTarget(); 

	public double getDefaultTemperature();

	public double getTemperature(); 

	/**
	 * The the outline speed and the infill speed [0,1]
	 */
	public double getInfillSpeed();

	public double getOutlineSpeed();
	
	/**
	 * The length in mm to speed up when going round corners
	 */
	public double getAngleSpeedUpLength();

	/**
	 * The factor by which to speed up when going round a corner.
	 * The formula is speed = baseSpeed*[1 - 0.5*(1 + ca)*getAngleSpeedFactor()]
	 * where ca is the cos of the angle between the lines.  So it goes fastest when
	 * the line doubles back on itself (returning 1), and slowest when it 
	 * continues straight (returning 1 - getAngleSpeedFactor()).
	 */	
	public double getAngleSpeedFactor();
	
	public void setCooler(boolean f) throws IOException ;
	
	public boolean isAvailable(); 

    public int getXYSpeed();
 
    public int getExtruderSpeed();

    public double getExtrusionSize();
 
    public double getExtrusionHeight();

    public double getExtrusionInfillWidth();
 
    public double getExtrusionOverRun();
 
    public long getExtrusionDelay();

    public int getCoolingPeriod();
 
    public double getOffsetX();
 
    public double getOffsetY();
 
    public double getOffsetZ();
}
