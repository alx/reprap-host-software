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

RrCSGPolygon: 2D polygons as boolean combinations of half-planes,
together with spatial quad tree and other tools

First version 14 November 2005

*/

//import java.io.*;

package org.reprap.geometry.polygons;

import java.util.*;


/**
 * Small class for containing results of hatch searches
 */
class rr_h_search
{
	public boolean join;
	public boolean not_finished;
	public int the_line;
	public int the_point;
	public double dsq;
}

/**
 * Polygons as CSG combinations of half spaces with recursive quad-tree
 * division of their containing boxes.
 */
public class RrCSGPolygon
{
	private RrCSG csg;            ///< The polygon
	private RrBox box;            ///< Its enclosing box
	private RrCSGPolygon q1,      ///< Quad tree division - NW
	q2,           ///< NE 
	q3,           ///< SE
	q4;           ///< SW
	private double resolution_2;  ///< Squared diagonal of the smallest box to go to
	
	/**
	 * Set one up
	 * @param p
	 * @param bx
	 */
	public RrCSGPolygon(RrCSG p, RrBox bx)
	{
		csg = p;
		box = new RrBox(bx);
		q1 = null;
		q2 = null;
		q3 = null;
		q4 = null;
		resolution_2 = box.d_2()*1.0e-8;  // Default - set properly
	}
	
	/**
	 * Deep copy
	 * @param p
	 */
	public RrCSGPolygon(RrCSGPolygon p)
	{
		csg = new RrCSG(p.csg);
		box = new RrBox(p.box);
		if(p.q1 != null)
		{
			q1 = new RrCSGPolygon(p.q1);
			q2 = new RrCSGPolygon(p.q2);
			q3 = new RrCSGPolygon(p.q3);
			q4 = new RrCSGPolygon(p.q4);
			resolution_2 = p.resolution_2;
		} else
		{
			q1 = null;
			q2 = null;
			q3 = null;
			q4 = null;
			resolution_2 = p.resolution_2;
		}
	}
	
	// get children etc
	
	public RrCSGPolygon c_1() { return q1; }
	public RrCSGPolygon c_2() { return q2; }
	public RrCSGPolygon c_3() { return q3; }
	public RrCSGPolygon c_4() { return q4; }
	public RrCSG csg() { return csg; }
	public RrBox box() { return box; }
	public double resolution() { return resolution_2; }
	
	
	/**
	 * Convert to a string - internal recursive call
	 * @param quad
	 */
	private String toString_r(String quad)
	{
		if(csg.operator() == RrCSGOp.UNIVERSE)
			quad = quad + "U";
		else
			quad = quad + Integer.toString(csg.complexity());
		
		if(q1 == null)
		{
			String result = quad + "\n";
			return result;
		} else
		{
			return(q1.toString_r(quad + ":NW-") + 
					q2.toString_r(quad + ":NE-") +
					q3.toString_r(quad + ":SE-") +
					q4.toString_r(quad + ":SW-"));
		}      
	}
	
	/**
	 * Convert to a string
	 */	
	public String toString()
	{
		return "RrCSGPolygon\n" + toString_r(":-");
	}
	
	/**
	 * Quad-tree division
	 * @param res_2
	 * @param swell
	 */
	public void divide(double res_2, double swell)
	{
		resolution_2 = res_2;
		
		// Anything as simple as a single corner, do nothing
		
		if(csg.complexity() < 3) 
			return;
		
		// Too small a box?
		
		if(box.d_2() < resolution_2)
			return;
		
		// For comlexities of 4 or less, check if regularization throws
		// some away.
		
		if(csg.complexity() < 5)
		{
			csg = csg.regularise();
			if(csg.complexity() < 3)
				return;
		}
		
		// Set up the quad-tree division
		
		Rr2Point sw = box.sw();
		Rr2Point nw = box.nw();
		Rr2Point ne = box.ne();
		Rr2Point se = box.se();
		Rr2Point cen = box.centre();
		
		// Prune the set to the four boxes, and put the results in the children
		
		RrBox s = new RrBox(Rr2Point.mul(Rr2Point.add(sw, nw), 0.5), 
				Rr2Point.mul(Rr2Point.add(nw, ne), 0.5));
		s = s.scale(swell);
		q1 = new RrCSGPolygon(csg.prune(s), s);
		
		s = new RrBox(cen, ne);
		s = s.scale(swell);
		q2 = new RrCSGPolygon(csg.prune(s), s);
		
		s = new RrBox(Rr2Point.mul(Rr2Point.add(sw, se), 0.5), 
				Rr2Point.mul(Rr2Point.add(se, ne), 0.5));
		s = s.scale(swell);
		q3 = new RrCSGPolygon(csg.prune(s), s);
		
		s = new RrBox(sw, cen);
		s = s.scale(swell);
		q4 = new RrCSGPolygon(csg.prune(s), s);
		
		// Recursively divide the children
		
		q1.divide(resolution_2, swell);
		q2.divide(resolution_2, swell);
		q3.divide(resolution_2, swell);
		q4.divide(resolution_2, swell);
	}
	
	/**
	 * Find the quad containing a point
	 * @param p
	 * @return
	 */
	public RrCSGPolygon quad(Rr2Point p)
	{
		if(q1 == null)
		{
			if(box.point_relative(p) != 0)
				System.err.println("find_quad(): point not in the box.");
		} else
		{
			Rr2Point cen = box.centre();
			if(p.x() >= cen.x())
			{
				if(p.y() >= cen.y())
					return(q2.quad(p));
				else
					return(q3.quad(p));
			} else
			{
				if(p.y() >= cen.y())
					return(q1.quad(p));
				else
					return(q4.quad(p));               
			}
		}
		
		return this;
	}
	
	
	/**
	 * Find the RrCSG expression that gives the potentaial at point p.
	 * Note this does NOT find the closest half-plane unless the point
	 * is on a surface.
	 * @param p
	 * @return
	 */
	public RrCSG leaf(Rr2Point p)
	{
		RrCSGPolygon q = quad(p);
		return(q.csg.leaf(p));
	}
	
	/**
	 * Find the potentaial at point p.
	 * @param p
	 * @return
	 */	
	public double value(Rr2Point p)
	{
		RrCSG c = leaf(p);
		return c.value(p);
	}
	
	/**
	 * Offset by a distance; grow or shrink the box by the same amount
	 * NB this assumes an undivided polygon.
	 * @param d
	 * @return
	 */
	public RrCSGPolygon offset(double d)
	{
		Rr2Point p = new Rr2Point(Math.sqrt(2)*d, Math.sqrt(2)*d);
		RrBox b = new RrBox( Rr2Point.add(box.ne(), p), Rr2Point.sub(box.sw(), p) );
		return new RrCSGPolygon(csg.offset(d), b);
	}
	
	 /**
	 * Find the nearest direction along the edge hp to direction
     * @param leaf
     * @param direction
     * @param result
     * @return vector in the edge and the inner product
     */
	private double nearest(RrHalfPlane hp, Rr2Point direction, 
			Rr2Point result)
	{	
		Rr2Point p = hp.normal().orthogonal();
		Rr2Point n = p.neg();
		double vp = Rr2Point.mul(p, direction);
		double vn = Rr2Point.mul(n, direction);
		if(vp > vn)
		{
			result.set(p);
			return vp;
		} else
		{
			result.set(n);
			return vn;
		}
	}
	
	 /**
	 * Find the nearest direction from a corner two to direction
     * @param two
     * @param here
     * @param direction
     * @return vector in the edge and the inner product
     */
	private double nearest(RrCSG two, Rr2Point here, 
			Rr2Point direction, Rr2Point result)
	{	
		if(two.complexity() != 2)
		{
			System.err.println("nearest(): not a corner!");
			result.set(direction);
			return -1;
		}
		Rr2Point p1 = new Rr2Point();
		Rr2Point p2 = new Rr2Point();
		double v1 = nearest(two.c_1().plane(), direction, p1);
		if(two.value(Rr2Point.add(here, p1)) > Math.sqrt(resolution_2))
		{
			v1 = -v1;
			p1 = p1.neg();
		}
		double v2 = nearest(two.c_2().plane(), direction, p2);
		if(two.value(Rr2Point.add(here, p2)) > Math.sqrt(resolution_2))
		{
			v2 = -v2;
			p2 = p2.neg();
		}	
		
		if(v1 > v2)
		{
			result.set(p1);
			return v1;
		} else
		{
			result.set(p2);
			return v2;
		}
	}
	
	 /**
	 * Walk round the edges of a polygon from here to there, trying
     * to start roughly in direction.
     * If here and there coincide, then a full circuit is returned.
     * @param here
     * @param there
     * @param direction
     * @return a polygon as the result
     */
    public RrPolygon meg(Rr2Point here, Rr2Point there, Rr2Point direction)
    {
            if(q1 == null)
            {
                    System.err.println("meg(): edge finding in an undivided polygon!  Making it up...");
                    double r2 = box.d_2()*1.0e-8;
                    divide(r2, 1);
            }

            RrPolygon result = new RrPolygon();
            RrCSGPolygon qh = quad(here);
            RrCSGPolygon qt = quad(there);
            int oncount = 0;
            double v;

            RrCSG leaf;

            switch (qh.csg.complexity())
            {
            case 0:
                    System.err.println("meg(): leaf quad with 0 complexity!");
                    return result;

            case 1:
                    v = csg.value(here);
                    if(v*v > resolution_2)
                    {
                            System.err.println("meg(): point not on single surface!");
                            return result;
                    }
                    leaf = csg;
                    oncount = 1;
                    break;

            case 2:
            		v = csg.c_1().value(here);
            		if(v*v <= resolution_2)
            			oncount = 1;
            		v = csg.c_2().value(here);
            		if(v*v <= resolution_2)
            			oncount = oncount + 2;
            		if(oncount < 1 || oncount > 3)
            		{
            			System.err.println("meg(): point not on double surface!");
                    	return result;
            		} else if (oncount == 1)
            			leaf = csg.c_1();
            		else if (oncount == 2)
            		{
            			leaf = csg.c_2();
            			oncount = 1;
            		} else
            		{
            			// On a corner
            		}
            		
                    break;

            default:
                    System.err.println("meg(): leaf quad with complexity greater than 2!");
                    return result;
            }

            return result;
    }

	
	
	/**
	 * Intersect a line with a polygon, adding to an existing
	 * unsorted list of the intersection parameters
	 * @param l0
	 * @param t
	 * @param big_wipe
	 * @return
	 */
	private List pl_intersect_r(RrLine l0, List t, boolean big_wipe)
	{
		RrInterval range;
		if(big_wipe)
			range = RrInterval.big_interval();
		else
			range = new RrInterval(0, 1);
		range = box.wipe(l0, range);
		if(range.empty())
			return t;
		
		if(q1 != null)
		{
			t = q1.pl_intersect_r(l0, t, big_wipe);
			t = q2.pl_intersect_r(l0, t, big_wipe);
			t = q3.pl_intersect_r(l0, t, big_wipe);
			t = q4.pl_intersect_r(l0, t, big_wipe);
		} else
		{
			double tx, v;
			Rr2Point p;
			
			switch(csg.complexity())
			{
			case 0:
				break;
				
			case 1:
				try
				{
					tx = csg.plane().cross_t(l0);
					if (range.in(tx))
						t.add(new Double(tx));
				}catch (rr_ParallelLineException ple)
				{}
				break;
				
			case 2:
				try
				{
					tx = csg.c_1().plane().cross_t(l0);
					if (range.in(tx))
					{
						p = l0.point(tx);
						v = csg.value(p); 
						if(v*v < resolution_2)
							t.add(new Double(tx));
					}
				}catch (rr_ParallelLineException ple)
				{}
				
				try
				{
					tx = csg.c_2().plane().cross_t(l0);
					if (range.in(tx))
					{
						p = l0.point(tx);
						v = csg.value(p); 
						if(v*v < resolution_2)
							t.add(new Double(tx));
					}
				}catch (rr_ParallelLineException ple)
				{}
				break;
				
			default:
				System.out.println("pl_intersect_r(): complicated quad ignored. Complexity: " +
						Integer.toString(csg.complexity()));
			}
		}
		
		return t;
	}
	
	public List pl_intersect(RrLine l0, boolean big_wipe)
	{
		if(q1 == null)
		{
			System.out.println("pl_intersect(): Ray intersection with undivided polygon!  Making it up...");
			double r2 = box.d_2()*1.0e-8;
			divide(r2, 1);
		}
		List t = new ArrayList();	
		t = pl_intersect_r(l0, t, big_wipe);
		if ((t.size()%2 != 0) && big_wipe)
			System.err.println("pl_intersect(): odd winding number!");
		return t;
	}
	
	
	/**
	 * Hatch a csg polygon parallel to line l0 with index gap
	 * @param l0
	 * @param gap
	 * @param fg
	 * @param fs
	 * @return a polygon list as the result with flag values f
	 */
	public RrPolygonList hatch(RrLine l0, double gap, int fg, int fs)
	{
		RrBox big = box.scale(1.1);
		double d = Math.sqrt(big.d_2());
		RrPolygonList result = new RrPolygonList();
		Rr2Point orth = new Rr2Point(-l0.direction().y(), l0.direction().x());
		orth.norm();
		
		int quad = (int)(2*Math.atan2(orth.y(), orth.x())/Math.PI);
		
		Rr2Point org;
		
		switch(quad)
		{
		case 0:
			org = big.sw();
			break;
			
		case 1:
			org = big.se();
			break;
			
		case 2:
			org = big.ne(); 
			break;
			
		case 3:
			org = big.nw();
			break;
			
		default:
			System.err.println("RrPolygon hatch(): The atan2 function doesn't seem to work...");
		org = big.sw();
		}
		
		double g = 0;
		
		//double proj = (Rr2Point.mul(orth, org) - Rr2Point.mul(orth, l0.origin()))/gap;
		//proj = (double)((int)proj + 1) - proj;
		//org = Rr2Point.add(org, Rr2Point.mul(orth, proj));
		
		orth = Rr2Point.mul(orth, gap);
		
		RrLine hatcher = new RrLine(org, Rr2Point.add(org, l0.direction()));
		RrPolygon r;
		
		while (g < d)
		{
			hatcher = hatcher.neg();
			List t_vals = pl_intersect(hatcher, true);
			if (t_vals.size() > 0)
			{
				r = RrPolygon.rr_t_polygon(t_vals, hatcher, fg, fs);
				result.append(r);
			}
			hatcher = hatcher.add(orth);
			g = g + gap;
		}
		return result;
	}
	
	/**
	 * Is the line between two points wholely within the polygon?
	 * @param here
	 * @param there
	 * @return 
	 */
	private boolean all_inside(Rr2Point here, Rr2Point there)
	{
		// The points are on the surface.  Are they on the _same_
		// surface?  (Suppose there's a gap in it, dummo?...)
		RrCSG ch = leaf(here);
		RrCSG ct = leaf(there);
		if(ch == ct)
			return true;
		
		RrLine line = new RrLine(here, there);
		if(value(line.point(0.5)) > 0) // Need to go round the corner here
			return false;
		
		List t = new ArrayList();	
		t = pl_intersect_r(line, t, false);
		double v;
		double r = Math.sqrt(resolution_2);
		for(int i = 0; i < t.size(); i++)
		{
			v = ((Double)(t.get(i))).doubleValue();
			if((v > r) && (v < 1 - r))
				return false;
		}
		
		return true;
	}
	
	
	private rr_h_search search_line(RrPolygon pg, Rr2Point here, double ds)
	{
		rr_h_search result = new rr_h_search();
		result.the_point = -1;
		result.dsq = ds;
		Rr2Point there;
		double d2;
		
		for(int i = 0; i < pg.size(); i += 2) 
		{
			if(pg.flag(i) >= 0) 
			{
				there = pg.point(i);
				d2 = Rr2Point.d_2(here, there);
				if(d2 < result.dsq) 
				{
					if(all_inside(here, there)) 
					{
						result.the_point = i;
						result.dsq = d2;
					} 
				}
				there = pg.point(i+1);
				d2 = Rr2Point.d_2(here, there);
				if(d2 < result.dsq) 
				{
					if(all_inside(here, there)) 
					{
						result.the_point = i + 1;
						result.dsq = d2;
						
					}
				}
			}
		}
		return result;
	}
	
	private rr_h_search search_join(RrPolygonList p_l, Rr2Point here, int line)
	{
		rr_h_search result = new rr_h_search();
		rr_h_search intermediate;
		result.not_finished = true;
		result.join = false;
		double dsq = Double.POSITIVE_INFINITY;
		int the_point = -1;
		int the_line = -1;
		RrPolygon pg;
		
		if(line > 0) 
		{
			pg = p_l.polygon(line - 1);
			intermediate = search_line(pg, here, dsq);
			if(intermediate.the_point >= 0) 
			{
				dsq = intermediate.dsq;
				the_line = line - 1;
				the_point = intermediate.the_point;
			}
		}
		if(line < p_l.size() - 1) 
		{
			pg = p_l.polygon(line + 1);
			intermediate = search_line(pg, here, dsq);
			if(intermediate.the_point >= 0) 
			{
				dsq = intermediate.dsq;
				the_line = line + 1;
				the_point = intermediate.the_point;
			}
		}
		
		if(the_point >= 0)
		{
			result.join = true;
			result.the_line = the_line;
			result.the_point = the_point;
		}
		
		return result;
	}
	
	private rr_h_search new_start(RrPolygonList p_l, Rr2Point here)
	{
		rr_h_search result = new rr_h_search();
		result.not_finished = false;        
		result.join = false;
		result.dsq = Double.POSITIVE_INFINITY;
		double d2;
		
		for(int i = 0; i < p_l.size(); i++)
		{
			RrPolygon pg = p_l.polygon(i);
			for(int j = 0; j < pg.size(); j += 2)
			{
				if(pg.flag(j) >= 0) 
				{
					Rr2Point there = pg.point(j);
					d2 = Rr2Point.d_2(here, there);
					if(d2 < result.dsq) 
					{
						result.dsq = d2;
						result.not_finished = true;
						result.the_line = i;
						result.the_point = j;   
					}
					there = pg.point(j + 1);
					d2 = Rr2Point.d_2(here, there);
					if(d2 < result.dsq) 
					{
						result.dsq = d2;
						result.not_finished = true;
						result.the_line = i;
						result.the_point = j + 1;   
					}
				}
			}
		}
		
		return result;       
	}
	
	public RrPolygon hatch_join(RrLine l0, double gap, int fg, int fs)
	{
		if(fg < 0 || fs < 0)
			System.err.println("hatch_join(): illegal negative flag value " + 
					Integer.toString(fg) + " or " + Integer.toString(fs));
		
		RrPolygonList p_l = hatch(l0, gap, 1, 0);
		RrPolygon p1 = new RrPolygon();
		
		int leng = p_l.size();
		if(leng <= 0)
			return p1;

		int i = 0;
		RrPolygon here_p = p_l.polygon(i);
		while(here_p.size() <= 0)
		{
			i++;
			if(i >= leng)
				break;
			here_p = p_l.polygon(i);
		}
		
		if(here_p.size() <= 0)
		{
			System.err.println("Nothing in the hatch!");
			return p1;
		}
		
		Rr2Point here = here_p.point(0);
		
		Rr2Point pa, pb;
		
		rr_h_search next = new_start(p_l, here);
		int line = next.the_line;
		boolean not_finished = next.not_finished;
		next.join = true;
		int flag = fs;
		
		while(not_finished) 
		{
			if(next.join) 
			{
				if(next.the_point%2 == 1) 
				{
					pa = p_l.polygon(next.the_line).point(next.the_point);
					pb = p_l.polygon(next.the_line).point(next.the_point - 1);
					p_l.polygon(next.the_line).flag(next.the_point - 1, -1);
				} else 
				{
					pa = p_l.polygon(next.the_line).point(next.the_point);
					p_l.polygon(next.the_line).flag(next.the_point, -1);
					pb = p_l.polygon(next.the_line).point(next.the_point + 1);
				}
				p1.append(pa, flag);
				flag = fg;
				here = pb;
				line = next.the_line;
				p1.append(here, flag);
			} else 
			{
				flag = fs;
				next = new_start(p_l, here);
				not_finished = next.not_finished;
				if(not_finished) 
				{
					if(next.the_point%2 == 1) 
					{
						pa = p_l.polygon(next.the_line).point(next.the_point);
						pb = p_l.polygon(next.the_line).point(next.the_point - 1);
						p_l.polygon(next.the_line).flag(next.the_point - 1, -1);
					} else 
					{
						pa = p_l.polygon(next.the_line).point(next.the_point);
						p_l.polygon(next.the_line).flag(next.the_point, -1);
						pb = p_l.polygon(next.the_line).point(next.the_point + 1);
					}
					p1.append(pa, flag);
					flag = fg;
					here = pb;
					line = next.the_line;
					p1.append(here, flag);
				}
			}
			if(not_finished)
				next = search_join(p_l, here, line);
		}
		
		return p1;
	}
	
}
