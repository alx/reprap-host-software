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
	 * find a polygon from a list of polygon/vertex pairs
	 * @Param i
	 * @param a
	 * @return the point
	 */
	private RrPolygon listPolygon(int i, List a)
	{
		chPair chp = (chPair)a.get(i);
		return polygon(chp.polygon);
	}
	
	
	/**
	 * find a flag from a list of polygon/vertex pairs
	 * @Param i
	 * @param a
	 * @return the point
	 */
	private int listFlag(int i, List a)
	{
		chPair chp = (chPair)a.get(i);
		return polygon(chp.polygon).flag(chp.vertex);
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
		Rr2Point p, q;
		RrCSG hull = RrCSG.universe();
		int i, iPlus;
		for(i = 0; i < hullPoints.size(); i++)
		{
			iPlus = (i + 1)%hullPoints.size();
			p = listPoint(i, hullPoints);
			q = listPoint(iPlus, hullPoints);
			hull = RrCSG.intersection(hull, new RrCSG(new RrHalfPlane(p, q)));
		}

		return hull;
	}
	
	/**
	 * Turn a list of hull points into a polygon
	 * @param hullPoints
	 * @return the hull as another polygon
	 */	
	public RrPolygon toRrPolygonHull(List hullPoints, int flag)
	{
		RrPolygon hull = new RrPolygon();
		
		for(int i = 0; i < hullPoints.size(); i++)
			hull.add(listPoint(i, hullPoints), flag);

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
		double v;
		int i = inConsideration.size() - 1;
		while(i >= 0)
		{
			p = listPoint(i, inConsideration);
			v = hull.value(p);
			if(v <= Math.sqrt(Preferences.tiny()))	// Was 1.0e-6
			{
				inConsideration.remove(i);
			}
			i--;
		}
	}
	
	/**
	 * Compute the convex hull of all the points in the list
	 * @param points
	 * @return list of point index pairs of the points on the hull
	 */
	private List convexHull(List points)
	{	
		if(points.size() < 3)
		{
			System.err.println("convexHull(): attempt to compute hull for " + points.size() + " points!");
			return new ArrayList();
		}
		
		List inConsideration = new ArrayList(points);
		
		int i;

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
			
		// Repeatedly add the point that's furthest outside the current hull
		
		int corner, after;
		RrCSG hull;
		double v, vMax;
		Rr2Point p, q;
		RrHalfPlane hp;
		while(inConsideration.size() > 0)
		{
			vMax = 0;   // Need epsilon?
			corner = -1;
			after = -1;
			for(int testPoint = inConsideration.size() - 1; testPoint >= 0; testPoint--)
			{
				p = listPoint(result.size() - 1, result);
				for(i = 0; i < result.size(); i++)
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
			
			// Get the first triangle in the right order
			
			if(result.size() == 3)
				clockWise(result);

			// Remove all points within the current hull from further consideration
			
			hull = toCSGHull(result);
			outsideHull(inConsideration, hull);
		}
		
		return result;
	}
	
	/**
	 * Construct a list of all the points in the polygons
	 * @return list of point index pairs of the points in the polygons
	 */
	private List allPoints()
	{
		List points = new ArrayList();
		for(int i = 0; i < size(); i++)
		{
			for(int j = 0; j < polygon(i).size(); j++)
				points.add(new chPair(i, j));
		}
		
		return points;
	}
	
	/**
	 * Compute the convex hull of all the polygons in the list
	 * @return list of point index pairs of the points on the hull
	 */
	public List convexHull()
	{
		return convexHull(allPoints());
	}
		
	/**
	 * Set the polygon flag values for the points in a list
	 * Also checks for a single polygon (true) or multiple (false).
	 * @param a
	 * @param flag
	 * @return
	 */
	private boolean flagSet(List a, int flag)
	{
		RrPolygon pg = listPolygon(0, a);
		boolean result = true;
		for(int i = 0; i < a.size(); i++)
		{
			chPair chp = (chPair)a.get(i);
		    polygon(chp.polygon).flag(chp.vertex, flag);
		    if(polygon(chp.polygon) != pg)
		    	result = false;
		}
		return result;
	}
	
	/**
	 * Get the next whole section to consider from list a
	 * @param a
	 * @param level
	 * @return the section (null for none left)
	 */
	private List polSection(List a, int level)
	{
		int flag, oldi;
		oldi = a.size() - 1;
		RrPolygon oldPg = listPolygon(oldi, a);
		int oldFlag = listFlag(oldi, a);
		RrPolygon pg = null;

		int ptr = -1;
		int pgStart = 0;
		for(int i = 0; i < a.size(); i++)
		{
			flag = listFlag(i, a);
			pg = listPolygon(i, a);
			if(pg != oldPg)
				pgStart = i;
			if(flag < level && oldFlag >= level && pg == oldPg)
			{
				ptr = oldi;
				break;
			}
			oldi = i;
			oldFlag = flag;
			oldPg = pg;
		}
		
		if(ptr < 0)
			return null;
		
		List result = new ArrayList();
		result.add(a.get(ptr));
		ptr++;
		if(ptr > a.size() - 1)
			ptr = pgStart;
		while(listFlag(ptr, a) < level)
		{
			result.add(a.get(ptr));
			ptr++;
			if(ptr > a.size() - 1)
				ptr = pgStart;
		}

		result.add(a.get(ptr));

		return result;
	}
	
	/**
	 * Find if the polygon for point i in a is in list res
	 * @param i
	 * @param a
	 * @param res
	 * @return true if it is
	 */
	private boolean inList(int i, List a, RrPolygonList pgl)
	{
		RrPolygon pg = listPolygon(i, a);
		for(int j = 0; j < pgl.size(); j++)
		{
			if(pgl.polygon(j) == pg)
				return true;
		}
		return false;
	}
	
	/**
	 * Remove all vertices from a list for polygons pgl
	 * @param a
	 * @param pgl
	 */
	private void removePolygonsVertices(List a, RrPolygonList pgl)
	{
		for(int i = a.size() - 1; i >= 0; i--)
		{
			if(inList(i, a, pgl))
				a.remove(i);
		}
	}
	
	/**
	 * Get all whole polygons from list a
	 * @param a
	 * @param level
	 * @return the polygons (null for none left)
	 */
	private RrPolygonList getComplete(List a, int level)
	{
		RrPolygonList res = new RrPolygonList();
		
		RrPolygon pg = listPolygon(0, a);
		int count = 0;
		boolean gotOne = true;
		for(int i = 0; i < a.size(); i++)
		{
			if(listPolygon(i, a) != pg)
			{
				if(count == pg.size() && gotOne)
					res.add(pg);
				count = 0;
				gotOne = true;
				pg = listPolygon(i, a);
			}
			if(listFlag(i, a) >= level)
				gotOne = false;
			count++;
		}
		
		if(count == pg.size() && gotOne)
			res.add(pg);
		
		if(res.size() > 0)
		{
			removePolygonsVertices(a, res);
			RrPolygonList result = new RrPolygonList();
			for(int i = 0; i < res.size(); i++)
			{
				pg = res.polygon(i);
				double area = pg.area();
				if(level%2 == 1)
				{
					if(area > 0)
						pg = pg.negate();
				} else
				{
					if(area < 0)
						pg = pg.negate();
				}
				result.add(pg);
			}
			
			return result;

		}
		else
		{
			return null;
		}
	}
	
	/**
	 * Find all the polygons that form the convex hull
	 * @param ch
	 * @return 
	 */
	private RrPolygonList outerPols(List ch)
	{
		RrPolygonList result = new RrPolygonList();
		RrPolygon pg = null;
		for(int i = 0; i < ch.size(); i++)
		{
			if(listPolygon(i, ch) != pg)
			{
				pg = listPolygon(i, ch);
				boolean inAlready = false;
				for(int j = 0; j < result.size(); j++)
				{
					if(pg == result.polygon(j))
					{
						inAlready = true;
						break;
					}
				}
				if(!inAlready)
					result.add(pg);
			}
		}
		
		return result;
	}
	
	/**
	 * Compute the CSG representation of a (sub)list recursively
	 * @param a
	 * @param level
	 * @return CSG representation
	 */
	private RrCSG toCSGRecursive(List a, int level, boolean closed)
	{	
		flagSet(a, level);	
		level++;
		List ch = convexHull(a);
		if(ch.size() < 3)
		{
			System.err.println("toCSGRecursive() - null convex hull!");
			return RrCSG.nothing();
		}
		
		boolean onePol = flagSet(ch, level);
		RrCSG hull;
		if(!onePol)
		{
			RrPolygonList op = outerPols(ch);

			if(level%2 == 1)
				hull = RrCSG.nothing();
			else
				hull = RrCSG.universe();
			for(int i = 0; i < op.size(); i++)
			{
				RrPolygonList pgl = new RrPolygonList();
				pgl.add(op.polygon(i));
				List all = pgl.allPoints();
				if(level%2 == 1)
					hull = RrCSG.union(hull, pgl.toCSGRecursive(all, level - 1, true));
				else
					hull = RrCSG.intersection(hull, pgl.toCSGRecursive(all, level - 1, true));
			}
			removePolygonsVertices(a, op);
			if(a.size() > 0)
			{
				if(level%2 == 1)
					return RrCSG.intersection(hull, toCSGRecursive(a, level, true));
				else
					return RrCSG.union(hull, toCSGRecursive(a, level, true));
			} else
			{
				return hull;
			}
		}else
		{
			if(level%2 == 1)
				hull = RrCSG.universe();
			else
				hull = RrCSG.nothing();
		}
		
		// First deal with all the polygons with no points on the hull 
		// (i.e. they are completely inside).
		
		if(closed)
		{
			RrPolygonList completePols = getComplete(a, level);
			if(completePols != null)
			{
				List all = completePols.allPoints();
				if(level%2 == 1)
					hull = RrCSG.intersection(hull,
							completePols.toCSGRecursive(all, level, true));
				else
					hull = RrCSG.union(hull, 
							completePols.toCSGRecursive(all, level, true));	
			}
		}
		
		// Set-theoretically combine all the real edges on the convex hull

		int i, oldi, flag, oldFlag, start;
		RrPolygon pg, oldPg;
		
		if(closed)
		{
			oldi = a.size() - 1;
			start = 0;
		} else
		{
			oldi = 0;
			start = 1;
		}
		
		for(i = start; i < a.size(); i++)
		{
			oldFlag = listFlag(oldi, a);
			oldPg = listPolygon(oldi, a);
			flag = listFlag(i, a);
			pg = listPolygon(i, a);

			if(oldFlag == level && flag == level && pg == oldPg)
			{
				RrHalfPlane hp = new RrHalfPlane(listPoint(oldi, a), listPoint(i, a));
				if(level%2 == 1)
					hull = RrCSG.intersection(hull, new RrCSG(hp));
				else
					hull = RrCSG.union(hull, new RrCSG(hp));
			} 
			
			oldi = i;
		}
		
		// Finally deal with the sections on polygons that form the hull that
		// are not themselves on the hull.
		
		List section = polSection(a, level);
		while(section != null)
		{
			if(level%2 == 1)
				hull = RrCSG.intersection(hull,
						toCSGRecursive(section, level, false));
			else
				hull = RrCSG.union(hull, 
						toCSGRecursive(section, level, false));
			section = polSection(a, level);
		}
		
		return hull;
	}
	
	/**
	 * Compute the CSG representation of all the polygons in the list
	 * using Kai Tang and Tony Woo's algorithm.
	 * @return CSG representation
	 */
	public RrCSGPolygon toCSG(double tolerance)
	{
		RrPolygonList pgl = new RrPolygonList(this);
		List all = pgl.allPoints();
		if(all.size() < 3)
		{
			System.err.println("RrCSGPolygon.toCSG(): less than three points!");
			return new RrCSGPolygon(RrCSG.nothing(), new RrBox(1));
		}
		pgl.flagSet(all, -1);
		pgl = pgl.getComplete(all, 0);
		all = pgl.allPoints();
		RrCSG expression = pgl.toCSGRecursive(all, 0, true);
		RrBox b = pgl.box.scale(1.1);
		expression = expression.simplify(tolerance);
		return new RrCSGPolygon(expression, b);
	}
	
}
