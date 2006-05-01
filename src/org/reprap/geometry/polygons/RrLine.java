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
 
 RrLine: 2D parametric line
 
 First version 20 May 2005
 This version: 9 March 2006
 
 */

package org.reprap.geometry.polygons;

/**
 * Teeny class to hold bisectors of line pairs
 */
class rr_bisector
{
	public RrLine b;      // The bisecting line
	public double s_angle;  // The sine of the angle 
	
	public rr_bisector(RrLine l, double a)
	{
		b = l;
		s_angle = a;
	}
}


/**
 * Class to hold and manipulate parametric lines
 */
public class RrLine
{
	private Rr2Point direction;
	private Rr2Point origin;
	
	/**
	 * Line between two points
	 * @param a
	 * @param b
	 */
	public RrLine(Rr2Point a, Rr2Point b)
	{
		origin = new Rr2Point(a);
		direction = Rr2Point.sub(b, a);
	}
	
	/**
	 * Copy constructor
	 * @param r
	 */
	public RrLine(RrLine r)
	{
		origin = new Rr2Point(r.origin);
		direction = new Rr2Point(r.direction);
	}
	
	/**
	 * Make fron an implicit half-plane
	 * @param p
	 */
	public RrLine(RrHalfPlane p)
	{
		origin = new Rr2Point(-p.normal().x()*p.offset(), -p.normal().y()*p.offset());
		direction = new Rr2Point(p.normal().y(), -p.normal().x());
	}
	
	// Convert to a string
	public String toString()
	{
		return "<" + origin.toString() + ", " + direction.toString() + ">";
	}
	
	// Return the contents
	
	public Rr2Point direction() { return direction; }
	public Rr2Point origin() { return origin; }
	
	/**
	 * The point at a given parameter value
	 * @param t
	 * @return
	 */
	public Rr2Point point(double t)
	{
		return Rr2Point.add(origin, Rr2Point.mul(direction, t));
	}
	
	
	/**
	 * Normalise the direction vector
	 */
	public void norm()
	{
		direction = direction.norm();
	}
	
	
	/**
	 * Arithmetic
	 * @return
	 */
	public RrLine neg()
	{
		RrLine a = new RrLine(this);
		a.direction = direction.neg();
		return a;
	}
	
	/**
	 * Move the origin
	 * @param b
	 * @return
	 */
	public RrLine add(Rr2Point b)
	{
		Rr2Point a = Rr2Point.add(origin, b);
		RrLine r = new RrLine(a, Rr2Point.add(a, direction));
		return r;
	}
	
	public RrLine sub(Rr2Point b)
	{
		Rr2Point a = Rr2Point.sub(origin, b);
		RrLine r = new RrLine(a, Rr2Point.add(a, direction));
		return r;
	}
	
	/**
	 * The parameter value where another line crosses
	 * @param a
	 * @return
	 * @throws rr_ParallelLineException
	 */
	public double cross_t(RrLine a) throws rr_ParallelLineException 
	{
		double det = Rr2Point.op(a.direction, direction);
		if (det == 0)
			throw new rr_ParallelLineException("cross_t: parallel lines.");  
		Rr2Point d = Rr2Point.sub(a.origin, origin);
		return Rr2Point.op(a.direction, d)/det;
	}
	
	
	/**
	 * The point where another line crosses
	 * @param a
	 * @return
	 * @throws rr_ParallelLineException
	 */
	public Rr2Point cross_point(RrLine a) throws rr_ParallelLineException
	{
		return point(cross_t(a));
	}
	
	
	/**
	 * The squared distance of a point from a line
	 * @param p
	 * @return
	 */
	public Rr2Point d_2(Rr2Point p)
	{
		double fsq = direction.x()*direction.x();
		double gsq = direction.y()*direction.y();
		double finv = 1.0/(fsq + gsq);
		Rr2Point j0 = Rr2Point.sub(p, origin);
		double fg = direction.x()*direction.y();
		double dx = gsq*j0.x() - fg*j0.y();
		double dy = fsq*j0.y() - fg*j0.x();
		double d2 = (dx*dx + dy*dy)*finv*finv;
		double t = Rr2Point.mul(direction, j0)*finv;
		return new Rr2Point(d2, t);
	}
	
	/**
	 * Normalised line that bisects the angle between
	 * two others joining points a, b, and c and the sine
	 * of the angle it makes with them.
	 * @param a
	 * @param b
	 * @param c
	 * @return
	 */
	static public rr_bisector bisect(Rr2Point a, Rr2Point b, Rr2Point c)
	{
		Rr2Point ab = (Rr2Point.sub(a, b)).norm();
		Rr2Point cb = (Rr2Point.sub(c, b)).norm();
		Rr2Point d = (Rr2Point.add(ab, cb)).norm();
		double ang = Rr2Point.op(ab, d);
		RrLine r = new RrLine(b, Rr2Point.add(b, d));
		return new rr_bisector(r, ang);
	}
}
