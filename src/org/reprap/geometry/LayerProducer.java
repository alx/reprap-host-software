/*
 * Created on May 1, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.reprap.geometry;

import java.io.IOException;

import org.reprap.Printer;
import org.reprap.ReprapException;
import org.reprap.geometry.polygons.Rr2Point;
import org.reprap.geometry.polygons.RrBox;
import org.reprap.geometry.polygons.RrCSG;
import org.reprap.geometry.polygons.RrCSGOp;
import org.reprap.geometry.polygons.RrCSGPolygon;
import org.reprap.geometry.polygons.RrInterval;
import org.reprap.geometry.polygons.RrLine;
import org.reprap.geometry.polygons.RrPolygon;
import org.reprap.geometry.polygons.RrPolygonList;

public class LayerProducer {
	private static final double extrusionWidth = 0.3;  ///< Extrusion thickness in millimeters
	private static int gapMaterial = 0;
	private static int solidMaterial = 1;
	

	private Printer printer;
	private RrPolygonList hatchedPolygons;
	private RrPolygonList borderPolygons;
	
	private RrCSGPolygon csg_p;
	private double scale;
	private Rr2Point p_0;
	private Rr2Point pos;
	
	
	/**
	 * @param reprap
	 * @param list
	 * @param hatchDirection
	 */
	public LayerProducer(Printer printer, RrPolygonList list, RrLine hatchDirection) {
		this.printer = printer;

		borderPolygons = list;
		
		RrPolygon hatched = list.hatch(hatchDirection, extrusionWidth,
				gapMaterial, solidMaterial);

		hatchedPolygons = new RrPolygonList();
		hatchedPolygons.append(hatched);
		
		//new RrGraphics(p_list, false);
		
		csg_p = null;
		
		RrBox big = hatchedPolygons.box.scale(1.1);
		
		double width = big.x().length();
		double height = big.y().length();
	}
	
	private void plot(Rr2Point a) throws ReprapException, IOException
	{
		if (printer.isCancelled()) return;
		printer.printTo(a.x(), a.y(), printer.getZ());
		pos = a;
	}

	private void move(Rr2Point a) throws ReprapException, IOException
	{
		if (printer.isCancelled()) return;
		printer.moveTo(a.x(), a.y(), printer.getZ());
		pos = a;
	}


	/**
	 * Plot a polygon
	 * @throws IOException
	 * @throws ReprapException
	 */
	private void plot(RrPolygon p) throws ReprapException, IOException
	{
		int leng = p.size();
		for(int j = 0; j <= leng; j++)
		{
			int i = j%leng;
			int f = p.flag(i);
			if(f != 0 && j != 0)
			{
				if (printer.isCancelled()) return;
				plot(p.point(i));
			} else
				if (printer.isCancelled()) return;
				move(p.point(i)); 
		}
	}
	
	/**
	 * Plot a section of parametric line
	 * @throws IOException
	 * @throws ReprapException
	 */
	private void plot(RrLine a, RrInterval i) throws ReprapException, IOException
	{
		if(i.empty()) return;
		if (printer.isCancelled()) return;
		move(a.point(i.low()));
		if (printer.isCancelled()) return;
		plot(a.point(i.high()));
	}
	
	/**
	 * Plot a set in a box
	 * @throws IOException
	 * @throws ReprapException
	 */
	private void plot(RrCSG c, RrBox b) throws ReprapException, IOException
	{
		switch(c.complexity())
		{
		case 0:
			return;
			
			// One half-plane in the box
			
		case 1:
			if(c.plane() == null)
				System.err.println("plot(RrCSG, RrBox): hp not set.");
			RrLine ln = new RrLine(c.plane());
			RrInterval range = RrInterval.big_interval();
			range = b.wipe(ln, range);
			if(range.empty()) return;
			if (printer.isCancelled()) return;
			plot(ln, range);
			break;
			
			// Two - maybe a corner, or they may not intersect
			
		case 2:
			RrLine ln1 = new RrLine(c.c_1().plane());
			RrInterval range1 = RrInterval.big_interval();
			range1 = b.wipe(ln1, range1);
			RrLine ln2 = new RrLine(c.c_2().plane());
			RrInterval range2 = RrInterval.big_interval();
			range2 = b.wipe(ln2, range2);              
			if(c.operator() == RrCSGOp.INTERSECTION)
			{
				range2 = c.c_1().plane().wipe(ln2, range2);
				range1 = c.c_2().plane().wipe(ln1, range1);
			} else
			{
				range2 = c.c_1().plane().complement().wipe(ln2, range2);
				range1 = c.c_2().plane().complement().wipe(ln1, range1);                    
			}
			
			if (printer.isCancelled()) return;
			plot(ln1, range1);
			if (printer.isCancelled()) return;
			plot(ln2, range2);
			break;
			
		default:
			System.err.println("plot(RrCSG, RrBox): complexity > 2.");
		}
	}
	
	/**
	 * Plot a divided CSG polygon recursively
	 * @throws IOException
	 * @throws ReprapException
	 */
	private void plot(RrCSGPolygon p) throws ReprapException, IOException
	{
		if(p.c_1() == null)
		{
			if (printer.isCancelled()) return;
			plot(p.csg(), p.box());
		} else
		{
			if (printer.isCancelled()) return;
			plot(p.c_1());
			if (printer.isCancelled()) return;
			plot(p.c_2());
			if (printer.isCancelled()) return;
			plot(p.c_3());
			if (printer.isCancelled()) return;
			plot(p.c_4());
		}
	}
	
	/**
	 * Master plot function - draw everything
	 * @throws IOException
	 * @throws ReprapException
	 */
	public void plot() throws ReprapException, IOException
	{
		if (hatchedPolygons == null)
			plot(csg_p);
		else {
			int leng = borderPolygons.size();
			for(int i = 0; i < leng; i++) {
				plot(borderPolygons.polygon(i));
			}			
			leng = hatchedPolygons.size();
			for(int i = 0; i < leng; i++) {
				if (printer.isCancelled())
					break;
				plot(hatchedPolygons.polygon(i));
			}
		}
	}
	
}
