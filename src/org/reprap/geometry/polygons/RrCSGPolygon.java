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

import java.util.ArrayList;
import java.util.List;
import org.reprap.Preferences;

class snakeEnd
{
	public RrPolygon p;
	public RrHalfPlane h;
	public int index;
	
	snakeEnd(RrPolygon pl, RrHalfPlane hs, int i)
	{
		p = pl;
		h = hs;
		index = i;
	}
}

/**
 * Polygons as  CSG combinations of half spaces with recursive quad-tree
 * division of their containing boxes.
 * 
 * TO DO: Change the quad tree to a BSP tree?
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
		box = new RrBox(bx);
		q1 = null;
		q2 = null;
		q3 = null;
		q4 = null;
		resolution_2 = box.d_2()*Preferences.tiny();
		csg = p;
		visit1 = false;
		visit2 = false;
		sFactor = Preferences.swell();
		edgeCount = 0;
		corner = false;
		vertex = null;
		i1 = new RrInterval();
		i2 = new RrInterval();
	}
		
	// get children etc
	
	public RrCSGPolygon c_1() { return q1; }
	public RrCSGPolygon c_2() { return q2; }
	public RrCSGPolygon c_3() { return q3; }
	public RrCSGPolygon c_4() { return q4; }
	public RrCSG csg() { return csg; }
	public RrBox box() { return box; }
	public double resolution2() { return resolution_2; }
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
	 * Quad-tree division - recursive internal call
	 * @param res_2
	 * @param swell
	 */
	private void divide_r(double res_2, double swell)
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
			System.err.println("RrCSGPolygon.divide(): hit resolution limit!  Complexity: " +
					csg.complexity());
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
		double addX = 0.5*(ne.x() - sw.x())*(sFactor - 1);
		double addY = 0.5*(ne.y() - sw.y())*(sFactor - 1);
		
		// Prune the set to the four boxes, and put the results in the children
		
		Rr2Point newSW = Rr2Point.mul(Rr2Point.add(sw, nw), 0.5);
		Rr2Point newNE = Rr2Point.mul(Rr2Point.add(nw, ne), 0.5);
		RrBox s = new RrBox(Rr2Point.add(newSW, new Rr2Point(0, -addY)), 
				Rr2Point.add(newNE, new Rr2Point(addX, 0)));
		q1 = new RrCSGPolygon(csg.prune(s), s);
		
		s = new RrBox(Rr2Point.add(cen, new Rr2Point(-addX, -addY)), 
				ne);
		q2 = new RrCSGPolygon(csg.prune(s), s);
		
		newSW = Rr2Point.mul(Rr2Point.add(sw, se), 0.5);
		newNE = Rr2Point.mul(Rr2Point.add(se, ne), 0.5);
		s = new RrBox(Rr2Point.add(newSW, new Rr2Point(-addX, 0)), 
				Rr2Point.add(newNE, new Rr2Point(0, addY)));		
		q3 = new RrCSGPolygon(csg.prune(s), s);
		
		s = new RrBox(sw, 
				Rr2Point.add(cen, new Rr2Point(addX, addY)));		
		q4 = new RrCSGPolygon(csg.prune(s), s);
		
		// Recursively divide the children
		
		q1.divide_r(resolution_2, sFactor);
		q2.divide_r(resolution_2, sFactor);
		q3.divide_r(resolution_2, sFactor);
		q4.divide_r(resolution_2, sFactor);
	}
	
	/**
	 * Divide the CSG polygon into a quad tree, each leaf of
	 * which contains at most two planes.
	 * Evaluate the leaves, and store lists of intersections with
	 * the half-planes.
	 * @param res_2
	 * @param swell
	 */
	public void divide(double res_2, double swell)
	{
		csg = csg.simplify(Math.sqrt(res_2));
		csg.clearCrossings();
		divide_r(res_2, swell);
		csg.sortCrossings(true, this);
	}
	
	/**
	 * Generate the edges (if any) in a leaf quad
	 */
	public void evaluate()
	{
		edgeCount = 0;
		corner = false;
		vertex = null;
		
		switch(csg.operator())
		{
		case RrCSGOp.NULL:
		case RrCSGOp.UNIVERSE:	
			return;
			
			// One half-plane in the box:
			
		case RrCSGOp.LEAF:
			i1 = RrInterval.big_interval();
			i1 = box.wipe(csg.plane().pLine(), i1);
			if(i1.empty()) 
				return;
			edgeCount = 1;
			return;
			
			// Two - maybe a corner, or they may not intersect
			
		case RrCSGOp.UNION:
		case RrCSGOp.INTERSECTION:
			if(csg.complexity() != 2)
			{
				System.err.println("RrCSGPolygon.evaluate(): complexity: " + 
					csg.complexity());
				return;
			}
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
			
			// NB if the corner was in another box and this one (because of swell
			// overlap) only the first gets recorded.
			
			if(corner)
				corner = RrHalfPlane.cross(this);
			return;
			
		default:
			System.err.println("RrCSGPolygon.evaluate(): dud CSG operator!");
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
				System.err.println("RrCSGPolygon.quad(): point not in the box.");
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
	 * Find the potential at point p.
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
		RrCSG expression = csg.offset(d);
		expression = expression.simplify(Math.sqrt(resolution_2));
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
	 * Find the polygon starting at this quad
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
    	
    	if(now.find(c)%2 == 1)
    	{
    		System.err.println("RrCSGPolygon.meg(): end convergence!");
    		return result;
    	}
    	
    	int nextIndex;
    	do
    	{
    		if(!c.corner)
    			System.err.println("RrCSGPolygon.meg(): visiting non-corner quad!");
    		
    		result.add(c.vertex, flag);
    		c.visit2 = true;
    		nextIndex = now.find(c) + 1;
    		
    		if(nextIndex < 0 | nextIndex >= now.size())
    			System.err.println("RrCSGPolygon.meg(): fallen off the end of the line!");
    		
    		c = now.getQuad(nextIndex);
    		next = c.csg.c_1().plane();
    		if(next == now)
    			next = c.csg.c_2().plane();
    		now = next;
    	} while (c != this);
    	
    	return result;
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
     * Intersect a line with a polygon - recursive internal call
     * @param hp
     * @param range
     */
    private void lineIntersect_r(RrHalfPlane hp, RrInterval range)
	{
    	RrInterval newRange = box.wipe(hp.pLine(), range);
		if(newRange.empty())
			return;
		
		if(q1 != null)
		{
			q1.lineIntersect_r(hp, newRange);
			q2.lineIntersect_r(hp, newRange);
			q3.lineIntersect_r(hp, newRange);
			q4.lineIntersect_r(hp, newRange);
		} else
		{			
			switch(csg.operator())
			{
			case RrCSGOp.NULL:
			case RrCSGOp.UNIVERSE:	
				break;
				
			case RrCSGOp.LEAF:
				hp.maybeAdd(this, range);
				break;
				
			case RrCSGOp.INTERSECTION:
			case RrCSGOp.UNION:
				if(csg.complexity() != 2)
				{
					System.err.println("intersect_r(): comlexity = " + csg.complexity());
					return;
				}
				hp.maybeAdd(this, range);
				break;
				
			default:
				System.err.println("intersect_r(): dud CSG operator!");
			}
		}
	}
	
    /**
     * Intersect a half-plane line and a polygon, storing the sorted list
     * with the half-plane.
     * @param hp
     * @param big_wipe
     * @param up
     */
	public void lineIntersect(RrHalfPlane hp, RrInterval range, boolean up)
	{
		hp.removeCrossings();
		lineIntersect_r(hp, range);
		hp.sort(up, this);
		//hp.solidSet(this);
	}

//	/**
//	 * Is the line between two points wholely within the polygon?
//	 * @param here
//	 * @param there
//	 * @return 
//	 */
//	private boolean allInside(Rr2Point here, Rr2Point there)
//	{
//		// The points are on the surface.  Are they on the _same_
//		// surface?  (Suppose there's a gap in it, dummo?...)
//		RrCSG ch = leaf(here);
//		RrCSG ct = leaf(there);
//		if(ch == ct)
//			return true;
//		
//		RrHalfPlane hp = new RrHalfPlane(here, there);
//		if(value(hp.pLine().point(0.5)) > 0) // Need to go round the corner here
//			return false;
//		
//		lineIntersect(hp, new RrInterval(0, Rr2Point.sub(here, there).mod()), true);
//		double v;
//		double r = Math.sqrt(resolution_2);
//		for(int i = 0; i < hp.size(); i++)
//		{
//			v = hp.getParameter(i);
//			if((v > r) && (v < 1 - r))
//				return false;
//		}
//		
//		return true;
//	}
	
	
//	private RrPolygonList remainder(RrHalfPlane hp, int fg)
//	{
//		RrPolygonList segments = new RrPolygonList();
//		RrPolygon r;
//		for(int j = 0; j < hp.size() - 1; j += 2)
//		{
//			r = new RrPolygon();
//			r.add(hp.getPoint(j), fg);
//			r.add(hp.getPoint(j+1), fg);
//			segments.add(r);
//		}
//		return segments;
//	}
	
//	private RrPolygon antiWiggle(RrPolygon cand)
//	{
//		if(cand.size() <= 1)
//			return null;
//		
//		RrPolygon result = new RrPolygon();
//		int flag = cand.flag(1);
//		for(int i = 0; i < cand.size(); i += 2)
//		{
//			if(i != 0)
//			{
//				if(!allInside(cand.point(i+1), result.point(i-1)))
//					return null;
//			}
//			result.add(cand.point(i+1), flag);
//			result.add(cand.point(i), flag);
//		}
//		
//		return result;
//	}
	
    /**
     * Find the bit of polygon edge between start/originPlane and targetPlane
     * @param start
     * @param modelEdge
     * @param originPlane
     * @param targetPlane
     * @param flag
     * @return
     */
    public snakeEnd megGoToPlane(Rr2Point start, RrHalfPlane modelEdge, RrHalfPlane originPlane,
    		RrHalfPlane targetPlane, int flag)
    {
    	int beforeIndex = -1;

    	double t = modelEdge.pLine().nearest(start);
    	for(int i = 0; i < modelEdge.size(); i++)
    	{
    		if (modelEdge.getParameter(i) > t)
    			break;
       		beforeIndex = i;
    	}
    	
    	if(beforeIndex < 0 | beforeIndex >= modelEdge.size() - 1)
    	{
   			System.err.println("RrCSGPolygon.megGoToPlane(): can't find parameter in range!");
   			return null;
    	}
    	
    	Rr2Point pt = modelEdge.getPoint(beforeIndex + 1);
    	boolean backwards = originPlane.value(pt) <= 0;
    	if(backwards)
    		beforeIndex++;
    	
    	RrPolygon rPol = new RrPolygon();
    	RrCSGPolygon startQuad = modelEdge.getQuad(beforeIndex);
    	RrCSGPolygon c = startQuad;
    	RrHalfPlane next;
    	RrHalfPlane now = modelEdge;
    	int nextIndex;
    	
    	do
    	{
    		if(!c.corner)
    		{
    			System.err.println("RrCSGPolygon.megGoToPlane(): visiting non-corner quad!");
    			return null;
    		}
    		
       		if(backwards)
    			nextIndex = now.find(c) - 1;
    		else
    			nextIndex = now.find(c) + 1;
       		
       		if(nextIndex < 0 | nextIndex >= now.size()) //Hack - why needed?
       			return null;
       		
    	   	pt = now.getPoint(nextIndex);
        	if(targetPlane.value(pt) >= 0)
        	{
        		nextIndex = targetPlane.find(now);
        		if(nextIndex < 0)
        			return null;
        		rPol.add(targetPlane.getPoint(nextIndex), flag);
        		return new snakeEnd(rPol, targetPlane, nextIndex);
        	}
        	if(originPlane.value(pt) <= 0)
        		return null;
        	   		
    		c = now.getQuad(nextIndex);
    		rPol.add(c.vertex, flag);
    		next = c.csg.c_1().plane();
    		if(next == now)
    			next = c.csg.c_2().plane();
    		now = next;
    	} while (c != startQuad);
    	
    	System.err.println("RrCSGPolygon.megGoToPlane(): gone right round!");
    	return null;
    }
	
    /**
     * Take the start of a zig-zag hatch polyline and grow it as far as possible
     * @param hatches
     * @param thisHatch
     * @param thisPt
     * @param fg
     * @param fs
     * @return
     */
	private RrPolygon snakeGrow(List hatches, int thisHatch, int thisPt, int fg, int fs)
	{
		RrPolygon result = new RrPolygon();
		
		RrHalfPlane h = (RrHalfPlane)hatches.get(thisHatch);
		Rr2Point pt = h.pLine().point(h.getParameter(thisPt));
		result.add(pt, fs);
		snakeEnd jump;
		
		do
		{
			h.remove(thisPt);
			if(thisPt%2 != 0)
				thisPt--;
			pt = h.pLine().point(h.getParameter(thisPt));
			result.add(pt, fg);
			thisHatch++;
			if(thisHatch < hatches.size())
				jump = megGoToPlane(pt, h.getPlane(thisPt), h, 
					(RrHalfPlane)hatches.get(thisHatch), fg);
			else 
				jump = null;
			h.remove(thisPt);
			if(jump != null)
			{
				result.add(jump.p);
				h = jump.h;
				thisPt = jump.index;
			}
		} while(jump != null);
		
		return result;
	}
	
	/**
	 * Hatch a csg polygon parallel to line hp with index gap
	 * @param hp
	 * @param gap
	 * @param fg
	 * @param fs
	 * @return a polygon list as the result with flag values f
	 */
	public RrPolygonList hatch(RrHalfPlane hp, double gap, int fg, int fs)
	{
		RrBox big = box.scale(1.1);
		double d = Math.sqrt(big.d_2());
		
		Rr2Point orth = hp.normal();
		
		int quadPointing = (int)(2 + 2*Math.atan2(orth.y(), orth.x())/Math.PI);
		
		Rr2Point org = big.ne();
		
		switch(quadPointing)
		{
		case 0:
			break;
			
		case 1:
			org = big.nw();
			break;
			
		case 2:
			org = big.sw(); 
			break;
			
		case 3:
			org = big.se();
			break;
			
		default:
			System.err.println("RrPolygonList.hatch(): The atan2 function doesn't seem to work...");
		}
		
		RrHalfPlane hatcher = new 
			RrHalfPlane(org, Rr2Point.add(org, hp.pLine().direction()));

		List hatches = new ArrayList();
		
		double g = 0;		
		while (g < d)
		{
			lineIntersect(hatcher, RrInterval.big_interval(), true);
			if(hatcher.size() > 0)
				hatches.add(hatcher);
			hatcher = hatcher.offset(gap);
			g += gap;
		}
		
		RrPolygonList snakes = new RrPolygonList();
		int segment;
		do
		{
			segment = -1;
			for(int i = 0; i < hatches.size(); i++)
			{
				if(((RrHalfPlane)hatches.get(i)).size() > 0)
				{
					segment = i;
					break;
				}
			}
			if(segment >= 0)
			{
				snakes.add(snakeGrow(hatches, segment, 0, fg, fs));
			}
		} while(segment >= 0);
		
		return snakes.nearEnds();
	}
}
