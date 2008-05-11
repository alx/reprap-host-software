package org.reprap;

import java.awt.print.PrinterAbortException;
import java.io.IOException;
import javax.media.j3d.*;
import javax.swing.JCheckBoxMenuItem;
import org.reprap.gui.Previewer;
import org.reprap.devices.GenericExtruder;

public interface Printer {

	/**
	 * Method to calibrate the printer.
	 */
	public void calibrate();

//	public void printSegment(double startX, double startY,
//			double startZ, double endX, double endY, double endZ) throws ReprapException, IOException;
	
	/**
	 * Move the printer carriage to the give x, y and z position <b>while extruding material<b>
	 * 
	 * @param x absolute x position in millimeters relative to the home position. 
	 * Range between [0..???]
	 * @param y absolute y position in millimters relative to the home position. 
	 * Range betwen [0..???]
	 * @param z absolute z position in millimters relative to the home position.
	 * Range between [0..???]
	 * @param startUp ?
	 * @param endUp ?
	 * @throws ReprapException
	 * @throws IOException 
	 */
	public void moveTo(double x, double y, double z, boolean startUp, boolean endUp) throws ReprapException, IOException;
	
	/**
	 * Move the printer carriage to the give x, y and z position <b>while extruding material<b>
	 * 
	 * @param x absolute x position in millimeters relative to the home position. 
	 * Range between [0..???]
	 * @param y absolute y position in millimters relative to the home position. 
	 * Range betwen [0..???]
	 * @param z absolute z position in millimters relative to the home position.
	 * Range between [0..???]
	 * @param turnOff True if extruder should be turned off at end of this segment.
	 * @throws ReprapException
	 * @throws IOException 
	 */
	public void printTo(double x, double y, double z, boolean turnOff) throws ReprapException, IOException;
	
	/**
	 * Fire up the extruder for a lead-in
	 * @param firstOneInLayer (first after the layer pause, or any old polygon?)
	 */
	public void printStartDelay(boolean firstOneInLayer);	
	
	/**
	 * Sync to zero X location.
	 * @throws ReprapException
	 * @throws IOException
	 */
	public void homeToZeroX() throws ReprapException, IOException;
	
	/**
	 * Sync to zero Y location.
	 * @throws ReprapException
	 * @throws IOException
	 */
	public void homeToZeroY() throws ReprapException, IOException; 
	
	/**
	 * Select a specific material to print with
	 * @param attributes with name of the material
	 */
	public void selectExtruder(Attributes att);
	
	/**
	 * Select a specific material to print with
	 * @param extr identifier of the material
	 */
	public void selectExtruder(int extr);
	
	/**
	 * Indicates end of job, homes extruder, powers down etc
	 * @throws Exception
	 */
	public void terminate() throws Exception;
	
	/**
	 * Dispose of the printer
	 */
	public void dispose();
	
	
//	public int getSpeed();
	
	/**
	 * @param speed
	 */
	public void setSpeed(int speed);
	
	/**
	 * @param speed
	 */
	public void setFastSpeed(int speed);
	
	/**
	 * @return the extruder speed
	 */
	public int getSpeedZ();
	
	/**
	 * @param speed
	 */
	public void setSpeedZ(int speed);
	
//	public int getExtruderSpeed();
//	public void setExtruderSpeed(int speed);
	
	/**
	 * @param previewer
	 */
	public void setPreviewer(Previewer previewer);
	
//	public void setTemperature(int temperature) throws Exception;
//	public double getInfillSpeed();
//	public double getOutlineSpeed();
	
	/**
	 * @return is cancelled when ...?
	 */
	public boolean isCancelled();
	
//	public double getAngleSpeedUpLength();
//	public double getAngleSpeedFactor();
	
	/**
	 * @return the extrider fast speed
	 */
	public int getFastSpeed();
	
	/**
	 * @throws Exception
	 */
	public void initialise() throws Exception;
	
	/**
	 * @return current X position
	 */
	public double getX();
	
	/**
	 * @return current Y position
	 */
	public double getY();
	
	/**
	 * @return current Z position
	 */
	public double getZ();

	/**
	 * @return the extruder for the printer
	 */
	public Extruder getExtruder();
	public void stopExtruding() throws IOException;
	public void stopValve() throws IOException;

	/**
	 * Allow the user to manually calibrate the Z axis position to deal
	 * with special circumstances like different extruder sizes, platform
	 * additions or subtractive fabrication.
	 * 
	 * @throws IOException
	 */
	public void setZManual() throws IOException;

	/**
	 * Allow the user to manually calibrate the Z axis position to deal
	 * with special circumstances like different extruder sizes, platform
	 * additions or subtractive fabrication.
	 * 
	 * @param zeroPoint The point the user selects will be treated as the
	 * given Z value rather than 0.0 
	 */
	public void setZManual(double zeroPoint) throws IOException;
	
	/**
	 * Returns the extrusion size for the currently selected material
	 * @return the size in mm
	 */
//	public double getExtrusionSize();
	
	/**
	 * Returns the gap between infill lines
	 * @return the size in mm
	 */
//	public double getInfillWidth();	
	
	/**
	 * Related to the getExtrusionSize, except this is the height
	 * of material that forms after any settling has taken place. 
	 * @return the extrusion height in mm
	 */
//	public double getExtrusionHeight();

	
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
//	public void setCooling(boolean enable) throws IOException;
	
	/**
	 * Get the length before the end of a track to turn the extruder off
	 * to allow for the delay in the stream stopping.
	 * @return
	 */
//	public double getOverRun();
	
	/**
	 * Get the number of milliseconds to wait between turning an 
	 * extruder on and starting to move it.
	 * @return
	 */
//	public long getDelay();

	/**
	 * @return total time the extruder has been moving in seconds
	 */
	public double getTotalElapsedTime();
	
	/**
	 * @param ls
	 */
	public void setLowerShell(BranchGroup ls);
	
	/**
	 * @param name
	 * @return the extruder for the material called name; null if not found.
	 */
	public Extruder getExtruder(String name);
	
	/**
	 * Get the list of all the extruders
	 * @return
	 */
	public Extruder[] getExtruders();
	
	/**
	 * Just finished a layer
	 * @param layerNumber
	 */
	public void finishedLayer(int layerNumber) throws Exception;
	
	/**
	 * Do whatever needs to be done between one layer and the next
	 * @param layerNumber
	 */
	public void betweenLayers(int layerNumber) throws Exception;
	
	/**
	 * Just about to start the next layer
	 * @param layerNumber
	 */
	public void startingLayer(int layerNumber) throws Exception;
	
	/**
	 * Set the source checkbox used to determine if there should
	 * be a pause between segments.
	 * 
	 * @param segmentPause The source checkbox used to determine
	 * if there should be a pause.  This is a checkbox rather than
	 * a boolean so it can be changed on the fly. 
	 */
	public void setSegmentPause(JCheckBoxMenuItem segmentPause);
	
	/**
	 * Set the source checkbox used to determine if there should
	 * be a pause between layers.
	 * 
	 * @param layerPause The source checkbox used to determine
	 * if there should be a pause.  This is a checkbox rather than
	 * a boolean so it can be changed on the fly.
	 */
	public void setLayerPause(JCheckBoxMenuItem layerPause);
	
	public void setCancelled(boolean c);
         
}
