/*
 
 RepRap
 ------
 
 The Replicating Rapid Prototyper Project
 
 
 Copyright (C) 2005
 Adrian Bowyer & The University of Bath
 
 http://reprap.org
 
 Principal author:
 
 Adrian Bowyer
 Department of Mechanical Engineering
 Faculty of Engineering and Design
 University of Bath
 Bath BA2 7AY
 U.K.
 
 e-mail: A.Bowyer@bath.ac.uk
 
 RepRap is free; you can redistribute it and/or
 modify it under the terms of the GNU Library General Public
 Licence as published by the Free Software Foundation; either
 version 2 of the Licence, or (at your option) any later version.
 
 RepRap is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Library General Public Licence for more details.
 
 For this purpose the words "software" and "library" in the GNU Library
 General Public Licence are taken to mean any and all computer programs
 computer files data results documents and other copyright information
 available from the RepRap project.
 
 You should have received a copy of the GNU Library General Public
 Licence along with RepRap; if not, write to the Free
 Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA,
 or see
 
 http://www.gnu.org/
 
 =====================================================================
 
 
 RrPolygonList: A collection of 2D polygons
 
 First version 20 May 2005
 This version: 1 May 2006 (Now in CVS - no more comments here)
 
 */

package org.reprap.geometry.polygons;

import java.io.*;
import java.util.*;

/**
 * chPair - small class to hold double pointers for convex hull calculations.
 */
class chPair
{
	public int polygon;
	public int vertex;
	
	chPair(int p, int v)
	{
		polygon = p;
		vertex = v;
	}
}
/**
 * RrPolygonList: A collection of 2D polygons
 * 
 * List of polygons class.  This too maintains a maximum enclosing rectangle.
 * Each polygon has an associated type that can be used to record any attribute
 * of the polygon. 
 */
public class RrPolygonList
{
	public List polygons;
	public RrBox box;
	
	// Empty constructor
	
	public RrPolygonList()
	{
		polygons = new ArrayList();
		box = new RrBox();
	}
	
	// Get the data
	
	public RrPolygon polygon(int i)
	{
		return (RrPolygon)polygons.get(i);
	}
	
	public int size()
	{
		return polygons.size();
	}
	
	/**
	 * Deep copy
	 * @param lst
	 */
	public RrPolygonList(RrPolygonList lst)
	{
		polygons = new ArrayList();
		box = new RrBox(lst.box);
		int leng = lst.size();
		for(int i = 0; i < leng; i++)
			polygons.add(new RrPolygon(lst.polygon(i)));
	}
	
	/**
	 * Put a new list on the end
	 * @param lst
	 */
	public void add(RrPolygonList lst)
	{
		int leng = lst.size();
		if(leng == 0)
			return;
		for(int i = 0; i < leng; i++)
			polygons.add(new RrPolygon(lst.polygon(i)));
		box.expand(lst.box);
	}
	
	/**
	 * Add one new polygon to the list
	 * @param p
	 */
	public void add(RrPolygon p)
	{
		//add(p.no_cross());
		polygons.add(p);
		box.expand(p.box);
	}
	
	
	/**
	 * Negate all the polygons
	 * @return
	 */
	public RrPolygonList negate()
	{
		RrPolygonList result = new RrPolygonList();
		int leng = size();
		for(int i = 0; i < leng; i++)
		{
			result.polygons.add(polygon(i).negate());
		}
		result.box = new RrBox(box);
		return result;
	}
	
	/**
	 * Write as an SVG xml to file opf
	 * @param opf
	 */
	public void svg(PrintStream opf)
	{
		opf.println("<?xml version=\"1.0\" standalone=\"no\"?>");
		opf.println("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"");
		opf.println("\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
		opf.println("<svg");
		opf.println(" width=\"" + Double.toString(box.x().length()) + "mm\"");
		opf.println(" height=\""  + Double.toString(box.y().length()) +  "mm\"");
		opf.print(" viewBox=\"" + Double.toString(box.x().low()));
		opf.print(" " + Double.toString(box.y().low()));
		opf.print(" " + Double.toString(box.x().high()));
		opf.println(" " + Double.toString(box.y().high()) + "\"");
		opf.println(" xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">");
		opf.println(" <desc>RepRap polygon list - http://reprap.org</desc>");
		
		int leng = size();
		for(int i = 0; i < leng; i++)
			polygon(i).svg(opf);
		
		opf.println("</svg>");
	}
	
	/**
	 * Simplify all polygons by length d
	 * N.B. this may throw away small ones completely
	 * @param d
	 * @return
	 */
	public RrPolygonList simplify(double d)
	{
		RrPolygonList r = new RrPolygonList();
		int leng = size();
		double d2 = d*d;
		
		for(int i = 0; i < leng; i++)
		{
			RrPolygon p = polygon(i);
			if(p.box.d_2() > 2*d2)
				r.add(p.simplify(d));
		}
		
		return r;
	}
	
	
	
	// Convex hull code - this uses the QuickHull algorithm
	
	/**
	 * find a point from a list of polygon/vertex pairs
	 * @Param i
	 * @param a
	 * @return the point
	 */
	private Rr2Point listPoint(int i, List a)
	{
		chPair chp = (chPair)a.get(i);
		return polygon(chp.polygon).point(chp.vertex);
	}
	
	/**
	 * find the top (+y) point of the polygon list
	 * @return the index/polygon pair of the point
	 */
	private int topPoint(List chps)
	{
		int top = 0;
		double yMax = listPoint(top, chps).y();
		double y;

		for(int i = 1; i < chps.size(); i++)
		{
			y = listPoint(i, chps).y();
			if(y > yMax)
			{
				yMax = y;
				top = i;
			}
		}
		
		return top;
	}
	
	/**
	 * find the bottom (-y) point of the polygons
	 * @return the index in the list of the point
	 */
	private int bottomPoint(List chps)
	{
		int bot = 0;
		double yMin = listPoint(bot, chps).y();
		double y;

		for(int i = 1; i < chps.size(); i++)
		{
			y = listPoint(i, chps).y();
			if(y < yMin)
			{
				yMin = y;
				bot = i;
			}
		}
		
		return bot;
	}

	/**
	 * Put the points on a triangle in the right order
	 * @param a
	 */
	private void clockWise(List a)
	{
		if(a.size() == 3)
		{
			Rr2Point q = Rr2Point.sub(listPoint(1, a), listPoint(0, a));
			Rr2Point r = Rr2Point.sub(listPoint(2, a), listPoint(0, a));
			if(Rr2Point.op(q, r) > 0)
			{
				Object k = a.get(0);
				a.set(0, a.get(1));
				a.set(1, k);
			}
		} else
			System.err.println("clockWise(): not called for a triangle!");
	}
	
	
	/**
	 * Turn the list of hull points into a CSG convex polygon
	 * @param hullPoints
	 * @return CSG representation
	 */	
	public RrCSG toCSGHull(List hullPoints)
	{
		Rr2Point p = listPoint(hullPoints.size() - 1, hullPoints);
		Rr2Point q;
		RrCSG hull = RrCSG.universe();
		for(int i = 0; i < hullPoints.size(); i++)
		{
			q = listPoint(i, hullPoints);
			hull = RrCSG.intersection(hull, new RrCSG(new RrHalfPlane(p, q)));
			p = q;
		}

		return hull;
	}
	
	/**
	 * Turn a list of hull points into a polygon
	 * @param hullPoints
	 * @return the hull as another polygon
	 */	
	public RrPolygon toRrPolygonHull(List hullPoints)
	{
		RrPolygon hull = new RrPolygon();
		
		for(int i = 0; i < hullPoints.size(); i++)
			hull.add(listPoint(i, hullPoints), 1);

		return hull;
	}
	
	/**
	 * Remove all the points in a list that are within or on the hull
	 * @param inConsideration
	 * @param hull
	 */		
	private void outsideHull(List inConsideration, RrCSG hull)
	{
		Rr2Point p;
		int i = inConsideration.size() - 1;
		while(i >= 0)
		{
			p = listPoint(i, inConsideration);
			if(hull.value(p) <= 0)                // Need an epsilon here?
				inConsideration.remove(i);
			i--;
		}
	}
	
	/**
	 * Compute the convex hull of all the polygons in the list
	 * @return list of point index pairs of the points on the hull
	 */
	public List convexHull()
	{	
		// Initialise the points being considered to all the points
		
		List inConsideration = new ArrayList();
		for(int i = 0; i < size(); i++)
		{
			for(int j = 0; j < polygon(i).size(); j++)
				inConsideration.add(new chPair(i, j));
		}
		
		// The top-most and bottom-most points must be on the hull
		
		List result = new ArrayList();
		int t = topPoint(inConsideration);
		int b = bottomPoint(inConsideration);
		result.add(inConsideration.get(t));
		result.add(inConsideration.get(b));
		if(t > b)
		{
			inConsideration.remove(t);
			inConsideration.remove(b);
		} else
		{
			inConsideration.remove(b);
			inConsideration.remove(t);			
		}
			
		// Repeatedly add the point that's furthest from the current hull
		
		int corner, after;
		RrCSG hull;
		double v, vMax;
		while(inConsideration.size() > 0)
		{
			vMax = 0;
			corner = -1;
			after = -1;
			for(int testPoint = inConsideration.size() - 1; testPoint >= 0; testPoint--)
			{
				Rr2Point p = listPoint(result.size() - 1, result);
				Rr2Point q;
				RrHalfPlane hp;
				for(int i = 0; i < result.size(); i++)
				{
					q = listPoint(i, result);
					hp = new RrHalfPlane(p, q);
					v = hp.value(listPoint(testPoint, inConsideration));
					if(result.size() == 2)
						v = Math.abs(v);
					if(v > vMax)
					{
						after = i;
						vMax = v;
						corner = testPoint;
					}
					p = q;
				}
			}
			
			if(corner >= 0)
			{
				result.add(after, inConsideration.get(corner));
				inConsideration.remove(corner);
			} else if(inConsideration.size() > 0)
			{
				System.err.println("convexHull(): points left, but none included!");
				return result;
			}
			
			if(result.size() == 3)
				clockWise(result);

			hull = toCSGHull(result);
			outsideHull(inConsideration, hull);
		}
		
		return result;
	}
	
	
	
	/**
	 * Intersect a line with a polygon list, returning an
	 * unsorted list of the intersection parameters
	 * @param l0
	 * @return
	 */
	public List pl_intersect(RrLine l0)
	{
		int leng = size();
		List t = new ArrayList();
		
		for(int i = 0; i < leng; i++)
		{
			List t1 = polygon(i).pl_intersect(l0);
			int leng1 = t1.size();
			for(int j = 0; j < leng1; j++)
				t.add(t1.get(j));
		}
		return t;
	}
	
	
	/**
	 * Offset every polygon in the list
	 * @param d
	 * @return
	 */
	public RrPolygonList offset(double d)
	{
		int leng = size();
		RrPolygonList r = new RrPolygonList();
		for (int i = 0; i < leng; i++)
			r.add(polygon(i).offset(d));
		return r;
	}
	
	
	/**
	 * Hatch a polygon list parallel to line l0 with index gap
	 * Returning a polygon as the result with flag values f
	 * @param l0
	 * @param gap The size of the gap between hatching strokes
	 * @param fg
	 * @param fs
	 * @return
	 */
	public RrPolygon hatch(RrLine l0, double gap, int fg, int fs)
	{
		RrBox big = box.scale(1.1);
		double d = Math.sqrt(big.d_2());
		RrPolygon r = new RrPolygon();
		Rr2Point orth = new Rr2Point(-l0.direction().y(), l0.direction().x());
		orth.norm();
		
		int quad = (int)(2*Math.atan2(orth.y(), orth.x())/Math.PI);
		
		Rr2Point org;
		
		switch(quad)
		{
		case 0:
			org = big.sw();
			break;
			
		case 1:
			org = big.se();
			break;
			
		case 2:
			org = big.ne();  
			break;
			
		case 3:
			org = big.nw();
			break;
			
		default:
			System.err.println("RrPolygon hatch(): The atan2 function doesn't seem to work...");
		    org = big.sw();
		}
		
		double g = 0;

		orth = Rr2Point.mul(orth, gap);
		
		RrLine hatcher = new RrLine(org, Rr2Point.add(org, l0.direction()));
		
		while (g < d)
		{
			hatcher = hatcher.neg();
			List t_vals = pl_intersect(hatcher);
			if (t_vals.size() > 0)
			{
				java.util.Collections.sort(t_vals);
				r.add(RrPolygon.rr_t_polygon(t_vals, hatcher, fg, fs));
			}
			hatcher = hatcher.add(orth);
			g = g + gap;
		}
		r.flags.set(0, new Integer(0));
		return r;
	}
	
	
}
