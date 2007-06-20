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
import java.io.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import org.reprap.gui.STLObject;
import org.reprap.Preferences;

// Small class to hold line segments and the quads in which their ends lie

class LineSegment
{
	public Rr2Point a, b;
	public STLSlice qa, qb;
	
	/**
	 * Constructor takes two intersection points with an STL triangle edge.
	 * @param p
	 * @param q
	 */
	public LineSegment(Rr2Point p, Rr2Point q)
	{
		a = p;
		b = q;
		qa = null;
		qb = null;
	}

	/**
	 * A quad contains (we hope...) the ends of two segments - record that
	 * @param q
	 */
	public static void setQuad(STLSlice q)
	{
		if(q.edges().size() != 2)
			System.err.println("LineSegment.setQuad(): wrong edge count: " + 
					q.edges().size());
		int count = 0;
		if(q.box().point_relative(q.segment(0).a) == 0)
		{
			q.segment(0).qa = q;
			count++;
		}
		if(q.box().point_relative(q.segment(0).b) == 0)
		{
			q.segment(0).qb = q;
			count++;
		}
		if(q.box().point_relative(q.segment(1).a) == 0)
		{
			q.segment(1).qa = q;
			count++;
		}
		if(q.box().point_relative(q.segment(1).b) == 0)
		{
			q.segment(1).qb = q;
			count++;
		}
		
		if(count != 2)
			System.err.println("LineSegment.setQuad(): dud end count = " + count);
	}
}

public class STLSlice 
{
	List stls;                     ///< The STL objects in 3D
	private List edges;            ///< List of the edges with points in this one
	private RrBox box;             ///< Its enclosing box
	private STLSlice q1,           ///< Quad tree division - NW
	q2,                            ///< NE 
	q3,                            ///< SE
	q4;                            ///< SW
	private double resolution_2;   ///< Squared diagonal of the smallest box to go to
	private double sFactor;        ///< Swell factor for division
	private boolean visited;       ///< Flag to indicate the quad's been dealt with
	private static List triangles; ///< All the STL triangles and part-triangles below Z
	private Shape3D below;         ///< Made from the below-Z triangles
	
	/**
	 * Null constructor just initialises a few things.
	 */
	public STLSlice()
	{
		edges = new ArrayList();
		q1 = null;
		q2 = null;
		q3 = null;
		q4 = null;
		box = new RrBox();
		visited = false;
		stls = null;
		sFactor = 1;
		resolution_2 = Preferences.tiny();
		below = null;
		triangles = new ArrayList();
	}
	
	/**
	 * This constructor records the list of STL objects. 
	 * @param s
	 */
	public STLSlice(List s)
	{
		this();
		stls = s;
	}
	
	/**
	 * Add a new line segment to the list.
	 * @param p
	 * @param q
	 */
	public void add(Rr2Point p, Rr2Point q)
	{
		edges.add(new LineSegment(p, q));
	}
	
	/**
	 * Set the resolution to the square of a length.
	 * @param r
	 */
	public void setResolution(double r)
	{
		resolution_2 = r*r;
	}
	
	// Return the contents
	
	public RrBox box()
	{
		return box;
	}
	
	public List edges()
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
	
	public LineSegment segment(int i)
	{
		return (LineSegment)edges.get(i);
	}
	
	public Shape3D getShape3D()
	{
		//return null;
		return below;
	}
	
	/**
	 * Not sure about this - at the moment it clicks all points
	 * onto an 0.01 mm grid.
	 * @param x
	 * @return grid value nearest x
	 */
	private double toGrid(double x)
	{
		//return x;
		return (double)((int)(x*Preferences.grid() + 0.5))*Preferences.gridRes();
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
		boolean twoBelow = false;
		
		if(p.z < z)
			pat = pat | 1;
		if(q.z < z)
			pat = pat | 2;
		if(r.z < z)
			pat = pat | 4;
		
		switch(pat)
		{
		case 0:
			return;
		case 7:
			triangles.add(p);
			triangles.add(q);
			triangles.add(r);
			return;
		case 6:
			twoBelow = true;
		case 1:
			odd = p;
			even1 = q;
			even2 = r;
			break;
		case 5:
			twoBelow = true;
		case 2:
			odd = q;
			even1 = p;
			even2 = r;
			break;
		case 3:
			twoBelow = true;
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
		Rr2Point e1 = new Rr2Point(odd.x + t*even1.x, odd.y + t*even1.y);	
		Point3d e3_1 = new Point3d(e1.x(), e1.y(), z);
		e1 = new Rr2Point(toGrid(e1.x()), toGrid(e1.y()));
		t = (z - odd.z)/even2.z;
		Rr2Point e2 = new Rr2Point(odd.x + t*even2.x, odd.y + t*even2.y);
		Point3d e3_2 = new Point3d(e2.x(), e2.y(), z);
		e2 = new Rr2Point(toGrid(e2.x()), toGrid(e2.y()));
		
		
		if(!Rr2Point.same(e1, e2, Preferences.lessGridSquare()))
		{
			add(e1, e2);
			box.expand(e1);
			box.expand(e2);
		}
		
		if(twoBelow)
		{
			even1.add((Tuple3d)odd);
			even2.add((Tuple3d)odd);
			triangles.add(even1);
			triangles.add(even2);
			triangles.add(e3_1);
			triangles.add(e3_2);
			triangles.add(e3_1);
			triangles.add(even2);
		} else
		{
			triangles.add(odd);
			triangles.add(e3_1);
			triangles.add(e3_2);
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
	
	/**
	 * Constructor for building a branch quad in the division
	 * @param pgl
	 * @param b
	 * @param res
	 * @param fac
	 */
	private STLSlice(List pgl, RrBox b, double res, double fac)
	{
		edges = pgl;
		box = b;
		q1 = null;
		q2 = null;
		q3 = null;
		q4 = null;
		resolution_2 = res;
		sFactor = fac;
		visited = false;
		prune();  // Remove edges not ending in this quad.
	}
	
	/**
	 * Prune the edge list to the box so that only segments
	 * with endpoints in the box are retained.
	 */
	private void prune()
	{
		List result = new ArrayList();
		
		for(int i = 0; i < edges.size(); i++)
		{
			if(box.point_relative(segment(i).a) == 0 ||
					box.point_relative(segment(i).b) == 0)
				result.add(edges.get(i));
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
		
//		 Put the results in the children
		
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
	public void divide()
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
			if(edges.size() <= 0)
				return;
			
			boolean divideFurther = false;
	   		for(int i = 0; i < edges.size(); i++)
    		{
    			if(box.point_relative(segment(i).a) == 0 &&  
    					box.point_relative(segment(i).b) == 0)
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
	   		{
	   			// We end up here occasionally - it seems to do no harm...
	   			//System.err.println("STLSlice.divide(): only one end in box!");
	   		} else
	   			LineSegment.setQuad(this);
		}
	}
	
	 /**
	 * Check the list to find an unvisited corner
     */
    private STLSlice findCorner()
    {
    	STLSlice result = null;
    	
		for (int i = 0; i < edges.size(); i++)
		{
			LineSegment s = segment(i);
			if (!s.qa.visited && !s.qb.visited)
				return s.qa;
		}
 
    	return result;
    }

    /**
     * Useful for debugging - plot a bit of the quad tree.
     *
     */
    private void quickPlot()
    {
    	RrGraphics g = new RrGraphics(box.scale(1.5), true);
		g.addSTL(this);
		System.out.print("Type any character: ");
		System.out.flush();
		try
		{
			System.in.read();
		} catch(IOException err)
		{
			System.err.print("Uh?");
		}
		g = null;
    }
//    
//    /**
//     * Useful for debugging - print statistics
//     *
//     */
//    public void reportStats()
//    {
//    	int single = 0;
//    	int twin = 0;
//    	for(int i = 0; i < edges.size(); i++)
//    	{
//    		LineSegment s = segment(i);
//    		if(s.qa == null)
//    			single++;
//    		if(s.qb == null)
//    			single++;
//    		if(s.qa != null && s.qb != null)
//    			twin++;
//    	}
//    	System.out.println("STLSlice.reportStats() - lines: " + edges.size()
//    			+ " double-ended quads: " + twin +
//    			" single ends: " + single);
//    }
    
	/**
	 * Stitch up the line segment ends in the quad tree.
	 * @param fg
	 * @param fs
	 * @return a list of all the resulting polygons.
	 */
	private RrPolygonList conquer(int fg, int fs)
	{
		RrPolygonList pgl = new RrPolygonList();
		RrPolygon pg;
		STLSlice corner, newCorner, startCorner;
		LineSegment edge, newEdge;
		Rr2Point p0, p1;
		
		startCorner = findCorner();
		while(startCorner != null)
		{
			corner = startCorner;
			edge = corner.segment(0);
			pg = new RrPolygon();
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
				
				newEdge = corner.segment(1);
				if(edge == newEdge)
					newEdge = corner.segment(0);
				
				p0 = edge.a;
				if(edge.qa != corner)
					p0 = edge.b;
				
				p1 = newEdge.a;
				newCorner = newEdge.qb;
				if(corner == newCorner)
				{
					newCorner = newEdge.qa;
					p1 = newEdge.b;
				}
				
				Rr2Point vertex = Rr2Point.mul(0.5, Rr2Point.add(p0, p1));
				pg.add(vertex, fg);
				edge = newEdge;
				corner = newCorner;
			} while (corner != startCorner);
			if(pg.size() > 2)  // Throw away "noise"...
			{
				pg.flag(pg.size() - 1, fs);
				pgl.add(pg);
			}
			startCorner = findCorner();
		}
		return pgl;
	}
	
	/**
	 * Find the maximum height of the object(s) to be built
	 * @return that height
	 */
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
	 * @return a CSG representation of all the polygons in the slice
	 */
	public RrCSGPolygon slice(double z, int fg, int fs, Color3f baseColour)
	{
		Point3d p, q, r;
		Vector3f a, b, c, normal;
		
		if(stls == null)
		{
			System.err.println("slice(): no STL list loaded!");
			return null;
		}
			
		// Clear out any residues from last z value
		
		q1 = null;
		q2 = null;
		q3 = null;
		q4 = null;
		visited = false;
		sFactor = 1;
		resolution_2 = Preferences.tiny(); 
		edges = new ArrayList();
		
		// Run through all the STL objects adding line segments to edges
		// wherever the z plane cuts them.
		
		STLObject stl;
		Transform3D trans;
		BranchGroup bg;
		Enumeration things;
		
		triangles = new ArrayList();
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
		
		if(triangles.size() > 0)
		{
			
			TriangleArray t = new TriangleArray(triangles.size(), GeometryArray.COORDINATES | GeometryArray.COLOR_3 | GeometryArray.NORMALS);
			for(int i = 0; i < triangles.size(); i = i + 3)
			{
				p = (Point3d)triangles.get(i);
				q = (Point3d)triangles.get(i+1);
				r = (Point3d)triangles.get(i+2);
				
				t.setCoordinate(i, p);
				t.setColor(i, baseColour);
				t.setCoordinate(i+1, q);
				t.setColor(i+1, baseColour);
				t.setCoordinate(i+2, r);
				t.setColor(i+2, baseColour);
				q.sub(p);
				r.sub(p);
				a = new Vector3f(q);
				b = new Vector3f(r);
				normal = a;
				normal.cross(a, b);
				normal.normalize();
				t.setNormal(i, normal);
				t.setNormal(i+1, normal);
				t.setNormal(i+2, normal);
			}
			below = new Shape3D(t);
			triangles = new ArrayList();
		}
		
		// Make sure nothing falls down the cracks.
		
		sFactor = Preferences.swell();
		box = box.scale(sFactor);
		resolution_2 = box.d_2()*Preferences.tiny();
		
		// Recursively generate the quad tree.  The aim is to have each
		// leaf quad containing either 0 or 2 ends of different line
		// segments.  Then we just run round joining up all the pairs of
		// ends.

		divide();

		// Run round joining up all the pairs of ends...
		
		RrPolygonList pgl = conquer(fg, fs);
		
		// Remove wrinkles
	   	//RrGraphics g = new RrGraphics(pgl, false);
		pgl = pgl.simplify(Preferences.gridRes()*1.5);
		//RrGraphics g2 = new RrGraphics(pgl, false);
		// Check for a silly result.
		
		if(pgl.size() < 1)
		{
			System.err.println("slice(): nothing there!");
			return null;
		} else
			return pgl.toCSG(Preferences.tiny());
	}
}
