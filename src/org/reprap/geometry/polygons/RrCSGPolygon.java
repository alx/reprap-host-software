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

import org.reprap.Attributes;
import org.reprap.Preferences;

/**
 * This class stores ends of the zig-zag infill pattern.
 */
class snakeEnd
{
	/**
	 * 
	 */
	public RrPolygon p;
	
	/**
	 * 
	 */
	public RrHalfPlane h;
	
	/**
	 * 
	 */
	public int index;
	
	/**
	 * @param pl
	 * @param hs
	 * @param i
	 */
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
 * TODO: Change the quad tree to a BSP tree?
 */
public class RrCSGPolygon
{
	
	/**
	 * The polygon 
	 */
	private RrCSG csg;
	
	/**
	 * Its enclosing box
	 */
	private RrBox box;   
	
	/**
	 * Quad tree division, respectively: NW, NE, SE, SW 
	 */
	private RrCSGPolygon q1, q2, q3, q4;
	
	/**
	 * This boxe's parent
	 */
	private RrCSGPolygon parent;
	
	/**
	 * Used by the edge-generation software. 
	 */
	private boolean visit1, visit2;     
	
	/**
	 * A leaf quad can contain 0, 1 or 2 edges
	 * 
	 * Number of edges in the box 
	 */
	private int edgeCount;          
	
	/**
	 * Does this box contain a vertex? 
	 */
	private boolean corner;         
	
	/**
	 * Edge parametric intervals
	 */
	private RrInterval i1, i2;     
	
	/**
	 * The vertex, if it exists
	 */
	private Rr2Point vertex;
	
	/**
	 * the attributes of this polygon
	 */
	private Attributes att;
	
	/**
	 * Set one up - private call, with no division (dummy is just a
	 * distinguishing argument).
	 * @param p
	 * @param bx
	 * @param a
	 */
	private RrCSGPolygon(RrCSG p, RrBox bx, Attributes a, int dummy)
	{
		if(a == null)
			System.err.println("RrCSGPolygon(): null attributes!");
		box = new RrBox(bx);
		att = a;
		q1 = null;
		q2 = null;
		q3 = null;
		q4 = null;
		parent = null;
		csg = p;
		visit1 = false;
		visit2 = false;
		edgeCount = 0;
		corner = false;
		vertex = null;
		i1 = new RrInterval();
		i2 = new RrInterval();
	}
	
	/**
	 * Set one up - public call, always divides
	 * @param p
	 * @param bx
	 * @param a
	 */
	public RrCSGPolygon(RrCSG p, RrBox bx, Attributes a)
	{
		this(p, bx, a, 1);
		divide();
	}
	
	
	/**
	 * Get children etc
	 * @return children
	 */
	public RrCSGPolygon c1() { return q1; }
	public RrCSGPolygon c2() { return q2; }
	public RrCSGPolygon c3() { return q3; }
	public RrCSGPolygon c4() { return q4; }
	public RrCSGPolygon parent() { return parent; }
	public RrCSG csg() { return csg; }
	public RrBox box() { return box; }
	public int complexity() { return csg.complexity(); }
	public int edges() { return edgeCount; }
	public boolean corner() { return corner; }
	public Rr2Point vertex() { return vertex; }
	public RrInterval interval1() { return i1; } 
	public RrInterval interval2() { return i2; }
	public Attributes getAttributes() { return att; }
	
	/**
	 * This quad is a leaf if it has no children; just check the first.
	 * @return
	 */
	public boolean leaf()
	{
		return q1 == null;
	}
	
	/**
	 * Find the topmost quad
	 * @return
	 */
	public RrCSGPolygon root()
	{
		if(parent != null)
			return parent.root();
		else
			return this;
	}
	
	/**
	 * Convert to a string - internal recursive call
	 * @param quad
	 */
	private String toString_r(String quad)
	{
		quad = quad + csg.toString() + "\n";
		
		if(q1 == null)
		{
			return quad;
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
	 * Quad tree division - make the 4 sub quads.
	 */
	private void makeQuads()
	{
//		 Set up the quad-tree division
		
		Rr2Point sw = box.sw();
		Rr2Point nw = box.nw();
		Rr2Point ne = box.ne();
		Rr2Point se = box.se();
		
		Rr2Point cen = box.centre();
		
		// Prune to slightly bigger boxes than the ones we end up using to make
		// sure nothing slips down the cracks.
		
		double wb = cen.x() - (cen.x() - sw.x())*(Preferences.swell() - 1);
		double wo = sw.x() - (cen.x() - sw.x())*(Preferences.swell() - 1);
		double nb = cen.y() + (nw.y() - cen.y())*(Preferences.swell() - 1);
		double no = nw.y() + (nw.y() - cen.y())*(Preferences.swell() - 1);
		double eb = cen.x() + (se.x() - cen.x())*(Preferences.swell() - 1);
		double eo = se.x() + (se.x() - cen.x())*(Preferences.swell() - 1);
		double sb = cen.y() - (cen.y() - sw.y())*(Preferences.swell() - 1);
		double so = sw.y() - (cen.y() - sw.y())*(Preferences.swell() - 1);
//		double ws = cen.x() - (cen.x() - sw.x())*(Preferences.swell() - 1)*0.5;
//		double ns = cen.y() + (nw.y() - cen.y())*(Preferences.swell() - 1)*0.5;
//		double es = cen.x() + (se.x() - cen.x())*(Preferences.swell() - 1)*0.5;
//		double ss = cen.y() - (cen.y() - sw.y())*(Preferences.swell() - 1)*0.5;
		
//		 Put the results in the children
		
		RrBox bb = new RrBox(new Rr2Point(wo, no), new Rr2Point(eb, sb));
		//RrBox bs = new RrBox(nw, new Rr2Point(es, ss));
		RrBox bs = new RrBox(nw, cen);
		q1 = new RrCSGPolygon(csg.prune(bb), bs, att, 1);
		q1.parent = this;

		bb = new RrBox(new Rr2Point(eo, no), new Rr2Point(wb, sb));
		//bs = new RrBox(ne, new Rr2Point(ws, ss));
		bs = new RrBox(ne, cen);
		q2 = new RrCSGPolygon(csg.prune(bb), bs, att, 1);
		q2.parent = this;
		
		bb = new RrBox(new Rr2Point(eo, so), new Rr2Point(wb, nb));
		//bs = new RrBox(se, new Rr2Point(ws, ns));
		bs = new RrBox(se, cen);
		q3 = new RrCSGPolygon(csg.prune(bb), bs, att, 1);
		q3.parent = this;
		
		bb = new RrBox(new Rr2Point(wo, so), new Rr2Point(eb, nb));
		//bs = new RrBox(sw, new Rr2Point(es, ns));
		bs = new RrBox(sw, cen);
		q4 = new RrCSGPolygon(csg.prune(bb), bs, att, 1);
		q4.parent = this;
	}
	
	
	/**
	 * Quad-tree division - recursive internal call
	 * @param res_2
	 * @param swell
	 */
	private void divide_r()
	{

		// Anything as simple as a single corner, evaluate and go home
		
		if(complexity() < 3)
		{
			evaluate();
			return;
		}
		
		// Too small a box?
		
		if(box.dSquared() < Preferences.boxResolutionSquared())
		{
			System.err.println("RrCSGPolygon.divide(): hit resolution limit!  Complexity: " +
					csg.complexity());
			csg = RrCSG.nothing();  // Throw it away!  (It is small...)
			return;
		}
		
		// For comlexities of 4 or less, check if regularization throws
		// some away.
		
		if(complexity() < 5)
		{
			csg = csg.regularise();
			if(complexity() < 3)
			{
				evaluate();
				return;
			}
		}
		
		// Set up the quad-tree division
		
		makeQuads();
		
		// Recursively divide the children
		
		q1.divide_r();
		q2.divide_r();
		q3.divide_r();
		q4.divide_r();
	}
	
	/**
	 * Divide the CSG polygon into a quad tree, each leaf of
	 * which contains at most two planes.
	 * Evaluate the leaves, and store lists of intersections with
	 * the half-planes.
	 */
	private void divide()
	{
		csg.removeDuplicates();
		csg.clearCrossings();
		divide_r();
		csg.sortCrossings();
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
		case NULL:
		case UNIVERSE:	
			return;
			
			// One half-plane in the box:
			
		case LEAF:
			i1 = RrInterval.bigInterval();
			i1 = box.wipe(csg.plane().pLine(), i1);
			if(i1.empty()) 
				return;
			edgeCount = 1;
			return;
			
			// Two - maybe a corner, or they may not intersect
			
		case UNION:
		case INTERSECTION:
			if(csg.complexity() != 2)
			{
				System.err.println("RrCSGPolygon.evaluate(): complexity = " + 
					csg.complexity());
				return;
			}
			i1 = RrInterval.bigInterval();
			i1 = box.wipe(csg.c1().plane().pLine(), i1);
			
			i2 = RrInterval.bigInterval();
			i2 = box.wipe(csg.c2().plane().pLine(), i2);
			
			if(csg.operator() == RrCSGOp.INTERSECTION)
			{
				i2 = csg.c1().plane().wipe(csg.c2().plane().pLine(), i2);
				i1 = csg.c2().plane().wipe(csg.c1().plane().pLine(), i1);
			} else
			{
				i2 = csg.c1().plane().complement().wipe(
						csg.c2().plane().pLine(), i2);
				i1 = csg.c2().plane().complement().wipe(
						csg.c1().plane().pLine(), i1);                    
			}
			
			if(!i1.empty())
				edgeCount++;
			if(!i2.empty())
				edgeCount++;
			
			try
			{
				vertex = csg.c1().plane().crossPoint(csg.c2().plane());
				if(box.pointRelative(vertex) == 0)
					corner = true;
				else
					vertex = null;
			} catch (RrParallelLineException ple) {}
			
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
	 * @return quad containing point p
	 */
	public RrCSGPolygon quad(Rr2Point p)
	{
		if(leaf())
		{
			if(box.pointRelative(p) != 0)
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
	 * Find the RrCSG expression that gives the potential at point p.
	 * Note this does NOT find the closest half-plane unless the point
	 * is on a surface.
	 * @param p
	 * @return CSG object 
	 */
	public RrCSG leaf(Rr2Point p)
	{
		RrCSGPolygon q = quad(p);
		return(q.csg.leaf(p));
	}
	
	/**
	 * The gradient at a point
	 * @param p
	 * @return
	 */
	public Rr2Point grad(Rr2Point p)
	{
		return(leaf(p).grad(p));
	}
	
	/**
	 * Find the potential at point p.
	 * @param p
	 * @return potential of point p
	 */	
	public double value(Rr2Point p)
	{
		// Conventional answer for when we're outside the box
		
		if(box.pointRelative(p) != 0)
			return 1;
		
		// Inside - calculate.
		
		RrCSG c = leaf(p);
		return c.value(p);
	}
	
	/**
	 * Plot it for debugging
	 *
	 */
	public void debugPlot()
	{
		RrCSGPolygon temp = new RrCSGPolygon(csg, box, att);
		//temp.divide();
		new RrGraphics(temp);		
	}
	
	/**
	 * Offset by a distance; grow or shrink the box by the same amount
	 * If the old polygon was divided, the new one will be too.
	 * If we shrink out of existence, a standard null object is returned.
	 * @param d
	 * @return offset polygon object by distance d
	 */
	public RrCSGPolygon offset(double d)
	{	
		RrBox b;
		if(-d >= 0.5*box.x().length() || -d >= 0.5*box.y().length())
		{
			b = new RrBox(new Rr2Point(0,0), new Rr2Point(1,1));
			return new RrCSGPolygon(RrCSG.nothing(), b, att);
		}
		Rr2Point p = new Rr2Point(d,d);
		b = new RrBox( Rr2Point.sub(box.sw(), p), Rr2Point.add(box.ne(), p) );
		RrCSG expression = csg.offset(d);
		expression.removeDuplicates();
		RrCSGPolygon result = new RrCSGPolygon(expression, b, att);
//		if(!leaf())
//			result.divide();
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
    	
    	if(!leaf())
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
 
    	if(!leaf())
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
    public RrPolygon meg()
    {
    	RrPolygon result = new RrPolygon(att);
    	
    	RrCSGPolygon c = this;
    	RrHalfPlane now, next;
    	now = csg.c1().plane();
    	if(now.find(c)%2 == 1)      // Subtle, or what?  Finds the line with an even index so
    		now = csg.c2().plane(); // we head off along it in the right direction.
    	
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
    		
    		result.add(c.vertex);
    		c.visit2 = true;
    		nextIndex = now.find(c) + 1;
    		
    		if(nextIndex < 0 | nextIndex >= now.size())
    		{
    			System.err.println("RrCSGPolygon.meg(): fallen off the end of the line!");
    			return result; // Best we can do in the circs...
    		}
    		
    		c = now.getQuad(nextIndex);
    		next = c.csg.c1().plane();
    		if(next == now)
    			next = c.csg.c2().plane();
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
    public RrPolygonList megList()
    {
    	clearVisited(true, true);

    	RrPolygonList result = new RrPolygonList();
    	RrPolygon m;
    	
    	RrCSGPolygon vtx = findCorner(true, true);
    	while(vtx != null)
    	{
    		m = vtx.meg();
    		if(m.size() > 0)
    		{
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
		
		if(!leaf())
		{
			q1.lineIntersect_r(hp, newRange);
			q2.lineIntersect_r(hp, newRange);
			q3.lineIntersect_r(hp, newRange);
			q4.lineIntersect_r(hp, newRange);
		} else
			hp.maybeAdd(this, newRange);
	}
	
    /**
     * Intersect a half-plane line and a polygon, storing the sorted list
     * with the half-plane.
     * @param hp
	 * @param range
	 * @param up
	 */
	public void lineIntersect(RrHalfPlane hp, RrInterval range)
	{
		hp.removeCrossings();
//		lineIntersect_r(hp, range);
//		hp.sort();
//		hp.solidSet(this);
		
		RrBox b = box.scale(1.01);
		
		range = b.wipe(hp.pLine(), range);
		
		double step = Preferences.machineResolution()/hp.pLine().direction().mod();
		double tOld = range.low();
		Rr2Point p = hp.pLine().point(tOld);
		double vOld = csg().value(p);
		if(vOld <= 0)
		{
			System.err.println("RrCSGPolygon.lineIntersect(): starting potential 0 or negative: " + vOld);
			vOld = -1;
		}
		double t = tOld;
		double v = vOld;
		while(t < range.high())
		{
			t += step;
			p = hp.pLine().point(t);
			v = value(p);
			if(v == 0)
				v = -vOld;
			if(v*vOld < 0)
			{
				RrInterval ti = new RrInterval(tOld, t);
				LineIntersection bc = hp.binaryChop(this, ti);
				if(bc == null)
					System.err.println("RrCSGPolygon.lineIntersect(): no intersection found in: " + 
							ti.toString() + " values: " + vOld + " " + v);
				else
					hp.add(bc);
			}
			vOld = v;
			tOld = t;
		}
		if(v <= 0)
			System.err.println("RrCSGPolygon.lineIntersect(): ending potential 0 or negative: " + v);		
		
		hp.solidSet(this);
	}

    /**
     * Find the bit of polygon edge between start/originPlane and targetPlane
     * @param start
     * @param modelEdge
     * @param originPlane
     * @param targetPlane
     * @param flag
     * @return polygon edge between start/originaPlane and targetPlane
     */
    public snakeEnd megGoToPlane(Rr2Point start, RrHalfPlane modelEdge, RrHalfPlane originPlane,
    		RrHalfPlane targetPlane)
    {
    	int beforeIndex = -1;

    	double t = modelEdge.pLine().nearest(start);
    	for(int i = 0; i < modelEdge.size(); i++)
    	{
    		if (modelEdge.getParameter(i) >= t)
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
    	
    	RrPolygon rPol = new RrPolygon(att);
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
        		rPol.add(targetPlane.getPoint(nextIndex));
        		return new snakeEnd(rPol, targetPlane, nextIndex);
        	}
        	if(originPlane.value(pt) <= 0)
        		return null;
        	   		
    		c = now.getQuad(nextIndex);
    		rPol.add(c.vertex);
    		next = c.csg.c1().plane();
    		if(next == now)
    			next = c.csg.c2().plane();
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
     * @return zigzag hatch polygon
     */
	private RrPolygon snakeGrow(List<RrHalfPlane> hatches, int thisHatch, int thisPt)
	{
		RrPolygon result = new RrPolygon(att);
		
		RrHalfPlane h = hatches.get(thisHatch);
		Rr2Point pt = h.pLine().point(h.getParameter(thisPt));
		result.add(pt);
		snakeEnd jump = null;
		
		do
		{
			h.remove(thisPt);
			if(thisPt%2 != 0)
				thisPt--;
			if(h.size() > thisPt)
			{
				pt = h.pLine().point(h.getParameter(thisPt));
				result.add(pt);
				thisHatch++;
				if(thisHatch < hatches.size())
					jump = megGoToPlane(pt, h.getPlane(thisPt), h, 
							hatches.get(thisHatch));
				else 
					jump = null;
				h.remove(thisPt);
				if(jump != null)
				{
					result.add(jump.p);
					h = jump.h;
					thisPt = jump.index;
				}
			} else
				jump = null;
		} while(jump != null);
		
		return result;
	}
	
	/**
	 * Hatch a csg polygon parallel to line hp with index gap
	 * @param hp
	 * @param gap
	 * @param fg
	 * @param fs
	 * @return a polygon list as the result
	 */
	public RrPolygonList hatch(RrHalfPlane hp, double gap)
	{
		RrBox big = box.scale(1.1);
		double d = Math.sqrt(big.dSquared());
		
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
			System.err.println("RrCSGPolygon.hatch(): The atan2 function doesn't seem to work...");
		}
		
		RrHalfPlane hatcher = new 
			RrHalfPlane(org, Rr2Point.add(org, hp.pLine().direction()));

		List<RrHalfPlane> hatches = new ArrayList<RrHalfPlane>();
		
		double g = 0;		
		while (g < d)
		{
			lineIntersect(hatcher, RrInterval.bigInterval());
			if(hatcher.size() > 0)
				hatches.add(hatcher);
			hatcher = hatcher.offset(gap);
			g += gap;
		}
		
		//new RrGraphics(this, hatches);
		//RrPolygonList pgl = megList();
		//new RrGraphics(pgl);
		
		RrPolygonList snakes = new RrPolygonList();
		int segment;
		do
		{
			segment = -1;
			for(int i = 0; i < hatches.size(); i++)
			{
				if((hatches.get(i)).size() > 0)
				{
					segment = i;
					break;
				}
			}
			if(segment >= 0)
			{
				snakes.add(snakeGrow(hatches, segment, 0));
			}
		} while(segment >= 0);
		
		return snakes.nearEnds();
	}
}
