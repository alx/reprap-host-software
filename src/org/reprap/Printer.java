package org.reprap;

import java.io.IOException;

import org.reprap.gui.Previewer;

public interface Printer {

	public void calibrate();

	public void printSegment(double startX, double startY,
			double startZ, double endX, double endY, double endZ) throws ReprapException, IOException;
	
	public void moveTo(double x, double y, double z) throws ReprapException, IOException;
	public void printTo(double x, double y, double z) throws ReprapException, IOException;

	public void selectMaterial(int materialIndex);
	
	/**
	 * Indicates end of job, homes extruder, powers down etc
	 *
	 */
	public void terminate() throws Exception;
	public void dispose();
	
	
	public int getSpeed();
	public void setSpeed(int speed);
	public int getSpeedZ();
	public void setSpeedZ(int speed);
	public int getExtruderSpeed();
	public void setExtruderSpeed(int speed);
	public void setPreviewer(Previewer previewer);
	public void setTemperature(int temperature) throws Exception;

	public boolean isCancelled();

	public void initialise() throws Exception;
	
	public double getX();
	public double getY();
	public double getZ();
	
	/**
	 * Returns the extrusion size for the currently selected material
	 * @return the size in mm
	 */
	public double getExtrusionSize();
	
	/**
	 * Returns the gap between infill lines
	 * @return the size in mm
	 */
	public double getInfillWidth();	
	
	/**
	 * Related to the getExtrusionSize, except this is the height
	 * of material that forms after any settling has taken place. 
	 * @return the extrusion height in mm
	 */
	public double getExtrusionHeight();

	
	/**
	 * Get the total distance moved (whether extruding or not)
	 * @return a double representing the distance travelled (mm)
	 */
	public double getTotalDistanceMoved();
	
	/**
	 * Get the total distance moved while extruding
	 * @return a double representing the distance travelled (mm)
	 */
	public double getTotalDistanceExtruded();
	
	/**
	 * Turn on or off the layer cooling system
	 * @param enable
	 */
	public void setCooling(boolean enable) throws IOException;
	
	/**
	 * Get the length before the end of a track to turn the extruder off
	 * to allow for the delay in the stream stopping.
	 * @return
	 */
	public double getOverRun();
	
	/**
	 * Get the number of milliseconds to wait between turning an 
	 * extruder on and starting to move it.
	 * @return
	 */
	public long getDelay();

	public double getTotalElapsedTime();
}
