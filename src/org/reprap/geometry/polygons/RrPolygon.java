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
import org.reprap.geometry.LayerProducer;
import org.reprap.Attributes;
import org.reprap.Preferences;

/**
 * The main boundary-representation polygon class
 */
public class RrPolygon
{
	/**
	 * Used to choose the starting point for a randomized-start copy of a polygon
	 */
	private static Random rangen = new Random(918273);
	
	/**
	 * The (X, Y) points rond the polygon as Rr2Points
	 */
	private List points;
	
	/**
	 * General purpose list of integer flag values, one for each point
	 */
	//private List flags = null;
	
	/**
	 * The atributes of the STL object that this polygon represents
	 */
	private Attributes att;
	
	/**
	 * The minimum enclosing X-Y box round the polygon
	 */
	private RrBox box;
	
	/**
	 * Make an empty polygon
	 */
	public RrPolygon(Attributes a)
	{
		if(a == null)
			System.err.println("RrPolygon(): null attributes!");
		points = new ArrayList();
		//flags = null;
		att = a;
		box = new RrBox();
	}
	
	/**
	 * Get the data
	 * @param i
	 * @return i-th point object of polygon
	 */
	public Rr2Point point(int i)
	{
		return new Rr2Point((Rr2Point)points.get(i));
	}
	
	/**
	 * @param i
	 * @return i-th flag
	 */
//	private int flag(int i)
//	{
//		if(flags == null)
//			System.err.println("RrPolygon.flag(i): flags not initialized.");
//		return ((Integer)flags.get(i)).intValue();
//	}
	
	/**
	 * As a string
	 * @return string representation of polygon
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
//	private void flag(int i, int f)
//	{
//		if(flags == null)
//			System.err.println("RrPolygon.flag(i): flags not initialized.");
//		flags.set(i, new Integer(f));
//	}
	
	/**
	 * Length
	 * @return number of points in polygon
	 */
	public int size()
	{
		return points.size();
	}
	
	/**
	 * Deep copy - NB: attributes _not_ deep copied
	 * @param p
	 */
	public RrPolygon(RrPolygon p)
	{
		this(p.att);
		for(int i = 0; i < p.size(); i++)
			add(new Rr2Point(p.point(i))); //, new Integer((p.flag(i))));		
	}
	
	/**
	 * Add a new point and its flag value to the polygon
	 * @param p
	 * @param f
	 */
	public void add(Rr2Point p)
	{
		points.add(new Rr2Point(p));
		//flags.add(new Integer(f));
		box.expand(p);
	}
	
	/**
	 * @return the attributes
	 */
	public Attributes getAttributes() { return att; }
	
	/**
	 * @return the current surrounding box
	 */
	public RrBox getBox() { return box; }
	
	/**
	 * Put a new polygon and its flag values on the end
	 * (N.B. Attributes of the new polygon are ignored)
	 * @param p
	 */
	public void add(RrPolygon p)
	{
		if(p.size() == 0)
			return;
		for(int i = 0; i < p.size(); i++)
		{
			points.add(new Rr2Point(p.point(i)));
			//flags.add(new Integer(p.flag(i))); 
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
//		if(flags != null)
//			flags.remove(i);
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
	 * @return reversed polygon object
	 */
	public RrPolygon negate()
	{
		RrPolygon result = new RrPolygon(att);
		int f;
		for(int i = size() - 1; i >= 0; i--)
		{
//			if(i > 0)
//				f = flag(i-1);
//			else
//				f = flag(size() - 1);
			result.add(point(i)); //, f);
		}
		return result;
	}
	
	/**
	 * @return same polygon starting at a random vertex
	 */
	public RrPolygon randomStart()
	{
		RrPolygon result = new RrPolygon(att);
		int i = rangen.nextInt(size());
		for(int j = 0; j < size(); j++)
		{
			result.add(new Rr2Point(point(i))); //, new Integer((flag(i))));
			i++;
			if(i >= size())
				i = 0;
		}
		return result;
	}
	
	/**
	 * @return same polygon starting at point incremented from last polgon
	 */
	public RrPolygon incrementedStart(int layerNumber)
	{
		RrPolygon result = new RrPolygon(att);
		int i = layerNumber % size();
		for(int j = 0; j < size(); j++)
		{
			result.add(new Rr2Point(point(i))); //, new Integer((flag(i))));
			i++;
			if(i >= size())
				i = 0;
		}
		return result;
	}
	
	/**
	 * Signed area (-ve result means polygon goes anti-clockwise)
	 * @return signed area
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
	 * Backtrack a given distance, inserting a new point there and returning its index
	 * @param distance to backtrack
	 * @return index of the inserted point
	 */
	public int backStep(double d, boolean outline)
	{
		Rr2Point last, p;
		int start = size() - 1;
		if(outline)
			last = point(0);
		else
		{
			last = point(start);
			start--;
		}
		double sum = 0;
		for(int i = start; i >= 0; i--)
		{
			p = point(i);
			sum += Rr2Point.d(p, last);
			if(sum > d)
			{
				sum = sum - d;
				p = Rr2Point.sub(last, p);
				sum = sum/p.mod();
				p = Rr2Point.add(point(i), Rr2Point.mul(sum, p));
				int j = i + 1;
				if(j < size())
				{
					points.add(j, p);
					//flags.add(j, flags.get(i));
				} else
				{
					points.add(p);
					//flags.add(flags.get(i));					
				}
				return(j);
			}
			last = p;
		}
		return 0;
	}
	
	/**
	 * @param v1
	 * @param d2
	 * @return ??
	 */
	private int findAngleStart(int v1, double d2)
	{
		int leng = size();
		Rr2Point p1 = point(v1%leng);
		int v2 = v1;
		for(int i = 0; i <= leng; i++)
		{
			v2++;
			RrLine line = new RrLine(p1, point(v2%leng));
			for (int j = v1+1; j < v2; j++)
			{
				if (line.d_2(point(j%leng)).x() > d2)
					return v2 - 1;
			}	
		}
		System.err.println("RrPolygon.findAngleStart(): polygon is all one straight line!");
		return -1;
	}
	
	/**
	 * Simplify a polygon by deleting points from it that
	 * are closer than d to lines joining other points
	 * @param d
	 * @return simplified polygon object
	 */
	public RrPolygon simplify(double d)
	{
		int leng = size();
		if(leng <= 3)
			return new RrPolygon(this);
		RrPolygon r = new RrPolygon(att);
		double d2 = d*d;
		int v1 = findAngleStart(0, d2);
		// We get back -1 if the points are in a straight line.
		if (v1<0)
			return new RrPolygon(this);
		r.add(point(v1%leng)); //, flag(v1%leng));
		int v2 = v1;
		while(true)
		{
			// We get back -1 if the points are in a straight line. 
			v2 = findAngleStart(v2, d2);
			if((v2 > leng)||(v2<0))
				return(r);
			r.add(point(v2%leng)); //, flag(v2%leng));
		}
		// The compiler is very clever to spot that no return
		// is needed here...
	}
	
	/**
	 * Remove solitary edges that are shorter than tiny from the
	 * polygon if they are preceeded and followed by gap material.
	 * @param tiny
	 * @return filtered polygon object
	 */
	
//	public RrPolygon filterShort(double tiny)
//	{
//		RrPolygon r = new RrPolygon(att);
//		int oldEdgeFlag = flag(size()-1);
//		int i, ii;
//		
//		for(i = 1; i <= size(); i++)
//		{
//			ii = i%size();
//			if(oldEdgeFlag == LayerProducer.gapMaterial() && flag(ii) == LayerProducer.gapMaterial())
//			{
//				double d = Rr2Point.sub(point(ii), point(i - 1)).mod();
//				if(d > tiny)
//					r.add(point(i - 1), flag(i - 1));
//				//else
//					//System.out.println("Tiny edge removed.");
//			} else
//				r.add(point(i - 1), flag(i - 1));
//			oldEdgeFlag = flag(i - 1);
//		}
//		
//		// Anything left?
//		
//		for(i = 0; i < r.size(); i++)
//		{
//			if(r.flag(i) != LayerProducer.gapMaterial())
//				return r;
//		}
//		
//		// Nothing left
//		
//		return new RrPolygon(att);
//	}
	
	// ****************************************************************************
	
	// Convex hull code - this uses the QuickHull algorithm
	// It finds the convex hull of a list of points from the polygon
	// (which can be the whole polygon if the list is all the points.
	// of course).
	
	/**
	 * find a point from a list of polygon points
	 * @Param i
	 * @param a
	 * @return the point
	 */
	private Rr2Point listPoint(int i, List a)
	{
		return point(((Integer)a.get(i)).intValue());
	}
		
	/**
	 * find a flag from a list of polygon points
	 * @Param i 
	 * @param a
	 * @return the point
	 */
//	private int listFlag(int i, List a)
//	{
//		return flag(((Integer)a.get(i)).intValue());
//	}
		
	/**
	 * find the top (+y) point of a polygon point list
	 * @return the index in the list of the point
	 */
	private int topPoint(List a)
	{
		int top = 0;
		double yMax = listPoint(top, a).y();
		double y;

		for(int i = 1; i < a.size(); i++)
		{
			y = listPoint(i, a).y();
			if(y > yMax)
			{
				yMax = y;
				top = i;
			}
		}
		
		return top;
	}
	
	/**
	 * find the bottom (-y) point of a polygon point list
	 * @return the index in the list of the point
	 */
	private int bottomPoint(List a)
	{
		int bot = 0;
		double yMin = listPoint(bot, a).y();
		double y;

		for(int i = 1; i < a.size(); i++)
		{
			y = listPoint(i, a).y();
			if(y < yMin)
			{
				yMin = y;
				bot = i;
			}
		}
		
		return bot;
	}

	/**
	 * Put the points on a triangle (list a) in the right order
	 * @param a
	 */
	private void clockWise(List a)
	{
		if(a.size() == 3)
		{
			Rr2Point q = Rr2Point.sub(listPoint(1, a), listPoint(0, a));
			Rr2Point r = Rr2Point.sub(listPoint(2, a), listPoint(0, a));
			if(Rr2Point.op(q, r) > 0)
			{
				Object k = a.get(0);
				a.set(0, a.get(1));
				a.set(1, k);
			}
		} else
			System.err.println("clockWise(): not called for a triangle!");
	}
	
	
	/**
	 * Turn the list of hull points into a CSG convex polygon
	 * @param hullPoints
	 * @return CSG representation
	 */	
	public RrCSG toCSGHull(List hullPoints)
	{
		Rr2Point p, q;
		RrCSG hull = RrCSG.universe();
		p = listPoint(hullPoints.size() - 1, hullPoints);
		for(int i = 0; i < hullPoints.size(); i++)
		{
			q = listPoint(i, hullPoints);
			hull = RrCSG.intersection(hull, new RrCSG(new RrHalfPlane(p, q)));
			p = q;
		}

		return hull;
	}
	
	/**
	 * Remove all the points in a list that are within or on the hull
	 * @param inConsideration
	 * @param hull
	 */		
	private void outsideHull(List inConsideration, RrCSG hull)
	{
		Rr2Point p;
		double small = Math.sqrt(Preferences.tiny());
		for(int i = inConsideration.size() - 1; i >= 0; i--)
		{
			p = listPoint(i, inConsideration);
			if(hull.value(p) <= small)	
			{
				inConsideration.remove(i);
			}
		}
	}
	
	/**
	 * Compute the convex hull of all the points in the list
	 * @param points
	 * @return list of point index pairs of the points on the hull
	 */
	private List convexHull(List points)
	{	
		if(points.size() < 3)
		{
			System.err.println("convexHull(): attempt to compute hull for " + points.size() + " points!");
			return new ArrayList();
		}
		
		List inConsideration = new ArrayList(points);
		
		int i;

		// The top-most and bottom-most points must be on the hull
		
		List result = new ArrayList();
		int t = topPoint(inConsideration);
		int b = bottomPoint(inConsideration);
		result.add(inConsideration.get(t));
		result.add(inConsideration.get(b));
		if(t > b)
		{
			inConsideration.remove(t);
			inConsideration.remove(b);
		} else
		{
			inConsideration.remove(b);
			inConsideration.remove(t);			
		}
			
		// Repeatedly add the point that's furthest outside the current hull
		
		int corner, after;
		RrCSG hull;
		double v, vMax;
		Rr2Point p, q;
		RrHalfPlane hp;
		while(inConsideration.size() > 0)
		{
			vMax = 0;   // Need epsilon?
			corner = -1;
			after = -1;
			for(int testPoint = inConsideration.size() - 1; testPoint >= 0; testPoint--)
			{
				p = listPoint(result.size() - 1, result);
				for(i = 0; i < result.size(); i++)
				{
					q = listPoint(i, result);
					hp = new RrHalfPlane(p, q);
					v = hp.value(listPoint(testPoint, inConsideration));
					if(result.size() == 2)
						v = Math.abs(v);
					if(v > vMax)
					{
						after = i;
						vMax = v;
						corner = testPoint;
					}
					p = q;
				}
			}
			
			if(corner >= 0)
			{
				result.add(after, inConsideration.get(corner));
				inConsideration.remove(corner);
			} else if(inConsideration.size() > 0)
			{
				System.err.println("convexHull(): points left, but none included!");
				return result;
			}
			
			// Get the first triangle in the right order
			
			if(result.size() == 3)
				clockWise(result);

			// Remove all points within the current hull from further consideration
			
			hull = toCSGHull(result);
			outsideHull(inConsideration, hull);
		}
		
		return result;
	}
	
	// **************************************************************************
	
	// Convert polygon to CSG form 
	// using Kai Tang and Tony Woo's algorithm.
	
	/**
	 * Construct a list of all the points in the polygon
	 * @return list of indices of points in the polygons
	 */
	private List allPoints()
	{
		List points = new ArrayList();
		for(int i = 0; i < size(); i++)
				points.add(new Integer(i));
		return points;
	}
	
// 	private void flagSet(int f, List a)
// 	{
// 	 		for(int i = 0; i < a.size(); i++)
//  			flag(((Integer)a.get(i)).intValue(), f);
//  	}
	
	/**
	 * Set all the flag values in a list the same
	 * @param f
	 */
	private void flagSet(int f, List a, int[] flags)
	{
//		if(flags == null)
//		{
//			flags = new int[size()];
//			for(int i = 0; i < a.size(); i++)
//				flags[((Integer)a.get(i)).intValue()] = f;
//		} else
//		{
			for(int i = 0; i < a.size(); i++)
				flags[((Integer)a.get(i)).intValue()] = f;
//		}

	}	
	
	/**
	 * Get the next whole section to consider from list a
	 * @param a
	 * @param level
	 * @return the section (null for none left)
	 */
	private List polSection(List a, int level, int[] flags)
	{
		int flag, oldi;
		oldi = a.size() - 1;
		int oldFlag = flags[((Integer)a.get(oldi)).intValue()]; //listFlag(oldi, a);

		int ptr = -1;
		for(int i = 0; i < a.size(); i++)
		{
			flag = flags[((Integer)a.get(i)).intValue()];//listFlag(i, a);

			if(flag < level && oldFlag >= level) 
			{
				ptr = oldi;
				break;
			}
			oldi = i;
			oldFlag = flag;
		}
		
		if(ptr < 0)
			return null;
		
		List result = new ArrayList();
		result.add(a.get(ptr));
		ptr++;
		if(ptr > a.size() - 1)
			ptr = 0;
		while(flags[((Integer)a.get(ptr)).intValue()] < level) //listFlag(ptr, a)
		{
			result.add(a.get(ptr));
			ptr++;
			if(ptr > a.size() - 1)
				ptr = 0;
		}

		result.add(a.get(ptr));

		return result;
	}
	
	/**
	 * Compute the CSG representation of a (sub)list recursively
	 * @param a
	 * @param level
	 * @return CSG representation
	 */
	private RrCSG toCSGRecursive(List a, int level, boolean closed, int[] flags)
	{	
		flagSet(level, a, flags);	
		level++;
		List ch = convexHull(a);
		if(ch.size() < 3)
		{
			System.err.println("toCSGRecursive() - null convex hull!");
			return RrCSG.nothing();
		}
		
		flagSet(level, ch, flags);
		RrCSG hull;


		if(level%2 == 1)
			hull = RrCSG.universe();
		else
			hull = RrCSG.nothing();

		// Set-theoretically combine all the real edges on the convex hull

		int i, oldi, flag, oldFlag, start;
		
		if(closed)
		{
			oldi = a.size() - 1;
			start = 0;
		} else
		{
			oldi = 0;
			start = 1;
		}
		
		for(i = start; i < a.size(); i++)
		{
			oldFlag = flags[((Integer)a.get(oldi)).intValue()]; //listFlag(oldi, a);
			flag = flags[((Integer)a.get(i)).intValue()]; //listFlag(i, a);

			if(oldFlag == level && flag == level)
			{
				RrHalfPlane hp = new RrHalfPlane(listPoint(oldi, a), listPoint(i, a));
				if(level%2 == 1)
					hull = RrCSG.intersection(hull, new RrCSG(hp));
				else
					hull = RrCSG.union(hull, new RrCSG(hp));
			} 
			
			oldi = i;
		}
		
		// Finally deal with the sections on polygons that form the hull that
		// are not themselves on the hull.
		
		List section = polSection(a, level, flags);
		while(section != null)
		{
			if(level%2 == 1)
				hull = RrCSG.intersection(hull,
						toCSGRecursive(section, level, false, flags));
			else
				hull = RrCSG.union(hull, 
						toCSGRecursive(section, level, false, flags));
			section = polSection(a, level, flags);
		}
		
		return hull;
	}
	
	/**
	 * Convert a polygon to CSG representation
	 * @param tolerance
	 * @return CSG polygon object based on polygon and tolerance 
	 */
	public RrCSGPolygon toCSG(double tolerance)
	{
		
		RrPolygon copy = new RrPolygon(this);
		if(copy.area() < 0)
			copy = copy.negate();

		List all = copy.allPoints();
		int [] flags = new int[copy.size()];
		RrCSG expression = copy.toCSGRecursive(all, 0, true, flags);

		RrBox b = copy.box.scale(1.1);
		//expression = expression.simplify(tolerance);
		if(att == null)
			System.err.println("toCSG(): null attribute!");
		RrCSGPolygon result = new RrCSGPolygon(expression, b, att);
		
		return result;
	}
	
}


