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
 This version: 2 October 2005 (translation to Java)
 
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
	 * Take a range of parameter values and a line, and find
     * the intersection of that range with the part of the line
     * (if any) in the box.
	 * @param a
	 * @param range
	 * @return
	 */
	public RrInterval wipe(RrLine a, RrInterval range)
	{
		if(range.empty()) return range;
		
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
		if(p.x() > x.high())
			result |= rr_E;
		if(p.x() < x.low())
			result |= rr_W;
		if(p.y() > y.high())
			result |= rr_N;
		if(p.y() < y.low())
			result |= rr_S;        
		return result;
	}
}

