package org.reprap.machines;

import java.io.IOException;
import java.util.Properties;

import org.reprap.CartesianPrinter;
import org.reprap.ReprapException;
import org.reprap.comms.Communicator;
import org.reprap.comms.snap.SNAPAddress;
import org.reprap.comms.snap.SNAPCommunicator;
import org.reprap.devices.GenericExtruder;
import org.reprap.devices.GenericStepperMotor;
import org.reprap.devices.pseudo.LinePrinter;
import org.reprap.gui.Previewer;

/**
 * 
 * A Reprap printer is a 3-D cartesian printer with one or more
 * extruders
 *
 */
public class Reprap implements CartesianPrinter {
	private final int localNodeNumber = 0;
	private final int baudRate = 19200;

	private Communicator communicator;
	private Previewer previewer = null;

	private GenericStepperMotor motorX;
	private GenericStepperMotor motorY;
	private GenericStepperMotor motorZ;

	double totalDistanceMoved = 0.0;
	double totalDistanceExtruded = 0.0;

	private LinePrinter layer;
	
	double scaleX, scaleY, scaleZ;
	
	double currentX, currentY, currentZ;
	
	double offsetX, offsetY, offsetZ;
	
	
	private int speed = 236;  			// Initial default speed
	private int speedExtruder = 200;    // Initial default extruder speed
	
	private double extrusionSize;
	private double extrusionHeight;
	
	private GenericExtruder extruder;  ///< Only one supported for now

	private boolean excludeZ = false;  ///< Don't perform Z operations.  Should be removed later.
	
	public Reprap(Properties config) throws Exception {
		int axes = Integer.parseInt(config.getProperty("AxisCount"));
		if (axes != 3)
			throw new Exception("A Reprap printer must contain 3 axes");
		int extruders = Integer.parseInt(config.getProperty("ExtruderCount"));
		if (extruders < 1)
			throw new Exception("A Reprap printer must contain at least one extruder");
		
		offsetX = offsetY = offsetZ = 0.0;
		
		String commPortName = config.getProperty("Port");
		
		SNAPAddress myAddress = new SNAPAddress(localNodeNumber); 
		communicator = new SNAPCommunicator(commPortName, baudRate, myAddress);
		
		motorX = new GenericStepperMotor(communicator,
				new SNAPAddress(config.getProperty("Axis1Address")),
				Integer.parseInt(config.getProperty("Axis1Torque")));
		motorY = new GenericStepperMotor(communicator,
				new SNAPAddress(config.getProperty("Axis2Address")),
				Integer.parseInt(config.getProperty("Axis2Torque")));
		motorZ = new GenericStepperMotor(communicator,
				new SNAPAddress(config.getProperty("Axis3Address")),
				Integer.parseInt(config.getProperty("Axis3Torque")));
		
		extruder = new GenericExtruder(communicator,
				new SNAPAddress(config.getProperty("Extruder1Address")),
				Double.parseDouble(config.getProperty("Extruder1Beta")),
				Double.parseDouble(config.getProperty("Extruder1Rz")),
				Double.parseDouble(config.getProperty("Extruder1hm")),
				Double.parseDouble(config.getProperty("Extruder1hb")),
				Integer.parseInt(config.getProperty("Extruder1MaxSpeed"))
				);

		try {
			extrusionSize = Double.parseDouble(config.getProperty("ExtrusionSize"));
			extrusionHeight = Double.parseDouble(config.getProperty("ExtrusionHeight"));
		} catch (Exception ex) {
			extrusionSize = extrusionHeight = 1.0;
		}
		
		layer = new LinePrinter(motorX, motorY, extruder);

		// TODO This should be from calibration
		try {
			scaleX = Double.parseDouble(config.getProperty("Axis1Scale"));
			scaleY = Double.parseDouble(config.getProperty("Axis2Scale"));
			scaleZ = Double.parseDouble(config.getProperty("Axis3Scale"));
		} catch (Exception ex) {
			System.out.println("Warning: axis scaling not loaded, reverting to defaults");
			// Assume 400 steps per turn, 1.5mm travel per turn
			scaleX = scaleY = scaleZ = 400.0 / 1.5;
		}

		try {
			offsetX = Double.parseDouble(config.getProperty("Extruder1OffsetX"));
			offsetY = Double.parseDouble(config.getProperty("Extruder1OffsetY"));
			offsetZ = Double.parseDouble(config.getProperty("Extruder1OffsetZ"));
		} catch (Exception ex) {
		}
		
		try {
			currentX = convertToPositionZ(motorX.getPosition());
			currentY = convertToPositionZ(motorY.getPosition());
		} catch (Exception ex) {
			throw new Exception("Warning: X and/or Y controller not responding, cannot continue");
		}
		try {
			currentZ = convertToPositionZ(motorZ.getPosition());
		} catch (Exception ex) {
			System.out.println("Z axis not responding and will be ignored");
			excludeZ = true;
		}
		
	}
	
	public void calibrate() {
	}

	public void printSegment(double startX, double startY, double startZ, double endX, double endY, double endZ) throws ReprapException, IOException {
		moveTo(startX, startY, startZ);
		printTo(endX, endY, endZ);
	}

	public void moveTo(double x, double y, double z) throws ReprapException, IOException {
		if (isCancelled()) return;

		layer.moveTo(convertToStepX(x), convertToStepY(y), speed);
		totalDistanceMoved += segmentLength(x - currentX, y - currentY);

		if (z != currentZ) {
			totalDistanceMoved += Math.abs(currentZ - z);
			if (!excludeZ) motorZ.seekBlocking(speed, convertToStepZ(z));
		}
		currentX = x;
		currentY = y;
		currentZ = z;
	}

	public void printTo(double x, double y, double z) throws ReprapException, IOException {
		if (isCancelled()) return;
		EnsureNotEmpty();
		if (isCancelled()) return;
		EnsureHot();
		if (isCancelled()) return;

		if ((x != convertToPositionX(layer.getCurrentX()) || y != convertToPositionY(layer.getCurrentY())) && z != currentZ)
			throw new ReprapException("Reprap cannot print a line across 3 axes simultaneously");

		if (previewer != null)
			previewer.addSegment(convertToPositionX(layer.getCurrentX()),
					convertToPositionY(layer.getCurrentY()), currentZ,
					x, y, z);

		if (isCancelled()) return;
		
		
		if (x == convertToPositionX(layer.getCurrentX()) && y == convertToPositionY(layer.getCurrentY()) && z != currentZ) {
			// Print a simple vertical extrusion
			// TODO extrusion speed should be based on actual head speed
			// which depends on the angle of the line
			double distance = Math.abs(currentZ - z);
			totalDistanceExtruded += distance;
			totalDistanceMoved += distance;
			extruder.setExtrusion(speedExtruder);
			if (!excludeZ) motorZ.seekBlocking(speed, convertToStepZ(z));
			extruder.setExtrusion(0);
			currentZ = z;
			return;
		}

		// Otherwise printing only in X/Y plane
		double distance = segmentLength(x - currentX, y - currentY);
		totalDistanceExtruded += distance;
		totalDistanceMoved += distance;
		layer.printTo(convertToStepX(x), convertToStepY(y), speed, speedExtruder);
		currentX = x;
		currentY = y;
	}

	public void selectMaterial(int materialIndex) {
		if (isCancelled()) return;

		if (previewer != null)
			previewer.setMaterial(materialIndex, extrusionSize, extrusionHeight);

		if (isCancelled()) return;
		// TODO Select new material
		// TODO Load new x/y/z offsets for the new extruder
	}

	protected int convertToStepX(double n) {
		return (int)((n + offsetX) * scaleX);
	}

	protected int convertToStepY(double n) {
		return (int)((n + offsetY) * scaleY);
	}

	protected int convertToStepZ(double n) {
		return (int)((n + offsetZ) * scaleZ);
	}

	protected double convertToPositionX(int n) {
		return n / scaleX - offsetX;
	}

	protected double convertToPositionY(int n) {
		return n / scaleY - offsetY;
	}

	protected double convertToPositionZ(int n) {
		return n / scaleZ - offsetZ;
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#terminate()
	 */
	public void terminate() throws Exception {
		motorX.setIdle();
		motorY.setIdle();
		motorX.setIdle();
		extruder.setExtrusion(0);
		extruder.setTemperature(0);
	}
	
	public void dispose() {
		motorX.dispose();
		motorY.dispose();
		motorZ.dispose();
		extruder.dispose();
		communicator.close();
		communicator.dispose();
	}

	/**
	 * @return Returns the speed.
	 */
	public int getSpeed() {
		return speed;
	}
	/**
	 * @param speed The speed to set.
	 */
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	/**
	 * @return Returns the speedExtruder.
	 */
	public int getExtruderSpeed() {
		return speedExtruder;
	}
	/**
	 * @param speedExtruder The speedExtruder to set.
	 */
	public void setExtruderSpeed(int speedExtruder) {
		this.speedExtruder = speedExtruder;
	}
	
	public void setPreviewer(Previewer previewer) {
		this.previewer = previewer;
	}

	public void setTemperature(int temperature) throws Exception {
		extruder.setTemperature(temperature);
	}

	private void EnsureNotEmpty() {
		if (!extruder.isEmpty()) return;
		
		while (extruder.isEmpty() && !isCancelled()) {
			if (previewer != null)
				previewer.setMessage("Extruder is out of feedstock.  Waiting for refill.");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		if (previewer != null) previewer.setMessage(null);
	}
	
	private void EnsureHot() {
		double threshold = extruder.getTemperatureTarget() * 0.95;
		
		if (extruder.getTemperature() >= threshold)
			return;

		while(extruder.getTemperature() < threshold && !isCancelled()) {
			if (previewer != null) previewer.setMessage("Waiting for extruder to reach working temperature (" + Math.round(extruder.getTemperature()) + ")");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		if (previewer != null) previewer.setMessage(null);
		
	}

	public boolean isCancelled() {
		if (previewer == null)
			return false;
		return previewer.isCancelled();
	}
	
	public void initialise() {
		if (previewer != null)
			previewer.reset();
	}

	public double getX() {
		return currentX;
	}

	public double getY() {
		return currentY;
	}

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
	
	public double segmentLength(double x, double y) {
		return Math.sqrt(x*x + y*y);
	}
	
	public double getExtrusionSize() {
		return extrusionSize;
	}

	public double getExtrusionHeight() {
		return extrusionHeight;
	}
}


