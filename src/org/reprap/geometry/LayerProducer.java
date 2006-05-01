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
import org.reprap.geometry.polygons.RrGraphics;
import org.reprap.geometry.polygons.RrInterval;
import org.reprap.geometry.polygons.RrLine;
import org.reprap.geometry.polygons.RrPolygon;
import org.reprap.geometry.polygons.RrPolygonList;

public class LayerProducer {
	private static final double extrusionWidth = 0.3;  ///< Extrusion thickness in millimeters
	private static int gapMaterial = 0;
	private static int solidMaterial = 1;
	

	private Printer printer;
	private RrPolygonList p_list;
	
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

		RrPolygon hatched = list.hatch(hatchDirection, extrusionWidth,
				gapMaterial, solidMaterial);

		p_list = new RrPolygonList();
		p_list.append(hatched);
		new RrGraphics(p_list, true);
		
		csg_p = null;
		
		RrBox big = p_list.box.scale(1.1);
		
		double width = big.x().length();
		double height = big.y().length();
	}
	
	private void plot(Rr2Point a) throws ReprapException, IOException
	{
		printer.printTo(a.x(), a.y(), printer.getZ());
		pos = a;
	}

	private void move(Rr2Point a) throws ReprapException, IOException
	{
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
				plot(p.point(i));
			} else
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
		move(a.point(i.low()));
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
			
			plot(ln1, range1);
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
			plot(p.csg(), p.box());
		} else
		{
			plot(p.c_1());
			plot(p.c_2());
			plot(p.c_3());
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
		if(p_list == null)
			plot(csg_p);
		else
		{
			int leng = p_list.size();
			for(int i = 0; i < leng; i++)
				plot(p_list.polygon(i));
		}
	}
	
}
