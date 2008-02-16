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
 
 RrHalfPlane: 2D planar half-spaces
 
 First version 20 May 2005
 This version: 9 March 2006
 
 */

package org.reprap.geometry.polygons;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Collections;
import org.reprap.Preferences;

/**
 * Small class to hold the results of intersection calculations
 * Hold a line intersection parameter and whether it's valid
 * to include it.
 * 
 * @author ensab
 *
 */
class testIntersection
{
	public double t;
	public boolean include;
	public testIntersection(double tt, boolean cc)
	{
		t = tt;
		include = cc;
	}
}

/**
 * Small class to hold parameter/quad pairs
 * @author Adrian
 *
 */
class LineIntersection implements Comparator
{
	/**
	 * The line's parameter 
	 */
	private double t;
	
	/**
	 * Quad containing hit plane 
	 */
	private RrCSGPolygon quad;
	
	/**
	 * The hit plane
	 */
	private RrHalfPlane hp;
	
	/**
	 * @param v
	 * @param q
	 */
	public LineIntersection(double v, RrCSGPolygon q, RrHalfPlane h)
	{
		t = v;
		quad = q;
		hp = h;
	}
	
	/**
	 * Null constructor for comparisons
	 *
	 */
	public LineIntersection()
	{
		t = 0;
		quad = null;
	}
	
	/**
	 * @return
	 */
	public double parameter() { return t; }
	
	/**
	 * @return
	 */
	public RrCSGPolygon quad() { return quad; }
	
	/**
	 * @return
	 */
	public RrHalfPlane plane() { return hp; }
	
	/**
	 * Compare parameters for sorting
	 */
	public final int compare(Object a, Object b)
	{
		if(((LineIntersection)a).t < ((LineIntersection)b).t)
			return -1;
		else if (((LineIntersection)a).t > ((LineIntersection)b).t)
			return 1;
		return 0;
	}
}


/**
 * Class to hold and manipulate linear half-planes
 */
public class RrHalfPlane
{
	
	/**
	 * The half-plane is normal*(x, y) + offset <= 0 
	 */
	private Rr2Point normal; 
	private double offset;
	
	/**
	 * Keep the parametric equivalent to save computing it
	 */
	private RrLine p;
	
	/**
	 * List of intersections with others
	 */
	private List<LineIntersection> crossings;
	
	/**
	 * Convert a parametric line
	 * @param l
	 */
	public RrHalfPlane(RrLine l)
	{
		p = new RrLine(l);
		p.norm();
		normal = new Rr2Point(-p.direction().y(), p.direction().x());
		offset = -Rr2Point.mul(l.origin(), normal());
		crossings = new ArrayList<LineIntersection>();
	}
	
	/**
	 * Make one from two points on its edge
	 * @param a
	 * @param b
	 */
	public RrHalfPlane(Rr2Point a, Rr2Point b)
	{
		this(new RrLine(a, b));
	}   
	
	/**
	 * Deep copy
	 * @param a
	 */
	public RrHalfPlane(RrHalfPlane a)
	{
		normal = new Rr2Point(a.normal);
		offset = a.offset;
		p = new RrLine(a.p);
		crossings = new ArrayList<LineIntersection>(); // No point in deep copy -
		                             // No pointers would match
	}
	
	/**
	 * Get the parametric equivalent
	 * @return parametric equivalent of a line
	 */
	public RrLine pLine()
	{
		return p;
	}
	
	/**
	 * TODO: make this spot complements too.
	 * Is another line the same within a tolerance?
	 * @param a
	 * @param b
	 * @return true if the distance between halfplane a and b is less then the tolerance, otherwise false
	 */
	public static boolean same(RrHalfPlane a, RrHalfPlane b)
	{
		if(!Rr2Point.same(a.normal, b.normal))
			return false;

		//double rms = Math.sqrt((a.offset*a.offset + b.offset*b.offset)*0.5);
		if(Math.abs(a.offset - b.offset) > Preferences.pointResolution()) //*rms)
			return false;
		
		return true;
	}
	
	/**
	 * Get the components
	 * @return components?
	 */
	public Rr2Point normal() { return normal; }
	public Rr2Point grad() { return normal; }   // For completeness
	public double offset() { return offset; }
	

	
	/**
	 * Change the sense
	 * @return complent of half plane
	 */
	public RrHalfPlane complement()
	{
		RrHalfPlane r = new RrHalfPlane(this);
		r.normal = r.normal.neg();
		r.offset = -r.offset;
		r.p = r.p.neg();
		return r;
	}
	
	/**
	 * Move
	 * @param d
	 * @return offset halfplane
	 */
	public RrHalfPlane offset(double d)
	{
		RrHalfPlane r = new RrHalfPlane(this);
		r.offset = r.offset - d;
		r.p = p.offset(d);
		return r;
	}
	
	
	/**
	 * Find the potential value of a point
	 * @param p
	 * @return potential value of point p
	 */
	public double value(Rr2Point p)
	{
		return offset + Rr2Point.mul(normal, p);
	}
	
	
	/**
	 * Find the potential interval of a box
	 * @param b
	 * @return potential interval of box b
	 */
	public RrInterval value(RrBox b)
	{
		RrInterval xi = RrInterval.mul(b.x(), normal.x());
		RrInterval yi = RrInterval.mul(b.y(), normal.y());
		RrInterval sp = RrInterval.add(xi, yi);
		RrInterval r = RrInterval.add(sp, offset);
		return r;
	}
	
	/**
	 * The point where another line crosses
	 * @param a
	 * @return cross point
	 * @throws RrParallelLineException
	 */
	public Rr2Point crossPoint(RrHalfPlane a) throws RrParallelLineException
	{
		double det = Rr2Point.op(normal, a.normal);
		if(det == 0)
			throw new RrParallelLineException("crossPoint: parallel lines.");
		det = 1/det;
		double x = normal.y()*a.offset - a.normal.y()*offset;
		double y = a.normal.x()*offset - normal.x()*a.offset;
		return new Rr2Point(x*det, y*det);
	}
	
	/**
	 * Add a crossing
	 * @param qc
	 */
	public static boolean cross(RrCSGPolygon qc)
	{		
		if(qc.corner())
		{
			RrInterval range = RrInterval.bigInterval();
			boolean b = qc.csg().c1().plane().maybeAdd(qc, range);
			range = RrInterval.bigInterval();
			b = b & qc.csg().c2().plane().maybeAdd(qc, range);
			return (b);
		}
		System.err.println("RrHalfPlane.cross(): called for non-corner!");
		return false;
	}
	
	/**
	 * Find a crossing
	 * @param q
	 * @return the index of the quad
	 */
	public int find(RrCSGPolygon q)
	{	
		for(int i = 0; i < crossings.size(); i++)
		{
			if(getQuad(i) == q)
				return i;
		}
		System.err.println("RrHalfPlane.find(): quad not found!");
		return -1;
	}
	
	/**
	 * Find the index of a crossing plane
	 * @param h
	 * @return index of the plane
	 */
	public int find(RrHalfPlane h)
	{	
		for(int i = 0; i < crossings.size(); i++)
		{
			if(getPlane(i) == h)
				return i;
		}
		System.err.println("RrHalfPlane.find(): plane not found!");
		return -1;
	}
	
	/**
	 * Remove all crossings
	 */
	public void removeCrossings()
	{
		crossings = new ArrayList<LineIntersection>();
	}
		
	/**
	 * Remove a crossing from the list
	 * @param i identifier of the crossing to be removed from the list 
	 */
	public void remove(int i)
	{
		crossings.remove(i);
	}
	
	/**
	 * Add in a new intersection
	 * @param c
	 */
	public void add(LineIntersection c)
	{
		crossings.add(c);
	}
	
	/**
	 * Sort on parameter value.
	 */
	public void sort()
	{
		Collections.sort(crossings, new LineIntersection());
	}
	
	/**
	 * Return the plane as a string
	 * @return string representation
	 */
	public String toString()
	{
		return "|" + normal.toString() + ", " + Double.toString(offset) + "|";
	} 
	
//	/**
//	 * Parameter value on a line where this crosses
//	 * @param a
//	 * @return parameter value
//	 * @throws RrParallelLineException
//	 */
//	public double crossParameter(RrLine a) throws RrParallelLineException 
//	{
//		double det = Rr2Point.mul(a.direction(), normal);
//		if (det == 0)
//			throw new RrParallelLineException("crossParameter: parallel lines.");  
//		return -value(a.origin())/det;
//	}
	
	/**
	 * Point where a parametric line crosses
	 * @param a
	 * @return cross point
	 * @throws RrParallelLineException
	 */
	public Rr2Point crossPoint(RrLine a) throws RrParallelLineException
	{
		return a.point(a.crossParameter(this));
	}
	
	/**
	 * Take a range of parameter values and a line, and find
	 * the intersection of that range with the part of the line
	 * (if any) on the solid side of the half-plane.
	 * @param a
	 * @param range
	 * @return intersection interval
	 */
	public RrInterval wipe(RrLine a, RrInterval range)
	{
		if(range.empty()) return range;
		
		// Which way is the line pointing relative to our normal?
		
		boolean wipe_down = (Rr2Point.mul(a.direction(), normal) >= 0);
		
		double t;
		
		try
		{
			t = a.crossParameter(this);
			if (t >= range.high())
			{
				if(wipe_down)
					return range;
				else
					return new RrInterval();
			} else if (t <= range.low())
			{
				if(wipe_down)
					return new RrInterval();
				else
					return range;                
			} else
			{
				if(wipe_down)
					return new RrInterval(range.low(), t);
				else
					return new RrInterval(t, range.high());                 
			}
		} catch (RrParallelLineException ple)
		{
			t = value(a.origin());
			if(t <= 0)
				return range;
			else
				return new RrInterval();  
		}
	}
	
	/**
	 * The number of crossings
	 * @return number of crossings
	 */
	public int size()
	{
		return crossings.size();
	}
	
	/**
	 * Get the i-th crossing parameter
	 * @param i
	 * @return i-th crossing parameter
	 */
	public double getParameter(int i)
	{
		return crossings.get(i).parameter();
	}
	
	/**
	 * i-th point from the crossing list
	 * @param i
	 * @return i-th point
	 */
	public Rr2Point getPoint(int i)
	{
		return pLine().point(getParameter(i));
	}
	
	/**
	 * Get the i-th quad
	 * @param i
	 * @return i-th quad
	 */
	public RrCSGPolygon getQuad(int i)
	{
		return crossings.get(i).quad();
	}
	
	/**
	 * Get the i-th CSG for the plane
	 * @param i
	 * @return i-th CSG
	 */
//	public RrCSG getCSG(int i)
//	{
//		RrCSGPolygon q = getQuad(i);
//		if(q.csg().complexity() == 1)
//			return q.csg();
//		else if(q.csg().complexity() == 2)
//		{
//			if(q.csg().c1().plane() == this)
//				return q.csg().c2();
//			if(q.csg().c2().plane() == this)			
//				return q.csg().c1();
//			
//			double t = getParameter(i);
//			Rr2Point pt = pLine().point(t);
//			double v = Math.abs(q.csg().c1().plane().value(pt));
//			if(Math.abs(q.csg().c2().plane().value(pt)) < v)
//				return q.csg().c2();
//			else
//				return q.csg().c1();
//		}
//		
//		System.err.println("RrHalfPlane.getCSG(): complexity > 2: " + q.csg().complexity());
//		return RrCSG.nothing();
//	}
	
	/**
	 * Get the i-th plane.
	 * @param i
	 * @return i-th plane
	 */
	public RrHalfPlane getPlane(int i)
	{
		return crossings.get(i).plane();
	}
	
	private boolean fixSection(int i, RrCSGPolygon p)
	{
		RrInterval range = new RrInterval(getParameter(i), getParameter(i + 1));
		RrInterval innerRange = new RrInterval(range.low() + range.length()*0.1, range.high() - range.length()*0.1);
		double vLow = p.value(pLine().point(innerRange.low()));
		double vHigh = p.value(pLine().point(innerRange.high()));
		if(vLow*vHigh > 0)
			return false;
		System.err.print("RrHalfPlane.fixSection(): range " + innerRange.toString());
		LineIntersection bc = binaryChop(p, innerRange);
		if(bc == null)
		{	
			System.err.println("RrHalfPlane.fixSection(): odd signs, but no intersection found!");
			return false;
		}
		System.err.println(" chopped at " + bc.parameter());
		crossings.add(i+1, bc);
		return true;
	}
	
	private void otherEnd(int i, RrCSGPolygon p)
	{
		Rr2Point dir = p.grad(pLine().point(getParameter(i)));
		double way = Rr2Point.mul(dir, pLine().direction());
		if(way > 0)
			way = -1;
		else
			way = 1;
		double big = Math.sqrt(p.box().dSquared());
		double d = big*0.025*way;
		double t0 = getParameter(i) + way*Preferences.pointResolution()*10;
		double t1 = t0;
		double v = p.csg().value(pLine().point(t0));
		while(Math.abs(d) < Math.abs(big*2) && v < 0)
		{
			d *= 2;
			t1 = t0 + d;
			v = p.csg().value(pLine().point(t1));
		}
		
		if(v < 0)
		{	
			System.err.println("RrHalfPlane.otherEnd(): no positive point found!");
			return;
		}
		
		RrInterval section;
		if(t0 < t1)
			section = new RrInterval(t0, t1);
		else
			section = new RrInterval(t1, t0);
		
		LineIntersection bc = binaryChop(p, section);
		if(bc == null)
		{	
			System.err.println("RrHalfPlane.otherEnd(): odd signs, but no intersection found!");
			return;
		}
		
		if(way < 0 && i != 0)		
			crossings.add(i-1, bc); // TODO: Not sure about this...
		else
			crossings.add(i, bc);
	}
	
	private void singleCrossing(RrCSGPolygon p)
	{
		if(size() > 1)
			return;
		otherEnd(0, p);
	}
	
	private void fixEnds(RrCSGPolygon p)
	{
		double v = p.csg().value(pLine().point(getParameter(0) - Preferences.pointResolution()*10));
		if(v < 0)
			otherEnd(0, p);
		
		v = p.csg().value(pLine().point(getParameter(size() - 1) + Preferences.pointResolution()*10));
		if(v < 0)
			otherEnd(size() - 1, p);
	}
	
	/**
	 * Take the sorted list of parameter values and a shape, and
	 * make sure they alternate solid/void/solid etc.  
	 * @param p
	 */
	public void solidSet(RrCSGPolygon p)
	{
		if(size() == 0)
			return;
		
		singleCrossing(p);
		
		fixEnds(p);
		
		int i = 0;
		while(i < size() - 1)
		{
			if(!fixSection(i, p))
				i++;
		}
		
		
		
//		if(size()%2 == 0)
//			return;
//		
//		int shortest = -1;
//		double shortLen = Double.POSITIVE_INFINITY;
//		double s;
//		for(int i = 0; i < size()-1; i++)
//		{
//			double pi = getParameter(i);
//			double pi1 = getParameter(i+1);
//			s = Math.abs(pi1 - pi);
//			if(s < shortLen)
//			{
//				shortest = i;
//				shortLen = s;
//			}
//			if(shortest >= 0)
//				crossings.remove(shortest);
//			else
//				System.err.println("RrHalfPlane.solidSet(): no shortest length! crossing count: " +
//						crossings.size());
//		}
		
//		double s, v;
//		boolean odd = true;
//		int i = 0;
//		int shortest = -1;
//		double shortLen = Double.POSITIVE_INFINITY;
//		while(i < size() - 1)
//		{
//			double pi = getParameter(i);
//			double pi1 = getParameter(i+1);
//			v = 0.5*(pi + pi1);
//			s = Math.abs(pi1 - pi);
//			if(s < shortLen)
//			{
//				shortest = i;
//				shortLen = s;
//			}
//			boolean tiny = s < Preferences.pointResolution();
//			v = p.value(pLine().point(v));
//			if(odd)
//			{
//				if(v > 0)
//				{
//					if(tiny)
//						crossings.remove(i);
//					else
//						crossings.add(i, crossings.get(i));
//				}
//			} else
//			{
//				if(v <= 0)
//				{
//					if(tiny)
//						crossings.remove(i);
//					else
//						crossings.add(i, crossings.get(i));
//				}	
//			}
//			odd = !odd;
//			i++;
//		}
//		if (size()%2 != 0)    // Nasty hack that seems to work...
//		{
//			System.err.println("RrHalfPlane.solidSet(): odd number of crossings: " +
//					crossings.size() + ", shortest interval is: " + shortLen);
//			if(shortest >= 0)
//				crossings.remove(shortest);
//		}
		
//		if(size() == 0)
//			return;
//		
//		if(size() < 2)
//		{
//			System.err.println("RrHalfPlane.solidSet(): fewer than 2 intersections: " + size());
//			return;
//		}
//		
//		double v;
//		boolean lastSolid = false;
//		int top = size() - 1;
//		while(top >= 1)
//		{
//			int bottom = top - 1;
//			double pBottom = getParameter(bottom);
//			double pTop = getParameter(top);
//
//			v = 0.5*(pBottom + pTop);
//			v = p.value(pLine().point(v));
//			
//			if(lastSolid)
//			{
//				if(v < 0)
//				{
//						crossings.remove(top);
//				} else
//					lastSolid = false;
//			} else
//			{
//				if(v > 0)
//				{
//						crossings.remove(top);
//				} else
//					lastSolid = true;
//			}
//			top = bottom;
//		}
//		
//		if (size()%2 != 0)    // Nasty hack that seems to work...
//		{
//			double shortLen = Double.POSITIVE_INFINITY;
//			int shortest = -1;
//			for(int i = 0; i < size() - 1; i++)
//			{
//				int j = i + 1;
//				v = Math.abs(getParameter(j) - getParameter(i));
//				if(v < shortLen)
//				{
//					shortLen = v;
//					shortest = i;
//				}
//			}
//			System.err.println("RrHalfPlane.solidSet(): odd number of crossings: " +
//					crossings.size() + ", shortest interval is: " + shortLen);
//			
//
//			if(shortest >= 0)
//				crossings.remove(shortest);
//		}
	}

	public LineIntersection binaryChop(RrCSGPolygon q, RrInterval range)
	{		
		Rr2Point g;
		boolean lowSolid, highSolid;
		
		Rr2Point pLow = pLine().point(range.low());
		double vLow = q.csg().value(pLow);  // Use q.csg() to avoid rejection cos we're on an edge of q
		if(Math.abs(vLow) < Preferences.pointResolution())
		{
			g = q.grad(pLow);
			lowSolid = (Rr2Point.mul(g, pLine().direction()) < 0);
		} else
			lowSolid = (vLow <= 0);
		
		Rr2Point pHigh = pLine().point(range.high());
		double vHigh = q.csg().value(pHigh);  // Use q.csg() to avoid rejection cos we're on an edge of q
		if(Math.abs(vHigh) < Preferences.pointResolution())
		{
			g = q.grad(pHigh);
			highSolid = (Rr2Point.mul(g, pLine().direction()) > 0);
		} else
			highSolid = (vHigh <= 0);
		
		if(lowSolid == highSolid)
			return null;
		
		double middle;
		Rr2Point pt;
		
		int count = 0; // Just in case...
		
		while(range.length() > Preferences.pointResolution() && count < 20)
		{
			middle = range.cen();
			pt = pLine().point(middle);
			double v = q.csg().value(pt);
			if(lowSolid)
			{
				if(v <= 0)
					range = new RrInterval(middle, range.high());
				else
					range = new RrInterval(range.low(), middle);
			} else
			{
				if(v > 0)
					range = new RrInterval(middle, range.high());
				else
					range = new RrInterval(range.low(), middle);				
			}
			count++;
		}
		
		middle = range.cen();
		pt = pLine().point(middle);
		RrHalfPlane pl = q.leaf(pt).plane();
		if(Math.abs(pl.value(pt)) > Preferences.pointResolution()*4)
		{
			System.err.println("RrHAlfPlane.binaryChop(): point too far from plane: " + pl.value(pt));
		}
		return new LineIntersection(middle, q, pl);
		
	}
	
	/**
	 * Test a crossing to see if we have an intersection with p with a parameter within bounds.
	 * @param p
	 * @param q
	 * @param range
	 * @return true if p in q may be added, otherwise false, plus the crossing parameter
	 */
	private testIntersection testCross(RrHalfPlane p, RrCSGPolygon q, RrInterval range)
	{	
		// Ensure no duplicates
		
		for(int i = 0; i < crossings.size(); i++)
		{
			if(getPlane(i) == p)
				return new testIntersection(0, false);     // Say no, because we've already got it
		}
		
		try
		{
			double v = pLine().crossParameter(p);
			if(v >= range.low() && v < range.high())
				return new testIntersection(v, true);						
		} catch (RrParallelLineException ple)
		{}
		
		return new testIntersection(0, false);
	}
	
	/**
	 * Add quad q if it contains a half-plane with an 
	 * intersection with a parameter within bounds.
	 * @param q
	 * @param range
	 * @return true if quad q has been added, otherwise false
	 */
	public boolean maybeAdd(RrCSGPolygon q, RrInterval range)
	{
		RrInterval newRange = q.box().wipe(pLine(), range);
		if(newRange.empty())
			return false;
		
		testIntersection ti1, ti2;
		RrHalfPlane p1, p2;
		
		switch(q.csg().operator())
		{
		case NULL:
		case UNIVERSE:
			return false;
		
		case LEAF:
//			p1 = q.csg().plane();
//			ti1 = testCross(p1, q, newRange);
//			if(ti1.include)
//			{
//				crossings.add(new LineIntersection(ti1.t, q, p1));
//				return true;
//			} else
//				return false;
			LineIntersection li = binaryChop(q, range);
			if(li != null)
			{
				crossings.add(li);
				return true;
			} else
				return false;
			
		case INTERSECTION:
		case UNION:	
			if(q.csg().complexity() != 2)
			{
				System.err.println("RrHalfPlane.maybeAdd(): too complex: " + q.csg().complexity());
				return false;
			}
			
			// Deal with the cases where we are half of the CSG expression
			// Being cut by the plane that's the other half.
			
			p1 = q.csg().c1().plane();
			p2 = q.csg().c2().plane();
			
			if(p1 == this)
			{
				ti2 = testCross(p2, q, newRange);
				if(ti2.include)
				{
					crossings.add(new LineIntersection(ti2.t, q, p2));
					return true;
				}
				return false;
			}

			if(p2 == this)
			{
				ti1 = testCross(p1, q, newRange);
				if(ti1.include)
				{
					crossings.add(new LineIntersection(ti1.t, q, p1));
					return true;
				}
				return false;
			}				
			
			// Deal with the cases where we are not a part of the CSG expression
			
			li = binaryChop(q, range);
			if(li != null)
			{
				crossings.add(li);
				return true;
			}
			
			ti1 = testCross(p1, q, newRange);
			ti2 = testCross(p2, q, newRange);
			
			// Expression values where we cross the box (don't use q.value() as
			// it would sometimes reject the point as it's on the very edge of
			// the box).
			
			double vLow = q.csg().value(pLine().point(newRange.low()));
			double vHigh = q.csg().value(pLine().point(newRange.high()));
					
			if(ti1.include && ti2.include)
			{
				// Our crossing points with both p1 and p2 are in q's box
				
				if(vLow*vHigh < 0)
				{
					double v1 = q.value(pLine().point(ti1.t));
					double v2 = q.value(pLine().point(ti2.t));
					if(Math.abs(v1) > Math.abs(v2))
						crossings.add(new LineIntersection(ti2.t, q, p2));
					else
						crossings.add(new LineIntersection(ti1.t, q, p1));
					return true;
				}
				
				double vMid = q.value(pLine().point((ti1.t + ti2.t)*0.5));
				
				if(vMid*vHigh < 0)
				{
					crossings.add(new LineIntersection(ti1.t, q, p1));
					crossings.add(new LineIntersection(ti2.t, q, p2));
					return true;
				}				
				return false;
			} else if(ti1.include)  
			{
				// Our crossing points with p1 only is in q's box
				
				if(vLow*vHigh < 0)
				{
					crossings.add(new LineIntersection(ti1.t, q, p1));
					return true;
				}
				return false;
			} else if(ti2.include)  
			{
				// Our crossing points with p2 only is in q's box
				
				if(vLow*vHigh < 0)
				{
					crossings.add(new LineIntersection(ti2.t, q, p2));
					return true;
				}
				return false;				
			}
			
			return false;
			
		default:
			System.err.println("RrHalfPlane.maybeAdd(): invalid CSG operator!");
		}
		
		return false;
	}
}