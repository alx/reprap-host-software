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
 
 
 STLSlice: deals with the slices through an STL object
 
 */

package org.reprap.geometry.polygons;

import java.util.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import org.reprap.gui.STLObject;

public class STLSlice 
{
	private static final int grid = 100;
	private static final double gridRes = 1.0/grid;
	private static final double lessGridSquare = gridRes*gridRes*0.01;
	private static final double tiny = 1.0e-8;
	List stls;
	private RrPolygonList edges;  // List of the edges with points in this one
	private RrBox box;            ///< Its enclosing box
	private STLSlice q1,      ///< Quad tree division - NW
	q2,           ///< NE 
	q3,           ///< SE
	q4;           ///< SW
	private double resolution_2;  ///< Squared diagonal of the smallest box to go to
	private double sFactor;       /// Swell factor for division
	private boolean visited;
	private static List onlyOne;
	
	/**
	 * Constructor just records the list of STL objects
	 * @param s
	 */
	public STLSlice(List s)
	{
		edges = null;
		q1 = null;
		q2 = null;
		q3 = null;
		q4 = null;
		visited = false;
		stls = s;
		sFactor = 1;
		resolution_2 = 1.0e-8; // Default - set properly
	}
	
	
	public RrBox box()
	{
		return box;
	}
	
	public RrPolygonList edges()
	{
		return edges;
	}
	
	public STLSlice c_1()
	{
		return q1;
	}
	public STLSlice c_2()
	{
		return q2;
	}
	public STLSlice c_3()
	{
		return q3;
	}
	public STLSlice c_4()
	{
		return q4;
	}
	
	// Need to deal with points in the plane.
	
	private double toGrid(double x)
	{
		//return x;
		return (double)((int)(x*grid + 0.5))/(double)grid;
	}
	
	/**
	 * Add the edge where the plane z cuts a triangle (if it does)
	 * @param p
	 * @param q
	 * @param r
	 * @param z
	 */
	private void addEdge(Point3d p, Point3d q, Point3d r, double z)
	{
		Point3d odd = null, even1 = null, even2 = null;
		int pat = 0;
		
		if(p.z < z)
			pat = pat | 1;
		if(q.z < z)
			pat = pat | 2;
		if(r.z < z)
			pat = pat | 4;
		
		switch(pat)
		{
		case 0:
		case 7:
			return;
		case 1:
		case 6:
			odd = p;
			even1 = q;
			even2 = r;
			break;
		case 2:
		case 5:
			odd = q;
			even1 = p;
			even2 = r;
			break;
		case 3:
		case 4:
			odd = r;
			even1 = p;
			even2 = q;
			break;
		default:
			System.err.println("addEdge(): the | function doesn't seem to work...");
		}
		
		even1.sub((Tuple3d)odd);
		even2.sub((Tuple3d)odd);
		double t = (z - odd.z)/even1.z;	
		Rr2Point e1 = new Rr2Point(toGrid(odd.x + t*even1.x), 
				toGrid(odd.y + t*even1.y));
		t = (z - odd.z)/even2.z;
		Rr2Point e2 = new Rr2Point(toGrid(odd.x + t*even2.x), 
				toGrid(odd.y + t*even2.y));
		
		if(!Rr2Point.same(e1, e2, lessGridSquare))
		{
			RrPolygon pg = new RrPolygon();
			pg.add(e1, 1);
			pg.add(e2, 0);
			edges.add(pg);
		}
	}
	
	/**
	 * Run through a Shape3D and set edges from it at plane z
	 * Apply the transform first
	 * @param shape
	 * @param trans
	 * @param z
	 */
	private void addAllEdges(Shape3D shape, Transform3D trans, double z)
    {
        GeometryArray g = (GeometryArray)shape.getGeometry();
        Point3d p1 = new Point3d();
        Point3d p2 = new Point3d();
        Point3d p3 = new Point3d();
        Point3d q1 = new Point3d();
        Point3d q2 = new Point3d();
        Point3d q3 = new Point3d();
        if(g.getVertexCount()%3 != 0)
        {
        	System.err.println("addAllEdges(): shape3D with vertices not a multiple of 3!");
        }
        if(g != null)
        {
            for(int i = 0; i < g.getVertexCount(); i+=3) 
            {
                g.getCoordinate(i, p1);
                g.getCoordinate(i+1, p2);
                g.getCoordinate(i+2, p3);
                trans.transform(p1, q1);
                trans.transform(p2, q2);
                trans.transform(p3, q3);
                addEdge(q1, q2, q3, z);
            }
        }
    }
	
	/**
	 * Unpack the Shape3D(s) from value and set edges from them
	 * @param value
	 * @param trans
	 * @param z
	 */
	private void recursiveSetEdges(Object value, Transform3D trans, double z) 
    {
        if(value instanceof SceneGraphObject) 
        {
            SceneGraphObject sg = (SceneGraphObject)value;
            if(sg instanceof Group) 
            {
                Group g = (Group)sg;
                java.util.Enumeration enumKids = g.getAllChildren( );
                while(enumKids.hasMoreElements())
                    recursiveSetEdges(enumKids.nextElement(), trans, z);
            } else if (sg instanceof Shape3D) 
            {
                addAllEdges((Shape3D)sg, trans, z);
            }
        }
    }
	
	private STLSlice(RrPolygonList pgl, RrBox b, double res, double fac)
	{
		edges = pgl;
		box = b;
		prune();
		q1 = null;
		q2 = null;
		q3 = null;
		q4 = null;
		resolution_2 = res;
		sFactor = fac;
		visited = false;
	}
	
	/**
	 * Prune the polygon list to the box so that only segments
	 * with endpoints in the box are retained.
	 * @return
	 */
	private void prune()
	{
		RrPolygonList result = new RrPolygonList();
		
		for(int i = 0; i < edges.size(); i++)
		{
			if(box.point_relative(edges.polygon(i).point(0)) == 0 ||
					box.point_relative(edges.polygon(i).point(1)) == 0)
				result.add(edges.polygon(i));
		}
				
		edges = result;
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
		
//		 Prune the set to the four boxes, and put the results in the children
		
		RrBox s = new RrBox(Rr2Point.mul(Rr2Point.add(sw, nw), 0.5), 
				Rr2Point.mul(Rr2Point.add(nw, ne), 0.5));
		s = s.scale(sFactor);
		q1 = new STLSlice(edges, s, resolution_2, sFactor);
		
		s = new RrBox(cen, ne);
		s = s.scale(sFactor);
		q2 = new STLSlice(edges, s, resolution_2, sFactor);
		
		s = new RrBox(Rr2Point.mul(Rr2Point.add(sw, se), 0.5), 
				Rr2Point.mul(Rr2Point.add(se, ne), 0.5));
		s = s.scale(sFactor);
		q3 = new STLSlice(edges, s, resolution_2, sFactor);
		
		s = new RrBox(sw, cen);
		s = s.scale(sFactor);
		q4 = new STLSlice(edges, s, resolution_2, sFactor);		
	}
	
	/**
	 * Quad tree division to end up with two (or no) ends in each box.
	 */
	private void divide()
	{
		if(box.d_2() < resolution_2)
		{
			System.err.println("STLSlice.divide(): hit resolution limit!");
			return;
		}
		
		if(edges.size() > 2)
		{
			makeQuads();
			q1.divide();
			q2.divide();
			q3.divide();
			q4.divide();
		} else
		{
			boolean divideFurther = false;
	   		for(int i = 0; i < edges.size(); i++)
    		{
    			RrPolygon pg = edges.polygon(i);
    			if(box.point_relative(pg.point(0)) == 0 &&  
    					box.point_relative(pg.point(1)) == 0)
    			{
    				divideFurther = true;
    				break;
    			}
    				
    		}
	   		if(divideFurther)
	   		{
	   			makeQuads();
				q1.divide();
				q2.divide();
				q3.divide();
				q4.divide();
	   		} else if(edges.size() == 1)
				onlyOne.add(this);
		}
	}
	
	/**
	 * Find (we hope rare) quads with only one end in and pair them up.
	 */
	private void fixSingletons()
	{
		if(onlyOne.size() <= 0)
			return;
		
		// Remove duplicates
		int i, j;
		RrPolygon pgi, pgj;
		i = onlyOne.size() - 2;
		while(i >= 0)
		{
			j = onlyOne.size() - 1;
			pgi = ((STLSlice)onlyOne.get(i)).edges.polygon(0);
			while(j > i)
			{
				pgj = ((STLSlice)onlyOne.get(j)).edges.polygon(0);
				if (pgi == pgj)
				{
					((STLSlice)onlyOne.get(j)).edges = new RrPolygonList();
					onlyOne.remove(j);
				}
				j--;
			}
			i--;
		}
		
		// Find nearest pairs
		i = 0;
		while(i < onlyOne.size() - 1)
		{
			double dmin = Double.POSITIVE_INFINITY;
			double d;
			int jNear = -1, endNear = -1;
			STLSlice stli = (STLSlice)onlyOne.get(i);
			pgi = stli.edges.polygon(0);
			Rr2Point pi = pgi.point(0);
			if(stli.box.point_relative(pi) != 0)
				pi = pgi.point(1);
			j = i + 1;
			while(j < onlyOne.size())
			{
				pgj = ((STLSlice)onlyOne.get(j)).edges.polygon(0);
				d = Rr2Point.d_2(pgi.point(0), pgj.point(0));
				if(d < dmin)
				{
					dmin = d;
					jNear = j;
					endNear = 0;
				}
				d = Rr2Point.d_2(pgi.point(0), pgj.point(1));
				if(d < dmin)
				{
					dmin = d;
					jNear = j;
					endNear = 1;
				}
				j++;
			}
			STLSlice stlj = (STLSlice)onlyOne.get(jNear);
			stlj.edges.polygon(0).point(endNear).set(pi);
			stli.edges.add(stlj.edges.polygon(0));
			stlj.edges = new RrPolygonList();
			onlyOne.remove(jNear);
			i++;
		}


		
		System.out.println("fixSingletons(): " + onlyOne.size() + " ends.");
	}
	
	/**
	 * Find the quad containing a point
	 * @param p
	 * @return
	 */
	public STLSlice quad(Rr2Point p)
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
	 * Walk the tree to find an unvisited corner
     */
    private STLSlice findCorner()
    {
    	STLSlice result = null;
 
    	if(q1 != null)
    	{
    		result = q1.findCorner();
    		if(result != null)
    			return result;
       		result = q2.findCorner();
    		if(result != null)
    			return result; 
      		result = q3.findCorner();
    		if(result != null)
    			return result; 
     		result = q4.findCorner();
    		if(result != null)
    			return result;   		
    	} else
    	{
    		if(edges.size() == 2 && !visited)
    		{
    			return this;
    		} else if(edges.size() != 0  && !visited)
    			System.err.println("STLSlice: quad edges: " + edges.size());
    	}
    	
    	return result;
    }
    
	 /**
	 * Walk the tree to set all instances of a corner visited
     */
    private void setVisited(RrPolygon p0, RrPolygon p1)
    {
    	if(visited)
    		return;
 
    	if(q1 != null)
    	{
    		q1.setVisited(p0, p1);
    		q2.setVisited(p0, p1);
    		q3.setVisited(p0, p1);
    		q4.setVisited(p0, p1);   		
    	} else
    	{
    		if(edges.size() != 2)
    			return;
    		int tot = 0;
    		for(int i = 0; i < 2; i++)
    		{
    			if(edges.polygon(i) == p0 || edges.polygon(i) == p1)
    				tot++;
    		}
    		if(tot == 2)
    			visited = true;
    	}
    }
    
    /**
	 * Walk the tree to reset all visited flags and check contents
     */
    private void sanityCheck()
    {
    	visited = false;
 
    	if(q1 != null)
    	{
    		q1.sanityCheck();
       		q2.sanityCheck();
       		q3.sanityCheck();
       		q4.sanityCheck();
    	} else
    	{
    		for(int i = 0; i < edges.size(); i++)
    		{
    			RrPolygon pg = edges.polygon(i);
    			if(box.point_relative(pg.point(0)) == 0 &&  
    					box.point_relative(pg.point(1)) == 0)
    				System.err.println("sanityCheck(): polygon with both ends in one box!");
    		}
    		if(edges.size() == 2 || edges.size() == 0)
    			return;
    		System.err.println("sanityCheck(): quad found with " + edges.size() + " edges.");
    		edges = new RrPolygonList();
    	}
    }
    
    
	/**
	 * Stitch up the ends in the quad tree.
	 */
	private void conquer()
	{
		RrPolygonList pgl = new RrPolygonList();
		RrPolygon pg, pg0;
		Rr2Point p0, p1;
		STLSlice corner;
		RrPolygon oldPg;
		
		STLSlice startCorner = findCorner();
		while(startCorner != null)
		{
			corner = startCorner;
			pg = new RrPolygon();
			oldPg = null;
			do
			{
				if(corner.visited)
				{
					System.err.println("conquer(): revisiting quad!");
					break;
				}
				corner.visited = true;
				
				if(corner.edges.size() != 2)
				{
					System.err.println("conquer(): dud quad contents:" +
							corner.edges.size());
					break;
				}
				
				pg0 = corner.edges.polygon(0);
				if(pg0 == oldPg)
					pg0 = corner.edges.polygon(1);
				setVisited(corner.edges.polygon(0), corner.edges.polygon(1));
				p0 = pg0.point(0);
				p1 = pg0.point(1);
				if(corner.box.point_relative(p0) != 0)
				{
					p1 = p0;
					p0 = pg0.point(1);
				}
				if(corner.box.point_relative(p0) != 0)
				{
					System.err.println("conquer(): neither end of segment in box!");
					break;
				}
				pg.add(p0, 1);
				oldPg = pg0;
				corner = quad(p1);
			} while (corner != startCorner);
			if(pg.size() > 2)
			{
				pg.flag(pg.size() - 1, 3);
				pgl.add(pg);
			}
			startCorner = findCorner();
		}
		edges = pgl;
//		q1 = null;
//		q2 = null;
//		q3 = null;
//		q4 = null;
	}
	

	public double maxZ()
	{
		STLObject stl;
		double result = Double.NEGATIVE_INFINITY;
		
		for(int i = 0; i < stls.size(); i++)
		{
			stl = (STLObject)stls.get(i);
			if(stl.size.z > result)
				result = stl.size.z;
		}
		return result;
	}
	
	/**
	 * 
	 * build a 2D polygon list of all edges in the plane z
	 * from all the objects in stls then turn it to CSG.
	 * @param z
	 * @return
	 */
	public RrCSGPolygon slice(double z)
	{
		edges = null;
		q1 = null;
		q2 = null;
		q3 = null;
		q4 = null;
		visited = false;
		sFactor = 1;
		resolution_2 = 1.0e-8; // Default - set properly
		
		edges = new RrPolygonList();
		STLObject stl;
		Transform3D trans;
		BranchGroup bg;
		Enumeration things;
		
		for(int i = 0; i < stls.size(); i++)
		{
			stl = (STLObject)stls.get(i);
			trans = stl.getTransform();
			bg = stl.getSTL();
			things = bg.getAllChildren();
			while(things.hasMoreElements()) 
			{
				Object value = things.nextElement();
				recursiveSetEdges(value, trans, z);
			}
		}
		box = edges.box.scale(1.1);
		//RrGraphics g = new RrGraphics(box.scale(1.5), true);		
		sFactor = 1.03;
		resolution_2 = box.d_2()*tiny;
		
		onlyOne = new ArrayList();
		//g.addPol(edges);
		divide();
		//RrGraphics g1 = new RrGraphics(box.scale(1.5), false);
		//g1.addSTL(this);
		fixSingletons();
		sanityCheck();
		conquer();
		edges = edges.simplify(gridRes*1.5);
		//g1.addSTL(this);
		System.out.println(edges.toString());
		

		
		if(edges.size() < 1)
		{
			System.err.println("slice(): nothing there!");
			return new RrCSGPolygon(RrCSG.nothing(), new RrBox());
		} else
			return edges.toCSG();
	}
}
