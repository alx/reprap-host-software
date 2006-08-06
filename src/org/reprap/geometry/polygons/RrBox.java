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
 
 RrBox: 2D rectangles
 
 First version 20 May 2005
 This version: 1 May 2006 (Now in CVS - no more comments here)
 
 */

package org.reprap.geometry.polygons;

/**
 * A 2D box is an X and a Y interval
 */
public class RrBox
{
	// Compass directions
	
	public static final byte rr_N = 0x01;
	public static final byte rr_E = 0x02;
	public static final byte rr_S = 0x04;
	public static final byte rr_W = 0x08;
	
	private RrInterval x;   ///< X interval
	private RrInterval y;   ///< Y interval
	private boolean empty;  ///< Anyone home?
	
	/**
	 * Default is empty
	 */ 
	public RrBox()
	{
		empty = true;
	}
	
	/**
	 * For when we need one that has just something in
	 * @param a
	 */
	public RrBox(double a)
	{
		x = new RrInterval(0, a);
		y = new RrInterval(0, a);
		empty = false;
	}
	
	/**
	 * Copy constructor
	 */
	public RrBox(RrBox b)
	{
		x = new RrInterval(b.x);
		y = new RrInterval(b.y);
		empty = b.empty;
	}
	
	/**
	 * Make from the corners
	 * @param sw
	 * @param ne
	 */
	public RrBox(Rr2Point sw, Rr2Point ne)
	{
		x = new RrInterval(sw.x(), ne.x());
		y = new RrInterval(sw.y(), ne.y());
		empty = x.empty() || y.empty();
	}
	
	/**
	 * Return the x interval
	 */
	public RrInterval x() { return x; }
	
	
	/**
	 * Return the y interval
	 * @return
	 */
	public RrInterval y() { return y; }
	
	
	/**
	 * Expand the box to incorporate another box or a point
	 * @param a
	 */
	public void expand(RrBox a)
	{
		if(a.empty)
			return;
		if(empty)
		{
			empty = false;
			x = new RrInterval(a.x);
			y = new RrInterval(a.y);
		} else
		{
			x.expand(a.x);
			y.expand(a.y);
		}
	}
	
	public void expand(Rr2Point a)
	{
		if(empty)
		{
			empty = false;
			x = new RrInterval(a.x(), a.x());
			y = new RrInterval(a.y(), a.y());
		} else
		{
			x.expand(a.x());
			y.expand(a.y());
		}
	}
	
	/**
	 * Corner points and center
	 */
	public Rr2Point ne()
	{
		return new Rr2Point(x.high(), y.high());
	}
	
	public Rr2Point sw()
	{
		return new Rr2Point(x.low(), y.low());
	}
	
	public Rr2Point se()
	{
		return new Rr2Point(x.high(), y.low());
	}
	
	public Rr2Point nw()
	{
		return new Rr2Point(x.low(), y.high());
	}   
	
	public Rr2Point centre()
	{
		return ((Rr2Point.mul(Rr2Point.add(ne(), sw()), 0.5)));
	}
	
	/**
	 * Scale the box by a factor about its center
	 * @param f
	 * @return
	 */
	public RrBox scale(double f)
	{
		RrBox r = new RrBox();
		if(empty)
			return r;
		f = 0.5*f;
		Rr2Point p = new Rr2Point(x.length()*f, y.length()*f);
		Rr2Point c = centre();
		r.expand(Rr2Point.add(c, p));
		r.expand(Rr2Point.sub(c, p));
		return r;
	}
	
	/**
	 * Convert to a string
	 */
	public String toString()
	{
		if(empty)
			return "<empty>";
		return "<BOX x:" + x.toString() + ", y: " + y.toString() + ">";
	}
	
	
	/**
	 * Squared diagonal
	 * @return
	 */
	public double d_2()
	{
		if(empty)
			return 0;
		return Rr2Point.d_2(sw(), ne());
	}

	/**
	 * Squared distance to a point
	 * @return
	 */
	public double d_2(Rr2Point p)
	{
		if(empty)
			return Double.POSITIVE_INFINITY;
		byte b = point_relative(p);
		double d1 = Double.POSITIVE_INFINITY, d2 = Double.POSITIVE_INFINITY;
		switch(b)
		{
		case 0:
			return 0;
			
		case 1:
			d1 = Rr2Point.d_2(p, nw());
			d2 = Rr2Point.d_2(p, ne());
			break;
			
		case 2:
			d1 = Rr2Point.d_2(p, ne());
			d2 = Rr2Point.d_2(p, se());
			break;
			
		case 3:
			return Rr2Point.d_2(p, ne());
			
		case 4:
			d1 = Rr2Point.d_2(p, sw());
			d2 = Rr2Point.d_2(p, se());
			break;
			
		case 6:
			return Rr2Point.d_2(p, se());
			
		case 8:
			d1 = Rr2Point.d_2(p, sw());
			d2 = Rr2Point.d_2(p, nw());
			break;
			
		case 9:
			return Rr2Point.d_2(p, nw());
			
		case 12:
			return Rr2Point.d_2(p, sw());
			
		default:
			System.err.println("RrBox.d_2(): dud value from point_relative()!");	
		}
		if(d2 < d1)
			return d2;
		else
			return d1;
	}
	

	/**
	 * Take a range of parameter values and a line, and find
     * the intersection of that range with the part of the line
     * (if any) in the box.
	 * @param a
	 * @param range
	 * @return
	 */
	public RrInterval wipe(RrLine a, RrInterval oldRange)
	{
		if(oldRange.empty()) return oldRange;
		
		RrInterval range = new RrInterval(oldRange);
		
		RrHalfPlane hp = new RrHalfPlane(sw(), nw());
		range = hp.wipe(a, range);
		if(range.empty()) return range;
		
		hp = new RrHalfPlane(nw(), ne());
		range = hp.wipe(a, range);
		if(range.empty()) return range;
		
		hp = new RrHalfPlane(ne(), se());
		range = hp.wipe(a, range);
		if(range.empty()) return range;
		
		hp = new RrHalfPlane(se(), sw());
		range = hp.wipe(a, range);
		return range;
	}
	
	/**
	 * Where is a point relative to a box?
	 * @param p
	 * @return
	 */
	public byte point_relative(Rr2Point p)
	{
		byte result = 0;
		if(p.x() >= x.high())
			result |= rr_E;
		if(p.x() < x.low())
			result |= rr_W;
		if(p.y() >= y.high())
			result |= rr_N;
		if(p.y() < y.low())
			result |= rr_S;        
		return result;
	}
}

