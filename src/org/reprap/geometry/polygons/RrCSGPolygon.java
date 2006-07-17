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


package org.reprap.geometry.polygons;

import java.util.*;


/**
 * Small class for containing results of hatch searches
 */
class RrHSearch
{
	public boolean join;
	public boolean notFinished;
	public int theLine;
	public int thePoint;
	public double dsq;
}


/**
 * Polygons as CSG combinations of half spaces with recursive quad-tree
 * division of their containing boxes.
 */
public class RrCSGPolygon
{
	private RrCSG csg;              ///< The polygon
	private RrBox box;              ///< Its enclosing box
	private RrCSGPolygon q1,        ///< Quad tree division - NW
	q2,                             ///< NE 
	q3,                             ///< SE
	q4;                             ///< SW
	private double resolution_2;    ///< Squared diagonal of the smallest box to go to
	private boolean visit1, visit2; ///< Used by the edge-generation software.
	private double sFactor;         ///< Swell factor for division
	private int edgeCount;          ///< Number of edges in the box
	private boolean corner;         ///< Is this box a vertex?
	private RrInterval i1, i2;      ///< Edge parametric intervals
	private Rr2Point vertex;        ///< The vertex, if it exists
	
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
		visit1 = false;
		visit2 = false;
		sFactor = 1;
		edgeCount = 0;
		corner = false;
		vertex = null;
		i1 = new RrInterval();
		i2 = new RrInterval();
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

		visit1 = p.visit1;
		visit2 = p.visit2;
		sFactor = p.sFactor;
		edgeCount = p.edgeCount;
		corner = p.corner;
		vertex = new Rr2Point(p.vertex);
	}
	
	// get children etc
	
	public RrCSGPolygon c_1() { return q1; }
	public RrCSGPolygon c_2() { return q2; }
	public RrCSGPolygon c_3() { return q3; }
	public RrCSGPolygon c_4() { return q4; }
	public RrCSG csg() { return csg; }
	public RrBox box() { return box; }
	public double resolution() { return resolution_2; }
	public double swell() { return sFactor; }
	public int edges() { return edgeCount; }
	public boolean corner() { return corner; }
	public Rr2Point vertex() { return vertex; }
	public RrInterval interval1() { return i1; } 
	public RrInterval interval2() { return i2; } 
	
	
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
		sFactor = swell;
		
		// Anything as simple as a single corner, evaluate and go home
		
		if(csg.complexity() < 3)
		{
			evaluate();
			return;
		}
		
		// Too small a box?
		
		if(box.d_2() < resolution_2)
		{
			System.err.println("RrCSGPolygon.divide(): hit resolution limit!");
			csg = RrCSG.nothing();  // Throw it away!  (It is small...)
			return;
		}
		
		// For comlexities of 4 or less, check if regularization throws
		// some away.
		
		if(csg.complexity() < 5)
		{
			csg = csg.regularise();
			if(csg.complexity() < 3)
			{
				evaluate();
				return;
			}
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
		s = s.scale(sFactor);
		q1 = new RrCSGPolygon(csg.prune(s), s);
		
		s = new RrBox(cen, ne);
		s = s.scale(sFactor);
		q2 = new RrCSGPolygon(csg.prune(s), s);
		
		s = new RrBox(Rr2Point.mul(Rr2Point.add(sw, se), 0.5), 
				Rr2Point.mul(Rr2Point.add(se, ne), 0.5));
		s = s.scale(sFactor);
		q3 = new RrCSGPolygon(csg.prune(s), s);
		
		s = new RrBox(sw, cen);
		s = s.scale(sFactor);
		q4 = new RrCSGPolygon(csg.prune(s), s);
		
		// Recursively divide the children
		
		q1.divide(resolution_2, sFactor);
		q2.divide(resolution_2, sFactor);
		q3.divide(resolution_2, sFactor);
		q4.divide(resolution_2, sFactor);
	}
	
	/**
	 * Generate the edges (if any) in a leaf quad
	 */
	public void evaluate()
	{
		edgeCount = 0;
		corner = false;
		vertex = null;
		
		switch(csg.complexity())
		{
		case 0:
			return;
			
			// One half-plane in the box:
			
		case 1:
			i1 = RrInterval.big_interval();
			i1 = box.wipe(csg.plane().pLine(), i1);
			if(i1.empty()) 
				return;
			edgeCount = 1;
			return;
			
			// Two - maybe a corner, or they may not intersect
			
		case 2:
			i1 = RrInterval.big_interval();
			i1 = box.wipe(csg.c_1().plane().pLine(), i1);
			
			i2 = RrInterval.big_interval();
			i2 = box.wipe(csg.c_2().plane().pLine(), i2);
			
			if(csg.operator() == RrCSGOp.INTERSECTION)
			{
				i2 = csg.c_1().plane().wipe(csg.c_2().plane().pLine(), i2);
				i1 = csg.c_2().plane().wipe(csg.c_1().plane().pLine(), i1);
			} else
			{
				i2 = csg.c_1().plane().complement().wipe(
						csg.c_2().plane().pLine(), i2);
				i1 = csg.c_2().plane().complement().wipe(
						csg.c_1().plane().pLine(), i1);                    
			}
			
			if(!i1.empty())
				edgeCount++;
			if(!i2.empty())
				edgeCount++;
			
			try
			{
				vertex = csg.c_1().plane().cross_point(csg.c_2().plane());
				if(box.point_relative(vertex) == 0)
				{
					corner = true;
				} else
				{
					corner = false;
					vertex = null;
				}
			} catch (RrParallelLineException ple)
			{
				corner = false;
				vertex = null;
			}
			return;
			
		default:
			System.err.println("RrCSGPolygon.evaluate(): complexity > 2.");
		}
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
	 * If the old polygon was divided, the new one will be too.
	 * If we shrink out of existence, a standard null object is returned.
	 * @param d
	 * @return
	 */
	public RrCSGPolygon offset(double d)
	{
		RrBox b;
		if(-d >= 0.5*box.x().length() || -d >= 0.5*box.y().length())
		{
			b = new RrBox(new Rr2Point(0,0), new Rr2Point(1,1));
			return new RrCSGPolygon(RrCSG.nothing(), b);
		}
		Rr2Point p = new Rr2Point(Math.sqrt(2)*d, Math.sqrt(2)*d);
		b = new RrBox( Rr2Point.sub(box.sw(), p), Rr2Point.add(box.ne(), p) );
		RrCSGPolygon result = new RrCSGPolygon(csg.offset(d), b);
		if(q1 != null)
			result.divide(resolution_2, sFactor);
		return result;
	}
	  
	 /**
	 * Walk the tree setting visited flags false
     */
    private void clearVisited(boolean v1, boolean v2)
    {
    	if(v1)
    		visit1 = false;
    	if(v2)
    		visit2 = false;
    	
    	if(q1 != null)
    	{
    		q1.clearVisited(v1, v2);
    		q2.clearVisited(v1, v2);
    		q3.clearVisited(v1, v2);
    		q4.clearVisited(v1, v2);    		
    	}
    }
    
	 /**
	 * Walk the tree to find an unvisited corner
     */
    private RrCSGPolygon findCorner(boolean v1, boolean v2)
    {
    	RrCSGPolygon result = null;
    	
    	if(corner && !(visit1 && v1) && !(visit2 && v2))
    		return this;
 
    	if(q1 != null)
    	{
    		result = q1.findCorner(v1, v2);
    		if(result != null)
    			return result;
       		result = q2.findCorner(v1, v2);
    		if(result != null)
    			return result; 
      		result = q3.findCorner(v1, v2);
    		if(result != null)
    			return result; 
     		result = q4.findCorner(v1, v2);
    		if(result != null)
    			return result;   		
    	}
    	
    	return result;
    }
        
    /**
	 * Find the polygon starting at...
	 * @param corner
	 * @param flag
     * @return the polygon 
     */
    public RrPolygon meg(int flag)
    {
    	RrPolygon result = new RrPolygon();
    	
    	RrCSGPolygon c = this;
    	RrHalfPlane now, next;
    	now = csg.c_1().plane();
    	if(now.find(c)%2 == 1)  // Subtle, or what?
    		now = csg.c_2().plane();
    	int nextIndex;
    	do
    	{
    		if(!c.corner)
    			System.err.println("RrCSGPolygon.meg(): visiting non-corner quad!");
    		result.add(c.vertex, flag);
    		c.visit2 = true;
    		nextIndex = now.find(c) + 1;
    		c = now.quad(nextIndex);
    		next = c.csg.c_1().plane();
    		if(next == now)
    			next = c.csg.c_2().plane();
    		now = next;
    	} while (c != this);
    	
    	return result;
    }
    
    /**
	 * For each half plane remove any existing crossing list.
     */
    private static void clearCrossings(RrCSG c)
    {
    	if(c.complexity() > 1)
    	{
    		clearCrossings(c.c_1());
    		clearCrossings(c.c_2());
    	} else
    	{
    		if(c.operator() == RrCSGOp.LEAF)
    			c.plane().removeCrossings();
    	}
    }
    
    
    /**
	 * For each half plane sort the crossing list.
     */
    private static void sortCrossings(RrCSG c)
    {
    	if(c.complexity() > 1)
    	{
    		sortCrossings(c.c_1());
    		sortCrossings(c.c_2());
    	} else
    	{
    		if(c.operator() == RrCSGOp.LEAF)
    			c.plane().sortCrossings(true);
    	}	
    }
    
    /**
	 * For each half plane record all the others that form
	 * a corner with it.
     */
    private void setCrossingLists()
    {
    	clearCrossings(csg);
    	RrCSGPolygon vtx = findCorner(true, true);
    	while(vtx != null)
    	{
    		if(!RrHalfPlane.cross(vtx))
    			vtx.visit1 = true;
    		vtx.visit2 = true;
    		vtx = findCorner(true, true);
    	}
    	clearVisited(false, true);
    	sortCrossings(csg);
    }
    
    /**
	 * Find all the polygons represented by a CSG object
	 * @param fg
	 * @param fs
     * @return a polygon list as the result
     */
    public RrPolygonList megList(int fg, int fs)
    {
    	clearVisited(true, true);
    	setCrossingLists();
    	RrPolygonList result = new RrPolygonList();
    	RrPolygon m;
    	
    	RrCSGPolygon vtx = findCorner(true, true);
    	while(vtx != null)
    	{
    		m = vtx.meg(fg);
    		if(m.size() > 0)
    		{
    			m.flag(0, fs);
    			if(m.size() > 2)
    				result.add(m);
    			else
    				System.err.println("megList(): polygon with < 3 sides!");
    		}
    		vtx = findCorner(true, true);
    	}
    	
    	return result;
    }
	
	/**
	 * Intersect a line with a polygon, adding to an existing
	 * unsorted list of the intersection parameters - recursive
	 * internal call.
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
				}catch (RrParallelLineException ple)
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
				}catch (RrParallelLineException ple)
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
				}catch (RrParallelLineException ple)
				{}
				break;
				
			default:
				System.err.println("pl_intersect_r(): complicated quad ignored. Complexity: " +
						Integer.toString(csg.complexity()));
			}
		}
		
		return t;
	}
	
	/**
	 * Take a sorted list of parameter values and a line, and
	 * make sure they alternate solid/void/solid etc.  Insert
	 * duplicate parameter values if need be to ensure this. 
	 * @param t
	 * @param l0
	 */
	private void solidSet(List t, RrLine l0)
	{
		
		double half, v;
		int i = 0;
		boolean odd = true;
		while(i < t.size()-1)
		{
			half = 0.5*(((Double)(t.get(i))).doubleValue() + 
				((Double)(t.get(i+1))).doubleValue());
			v = value(l0.point(half));
			if(odd)
			{
				if(v > 0)
					t.add(i, t.get(i));
			} else
			{
				if(v <= 0)
					t.add(i, t.get(i));
			}
			odd = !odd;
			i++;
		}
		if (t.size()%2 != 0)    // Nasty hack that seems to work...
			t.remove(t.size() - 1);
	}
	
	/**
	 * Intersect a line with a polygon, giving a list of the 
	 * intersection parameters.
	 * @param l0
	 * @param big_wipe
	 * @return a sorted list of parameter values.
	 */
	public List pl_intersect(RrLine l0, boolean big_wipe)
	{
		if(q1 == null)
		{
			System.err.println("pl_intersect(): Ray intersection with undivided polygon!  Making it up...");
			double r2 = box.d_2()*1.0e-8;
			divide(r2, 1);
		}
		List t = new ArrayList();	
		t = pl_intersect_r(l0, t, big_wipe);
		java.util.Collections.sort(t);
		if(big_wipe)
			solidSet(t, l0);
		return t;
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
	
	
	private RrHSearch search_line(RrPolygon pg, Rr2Point here, double ds)
	{
		RrHSearch result = new RrHSearch();
		result.thePoint = -1;
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
						result.thePoint = i;
						result.dsq = d2;
					} 
				}
				there = pg.point(i+1);
				d2 = Rr2Point.d_2(here, there);
				if(d2 < result.dsq) 
				{
					if(all_inside(here, there)) 
					{
						result.thePoint = i + 1;
						result.dsq = d2;
						
					}
				}
			}
		}
		return result;
	}
	
	private RrHSearch search_join(RrPolygonList p_l, Rr2Point here, int line)
	{
		RrHSearch result = new RrHSearch();
		RrHSearch intermediate;
		result.notFinished = true;
		result.join = false;
		double dsq = Double.POSITIVE_INFINITY;
		int thePoint = -1;
		int theLine = -1;
		RrPolygon pg;
		
		if(line > 0) 
		{
			pg = p_l.polygon(line - 1);
			intermediate = search_line(pg, here, dsq);
			if(intermediate.thePoint >= 0) 
			{
				dsq = intermediate.dsq;
				theLine = line - 1;
				thePoint = intermediate.thePoint;
			}
		}
		if(line < p_l.size() - 1) 
		{
			pg = p_l.polygon(line + 1);
			intermediate = search_line(pg, here, dsq);
			if(intermediate.thePoint >= 0) 
			{
				dsq = intermediate.dsq;
				theLine = line + 1;
				thePoint = intermediate.thePoint;
			}
		}
		
		if(thePoint >= 0)
		{
			result.join = true;
			result.theLine = theLine;
			result.thePoint = thePoint;
		}
		
		return result;
	}
	
	private RrHSearch new_start(RrPolygonList p_l, Rr2Point here)
	{
		RrHSearch result = new RrHSearch();
		result.notFinished = false;        
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
						result.notFinished = true;
						result.theLine = i;
						result.thePoint = j;   
					}
					there = pg.point(j + 1);
					d2 = Rr2Point.d_2(here, there);
					if(d2 < result.dsq) 
					{
						result.dsq = d2;
						result.notFinished = true;
						result.theLine = i;
						result.thePoint = j + 1;   
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
		
		RrPolygonList p_l = hatch(l0, gap, fg, fs);
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
		
		RrHSearch next = new_start(p_l, here);
		int line = next.theLine;
		boolean notFinished = next.notFinished;
		next.join = true;
		int flag = fs;
		
		while(notFinished) 
		{
			if(next.join) 
			{
				if(next.thePoint%2 == 1) 
				{
					pa = p_l.polygon(next.theLine).point(next.thePoint);
					pb = p_l.polygon(next.theLine).point(next.thePoint - 1);
					p_l.polygon(next.theLine).flag(next.thePoint - 1, -1);
				} else 
				{
					pa = p_l.polygon(next.theLine).point(next.thePoint);
					p_l.polygon(next.theLine).flag(next.thePoint, -1);
					pb = p_l.polygon(next.theLine).point(next.thePoint + 1);
				}
				p1.add(pa, flag);
				flag = fg;
				here = pb;
				line = next.theLine;
				p1.add(here, flag);
			} else 
			{
				flag = fs;
				next = new_start(p_l, here);
				notFinished = next.notFinished;
				if(notFinished) 
				{
					if(next.thePoint%2 == 1) 
					{
						pa = p_l.polygon(next.theLine).point(next.thePoint);
						pb = p_l.polygon(next.theLine).point(next.thePoint - 1);
						p_l.polygon(next.theLine).flag(next.thePoint - 1, -1);
					} else 
					{
						pa = p_l.polygon(next.theLine).point(next.thePoint);
						p_l.polygon(next.theLine).flag(next.thePoint, -1);
						pb = p_l.polygon(next.theLine).point(next.thePoint + 1);
					}
					p1.add(pa, flag);
					flag = fg;
					here = pb;
					line = next.theLine;
					p1.add(here, flag);
				}
			}
			if(notFinished)
				next = search_join(p_l, here, line);
		}
		
		return p1;
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
				result.add(r);
			}
			hatcher = hatcher.add(orth);
			g = g + gap;
		}
		return result;
	}
	
	private RrPolygonList remainder(List tList, RrLine l, int fg)
	{
		RrPolygonList segments = new RrPolygonList();
		RrPolygon r;
		for(int j = 0; j < tList.size(); j += 2)
		{
			r = new RrPolygon();
			r.add(l.point(lEntry(tList, j)), fg);
			r.add(l.point(lEntry(tList, j+1)), fg);
			segments.add(r);
		}
		return segments;
	}
	
	private static double lEntry(List tList, int i)
	{
		return ((Double)(tList.get(i))).doubleValue();
	}
	
	/**
	 * Hatch a csg polygon parallel to line l0 with index gap
	 * @param l0
	 * @param gap
	 * @param fg
	 * @param fs
	 * @return a polygon list as the result with flag values f
	 */
	public RrPolygonList newHatch(RrLine l0, double gap, int fg, int fs)
	{
		RrBox big = box.scale(1.1);
		double d = Math.sqrt(big.d_2());
		
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
				
		orth = Rr2Point.mul(orth, gap);
		
		RrLine hatcher = new RrLine(org, Rr2Point.add(org, l0.direction()));

		
		List hatchTs = new ArrayList();
		List hatchLs = new ArrayList();
		
		while (g < d)
		{
			hatchLs.add(hatcher);
			hatchTs.add(pl_intersect(hatcher, true));
			hatcher = hatcher.add(orth);
			g = g + gap;
		}

		RrPolygonList snakes = new RrPolygonList();
		RrPolygon s;
		
		for(int i = 0; i < hatchLs.size(); i++)
		{
			RrLine l = (RrLine)hatchLs.get(i);
			List tList = (List)hatchTs.get(i);
			if(tList.size() > 0)
			{
				if(snakes.size() <= 0)
					snakes.add(remainder(tList, l, fg));
				else
				{
					for(int j = 0; j < snakes.size(); j++)
					{
						s = snakes.polygon(j);
						Rr2Point end1 = s.point(s.size() - 1);
						int newSeg = -1;
						double d1 = Double.POSITIVE_INFINITY;
						double d2;
						Rr2Point end2;
						for(int k = 0; k < tList.size(); k++)
						{
							end2 = l.point(lEntry(tList, k));
							if(all_inside(end1, end2))
							{
								d2 = Rr2Point.d_2(end1, end2);
								if(d2 < d1)
								{
									d1 = d2;
									newSeg = k;
								}
							}
						}
						if(newSeg >= 0)
						{
							if(newSeg%2 == 0)
							{
								s.add(l.point(lEntry(tList, newSeg)), fg);
								s.add(l.point(lEntry(tList, newSeg + 1)), fg);
								tList.remove(newSeg+1);
								tList.remove(newSeg);
							} else
							{
								s.add(l.point(lEntry(tList, newSeg)), fg);
								s.add(l.point(lEntry(tList, newSeg - 1)), fg);
								tList.remove(newSeg);
								tList.remove(newSeg - 1);
							}
						}
					}
					snakes.add(remainder(tList, l, fg));
				}
			}
		}
		for(int j = 0; j < snakes.size(); j++)
		{
			s = snakes.polygon(j);
			s.flag(0, fs);
		}
		return snakes;
	}
}
