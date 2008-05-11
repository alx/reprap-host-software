package org.reprap.geometry;

import javax.media.j3d.*;
import javax.swing.JCheckBoxMenuItem;
import org.reprap.Preferences;
import org.reprap.Printer;
import org.reprap.geometry.polygons.*;
import org.reprap.gui.PreviewPanel;
import org.reprap.gui.RepRapBuild;
import org.reprap.machines.MachineFactory;
import org.reprap.machines.NullCartesianMachine;
import org.reprap.utilities.Debug;
import org.reprap.devices.pseudo.LinePrinter;

public class Producer {
	
	/**
	 * The machine doing the making
	 */
	protected Printer reprap;
	
	/**
	 * Line parallel to which odd-numbered layers will be hatched
	 */
	protected RrHalfPlane oddHatchDirection;
	
	/**
	 * Line parallel to which even-numbered layers will be hatched
	 */
	protected RrHalfPlane evenHatchDirection;
	
	/**
	 * The list of objects to be built
	 */
	protected RepRapBuild bld;

	
	/**
	 * @param preview
	 * @param builder
	 * @throws Exception
	 */
	public Producer(PreviewPanel preview, RepRapBuild builder) throws Exception {
		
		reprap = MachineFactory.create();
		reprap.setPreviewer(preview);
		if(preview != null)
			preview.setMachine(reprap);
		bld = builder;

		//		Original hatch vectors
		oddHatchDirection = new RrHalfPlane(new Rr2Point(0.0, 0.0), new Rr2Point(1.0, 1.0));
		evenHatchDirection = new RrHalfPlane(new Rr2Point(0.0, 1.0), new Rr2Point(1.0, 0.0));
		
//		//		Vertical hatch vector
//		oddHatchDirection = new RrHalfPlane(new Rr2Point(0.0, 0.0), new Rr2Point(0.0, 1.0));
//		evenHatchDirection = new RrHalfPlane(new Rr2Point(0.0, 1.0), new Rr2Point(0.0, 0.0));
	
//		//		Horizontal hatch vector
//		oddHatchDirection = new RrHalfPlane(new Rr2Point(0.0, 0.0), new Rr2Point(1.0, 0.0));
//		evenHatchDirection = new RrHalfPlane(new Rr2Point(1.0, 0.0), new Rr2Point(0.0, 0.0));
		

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
		reprap.setSegmentPause(segmentPause);
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
		reprap.setLayerPause(layerPause);
	}
	
	public void setCancelled(boolean c)
	{
		reprap.setCancelled(c);
	}
	
	/**
	 * @throws Exception
	 */
	public void produce() throws Exception 
	{
		int movementSpeedZ = 212;
		
		boolean subtractive = false;
		
		try {
			subtractive = Preferences.loadGlobalBool("Subtractive");
			movementSpeedZ = Preferences.loadGlobalInt("MovementSpeedZ(0..255)");
		} catch (Exception ex) {
			movementSpeedZ = 212;
			subtractive = false;
			System.err.println("Warning: could not load Z MovementSpeed and subtractive flag, using default");
		}
		

		reprap.setSpeedZ(movementSpeedZ);
		Debug.d("Intialising reprap");
		reprap.initialise();
		Debug.d("Selecting material 0");
		reprap.selectExtruder(0);
		Debug.d("Setting temperature");
		reprap.getExtruder().heatOn();
		
		// A "warmup" segment to get things in working order
		if (!subtractive) 
		{
			reprap.setSpeed(reprap.getExtruder().getXYSpeed());
			reprap.moveTo(1, 1, 0, false, false);
			
			// Workaround to get the thing to start heating up
			reprap.printTo(1, 1, 0, true);
			
			if(reprap.getExtruder().getNozzleClearTime() <= 0)
			{
				Debug.d("Printing warmup segments, moving to (1,1)");
				// Take it slow and easy.
				Debug.d("Printing warmup segments, printing to (1,60)");
				reprap.moveTo(1, 25, 0, false, false);
				reprap.setSpeed(LinePrinter.speedFix(reprap.getExtruder().getXYSpeed(), 
						reprap.getExtruder().getOutlineSpeed()));
				reprap.printTo(1, 60, 0, false);
				Debug.d("Printing warmup segments, printing to (3,60)");
				reprap.printTo(3, 60, 0, false);
				Debug.d("Printing warmup segments, printing to (3,25)");
				reprap.printTo(3, 25, 0, true);
				Debug.d("Warmup complete");
			}
			reprap.setSpeed(reprap.getFastSpeed());
			
		}
		
		// This should now split off layers one at a time
		// and pass them to the LayerProducer.  
		
		boolean isEvenLayer = true;
		STLSlice stlc;
		double zMax;

		bld.mouseToWorld();
		stlc = new STLSlice(bld.getSTLs());
		zMax = stlc.maxZ();

		double startZ;
		double endZ;
		double stepZ;
		if (subtractive) 
		{
			// Subtractive construction works from the top, downwards
			startZ = zMax;
			endZ = 0;
			stepZ = -reprap.getExtruder().getExtrusionHeight();
			reprap.setZManual(startZ);
		} else 
		{
			// Normal constructive fabrication, start at the bottom and work up.
			
			startZ = 0;
			endZ = zMax;
			
			stepZ = reprap.getExtruder().getExtrusionHeight();
		
		}
		
		int layerNumber = 0;
		
		for(double z = startZ; subtractive ? z > endZ : z < endZ; z += stepZ) {
			
			
			if (reprap.isCancelled())
				break;
			Debug.d("Commencing layer at " + z);

			// Change Z height
			reprap.moveTo(reprap.getX(), reprap.getY(), z, false, false);
			
			if (reprap.isCancelled())
				break;
			
			// Pretend we've just finished a layer first time;
			// All other times we really will have.
			
			reprap.finishedLayer(layerNumber);
			reprap.betweenLayers(layerNumber);
			
			RrCSGPolygonList slice = stlc.slice(z+reprap.getExtruder().getExtrusionHeight()*0.5); 
			BranchGroup lowerShell = stlc.getBelow();
			
			LayerProducer layer = null;
			if(slice.size() > 0)
				layer = new LayerProducer(reprap, z, slice, lowerShell,
						isEvenLayer?evenHatchDirection:oddHatchDirection, layerNumber);
			
			reprap.startingLayer(layerNumber);
			
			if (reprap.isCancelled())
				break;
			
			if(layer != null)
			{
				layer.plot();
				layer.destroy();
			}
			layer = null;
			
			slice.destroy();
			stlc.destroyLayer();

			isEvenLayer = !isEvenLayer;

			layerNumber++;
		}

		if (subtractive)
			reprap.moveTo(0, 0, startZ, true, true);
		else
			reprap.moveTo(0, 0, reprap.getZ(), true, true);
		
		reprap.terminate();

	}

	/**
	 * The total distance moved is the total distance extruded plus 
	 * plus additional movements of the extruder when no materials 
	 * was deposited
	 * 
	 * @return total distance the extruder has moved 
	 */
	public double getTotalDistanceMoved() {
		return reprap.getTotalDistanceMoved();
	}
	
	/**
	 * @return total distance that has been extruded in millimeters
	 */
	public double getTotalDistanceExtruded() {
		return reprap.getTotalDistanceExtruded();
	}
	
	/**
	 * TODO: This figure needs to get added up as we go along to allow for different extruders
	 * @return total volume that has been extruded
	 */
	public double getTotalVolumeExtruded() {
		return reprap.getTotalDistanceExtruded() * reprap.getExtruder().getExtrusionHeight() * 
		reprap.getExtruder().getExtrusionSize();
	}
	
	/**
	 * 
	 */
	public void dispose() {
		reprap.dispose();
	}

	/**
	 * @return total elapsed time in seconds between start and end of building the 3D object
	 */
	public double getTotalElapsedTime() {
		return reprap.getTotalElapsedTime();
	}
	
}
