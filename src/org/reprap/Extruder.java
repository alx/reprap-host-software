package org.reprap;
import java.io.IOException;
import javax.media.j3d.Appearance;

import org.reprap.Device;

public interface Extruder {

	
	/**
	 * Dispose of the extruder object 
	 */
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
	
	/**
	 * Turn the heater of the extruder on. Inital temperatur is defined by ???
	 * @throws Exception
	 */
	public void heatOn() throws Exception; 

	/**
	 * Set the temperature of the extruder at a given height. This height is given
	 * in centigrades, i.e. 100 equals 100 centigrades. 
	 * @param temperature The temperature of the extruder in centigrades
	 * @throws Exception
	 */
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
	
	/**
	 * @return the target temperature of the extruder
	 */
	public double getTemperatureTarget(); 

	/**
	 * @return the default temperature of the extruder
	 */
	public double getDefaultTemperature();

	/**
	 * @return the current temperature of the extruder 
	 */
	public double getTemperature(); 

	/**
	 * @return the infill speed as a value between [0,1]
	 */
	public double getInfillSpeed();

	/**
	 * @return the outline speed as a avlue between [0,1]
	 */
	public double getOutlineSpeed();
	
	/**
	 * @return The length in mm to speed up when going round corners
	 */
	public double getAngleSpeedUpLength();

	/**
	 * The factor by which to speed up when going round a corner.
	 * The formula is speed = baseSpeed*[1 - 0.5*(1 + ca)*getAngleSpeedFactor()]
	 * where ca is the cos of the angle between the lines.  So it goes fastest when
	 * the line doubles back on itself (returning 1), and slowest when it 
	 * continues straight (returning 1 - getAngleSpeedFactor()).
	 * @return the angle-speed factor 
	 */
	public double getAngleSpeedFactor();
	
	
	/**
	 * Turn the cooler (fan?) on or off
	 * @param f true if the cooler is to be turned on, false to turn off
	 * @throws IOException
	 */
	public void setCooler(boolean f) throws IOException ;
	
	/**
	 * Check if the extruder is available, which is determined by ???
	 * @return true if the extruder is available
	 */
	public boolean isAvailable(); 

    /**
     * The speed of X and Y movement
     * @return the XY speed
     */
    public int getXYSpeed();
 
    /**
     * @return the extruder speeds
     */
    public int getExtruderSpeed();

    /**
     * @return the extrusion size in millimeters
     */
    public double getExtrusionSize();
 
    /**
     * @return the extrusion height in millimeters
     */
    public double getExtrusionHeight();

    /**
     * @return the extrusion infill width in millimeters
     */
    public double getExtrusionInfillWidth();
 
    /**
     * @return the extrusion overrun in millimeters
     */
    public double getExtrusionOverRun();
 
    /**
     * @return the extrusion delay in seconds
     */
    public long getExtrusionDelay();

    /**
     * @return the cooling period in seconds
     */
    public int getCoolingPeriod();
 
    /**
     * @return the X offset in millimeters
     */
    public double getOffsetX();
 
    /**
     * @return the Y offset in millimeters
     */
    public double getOffsetY();
 
    /**
     * @return the Z offset in millimeters
     */
    public double getOffsetZ();
    
    /**
     * @return the appearance (colour) to use in the simulation window for this material
     */
    public Appearance getAppearance();  
    
    /**
     * @return the material name
     */
    public String toString();
    
    /**
     * @return whether nozzle wipe method is enabled or not 
     */
    public boolean getNozzleWipeEnabled();
    
    /**
     * @return the X-cord for the nozzle wiper
     */
    public int getNozzleWipeDatumX();

    /**
     * @return the Y-cord for the nozzle wiper
     */
    public int getNozzleWipeDatumY();
    
    /**
     * @return the length of the nozzle movement over the wiper
     */
    public int getNozzleWipeStroke();
    
    /**
     * @return the number of times the nozzle moves over the wiper
     */
    public int getNozzleWipeFreq();
    
    /**
     * @return the time to extrude before wiping the nozzle
     */
    public double getNozzleClearTime();
    
    /**
     * Start polygons at a random location round their perimiter
     * @return
     */
    public boolean randomStart();

    /**
     * get short lengths which need to be plotted faster
     * set -ve to turn this off.
     * @return
     */
    public double getShortLength();
    
    /**
     * Factor (between 0 and 1) to use to set the speed for
     * short lines.
     * @return
     */
    public double getShortSpeed();
    
    /**
     * Number of mm to overlap the hatching infill with the outline.  0 gives none; -ve will 
     * leave a gap between the two
     * @return
     */
    public double getInfillOverlap();
    
}
