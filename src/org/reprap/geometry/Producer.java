package org.reprap.geometry;

import java.net.URL;
import java.util.Properties;

import org.reprap.Printer;
import org.reprap.geometry.polygons.Rr2Point;
import org.reprap.geometry.polygons.RrLine;
import org.reprap.geometry.polygons.RrPolygon;
import org.reprap.geometry.polygons.RrPolygonList;
import org.reprap.gui.PreviewPanel;
import org.reprap.gui.RepRapBuild;
import org.reprap.machines.MachineFactory;

public class Producer {
	
	protected Printer reprap;
	protected RrLine oddHatchDirection;
	protected RrLine evenHatchDirection;
	
	public Producer(PreviewPanel preview, RepRapBuild builder) throws Exception {
		reprap = MachineFactory.create();
		reprap.setPreviewer(preview);
		
		oddHatchDirection = new RrLine(new Rr2Point(0.0, 0.0), new Rr2Point(1.0, 1.0));
		evenHatchDirection = new RrLine(new Rr2Point(0.0, 1.0), new Rr2Point(1.0, 0.0));
	}
	
	public void produce() throws Exception {

        // Fallback defaults
		int extrusionSpeed = 200;
		int extrusionTemp = 40;
		int movementSpeed = 230;
		
		try {
			Properties props = new Properties();
			URL url = ClassLoader.getSystemResource("reprap.properties");
			props.load(url.openStream());
		
			extrusionSpeed = Integer.parseInt(props.getProperty("ExtrusionSpeed"));
			extrusionTemp = Integer.parseInt(props.getProperty("ExtrusionTemp"));
			movementSpeed = Integer.parseInt(props.getProperty("MovementSpeed"));
		} catch (Exception ex) {
			System.out.println("Warning: could not load ExtrusionSpeed/MovementSpeed, using defaults");
		}

		
		reprap.initialise();
		reprap.selectMaterial(0);
		reprap.setSpeed(movementSpeed);
		reprap.setExtruderSpeed(extrusionSpeed);
		reprap.setTemperature(extrusionTemp);

		// This should now split off layers one at a time
		// and pass them to the LayerProducer.  At the moment,
		// we just construct a simple test layer and produce that.

		boolean isEvenLayer = true;
		for(double z = 0.0; z < 5.0; z += reprap.getExtrusionHeight()) {
			reprap.moveTo(reprap.getX(), reprap.getY(), z);
			
			if (reprap.isCancelled())
				break;
			RrPolygonList list = new RrPolygonList();

			// Add a square block
			RrPolygon a = new RrPolygon();
			Rr2Point p1 = new Rr2Point(10, 10);
			Rr2Point p2 = new Rr2Point(20, 10);
			Rr2Point p3 = new Rr2Point(20, 20);
			Rr2Point p4 = new Rr2Point(10, 20);
			a.append(p1, 1);
			a.append(p2, 1);
			a.append(p3, 1);
			a.append(p4, 1);
			list.append(a);
	
			// Add a hex block
			RrPolygon b = new RrPolygon();
			double hexSize = 10;
			double hexLongSize = Math.cos(Math.PI * 30. / 180.0);
			double hexShortSize = Math.sin(Math.PI * 30. / 180.0);
			double hexX = 35, hexY = 15;
			Rr2Point h1 = new Rr2Point(hexX - hexSize / 2.0, hexY - hexSize * hexLongSize);
			Rr2Point h2 = new Rr2Point(hexX - hexSize / 2.0 - hexSize * hexShortSize, hexY);
			Rr2Point h3 = new Rr2Point(hexX - hexSize / 2.0, hexY + hexSize * hexLongSize);
			Rr2Point h4 = new Rr2Point(hexX + hexSize / 2.0, hexY + hexSize * hexLongSize);
			Rr2Point h5 = new Rr2Point(hexX + hexSize / 2.0 + hexSize * hexShortSize, hexY);
			Rr2Point h6 = new Rr2Point(hexX + hexSize / 2.0, hexY - hexSize * hexLongSize);
			b.append(h1, 1);
			b.append(h2, 1);
			b.append(h3, 1);
			b.append(h4, 1);
			b.append(h5, 1);
			b.append(h6, 1);
			list.append(b);
	
			LayerProducer layer = new LayerProducer(reprap, list,
					isEvenLayer?evenHatchDirection:oddHatchDirection);
			layer.plot();
			
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
