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
 * Small class to return the results of a polygon's crossing itself
 */
class self_x
{
	public boolean flag;
	public int l0;
	public int l1;
	
	public self_x(boolean f, int a0, int a1)
	{
		flag = f;
		l0 = a0;
		l1 = a1;
	}
}

/**
 * The main polygon class
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
		int leng = p.size();
		for(int i = 0; i < leng; i++)
		{
			points.add(new Rr2Point(p.point(i)));
			flags.add(new Integer((p.flag(i)))); 
		}		
	}
	
	/**
	 * Polygon from a file
	 * @param f_name
	 */
	public RrPolygon(String f_name)
	{
		points = new ArrayList();
		flags = new ArrayList();
		box = new RrBox();
		
		try
		{
			FileInputStream inp =  new FileInputStream(f_name);
			DataInputStream ip = new DataInputStream(inp);
			
			Rr2Point r;
			double x, y;
			
			while(ip.available() != 0)
			{
				x = ip.readDouble();
				y = ip.readDouble();
				r = new Rr2Point(x, y);
				this.add(r, 1);
			}
			
			try
			{
				inp.close();
			}
			catch(IOException err)
			{
				System.err.println("RrPolygon(String): Can't close input file - " + f_name);
			}
		}
		catch(IOException err)
		{
			System.err.println("RrPolygon(String): Can't open input file - " + f_name);
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
		int leng = p.size();
		if(leng == 0)
			return;
		for(int i = 0; i < leng; i++)
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
	 * Test for a self-intersecting polygon.  If the first entry in the
	 * tuple returned is 0 the polygon does not self intersect.  If it is 1
	 * the next two tuple entries give the start of the line segments first
	 * encountered that intersect.  There may be others.
	 * @return
	 */
	public self_x self_cross()
	{
		int k;
		int leng = size();
		
		if(leng < 4)
			return new self_x(false, 0, 0);
		
		for(int i = 0; i < leng; i++)
		{
			int ip = (i + 1) % leng;
			RrLine s1 = new RrLine(point(i), point(ip));
			int m = i + 2;
			if(i > 0)
				k = leng;
			else
				k = leng - 1;
			for(int j = m; j <= k; j++)
			{
				int jp = (j + 1) % leng;
				RrLine s2 = new RrLine(point(j), point(jp));
				try
				{
					double t = s1.cross_t(s2);
					
					if (t >= 0 && t < 1)
					{
						try
						{
							t = s2.cross_t(s1);
							if (t >= 0 && t < 1)
								return new self_x(true, i, j);
						}
						catch(rr_ParallelLineException ple)
						{
							System.err.println("self_cross: A crosses B, but B does not cross A!");
						}
					}
				}
				catch (rr_ParallelLineException ple)
				{}
				
			}
		}
		return new self_x(false, 0, 0);
	}
	
	/**
	 * Remove self-intersections
	 * @return
	 */
	public RrPolygonList no_cross()
	{
		RrPolygonList result = new RrPolygonList();
		result.polygons.add(new RrPolygon(this));
		result.box.expand(box);
		return result;
		// Need some code in here...
	}
	
	/**
	 * Negate (i.e. reverse cyclic order)
	 * @return
	 */
	public RrPolygon negate()
	{
		RrPolygon result = new RrPolygon();
		int leng = size();
		for(int i = 1; i <= leng; i++)
		{
			result.add(point(leng - i), flag(leng - i));
		} 
		return result;
	}
	
	
	/**
	 * Offset a polygon to the right by d
	 * @param d
	 * @return
	 */
	public RrPolygon offset(double d)
	{
		int leng = size();
		RrPolygon r = new RrPolygon();
		int i = leng - 1;
		for (int j = 0; j < leng; j++)
		{
			int k = (j + 1) % leng;
			rr_bisector bs = RrLine.bisect(point(i), point(j), point(k));
			r.add(bs.b.point(d/bs.s_angle), flag(j));
			i = (i + 1) % leng;
		}
		return r;
	}
	
	/**
	 * Intersect a line with a polygon, returning an
	 * unsorted list of the intersection parameters
	 * @param l0
	 * @return
	 */
	public List pl_intersect(RrLine l0)
	{
		int leng = size();
		List t = new ArrayList();
		int it = 0;
		for(int i = 0; i < leng; i++)
		{
			int ip = (i + 1) % leng;
			RrLine l1 = new RrLine(point(i), point(ip));
			try
			{
				double s = l1.cross_t(l0);
				if(s >= 0 && s < 1)
				{
					try
					{
						s = l0.cross_t(l1);
						t.add(new Double(s));
						it++;
					}					
					catch(rr_ParallelLineException ple)
					{
						System.err.println("pl_intersect: A crosses B, but B does not cross A!");
					}
				}
			}
			catch (rr_ParallelLineException ple)
			{}
		}	
		return t;
	}
	
	/**
	 * Simplify a polygon by deleting points from it that
	 * are closer than d to lines joining other points
	 * @param d
	 * @return
	 */
	public RrPolygon simplify(double d)
	{
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
	
	/**
	 * Take a sorted list of parameter values and a line, and 
	 * turn them into a polygon.  Use the trace
	 * value to flag the start of solid lines.
	 * @param t
	 * @param line
	 * @param fg
	 * @param fs
	 * @return
	 */
	public static RrPolygon rr_t_polygon(List t, RrLine line, int fg, int fs)
	{
		RrPolygon r = new RrPolygon();
		int leng = t.size();
		for(int i = 0; i < leng-1; i = i+2)
		{
			r.add(line.point(((Double)(t.get(i))).doubleValue()), fg);
			r.add(line.point(((Double)(t.get(i+1))).doubleValue()), fs);
		}
		return r;
	}
	
	
	/**
	 * Figure out if all polygons in a list avoid the parametric interval [0, 1)
	 * in the line ln.
	 * @param ln
	 * @param avoid
	 * @return
	 */
	public boolean no_cross(RrLine ln, RrPolygonList avoid)
	{
		List t_vals = avoid.pl_intersect(ln);
		int leng = t_vals.size();
		for(int i = 0; i < leng; i++)
		{
			double t = ((Double)(t_vals.get(i))).doubleValue();
			if(t >= 0 && t < 1)
				return false;
		}
		return true;
	}
	
	
	/**
	 * Take a gappy polygon (as from the hatch function below)
	 * and join (almost) all the ends up while avoiding the polygons in RrPolygonList
	 * @param avoid
	 * @return
	 */
	public RrPolygon join_up(RrPolygonList avoid)
	{
		RrPolygon old = new RrPolygon(this);
		RrPolygon r = new RrPolygon();
		int i = 0;
		while(i < old.size() - 1)
		{
			Rr2Point p0 = new Rr2Point(old.point(i));
			Rr2Point p1 = new Rr2Point(old.point(i+1));
			int f = old.flag(i);
			if(old.flag(i+1) != 0)
				System.err.println("join_up: non alternating polygon.");
			old.remove(i);
			old.remove(i); // i.e. i+1...
			Rr2Point p3, p_near, p_far;
			int j_near = -1;
			p_near = null;
			p_far = null;
			double d2 = box.d_2();
			int leng = old.points.size();
			for(int j = 0; j < leng; j++)
			{
				p3 = old.point(j);
				double d = Rr2Point.d_2(p3, p0);
				RrLine lin;
				if(d < d2)
				{
					lin = new RrLine(p0, p3);
					if(no_cross(lin, avoid))
					{
						d2 = d;
						p_near = p0;
						p_far = p1;
						j_near = j;
					}
				}
				d = Rr2Point.d_2(p3, p1);
				if(d < d2)
				{
					lin = new RrLine(p1, p3);
					if(no_cross(lin, avoid))
					{
						d2 = d;
						p_near = p1;
						p_far = p0;
						j_near = j;
					}
				}
			}
			if(j_near < 0)
			{
				r.add(p0, f);
				r.add(p1, 0);
			} else
			{
				int j_far;
				if(old.flag(j_near) == 0)
					j_far = (j_near + 1)%leng;
				else
				{
					j_far = j_near - 1;
					if(j_far < 0)
						j_far = leng - 1;
				}
				r.add(p_far, f);
				r.add(p_near, f);
				r.add(old.point(j_near), f);
				r.add(old.point(j_far), f);
				if(j_far > j_near)
				{
					old.remove(j_near);
					if(j_near >= old.points.size())
						old.remove(0);
					else
						old.remove(j_near);
				} else
				{
					old.remove(j_far);
					if(j_far >= old.points.size())
						old.remove(0);
					else
						old.remove(j_far);
				}
			}
			
			i = i + 2;
		}
		return r;
	}
}


