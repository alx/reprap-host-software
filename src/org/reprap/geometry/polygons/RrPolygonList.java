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
import org.reprap.Preferences;

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
	 * Overwrite one of the polygons
	 * @param i
	 * @param p
	 */
	public void set(int i, RrPolygon p)
	{
		polygons.set(i, p);
	}
	
	/**
	 * Remove one from the list
	 * @param i
	 */
	public void remove(int i)
	{
		polygons.remove(i);
	}
	
	/**
	 * Deep copy
	 * @param lst
	 */
	public RrPolygonList(RrPolygonList lst)
	{
		polygons = new ArrayList();
		box = new RrBox(lst.box);
		for(int i = 0; i < lst.size(); i++)
			polygons.add(new RrPolygon(lst.polygon(i)));
	}
	
	/**
	 * Put a new list on the end
	 * @param lst
	 */
	public void add(RrPolygonList lst)
	{
		if(lst.size() == 0)
			return;
		for(int i = 0; i < lst.size(); i++)
			polygons.add(new RrPolygon(lst.polygon(i)));
		box.expand(lst.box);
	}
	
	/**
	 * Add one new polygon to the list
	 * @param p
	 */
	public void add(RrPolygon p)
	{
		polygons.add(p);
		box.expand(p.box);
	}
	
	/**
	 * Swap two in the list
	 * @param i
	 * @param j
	 */
	private void swap(int i, int j)
	{
		Object p = polygons.get(i);
		polygons.set(i, polygons.get(j));
		polygons.set(j, p);
	}
	
	/**
	 * Negate all the polygons
	 * @return
	 */
	public RrPolygonList negate()
	{
		RrPolygonList result = new RrPolygonList();
		for(int i = 0; i < size(); i++)
			result.polygons.add(polygon(i).negate());
		result.box = new RrBox(box);
		return result;
	}
	
	/**
	 * Negate one of the polygons (also swaps a couple of flags)
	 * @param i
	 */
	private void negate(int i)
	{
		RrPolygon p = polygon(i).negate();
		int fl = p.flag(0);
		p.flag(0, p.flag(p.size() - 1));
		p.flag(p.size() - 1, fl);
		polygons.set(i, p);
	}
	
	/**
	 * As a string
	 * @return
	 */
	public String toString()
	{
		String result = "Polygon List - polygons: ";
		result += size() + ", enclosing box: ";
		result += box.toString();
		for(int i = 0; i < size(); i++)
			result += "\n" + polygon(i).toString();
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
		double d2 = d*d;
		
		for(int i = 0; i < size(); i++)
		{
			RrPolygon p = polygon(i);
			if(p.box.d_2() > 2*d2)
				r.add(p.simplify(d));
		}
		
		return r;
	}
	
	/**
	 * Re-order and (if need be) reverse the order of the polygons
	 * in a list so the end of the first is near the start of the second and so on.
	 * This is a heuristic - it does not do a full travelling salesman...
	 * @return
	 */
	public RrPolygonList nearEnds()
	{
		RrPolygonList r = new RrPolygonList();
		if(size() <= 0)
			return r;
		
		int i;
		
		for(i = 0; i < size(); i++)
			r.add(polygon(i));
		
		int pg = 0;
		while(pg < r.size() - 1)
		{
			Rr2Point end = r.polygon(pg).point(r.polygon(pg).size() - 1);
			boolean neg = false;
			int near = -1;
			double d = Double.POSITIVE_INFINITY;
			pg++;
			for(i = pg; i < r.size(); i++)
			{
				Rr2Point e1 = r.polygon(i).point(0);
				double d2 = Rr2Point.d_2(end, e1);
				if(d2 < d)
				{
					near = i;
					d = d2;
					neg = false;
				}
				
				e1 = r.polygon(i).point(r.polygon(i).size() - 1);
				d2 = Rr2Point.d_2(end, e1);
				if(d2 < d)
				{
					near = i;
					d = d2;
					neg = true;
				}
				
			}
			
			if(near < 0)
			{
				System.err.println("RrPolygonList.nearEnds(): no nearest end found!");
				return r;
			}
			
			r.swap(pg, near);
			if(neg)
				r.negate(pg);
		}
		
		return r;
	}
	
	/**
	 * Remove edges that are shorter than tiny from the
	 *   polygons in the list if those edges are preceeded 
	 *   by gap material.  
	 * @param tiny
	 * @param flag
	 * @return
	 */
	public RrPolygonList filterShorts(double tiny)
	{
		RrPolygonList r = new RrPolygonList();
		int i;
		RrPolygon p;
		
		for(i = 0; i < size(); i++)
		{
			p = polygon(i).filterShort(tiny);
			if(p.size() > 0)
				r.add(polygon(i));
		}
		return r;
	}
	
	/**
	 * Is polygon i inside CSG polygon j?
	 * (Check twice to make sure...)
	 * @param i
	 * @param j
	 * @param csgPols
	 * @return
	 */
	private boolean inside(int i, int j, List csgPols)
	{
		RrCSG exp = (RrCSG)csgPols.get(j);
		Rr2Point p = polygon(i).point(0);
		boolean a = (exp.value(p) <= 0);
		p = polygon(i).point(polygon(i).size()/2);
		boolean b = (exp.value(p) <= 0);
		if (a != b)
			System.err.println("RrPolygonList:inside() - i is both inside and outside j!");
		return a;
	}
	
	/**
	 * Set every instance of m in the lists null, and replace m's
	 * own list with null
	 * @param m
	 * @param contains
	 */
	private void getRidOf(int m, List contains)
	{
		for(int i = 0; i < size(); i++)
		{
			if(i == m)
				contains.set(i, null);
			else
			{
				if(contains.get(i) != null)
				{
					List contain = (ArrayList)contains.get(i);
					for(int j = 0; j < contain.size(); j++)
					{
						if(contain.get(j) != null)
						{
							if(((Integer)contain.get(j)).intValue() == m)
								contain.set(j, null);
						}
					}
				}
			}
		}		
	}
	
	/**
	 * Count the non-null entries in a list.
	 * @param a
	 * @return
	 */
	private int activeCount(List a)
	{
		int count = 0;
		for(int i = 0; i < a.size(); i++)
			if(a.get(i) != null)
				count++;
		return count;
	}
	
	/**
	 * Take a list of CSG polygons, classify each as being inside other(s)
	 * (or not), and hence form a single CSG expression representing them all.
	 * @param csgPols
	 * @return
	 */
	private RrCSG resolveInsides(List csgPols)
	{
		int i, j, k, m;
		
		// For each polygon construct a list of all the others that
		// are inside it (if any).
		
		List contains = new ArrayList();
		for(i = 0; i < size(); i++)
		{
			List contain = new ArrayList();
			for(j = 0; j < size(); j++)
			{
				if(j != i)
				{
					if(inside(j, i, csgPols))
					{
						contain.add(new Integer(j));
					}
				}
			}
			contains.add(contain);
		}
		
		// Starting with polygons that just contain one other, take the difference.
		// Then go on to a contents of two, and so on.
		// Remove any polygon that has been subtracted from further consideration.
		
		int leng = 0;
		boolean notFinished = true;
		while(notFinished)
		{
			notFinished = false;
			leng++;
			for(i = 0; i < size(); i++)
			{
				if(contains.get(i) != null)
				{
					List contain = (ArrayList)contains.get(i);
					int ct = activeCount(contain);
					if(ct >= leng)
						notFinished = true;
					if(ct == leng)
					{
						RrCSG base = (RrCSG)csgPols.get(i);
						for(k = 0; k < contain.size(); k++)
						{
							if(contain.get(k) != null)
							{
								m = ((Integer)contain.get(k)).intValue();
								base = RrCSG.difference(base, (RrCSG)csgPols.get(m));
								getRidOf(m, contains);
							}
						}
						csgPols.set(i, base);
					}
				}
			}
		}
		
		// Union what's left
		
		RrCSG result = RrCSG.nothing();
		for(i = 0; i < size(); i++)
		{
			if(contains.get(i) != null)
					result = RrCSG.union(result, (RrCSG)csgPols.get(i));
		}	
		return result;	
	}
	
	/**
	 * Compute the CSG representation of all the polygons in the list
	 * @return CSG representation
	 */
	public RrCSGPolygon toCSG(double tolerance)
	{		
		List csgPols = new ArrayList();
		
		for(int i = 0; i < size(); i++)
			csgPols.add(polygon(i).toCSG(tolerance).csg());
		
		RrCSG expression = resolveInsides(csgPols);
		//expression = expression.simplify(tolerance);
		RrBox b = box.scale(1.1);
		RrCSGPolygon result = new RrCSGPolygon(expression, b);
		
		return result;
	}
	
}
