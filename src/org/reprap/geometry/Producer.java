package org.reprap.geometry;

import org.reprap.Printer;
import org.reprap.geometry.polygons.Rr2Point;
import org.reprap.geometry.polygons.RrLine;
import org.reprap.geometry.polygons.RrPolygon;
import org.reprap.geometry.polygons.RrPolygonList;
import org.reprap.gui.PreviewPanel;
import org.reprap.gui.RepRapBuild;
import org.reprap.machines.MachineFactory;

public class Producer {

	private Printer reprap;
	private RrLine hatchDirection;
	
	public Producer(PreviewPanel preview, RepRapBuild builder) throws Exception {
		reprap = MachineFactory.create();
		reprap.setPreviewer(preview);
		
		hatchDirection = new RrLine(new Rr2Point(0.0, 0.0), new Rr2Point(1.0, 1.0));
	}
	
	public void produce() throws Exception {
	
		reprap.initialise();
		reprap.selectMaterial(0);
		reprap.setSpeed(230);
		reprap.setExtruderSpeed(180);
		reprap.setTemperature(40);

		// This should now split off layers one at a time
		// and pass them to the LayerProducer.  At the moment,
		// we just construct a simple layer and produce that.
		
		Rr2Point p1 = new Rr2Point(10, 10);
		Rr2Point p2 = new Rr2Point(20, 10);
		Rr2Point p3 = new Rr2Point(20, 20);
		Rr2Point p4 = new Rr2Point(10, 20);

		RrPolygon a = new RrPolygon();
		a.append(p1, 1);
		a.append(p2, 1);
		a.append(p3, 1);
		a.append(p4, 1);

		RrPolygonList list = new RrPolygonList();
		list.append(a);

		LayerProducer layer = new LayerProducer(reprap, list, hatchDirection);
		layer.plot();
		//if (!reprap.isCancelled()) reprap.moveTo(20, 5, 0);
		//if (!reprap.isCancelled()) reprap.printTo(15, 10, 0);
		//if (!reprap.isCancelled()) reprap.printTo(20, 15, 0);
		//if (!reprap.isCancelled()) reprap.printTo(25, 10, 0);
		//if (!reprap.isCancelled()) reprap.printTo(20, 5, 0);
		
		reprap.terminate();

	}
	
	public void dispose() {
		reprap.dispose();
	}
	
}
