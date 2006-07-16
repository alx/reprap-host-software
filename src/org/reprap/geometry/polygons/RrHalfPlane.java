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

import java.util.*;


/**
 * Class to hold line intersections as represented by quad-tree leaves
 */
class RrLineCrossings 
{
	private List quads;
	private List t;
	
	public RrLineCrossings()
	{
		quads = new ArrayList();
		t = new ArrayList();
	}
	
	public RrLineCrossings(RrLineCrossings lx)
	{
		quads = new ArrayList();
		t = new ArrayList();
		for(int i = 0; i < lx.size(); i ++)
		{
			this.add(lx.quad(i), lx.param(i)); // NB - does not deep copy quads
		}
	}
	
	public void add(RrCSGPolygon q, double param)
	{
		quads.add(q);
		t.add(new Double(param));
	}
	
	public RrCSGPolygon quad(int i)
	{
		return (RrCSGPolygon)quads.get(i);
	}
	
	public double param(int i)
	{
		return ((Double)t.get(i)).doubleValue();
	}
	
	public int size()
	{
		return quads.size();
	}
	
	private void swap(int i, int j)
	{
		Object temp = t.get(i);
		t.set(i, t.get(j));
		t.set(j, temp);
		temp = quads.get(i);
		quads.set(i, quads.get(j));
		quads.set(j, temp);
	}
	
	public void sort(boolean up)
	{
		if(size()%2 != 0)
			System.err.println("RrLineCrossings.sort(): odd number of crossings!");

		// Lists will always be short, so N^2 sort is OK.
		
		for(int i = 0; i < size() - 1; i++)
			for(int j = i + 1; j < size(); j++)
			{
				if(up)
				{
					if(param(i) > param(j))
						swap(i, j);
				} else
				{
					if(param(i) < param(j))
						swap(i, j);
				}
			}
	}
}

// 

/**
 * Class to hold and manipulate linear half-planes
 */
public class RrHalfPlane
{
	
	// The half-plane is normal*(x, y) + offset <= 0
	
	private Rr2Point normal; 
	private double offset;
	private RrLine p;  // Keep the parametric equivalent to save computing it
	private RrLineCrossings crossings;  // List of intersections with others
	
	/**
	 * Convert a parametric line
	 * @param l
	 */
	public RrHalfPlane(RrLine l)
	{
		p = new RrLine(l);
		p.norm();
		double r = 1/l.direction().mod();
		normal = new Rr2Point(-l.direction().y()*r, l.direction().x()*r);
		offset = -Rr2Point.mul(l.origin(), normal());
		crossings = new RrLineCrossings();
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
		crossings = new RrLineCrossings(a.crossings);
	}
	
	/**
	 * Get the parametric equivalent
	 * @return
	 */
	public RrLine pLine()
	{
		return p;
	}
	
	/**
	 * Add a crossing
	 * @param a
	 * @param t
	 */
	private boolean add(RrCSGPolygon q, double t)
	{
		// Ensure no duplicates
		
		RrHalfPlane newhp = q.csg().c_1().plane();
		if(newhp == this)
			newhp = q.csg().c_2().plane();
		RrHalfPlane test;
		for(int i = 0; i < crossings.size(); i++)
		{
			test = crossings.quad(i).csg().c_1().plane();
			if(test == this)
				test = crossings.quad(i).csg().c_2().plane();
			if(test == newhp)
				return false;
		}
		crossings.add(q, t);
		return true;
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
			if(crossings.quad(i) == q)
				return i;
		}
		System.err.println("RrHalfPlane.find(): quad not found!");
		return 0;
	}
	
	/**
	 * Remove all crossings
	 * @param a
	 * @param t
	 */
	public void removeCrossings()
	{
		crossings = new RrLineCrossings();
	}
	
	/**
	 * Sort crossings
	 * @param a
	 * @param t
	 */
	public void sortCrossings(boolean up)
	{
		crossings.sort(up);
	}
	
	/**
	 * Return crossing quad
	 * @param i
	 * @return
	 */
	public RrCSGPolygon quad(int i)
	{
		return crossings.quad(i);
	}
	
	/**
	 * Return crossing parameter
	 * @param i
	 * @return
	 */
	public double param(int i)
	{
		return crossings.param(i);
	}
	
	/**
	 * Return the plane as a string
	 * @return
	 */
	public String toString()
	{
		return "|" + normal.toString() + ", " + Double.toString(offset) + "|";
	} 
	
	// Get the components
	
	public Rr2Point normal() { return normal; }
	public double offset() { return offset; }
	
	/**
	 * Is another line the same within a tolerance?
	 * @param a
	 * @param b
	 * @param tolerance
	 * @return
	 */
	public static boolean same(RrHalfPlane a, RrHalfPlane b, double tolerance)
	{
		if(Math.abs(a.normal.x() - b.normal.x()) > tolerance)
			return false;
		if(Math.abs(a.normal.y() - b.normal.y()) > tolerance)
			return false;
		double rms = Math.sqrt((a.offset*a.offset + b.offset*b.offset)*0.5);
		if(Math.abs(a.offset - b.offset) > tolerance*rms)
			return false;
		
		return true;
	}
	
	
	/**
	 * Change the sense
	 * @return
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
	 * @return
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
	 * @return
	 */
	public double value(Rr2Point p)
	{
		return offset + Rr2Point.mul(normal, p);
	}
	
	
	/**
	 * Find the potential interval of a box
	 * @param b
	 * @return
	 */
	public RrInterval value(RrBox b)
	{
		return RrInterval.add(RrInterval.add((RrInterval.mul(b.x(), normal.x())), 
				(RrInterval.mul(b.y(), normal.y()))), offset);
	}
	
	/**
	 * Add a crossing
	 * @param a
	 * @param b
	 */
	public static boolean cross(RrCSGPolygon qc)
	{
		double t;
		
		if(qc.corner())
		{
			try
			{
				t = qc.csg().c_2().plane().cross_t(
						qc.csg().c_1().plane().pLine());
				if(!qc.csg().c_1().plane().add(qc, t))
					return false;
			} catch (RrParallelLineException ple)
			{
				System.err.println("RrHalfPlane.cross(): parallel lines 1!");
				return false;
			}
			
			try
			{
				t = qc.csg().c_1().plane().cross_t(
						qc.csg().c_2().plane().pLine());
				if(!qc.csg().c_2().plane().add(qc, t))
				{
					System.err.println("RrHalfPlane.cross(): not symmetric!");
					return false;
				}
			} catch (RrParallelLineException ple)
			{
				System.err.println("RrHalfPlane.cross(): parallel lines 2!");
				return false;
			}
		} else
			System.err.println("RrHalfPlane.cross(): called for non-corner!");
		return true;
	}
	
	/**
	 * The point where another line crosses
	 * @param a
	 * @return
	 * @throws RrParallelLineException
	 */
	public Rr2Point cross_point(RrHalfPlane a) throws RrParallelLineException
	{
		double det = Rr2Point.op(normal, a.normal);
		if(det == 0)
			throw new RrParallelLineException("cross_point: parallel lines.");
		det = 1/det;
		double x = normal.y()*a.offset - a.normal.y()*offset;
		double y = a.normal.x()*offset - normal.x()*a.offset;
		return new Rr2Point(x*det, y*det);
	}
	
	/**
	 * Parameter value where a line crosses
	 * @param a
	 * @return
	 * @throws RrParallelLineException
	 */
	public double cross_t(RrLine a) throws RrParallelLineException 
	{
		double det = Rr2Point.mul(a.direction(), normal);
		if (det == 0)
			throw new RrParallelLineException("cross_t: parallel lines.");  
		return -value(a.origin())/det;
	}
	
	/**
	 * Point where a parametric line crosses
	 * @param a
	 * @return
	 * @throws RrParallelLineException
	 */
	public Rr2Point cross_point(RrLine a) throws RrParallelLineException
	{
		return a.point(cross_t(a));
	}
	
	/**
	 * Take a range of parameter values and a line, and find
	 * the intersection of that range with the part of the line
	 * (if any) on the solid side of the half-plane.
	 * @param a
	 * @param range
	 * @return
	 */
	public RrInterval wipe(RrLine a, RrInterval range)
	{
		if(range.empty()) return range;
		
		// Which way is the line pointing relative to our normal?
		
		boolean wipe_down = (Rr2Point.mul(a.direction(), normal) >= 0);
		
		double t;
		
		try
		{
			t = cross_t(a);
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
}
