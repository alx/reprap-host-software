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
import com.sun.j3d.loaders.Scene;
import org.reprap.gui.STLObject;

public class STLSlice 
{
	private static final int grid = 100;
	List stls;
	private RrPolygonList edges;  // List of the edges with points in this one
	private RrBox box;            ///< Its enclosing box
	private STLSlice q1,      ///< Quad tree division - NW
	q2,           ///< NE 
	q3,           ///< SE
	q4;           ///< SW
	private double resolution_2;  ///< Squared diagonal of the smallest box to go to
	private double sFactor;       /// Swell factor for division
	boolean visited;
	
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
		
		if(!Rr2Point.same(e1, e2, 1.0e-4))
		{
			RrPolygon pg = new RrPolygon();
			pg.add(e1, 1);
			pg.add(e2, 1);
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
        if( value instanceof SceneGraphObject != false ) 
        {
            // set the user data for the item
            SceneGraphObject sg = (SceneGraphObject) value;
            
            // recursively process group
            if( sg instanceof Group ) 
            {
                Group g = (Group) sg;
                
                // recurse on child nodes
                java.util.Enumeration enumKids = g.getAllChildren( );
                
                while( enumKids.hasMoreElements( ) != false )
                    recursiveSetEdges(enumKids.nextElement( ), trans, z);
            } else if ( sg instanceof Shape3D ) 
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
//			 Set up the quad-tree division
			
			Rr2Point sw = box.sw();
			Rr2Point nw = box.nw();
			Rr2Point ne = box.ne();
			Rr2Point se = box.se();
			Rr2Point cen = box.centre();
			
//			 Prune the set to the four boxes, and put the results in the children
			
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
			
			// Recursively divide the children
			
			q1.divide();
			q2.divide();
			q3.divide();
			q4.divide();
		}
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
	 * Walk the tree to find the polygon with an end nearest a point
	 * @param pg
	 * @param end
	 * @param ignoreVisited
	 * @result
     */
//	private double findEnd(RrPolygon pg, int end, boolean visitVisited,
//			double dClose, RrPolygon result)
//	{
//		Rr2Point p = pg.point(end);
//		
//		RrPolygon pgTest;
//
//		double d[] = new double[4];
//		STLSlice q[] = new STLSlice[4];
//		double dd;
//		STLSlice qq;
//		boolean gotAnswer = false;
//		
//		if(q1 != null)
//		{
//			d[0] = q1.box.d_2(p);
//			d[1] = q2.box.d_2(p);
//			d[2] = q3.box.d_2(p);
//			d[3] = q4.box.d_2(p);
//			q[0] = q1;
//			q[1] = q2;
//			q[2] = q3;
//			q[3] = q4;
//			
//			for(int i = 0; i < 3; i++)
//			{
//				for(int j = i+1; j < 4; j++)
//					if(d[i] > d[j])
//					{
//						dd = d[i];
//						d[i] = d[j];
//						d[j] = dd;
//						qq = q[i];
//						q[i] = q[j];
//						q[j] = qq;
//					}
//			}
//			
//			for(int i = 0; i < 4; i++)
//			{
//				if((!q[i].visited || visitVisited) && d[i] < dClose)
//				{
//					RrPolygon candidate = new RrPolygon();
//					dd = q[i].findEnd(pg, end, visitVisited, dClose, candidate);
//					if(dd < dClose)
//					{
//						dClose = dd;
//						result.set(candidate);
//					}
//				}	
//			}
//			return dClose;
//		} else
//		
//		while(!gotAnswer)
//		{
//			for(int i = 0; i < q.edges.size(); i++)
//			{
//				pgTest = q.edges.polygon(i);
//				if(pgTest != pg)
//				{
//					d1 = Rr2Point.d_2(p, pg.point(0));
//					d2 = Rr2Point.d_2(p, pg.point(1));
//					if(d2 < d1)
//						d1 = d2;
//					if(d1 < dClose)
//					{
//						result = pgTest;
//						dClose = d1;
//					}
//				}
//			}
//			if(q1 != null)
//			{
//				d1 = q1.box.d_2(p);
//				d2 = q2.box.d_2(p);
//				d3 = q3.box.d_2(p);
//				d4 = q4.box.d_2(p);
//				
//			}
//		}
//		
//	}
	
	 /**
	 * Walk the tree to find an unvisited corner
     */
    private STLSlice findCorner()
    {
    	STLSlice result = null;
    	
    	if(edges.size() == 2 && !visited)
    	{
    		return this;
    	}
 
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
    		if(edges.size() != 0  && !visited)
    			System.err.println("STLSlice: quad edges: " + edges.size());
    	}
    	
    	return result;
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
				p0 = pg0.point(0);
				p1 = pg0.point(1);
				if(corner.box.point_relative(p0) != 0)
				{
					p1 = p0;
					p0 = pg0.point(1);
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
		q1 = null;
		q2 = null;
		q3 = null;
		q4 = null;
	}
	
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
		sFactor = 1;
		resolution_2 = box.d_2()*1.0e-8;
		//g.addPol(edges);
		divide();
		conquer();
		edges.simplify(1.0e-2);
		//g.addSTL(this);	
		return edges.toCSG();
	}
}
