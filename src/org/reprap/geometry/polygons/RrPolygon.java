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
 
 
 RrPolygon: 2D polygons
 
 First version 20 May 2005
 This version: 1 May 2006 (Now in CVS - no more comments here)
 
 A polygon is an auto-extending list of Rr2Points.  Its end is 
 sometimes considered to join back to its beginning, depending
 on context.
 
 It also keeps its enclosing box.  
 
 Each point is stored with a flag value.  This can be used to flag the
 point as visited, or to indicate if the subsequent line segment is to
 be plotted etc.
 
 java.awt.Polygon is no use for this because it has integer coordinates.
 
 */

package org.reprap.geometry.polygons;

import java.io.*;
import java.util.*;


/**
 * The main boundary-representation polygon class
 */
public class RrPolygon
{
	public List points;
	public List flags;
	public RrBox box;
	
	/**
	 * Empty polygon
	 */
	public RrPolygon()
	{
		points = new ArrayList();
		flags = new ArrayList();
		box = new RrBox();
	}
	
	// Get the data
	
	public Rr2Point point(int i)
	{
		return new Rr2Point((Rr2Point)points.get(i));
	}
	public int flag(int i)
	{
		return ((Integer)flags.get(i)).intValue();
	}
	
	/**
	 * As a string
	 * @return
	 */
	public String toString()
	{
		String result = " Polygon -  vertices: ";
		result += size() + ", enclosing box: ";
		result += box.toString();
		result += "\n";
		for(int i = 0; i < size(); i++)
			result += point(i).toString();
		
		return result;
	}
	
	/**
	 * Change a flag value
	 * @param i
	 * @param f
	 */
	public void flag(int i, int f)
	{
		flags.set(i, new Integer(f));
	}
	
	/**
	 * Length
	 * @return
	 */
	public int size()
	{
		return points.size();
	}
	
	/**
	 * Deep copy
	 * @param p
	 */
	public RrPolygon(RrPolygon p)
	{
		points = new ArrayList();
		flags = new ArrayList();
		box = new RrBox(p.box);
		for(int i = 0; i < p.size(); i++)
		{
			points.add(new Rr2Point(p.point(i)));
			flags.add(new Integer((p.flag(i)))); 
		}		
	}
	
	
	/**
	 * Add a new point and its flag value to the polygon
	 * @param p
	 * @param f
	 */
	public void add(Rr2Point p, int f)
	{
		points.add(new Rr2Point(p));
		flags.add(new Integer(f));
		box.expand(p);
	}
	
	/**
	 * Put a new polygon and its flag values on the end
	 * @param p
	 */
	public void add(RrPolygon p)
	{
		if(p.size() == 0)
			return;
		for(int i = 0; i < p.size(); i++)
		{
			points.add(new Rr2Point(p.point(i)));
			flags.add(new Integer(p.flag(i))); 
		}
		box.expand(p.box);
	}
	
	
	/**
	 * Remove a point.
	 * N.B. This does not ammend the enclosing box
	 * @param i
	 */
	public void remove(int i)
	{
		points.remove(i);
		flags.remove(i);
	}
	
	/**
	 * Recompute the box (sometimes useful if points have been deleted) 
	 */
	public void re_box()
	{
		box = new RrBox();
		int leng = size();
		for(int i = 0; i < leng; i++)
		{
			box.expand((Rr2Point)points.get(i)); 
		}
	}
	
	
	/**
	 * Output the polygon in SVG XML format
	 * @param opf
	 */
	public void svg(PrintStream opf)
	{
		opf.println("<polygon points=\"");
		int leng = size();
		for(int i = 0; i < leng; i++)
			opf.println(Double.toString((point(i)).x()) + "," 
					+ Double.toString((point(i)).y()));
		opf.println("\" />");
	}
		
	/**
	 * Negate (i.e. reverse cyclic order)
	 * @return
	 */
	public RrPolygon negate()
	{
		RrPolygon result = new RrPolygon();
		for(int i = size() - 1; i >= 0; i--)
			result.add(point(i), flag(i));
		return result;
	}
	
	/**
	 * Signed area (-ve result means polygon goes anti-clockwise)
	 * @return
	 */
	public double area()
	{
		double a = 0;
		Rr2Point p, q;
		int j;
		for(int i = 1; i < size() - 1; i++)
		{
			j = i + 1;
			p = Rr2Point.sub(point(i), point(0));
			q = Rr2Point.sub(point(j), point(0));
			a += Rr2Point.op(q, p);
		} 
		return a*0.5;
	}
	

	
	/**
	 * Simplify a polygon by deleting points from it that
	 * are closer than d to lines joining other points
	 * @param d
	 * @return
	 */
	public RrPolygon simplify(double d)
	{
		if(size() <= 3)
			return new RrPolygon(this);
		RrPolygon r = new RrPolygon();
		int leng = size();
		double d2 = d*d;
		int i = 0;
		int jold = 0;
		while(i < leng - 1)
		{
			r.add(point(i), flag(i));
			int j = i + 1;
			find_ignored: while (j < leng + 1)
			{
				jold = j;
				j++;
				RrLine line = new RrLine(point(i), point(j%leng));
				for (int k = i+1; k < j; k++)
				{
					if (line.d_2(point(k%leng)).x() > d2)
						break find_ignored;
				}
			}
			i = jold;
		}
		return r;
	}

}


