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

import org.reprap.Attributes;
import org.reprap.Preferences;

/**
 * chPair - small class to hold double pointers for convex hull calculations.
 */
class chPair
{
	/**
	 * 
	 */
	public int polygon;
	
	/**
	 * 
	 */
	public int vertex;
	
	/**
	 * Destroy me and all I point to
	 */
	public void destroy()
	{
		// I don't point to anything
	}
	
	/**
	 * Destroy just me
	 */
	protected void finalize() throws Throwable
	{
		super.finalize();
	}
	
	/**
	 * @param p
	 * @param v
	 */
	chPair(int p, int v)
	{
		polygon = p;
		vertex = v;
	}
}

/**
 * tree - class to hold lists to build a containment tree
 * (that is a representation of which polygon is inside which,
 * like a Venn diagram).
 */
class treeList
{
	/**
	 * Index of this polygon in the list
	 */
	private int index;
	
	/**
	 * The polygons inside this one
	 */
	private List<treeList> children = null;
	
	/**
	 * The polygon that contains this one
	 */
	private treeList parent = null;
	
	/**
	 * Flag to prevent cyclic graphs going round forever
	 */
	private boolean beingDestroyed = false;
	
	/**
	 * Destroy me and all that I point to
	 */
	public void destroy() 
	{
		if(beingDestroyed) // Prevent infinite loop
			return;
		beingDestroyed = true;
		if(children != null)
		{
			for(int i = 0; i < children.size(); i++)
			{
				children.get(i).destroy();
				children.set(i, null);
			}
		}
		children = null;
		if(parent != null)
			parent.destroy();
		parent = null;	
	}
	
	/**
	 * Destroy just me
	 */
	protected void finalize() throws Throwable
	{
		children = null;
		parent = null;	
		super.finalize();
	}
	
	/**
	 * Constructor builds from a polygon index
	 * @param i
	 */
	public treeList(int i)
	{
		index = i;
		children = null;
		parent = null;
	}
	
	/**
	 * Add a polygon as a child of this one
	 * @param t
	 */
	public void addChild(treeList t)
	{
		if(children == null)
			children = new ArrayList<treeList>();
		children.add(t);
	}
	
	/**
	 * Get the ith polygon child of this one
	 * @param i
	 * @return
	 */
	public treeList getChild(int i)
	{
		if(children == null)
		{
			System.err.println("treeList: attempt to get child from null list!");
			return null;
		}
		return children.get(i);
	}
	
	/**
	 * Get the parent
	 * @return
	 */
	public treeList getParent()
	{
		return parent;
	}
	
	/**
	 * How long is the list (if any)
	 * @return
	 */
	public int size()
	{
		if(children != null)
			return children.size();
		else
			return 0;
	}
	
	/**
	 * Printable form
	 */
	public String toString()
	{
		String result;
		
		if(parent != null)
			result = Integer.toString(index) + "(^" + parent.index + "): ";
		else
			result = Integer.toString(index) + "(^null): ";
		
		for(int i = 0; i < size(); i++)
		{
			result += getChild(i).polygonIndex() + " ";
		}
		result += "\n";
		for(int i = 0; i < size(); i++)
		{
			result += getChild(i).toString();
		}
		return result;
	}
	
	
	/**
	 * Remove every instance of polygon t from the list
	 * @param t
	 */
	public void remove(treeList t)
	{		
		for(int i = size() - 1; i >= 0; i--)
		{
			if(getChild(i) == t)
			{
				children.remove(i);
			}
		}
	}
	

	/**
	 * Recursively walk the tree from here to find polygon target.
	 * @param node
	 * @param target
	 * @return
	 */
	public treeList walkFind(int target)
	{
		if(polygonIndex() == target)
			return this;
				
		for(int i = 0; i < size(); i++)
		{
			treeList result = getChild(i).walkFind(target);
			if(result != null)
				return result;
		}
		
		return null;
	}
	
	/**
	 * Walk the tree building a CSG expression to represent all
	 * the polygons as one thing.
	 * @param csgPols
	 * @return
	 */
	public RrCSG buildCSG(List<RrCSG> csgPols)
	{
		if(size() == 0)
			return csgPols.get(index);
		
		RrCSG offspring = RrCSG.nothing();
		
		for(int i = 0; i < size(); i++)
		{
			treeList iEntry = getChild(i);
			RrCSG iCSG = iEntry.buildCSG(csgPols);
			offspring = RrCSG.union(offspring, iCSG);
		}
		
		if(index < 0)
			return offspring;
		else
			return RrCSG.difference(csgPols.get(index), offspring);
	}
	
	/**
	 * Do a depth-first walk setting parents.  Any node that appears
	 * in more than one list should have the deepest possible parent 
	 * set as its parent, which is what we want.
	 * @param node
	 */
	public void setParents()
	{
		treeList child;
		int i;
		for(i = 0; i < size(); i++)
		{
			child = getChild(i);
			child.parent = this;
		}
		for(i = 0; i < size(); i++)
		{
			child = getChild(i);
			child.setParents();
		}		
	}
	
	/**
	 * get the index of the polygon
	 * @return
	 */
	public int polygonIndex()
	{
		return index;
	}
}

/**
 * RrPolygonList: A collection of 2D polygons
 * List of polygons class.  This too maintains a maximum enclosing rectangle.
 */
public class RrPolygonList
{
	/**
	 * 
	 */
	private List<RrPolygon> polygons = null;
	
	/**
	 * 
	 */
	private RrBox box = null;
	
	/**
	 * Flag to prevent cyclic graphs going round forever
	 */
	private boolean beingDestroyed = false;
	
	/**
	 * Destroy me and all that I point to
	 */
	public void destroy() 
	{
		if(beingDestroyed) // Prevent infinite loop
			return;
		beingDestroyed = true;
		if(polygons != null)
		{
			for(int i = 0; i < size(); i++)
			{
				polygons.get(i).destroy();
				polygons.set(i,null);
			}
			polygons = null;
		}
		if(box != null)
			box.destroy();
		box = null;
		beingDestroyed = false;
	}
	
	/**
	 * Destroy just me
	 */
	protected void finalize() throws Throwable
	{
		polygons = null;
		box = null;
		super.finalize();
	}
	
	/**
	 * Empty constructor
	 */
	public RrPolygonList()
	{
		polygons = new ArrayList<RrPolygon>();
		box = new RrBox();
	}
	
	/**
	 * Get the data
	 * @param i index of polygon to return
	 * @return polygon at index i
	 */
	public RrPolygon polygon(int i)
	{
		return polygons.get(i);
	}
	
	/**
	 * @return number of polygons in the list
	 */
	public int size()
	{
		return polygons.size();
	}
	
	/**
	 * @return the current enclosing box
	 */
	public RrBox getBox() { return box; }
	
	/**
	 * Overwrite one of the polygons
	 * @param i index of polygon to overwrite
	 * @param p polygon to set at index i
	 */
	public void set(int i, RrPolygon p)
	{
		polygons.set(i, p);
	}
	
	/**
	 * Remove one from the list
	 * @param i index of polygon to remove
	 */
	public void remove(int i)
	{
		polygons.remove(i);
	}
	
	/**
	 * Deep copy
	 * @param lst list of polygons to copy
	 */
	public RrPolygonList(RrPolygonList lst)
	{
		polygons = new ArrayList<RrPolygon>();
		box = new RrBox(lst.box);
		for(int i = 0; i < lst.size(); i++)
			polygons.add(new RrPolygon(lst.polygon(i)));
	}
	
	/**
	 * Put a new list on the end
	 * @param lst list to append to existing polygon list
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
	 * @param p polygon to add to the list
	 */
	public void add(RrPolygon p)
	{
		polygons.add(p);
		box.expand(p.getBox());
	}
	
	/**
	 * Swap two in the list
	 * @param i
	 * @param j
	 */
	private void swap(int i, int j)
	{
		RrPolygon p = polygons.get(i);
		polygons.set(i, polygons.get(j));
		polygons.set(j, p);
	}
	
	/**
	 * Negate all the polygons
	 * @return negated polygons
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
	 * Create a new polygon list with a random start vertex for each 
	 * polygon in the list
	 * @return new polygonlist
	 */
	public RrPolygonList randomStart()
	{
		RrPolygonList result = new RrPolygonList();
		for(int i = 0; i < size(); i++)
			result.add(polygon(i).randomStart());
		return result;
	}
	
	/**
	 * Negate one of the polygons
	 * @param i
	 */
	private void negate(int i)
	{
		RrPolygon p = polygon(i).negate();
		//int fl = p.flag(0);
		//p.flag(0, p.flag(p.size() - 1));
		//p.flag(p.size() - 1, fl);
		polygons.set(i, p);
	}
	
	/**
	 * As a string
	 * @return string representation of polygon list
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
	 * @return simplified polygon list
	 */
	public RrPolygonList simplify(double d)
	{
		RrPolygonList r = new RrPolygonList();
		double d2 = d*d;
		
		for(int i = 0; i < size(); i++)
		{
			RrPolygon p = polygon(i);
			if(p.getBox().dSquared() > 2*d2)
				r.add(p.simplify(d));
		}
		
		return r;
	}
	
	/**
	 * Re-order and (if need be) reverse the order of the polygons
	 * in a list so the end of the first is near the start of the second and so on.
	 * This is a heuristic - it does not do a full traveling salesman...
	 * @return new ordered polygon list
	 */
	public RrPolygonList nearEnds(Rr2Point startNearHere)
	{
		RrPolygonList r = new RrPolygonList();
		if(size() <= 0)
			return r;
		
		int i;
		
		for(i = 0; i < size(); i++)
			r.add(polygon(i));
		
		// Make the nearest end point on any polygon to startNearHere
		// go to polygon 0 and get it the right way round.
		
		boolean neg = false;
		double d = Double.POSITIVE_INFINITY;
		double d2;
		int near = -1;
		Rr2Point e1;
		
		if(startNearHere != null)
		{
			for(i = 0; i < size(); i++)
			{
				e1 = r.polygon(i).point(0);
				d2 = Rr2Point.dSquared(startNearHere, e1);
				if(d2 < d)
				{
					near = i;
					d = d2;
					neg = false;
				}
				e1 = r.polygon(i).point(r.polygon(i).size() - 1);
				d2 = Rr2Point.dSquared(startNearHere, e1);
				if(d2 < d)
				{
					near = i;
					d = d2;
					neg = true;
				}
			}

			if(near < 0)
			{
				System.err.println("RrPolygonList.nearEnds(): no nearest end found to start point!");
				return r;
			}

			r.swap(0, near);
			if(neg)
				r.negate(0);
		}
		
		
		// Go through the rest of the polygons getting them as close as
		// reasonable.
		
		int pg = 0;
		while(pg < r.size() - 1)
		{
			Rr2Point end = r.polygon(pg).point(r.polygon(pg).size() - 1);
			neg = false;
			near = -1;
			d = Double.POSITIVE_INFINITY;
			for(i = pg+1; i < r.size(); i++)
			{
				e1 = r.polygon(i).point(0);
				d2 = Rr2Point.dSquared(end, e1);
				if(d2 < d)
				{
					near = i;
					d = d2;
					neg = false;
				}
				
				e1 = r.polygon(i).point(r.polygon(i).size() - 1);
				d2 = Rr2Point.dSquared(end, e1);
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
			
			if(near != pg+1)
				r.swap(pg+1, near);
			if(neg)
				r.negate(pg+1);
			
			pg++;
		}
		
		return r;
	}
	
	/**
	 * Remove edges that are shorter than tiny from the
	 *   polygons in the list if those edges are preceeded 
	 *   by gap material.  
	 * @param tiny
	 * @return filtered polygon list
	 */
//	public RrPolygonList filterShorts(double tiny)
//	{
//		RrPolygonList r = new RrPolygonList();
//		int i;
//		RrPolygon p;
//		
//		for(i = 0; i < size(); i++)
//		{
//			p = polygon(i).filterShort(tiny);
//			if(p.size() > 0)
//				r.add(polygon(i));
//		}
//		return r;
//	}
	
	/**
	 * Is polygon i inside CSG polygon j?
	 * (Check twice to make sure...)
	 * @param i
	 * @param j
	 * @param csgPols
	 * @return true if the polygon is inside the CSG polygon, false if otherwise
	 */
	private boolean inside(int i, int j, List<RrCSG> csgPols)
	{
		RrCSG exp = csgPols.get(j);
		Rr2Point p = polygon(i).point(0);
		boolean a = (exp.value(p) <= 0);
		p = polygon(i).point(polygon(i).size()/2);
		boolean b = (exp.value(p) <= 0);
		if (a != b)
			System.err.println("RrPolygonList:inside() - i is both inside and outside j!");
		return a;
	}
	
//	/**
//	 * Set every instance of m in the lists null, and replace m's
//	 * own list with null
//	 * @param m
//	 * @param contains
//	 */
//	private void getRidOf(int m, List<ArrayList> contains)
//	{
//		for(int i = 0; i < size(); i++)
//		{
//			if(i == m)
//				contains.set(i, null);
//			else
//			{
//				if(contains.get(i) != null)
//				{
//					List<Integer> contain = contains.get(i);
//					for(int j = 0; j < contain.size(); j++)
//					{
//						if(contain.get(j) != null)
//						{
//							if((contain.get(j)).intValue() == m)
//								contain.set(j, null);
//						}
//					}
//				}
//			}
//		}		
//	}
	
//	/**
//	 * Count the non-null entries in a list.
//	 * @param a list with entries to be checked
//	 * @return number non-null entries in list a
//	 */
//	private int activeCount(List a)
//	{
//		int count = 0;
//		for(int i = 0; i < a.size(); i++)
//			if(a.get(i) != null)
//				count++;
//		return count;
//	}
	
	/**
	 * Take a list of CSG expressions, each one corresponding with the entry of the same 
	 * index in this class, classify each as being inside other(s)
	 * (or not), and hence form a single CSG expression representing them all.
	 * @param csgPols
	 * @param polAttributes
	 * @return single CSG expression based on csgPols list 
	 */
	private RrCSGPolygon resolveInsides(List<RrCSG> csgPols)
	{
		int i, j;
		
		treeList universe = new treeList(-1);
		universe.addChild(new treeList(0));
		
		// For each polygon construct a list of all the others that
		// are inside it (if any).
		
		for(i = 0; i < size() - 1; i++)
		{
			treeList isList = universe.walkFind(i);
			if(isList == null)
			{
				isList = new treeList(i);
				universe.addChild(isList);
			}

			for(j = i + 1; j < size(); j++)
			{
				treeList jsList = universe.walkFind(j);
				if(jsList == null)
				{
					jsList = new treeList(j);
					universe.addChild(jsList);
				}


				if(inside(j, i, csgPols))  // j inside i?
					isList.addChild(jsList);

				if(inside(i, j, csgPols))  // i inside j?
					jsList.addChild(isList);						
			}
		}
		
		// Set all the parent pointers
		
		universe.setParents();
		//System.out.println("---\n" + universe.toString() + "\n---\n");
		
		// Eliminate each leaf from every part of the tree except the node immediately above itself
		
		for(i = 0; i < size(); i++)
		{		
			treeList isList = universe.walkFind(i);
			if(isList == null)
				System.err.println("RrPolygonList.resolveInsides() - can't find list for polygon " + i);
			treeList parent = isList.getParent();
			if(parent != null)
			{
				parent = parent.getParent();
				while(parent != null)
				{
					parent.remove(isList);
					parent = parent.getParent();
				}
			}
		}
		//System.out.println("---\n" + universe.toString() + "\n---\n");
		
		// We now have a tree of containment.  universe is the root.
		// Walk the tree turning it into a single CSG expression
		
		RrCSG expression = universe.buildCSG(csgPols);
		
		RrCSGPolygon res = new RrCSGPolygon(expression, box.scale(1.1), polygon(0).getAttributes());
		//res.divide(0.0001, 0);
		//RrGraphics g2 = new RrGraphics(res, true);
		return res;		
	}
	
	/**
	 * Compute the CSG representation of all the polygons in the list
	 * @return CSG representation
	 */
	public RrCSGPolygon toCSG(double tolerance)
	{		
		List<RrCSG> csgPols = new ArrayList<RrCSG>();
		
		for(int i = 0; i < size(); i++)
			csgPols.add(polygon(i).toCSG(tolerance).csg());
		
		RrCSGPolygon polygons = resolveInsides(csgPols);
		//expression = expression.simplify(tolerance);
		
		return polygons;
	}
	
}
