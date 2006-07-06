package org.reprap.geometry;


import org.reprap.Preferences;
import org.reprap.Printer;
import org.reprap.geometry.polygons.*;
import org.reprap.gui.PreviewPanel;
import org.reprap.gui.RepRapBuild;
import org.reprap.machines.MachineFactory;

public class Producer {
	
	protected Printer reprap;
	protected RrLine oddHatchDirection;
	protected RrLine evenHatchDirection;
	protected RepRapBuild bld;
	
	public Producer(PreviewPanel preview, RepRapBuild builder) throws Exception {
		reprap = MachineFactory.create();
		reprap.setPreviewer(preview);
		bld = builder;
		oddHatchDirection = new RrLine(new Rr2Point(0.0, 0.0), new Rr2Point(1.0, 1.0));
		evenHatchDirection = new RrLine(new Rr2Point(0.0, 1.0), new Rr2Point(1.0, 0.0));
	}
	
	public RrPolygon square()
	{
		RrPolygon a = new RrPolygon();
		Rr2Point p1 = new Rr2Point(10, 10);
		Rr2Point p2 = new Rr2Point(20, 10);
		Rr2Point p3 = new Rr2Point(20, 20);
		Rr2Point p4 = new Rr2Point(10, 20);
		a.add(p1, 1);
		a.add(p2, 1);
		a.add(p3, 1);
		a.add(p4, 1);
		return a;
	}
	
	public RrCSGPolygon hex()
	{
		double hexSize = 20;
		double hexX = 15, hexY = 15;
		
		RrCSG r = RrCSG.universe();
		Rr2Point pold = new Rr2Point(hexX + hexSize/2, hexY);
		Rr2Point p;
		double theta = 0; 
		for(int i = 0; i < 6; i++)
		{
			theta += Math.PI * 60. / 180.0;
			p = new Rr2Point(hexX + Math.cos(theta)*hexSize/2, hexY + Math.sin(theta)*hexSize/2);
			r = RrCSG.intersection(r, new RrCSG(new RrHalfPlane(p, pold)));
			pold = p;
		}
		
		// Horrid hacks in multipliers next...
		return new RrCSGPolygon(r, new RrBox(new Rr2Point(hexX - hexSize*0.57, hexY - hexSize*0.61), 
				new Rr2Point(hexX + hexSize*0.537, hexY + hexSize*0.623)));
	}
	
	public RrCSGPolygon adriansTestShape()
	{
		Rr2Point p = new Rr2Point(3, 5);
		Rr2Point q = new Rr2Point(7, 27);
		Rr2Point r = new Rr2Point(32, 30);
		Rr2Point s = new Rr2Point(31, 1);
		
		Rr2Point pp = new Rr2Point(12, 21);
		Rr2Point qq = new Rr2Point(18, 32);
		Rr2Point rr = new Rr2Point(15, 17);    
		
		RrHalfPlane ph = new RrHalfPlane(p, q);
		RrHalfPlane qh = new RrHalfPlane(q, r);
		RrHalfPlane rh = new RrHalfPlane(r, s);
		RrHalfPlane sh = new RrHalfPlane(s, p);
		
		RrHalfPlane pph = new RrHalfPlane(pp, qq);
		RrHalfPlane qqh = new RrHalfPlane(qq, rr);
		RrHalfPlane rrh = new RrHalfPlane(rr, pp);
		
		RrCSG pc = new RrCSG(ph);
		RrCSG qc = new RrCSG(qh);
		RrCSG rc = new RrCSG(rh);
		RrCSG sc = new RrCSG(sh);
		
		pc = RrCSG.intersection(pc, qc);
		rc = RrCSG.intersection(sc, rc);		
		pc = RrCSG.intersection(pc, rc);
		
		RrCSG ppc = new RrCSG(pph);
		RrCSG qqc = new RrCSG(qqh);
		RrCSG rrc = new RrCSG(rrh);
		
		ppc = RrCSG.intersection(ppc, qqc);
		ppc = RrCSG.intersection(ppc, rrc);
		ppc = RrCSG.difference(pc, ppc);
		
		pc = ppc.offset(-5);
		ppc = RrCSG.difference(ppc, pc);
		
		RrCSGPolygon result = new RrCSGPolygon(ppc, new 
				RrBox(new Rr2Point(0,0), new Rr2Point(110,110)));
//		result.divide(1.0e-6, 1);
//		new RrGraphics(result, true);
		return result;
	}
	
	public void produce(boolean testPiece) throws Exception {

        // Fallback defaults
		int extrusionSpeed = 200;
		int extrusionTemp = 40;
		int movementSpeedXY = 230;
		int movementSpeedZ = 230;
		
		int coolingPeriod = Preferences.loadGlobalInt("CoolingPeriod"); 
		
		try {
			extrusionSpeed = Preferences.loadGlobalInt("ExtrusionSpeed");
			extrusionTemp = Preferences.loadGlobalInt("ExtrusionTemp");
			movementSpeedXY = Preferences.loadGlobalInt("MovementSpeed");
			movementSpeedZ = Preferences.loadGlobalInt("MovementSpeedZ");
		} catch (Exception ex) {
			System.out.println("Warning: could not load ExtrusionSpeed/MovementSpeed, using defaults");
		}
		
		System.out.println("Setting temperature and speed");
		reprap.setTemperature(extrusionTemp);
		reprap.setSpeed(movementSpeedXY);
		reprap.setSpeedZ(movementSpeedZ);
		System.out.println("Intialising reprap");
		reprap.initialise();
		System.out.println("Selecting material");
		reprap.selectMaterial(0);
		reprap.setExtruderSpeed(extrusionSpeed);

		// A "warmup" segment to get things in working order
		System.out.println("Printing warmup segments, moving to (0,5)");
		reprap.moveTo(0, 5, 0);
		System.out.println("Printing warmup segments, printing to (0,20)");
		reprap.printTo(0, 20, 0);
		System.out.println("Printing warmup segments, printing to (2,20)");
		reprap.printTo(2, 20, 0);
		System.out.println("Printing warmup segments, printing to (2,5)");
		reprap.printTo(2, 5, 0);
		
		// This should now split off layers one at a time
		// and pass them to the LayerProducer.  
		
		boolean isEvenLayer = true;
		STLSlice stlc;
		double zMax;
		if(testPiece)
		{
			stlc = null;
			zMax = 5;
		} else
		{
			bld.mouseToWorld();
			stlc = new STLSlice(bld.getSTLs());
			zMax = stlc.maxZ();
			//zMax = 1.6;
		}
		
		
		for(double z = 0; z < zMax; z += reprap.getExtrusionHeight()) {
			
			if (reprap.isCancelled())
				break;
			System.out.println("Commencing layer at " + z);

			// Change Z height
			reprap.moveTo(reprap.getX(), reprap.getY(), z);
			
			if (reprap.isCancelled())
				break;
			RrPolygonList list = new RrPolygonList();

// ************ Simon's example start
			
//			// Add a square block
//
//			list.add(square());
//	
//			// Add a hex block
//			
//			list.add(hex());
//
//			LayerProducer layer = new LayerProducer(reprap, list,
//					isEvenLayer?evenHatchDirection:oddHatchDirection);
			
// ************ Simon's examples end - Adrian's start
			LayerProducer layer;
			if(testPiece)
			{
				layer = new LayerProducer(reprap, hex(),
						isEvenLayer?evenHatchDirection:oddHatchDirection);
			} else
			{
				RrCSGPolygon slice = stlc.slice(z+reprap.getExtrusionHeight()*0.5);
				layer = new LayerProducer(reprap, slice,
						isEvenLayer?evenHatchDirection:oddHatchDirection);
//				slice.divide(1.0e-2, 1);
//				new RrGraphics(slice, true);
			}

// ************ Adrian's example end.
			
			layer.plot();
		
			// Layer cooling phase
			if (coolingPeriod > 0) {
				System.out.println("Starting a cooling period");
				reprap.setCooling(true);
				Thread.sleep(1000 * coolingPeriod);
				reprap.setCooling(false);
				System.out.println("End of cooling period");
			}
			
			isEvenLayer = !isEvenLayer;
		}
		
		reprap.moveTo(0, 0, reprap.getZ());
		
		reprap.terminate();

	}

	public double getTotalDistanceMoved() {
		return reprap.getTotalDistanceMoved();
	}
	
	public double getTotalDistanceExtruded() {
		return reprap.getTotalDistanceExtruded();
	}
	
	public double getTotalVolumeExtruded() {
		return reprap.getTotalDistanceExtruded() * reprap.getExtrusionHeight() * reprap.getExtrusionSize();
	}
	
	public void dispose() {
		reprap.dispose();
	}
	
}
