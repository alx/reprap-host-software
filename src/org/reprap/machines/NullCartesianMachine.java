package org.reprap.machines;

import java.io.IOException;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.media.j3d.*;

import org.reprap.Attributes;
import org.reprap.CartesianPrinter;
import org.reprap.Preferences;
import org.reprap.Extruder;
import org.reprap.ReprapException;
import org.reprap.gui.*;
import org.reprap.devices.NullExtruder;

/**
 *
 */
public class NullCartesianMachine implements CartesianPrinter {
	
	/**
	 * 
	 */
	private StatusMessage statusWindow;
	
	/**
	 * 
	 */
	private JCheckBoxMenuItem layerPauseCheckbox = null, segmentPauseCheckbox = null;
	
	/**
	 * 
	 */
	private Previewer previewer = null;

	/**
	 * 
	 */
	double totalDistanceMoved = 0.0;
	
	/**
	 * 
	 */
	double totalDistanceExtruded = 0.0;
	
	//double extrusionSize, extrusionHeight, infillWidth;
	
	/**
	 * 
	 */
	double currentX, currentY, currentZ;
	
	/**
	 * 
	 */
	private double overRun;
	
	/**
	 * 
	 */
	private long delay;

	/**
	 * 
	 */
	private long startTime;
	
	/**
	 * 
	 */
	private Extruder extruders[];
	
	/**
	 * 
	 */
	private int extruder;
	
	/**
	 * 
	 */
	private int extruderCount;

	/**
	 * @param config
	 */
	public NullCartesianMachine(Preferences config) {
		statusWindow = new StatusMessage(new JFrame());
		
		startTime = System.currentTimeMillis();
		
		extruderCount = config.loadInt("NumberOfExtruders");
		extruders = new NullExtruder[extruderCount];
		for(int i = 0; i < extruderCount; i++)
		{
			String prefix = "Extruder" + i + "_";
			extruders[i] = new NullExtruder(config, i);
		}
		extruder = 1;

		currentX = 0;
		currentY = 0;
		currentZ = 0;
		

	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#calibrate()
	 */
	public void calibrate() {
	}

	/**
	 * @param startX
	 * @param startY
	 * @param startZ
	 * @param endX
	 * @param endY
	 * @param endZ
	 * @throws ReprapException
	 * @throws IOException
	 */
	public void printSegment(double startX, double startY, double startZ, 
			double endX, double endY, double endZ, boolean turnOff) throws ReprapException, IOException {
		moveTo(startX, startY, startZ, true, true);
		printTo(endX, endY, endZ, turnOff);
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#moveTo(double, double, double, boolean, boolean)
	 */
	public void moveTo(double x, double y, double z, boolean startUp, 
			boolean endUp) throws ReprapException, IOException {
		if (isCancelled()) return;

		totalDistanceMoved += segmentLength(x - currentX, y - currentY);
		//TODO - next bit needs to take account of startUp and endUp
		if (z != currentZ)
			totalDistanceMoved += Math.abs(currentZ - z);

		currentX = x;
		currentY = y;
		currentZ = z;
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#printTo(double, double, double)
	 */
	public void printTo(double x, double y, double z, 
			boolean turnOff) throws ReprapException, IOException {
		if (segmentPauseCheckbox != null)
			if(segmentPauseCheckbox.isSelected())
				segmentPause();
		
		if (previewer != null)
			previewer.addSegment(currentX, currentY, currentZ, x, y, z);
		if (isCancelled()) return;

		double distance = segmentLength(x - currentX, y - currentY);
		if (z != currentZ)
			distance += Math.abs(currentZ - z);
		totalDistanceExtruded += distance;
		totalDistanceMoved += distance;
		
		
		
		currentX = x;
		currentY = y;
		currentZ = z;
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#selectMaterial(int)
	 */
	public void selectExtruder(int materialIndex) {
		if (isCancelled()) return;
		if(materialIndex < 0 || materialIndex >= extruderCount)
			System.err.println("Selected material (" + materialIndex + ") is out of range.");
		else
			extruder = materialIndex;
			
//		if (previewer != null)
//			previewer.setExtruder(extruders[extruder]);
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#selectMaterial(int)
	 */
	public void selectExtruder(Attributes att) {
		for(int i = 0; i < extruderCount; i++)
		{
			if(att.getMaterial().equals(extruders[i].toString()))
			{
				selectExtruder(i);
				return;
			}
		}
		System.err.println("selectExtruder() - extruder not found for: " + att.getMaterial());
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#terminate()
	 */
	public void terminate() throws IOException {
	}
	
	public void stopExtruding() {}
	
	public void stopValve() {}

	/**
	 * @return speed of the extruder
	 */
	public int getSpeed() {
		return 200;
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#getFastSpeed()
	 */
	public int getFastSpeed() {
		return getSpeed();
	}
	
	/**
	 * @return angle speedup length
	 */
	public double getAngleSpeedUpLength()
	{
		return 1;
	}
	
	/**
	 * @return angle speed factor
	 */
	public double getAngleSpeedFactor()
	{
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#setSpeed(int)
	 */
	public void setSpeed(int speed) {
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#setFastSpeed(int)
	 */
	public void setFastSpeed(int speed) {
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#getSpeedZ()
	 */
	public int getSpeedZ() {
		return 200;
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#setSpeedZ(int)
	 */
	public void setSpeedZ(int speed) {
	}

	/**
	 * @return the extruder speeds
	 */
	public int getExtruderSpeed() {
		return 200;
	}

	/**
	 * @param speed
	 */
	public void setExtruderSpeed(int speed) {
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#setPreviewer(org.reprap.gui.Previewer)
	 */
	public void setPreviewer(Previewer previewer) {
		this.previewer = previewer;
	}

	/**
	 * @param temperature
	 */
	public void setTemperature(int temperature) {
	}
	
	/**
	 * outline speed and the infill speed
	 */
	public double getOutlineSpeed()
	{
		return 1.0;
	}
	/**
	 * @return the infill speed
	 */
	public double getInfillSpeed()
	{
		return 1.0;
	}
	/* (non-Javadoc)
	 * @see org.reprap.Printer#dispose()
	 */
	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#isCancelled()
	 */
//	public boolean isCancelled() {
//		if (previewer != null)
//			return previewer.isCancelled();
//		return false;
//	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#initialise()
	 */
	public void initialise() {
		if (previewer != null)
			previewer.reset();
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#getX()
	 */
	public double getX() {
		return currentX;
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#getY()
	 */
	public double getY() {
		return currentY;
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#getZ()
	 */
	public double getZ() {
		return currentZ;
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#getTotalDistanceMoved()
	 */
	public double getTotalDistanceMoved() {
		return totalDistanceMoved;
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#getTotalDistanceExtruded()
	 */
	public double getTotalDistanceExtruded() {
		return totalDistanceExtruded;
	}

	/**
	 * @param x
	 * @param y
	 * @return segment length in millimeters
	 */
	public double segmentLength(double x, double y) {
		return Math.sqrt(x*x + y*y);
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#getExtrusionSize()
	 */
//	public double getExtrusionSize() {
//		return extrusionSize;
//	}
//
//	public double getExtrusionHeight() {
//		return extrusionHeight;
//	}
//	
//	public double getInfillWidth() {
//		return infillWidth;
//	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#setCooling(boolean)
	 */
	public void setCooling(boolean enable) {
	}
	
	/**
	 * Get the length before the end of a track to turn the extruder off
	 * to allow for the delay in the stream stopping.
	 * @return overrun in millimeters
	 */
	public double getOverRun() { return overRun; };
	
	/**
	 * Get the number of milliseconds to wait between turning an 
	 * extruder on and starting to move it.
	 * @return delay in milliseconds
	 */
	public long getDelay() { return delay; };

	/* (non-Javadoc)
	 * @see org.reprap.Printer#getTotalElapsedTime()
	 */
	public double getTotalElapsedTime() {
		long now = System.currentTimeMillis();
		return (now - startTime) / 1000.0;
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#printStartDelay(boolean)
	 */
	public void printStartDelay(boolean firstInLayer) {
		// This would extrude for the given interval to ensure polymer flow.
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#setLowerShell(javax.media.j3d.Shape3D)
	 */
	public void setLowerShell(BranchGroup ls)
	{
		if(previewer != null)
			previewer.setLowerShell(ls);
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#setZManual()
	 */
	public void setZManual() {
		setZManual(0.0);
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#setZManual(double)
	 */
	public void setZManual(double zeroPoint) {
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#homeToZeroX()
	 */
	public void homeToZeroX() throws ReprapException, IOException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#homeToZeroY()
	 */
	public void homeToZeroY() throws ReprapException, IOException {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#getExtruder()
	 */
	public Extruder getExtruder()
	{
		return extruders[extruder];
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#getExtruder(String)
	 */
	public Extruder getExtruder(String name)
	{
		for(int i = 0; i < extruderCount; i++)
			if(name.equals(extruders[i].toString()))
				return extruders[i];
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#getExtruder(String)
	 */
	public Extruder[] getExtruders()
	{
		return extruders;
	}
	
//	/**
//	 * Moves nozzle back and forth over wiper
//	 */
//	public void wipeNozzle() throws ReprapException, IOException {
//	}
	
	/**
	 * Just finished a layer
	 * @param layerNumber
	 */
	public void finishedLayer(int layerNumber) throws Exception
	{

	}
	
	/**
	 * Deals with all the actions that need to be done between one layer
	 * and the next.
	 */
	public void betweenLayers(int layerNumber) throws Exception
	{

	}
	
	/**
	 * Just about to start the next layer
	 * @param layerNumber
	 */
	public void startingLayer(int layerNumber) throws Exception
	{
		if (layerPauseCheckbox != null && layerPauseCheckbox.isSelected())
			layerPause();
	}
	/**
	 * Display a message indicating a segment is about to be
	 * printed and wait for the user to acknowledge
	 */
	private void segmentPause() {
		ContinuationMesage msg =
			new ContinuationMesage(null, "A new segment is about to be produced");
					//,segmentPauseCheckbox, layerPauseCheckbox);
		msg.setVisible(true);
		try {
			synchronized(msg) {
				msg.wait();
			}
		} catch (Exception ex) {
		}
		if (msg.getResult() == false)
			setCancelled(true);
		msg.dispose();
	}

	/**
	 * Display a message indicating a layer is about to be
	 * printed and wait for the user to acknowledge
	 */
	private void layerPause() {
		ContinuationMesage msg =
			new ContinuationMesage(null, "A new layer is about to be produced");
					//,segmentPauseCheckbox, layerPauseCheckbox);
		msg.setVisible(true);
		try {
			synchronized(msg) {
				msg.wait();
			}
		} catch (Exception ex) {
		}
		if (msg.getResult() == false)
			setCancelled(true);
		msg.dispose();
	}

	/**
	 * Set the source checkbox used to determine if there should
	 * be a pause between segments.
	 * 
	 * @param segmentPause The source checkbox used to determine
	 * if there should be a pause.  This is a checkbox rather than
	 * a boolean so it can be changed on the fly. 
	 */
	public void setSegmentPause(JCheckBoxMenuItem segmentPause) {
		segmentPauseCheckbox = segmentPause;
	}

	/**
	 * Set the source checkbox used to determine if there should
	 * be a pause between layers.
	 * 
	 * @param layerPause The source checkbox used to determine
	 * if there should be a pause.  This is a checkbox rather than
	 * a boolean so it can be changed on the fly.
	 */
	public void setLayerPause(JCheckBoxMenuItem layerPause) {
		layerPauseCheckbox = layerPause;
	}

	public void setMessage(String message) {
		if (message == null)
			statusWindow.setVisible(false);
		else {
			statusWindow.setMessage(message);
			statusWindow.setVisible(true);
		}
	}
	
	public boolean isCancelled() {
		return statusWindow.isCancelled();
	}

	public void setCancelled(boolean isCancelled) {
		statusWindow.setCancelled(isCancelled);
	}

}
