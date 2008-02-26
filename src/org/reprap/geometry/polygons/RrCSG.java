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
 
 */

package org.reprap.geometry.polygons;

import javax.media.j3d.Link;

/**
 * RepRap Constructive Solid Geometry class
 * 
 * RrCSG: 2D polygons as boolean combinations of half-planes
 * First version 14 November 2005 
 */
public class RrCSG
{
	
	/**
	 * Universal set 
	 */
	private static final RrCSG u = new RrCSG(true);  
	
	/**
	 * Null set  
	 */
	private static final RrCSG n = new RrCSG(false); 
	
	/**
	 * Leaf half plane 
	 */
	private RrHalfPlane hp;
	
	/**
	 * Type of set
	 */
	private RrCSGOp op;

	//private int op;			   ///< Will go at Java 1.5
	
	/**
	 * Non-leaf child operands 
	 */
	private RrCSG c1, c2; 
	
	/**
	 * The complement (if there is one) 
	 */
	private RrCSG comp;        
	
	/**
	 * How much is in here (leaf count)?
	 */
	private int complexity;
	
	/**
	 * Make a leaf from a single half-plane
	 * @param h
	 */
	public RrCSG(RrHalfPlane h)
	{
		hp = new RrHalfPlane(h);
		op = RrCSGOp.LEAF;
		c1 = null;
		c2 = null;
		comp = null;
		complexity = 1;
	}
	
	/**
	 * One off constructor for the universal and null sets
	 * @param b
	 */
	private RrCSG(boolean b)
	{
		hp = null;
		if(b)
			op = RrCSGOp.UNIVERSE;
		else
			op = RrCSGOp.NULL;
		c1 = null;
		c2 = null;
		comp = null;   // Resist temptation to be clever here
		complexity = 0;
	}
	
	/**
	 * Universal or null set
	 * @return universal or null set
	 */
	public static RrCSG universe()
	{
		return u;
	}
	
	/**
	 * @return nothing/null set
	 */
	public static RrCSG nothing()
	{
		return n;
	}
	
	/**
	 * Deep copy
	 * @param c
	 */
	public RrCSG(RrCSG c)
	{
		if(c == u || c == n)
			System.err.println("RrCSG deep copy: copying null or universal set.");
		
		if(c.hp != null)
			hp = new RrHalfPlane(c.hp);
		else
			hp = null;
		
		if(c.c1 != null)
			c1 = new RrCSG(c.c1);
		else
			c1 = null;
		
		if(c.c2 != null)
			c2 = new RrCSG(c.c2);
		else
			c2 = null;
		
		comp = null;  // This'll be built if it's needed
		
		op = c.op;
		complexity = c.complexity;
	}
	
	/**
	 * Get children, operator etc
	 * @return children
	 */
	public RrCSG c_1() { return c1; }
	public RrCSG c_2() { return c2; }
	public RrCSGOp operator() { return op; }
	//public int operator() { return op; }
	public RrHalfPlane plane() { return hp; }
	public int complexity() { return complexity; }
	
	/**
	 * Convert to a string
	 * @param result
	 * @param white
	 * @return string representation
	 */
	private String toString_r(String result, String white)
	{
		switch(op)
		{
		case LEAF:
			result = result + white + hp.toString() + "\n";
			break;
			
		case NULL:
			result = result + white + "0\n";
			break;
			
		case UNIVERSE:
			result = result + white + "U\n";
			break;
			
		case UNION:
			result = result + white + "+\n";
			white = white + " ";
			result = c1.toString_r(result, white);
			result = c2.toString_r(result, white);
			break;
			
		case INTERSECTION:
			result = result + white + "&\n";
			white = white + " ";
			result = c1.toString_r(result, white);
			result = c2.toString_r(result, white);
			break;
			
		default:
			System.err.println("toString_r(): invalid operator.");
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		String result = "RrCSG: complexity = " + 
			Integer.toString(complexity) + "\n";
		result = toString_r(result, " ");
		return result;
	}
	
	/**
	 * Private constructor for common work setting up booleans
	 * @param a
	 * @param b
	 */
	private RrCSG(RrCSG a, RrCSG b)
	{
		hp = null;
		comp = null;
		if(a.complexity <= b.complexity) // So we know the 1st child is the simplest
		{
			c1 = a;
			c2 = b;
		} else
		{
			c1 = b;
			c2 = a;
		}
		complexity = c1.complexity + c2.complexity;
	}
	
	/**
	 * Boolean operations, with de Morgan simplifications
	 * @param a
	 * @param b
	 * @return union of passed CSG objects a and b
	 */
	public static RrCSG union(RrCSG a, RrCSG b)
	{
		if(a == b)
			return a;
		if(a.op == RrCSGOp.NULL)
			return b;
		if(b.op == RrCSGOp.NULL)
			return a;
		if((a.op == RrCSGOp.UNIVERSE) || (b.op == RrCSGOp.UNIVERSE))
			return universe();
		
		if(a.comp != null && b.comp != null)
			if(a.comp == b)
				return universe();
		
		RrCSG r = new RrCSG(a, b);
		r.op = RrCSGOp.UNION;
		return r;
	}
	
	/**
	 * Boolean operation to perform an intersection
	 * @param a
	 * @param b
	 * @return intersection of passed CSG objects a and b
	 */
	public static RrCSG intersection(RrCSG a, RrCSG b)
	{
		if(a == b)
			return a;
		if(a.op == RrCSGOp.UNIVERSE)
			return b;
		if(b.op == RrCSGOp.UNIVERSE)
			return a;
		if((a.op == RrCSGOp.NULL) || (b.op == RrCSGOp.NULL))
			return nothing();
		
		if(a.comp != null && b.comp != null)
			if(a.comp == b)
				return nothing();
		
		RrCSG r = new RrCSG(a, b);
		r.op = RrCSGOp.INTERSECTION;
		return r;
	}
	
	/**
	 * Lazy evaluation for complement.
	 * @return complement
	 */
	public RrCSG complement()
	{		
		if(comp != null)
			return comp;
		
		RrCSG result;
		
		switch(op)
		{
		case LEAF:
			result = new RrCSG(hp.complement());
			break;
			
		case NULL:
			return universe();
			
		case UNIVERSE:
			return nothing();
			
		case UNION:
			result = intersection(c1.complement(), c2.complement());
			break;
			
		case INTERSECTION:
			result = union(c1.complement(), c2.complement());
			break;
			
		default:
			System.err.println("complement(): invalid operator.");
			return nothing();
		}
		
		// Remember, so we don't have to do it again.
		// (I do hope that the Java garbage collector is up to 
		// spotting this deadly embrace, or we - I mean it - has
		// a memory leak.)
		
		comp = result;
		result.comp = this;
		
		return comp;
	}
	
	/**
	 * Set difference is intersection with complement
	 * @param a
	 * @param b
	 * @return set difference as CSG object based on input CSG objects a and b
	 */		
	public static RrCSG difference(RrCSG a, RrCSG b)
	{
		return intersection(a, b.complement());
	}
	
	/**
	 * Make a rectangle
	 */
	public static RrCSG RrCSGFromBox(RrBox b)
	{
		RrCSG r = new RrCSG(new RrHalfPlane(b.nw(), b.ne()));
		r = RrCSG.intersection(r, new RrCSG(new RrHalfPlane(b.ne(), b.se())));
		r = RrCSG.intersection(r, new RrCSG(new RrHalfPlane(b.se(), b.sw())));
		r = RrCSG.intersection(r, new RrCSG(new RrHalfPlane(b.sw(), b.nw())));
		return r;
	}
	
	
//	/**
//	 * Regularise a set with a contents of 3
//	 * This assumes simplify has been run over this set
//	 * @return regularised CSG object
//	 */	
//	private RrCSG reg_3_old()
//	{
//		RrCSG result = this;
//		
//		if(complexity != 3)
//			return result;
//		
//		boolean different = true;
//		boolean acomp = false;
//		RrCSG temp;
//		if(c1 == c2.c1)
//			different = false;
//		if(c1 == c2.c2)
//		{
//			different = false;
//			temp = c2.c2;
//			c2.c2 = c2.c1;
//			c2.c1 = temp;
//		}
//		
//		if(c1.comp != null)
//		{
//			if(c1.comp == c2.c1)
//			{
//				different = false;
//				acomp = true;
//			}
//			if(c1.comp == c2.c2) 
//			{
//				different = false;
//				acomp = true;
//				temp = c2.c2;
//				c2.c2 = c2.c1;
//				c2.c1 = temp;
//			}
//		}
//		
//		if(different)
//			return result;
//		
//		if(acomp)
//		{
//			if(op == RrCSGOp.UNION) 
//			{
//				if(c2.op == RrCSGOp.UNION)
//					result = universe();
//				else
//					result = union(c1, c2.c2);
//			}else 
//			{
//				if(c2.op == RrCSGOp.UNION)
//					result = intersection(c1, c2.c2);
//				else
//					result = nothing();
//			}            
//		} else
//		{
//			if(op == RrCSGOp.UNION) 
//			{
//				if(c2.op == RrCSGOp.UNION)
//					result = c2;
//				else
//					result = c1;
//			}else 
//			{
//				if(c2.op == RrCSGOp.UNION)
//					result = c1;
//				else
//					result = c2;
//			}
//		}
//		return result;
//	}
	
	
//	/**
//	 * Regularise a set with a contents of 3
//	 * This assumes simplify has been run over this set
//	 * @return regularised CSG object
//	 */	
//	private RrCSG reg_3()
//	{
//		RrCSG r = this;
//		
//		if(complexity != 3)
//			return r;
//		
//		RrCSG a = c1;
//		RrCSG b = c2.c1;
//		RrCSG c = c2.c2;
//		
//		int caseVal = 0;
//		boolean noEquals = true;
//
//		if(a == b)
//			noEquals = false;
//		if(a == c)
//		{
//			noEquals = false;
//			caseVal |= 4;
//		}
//		if(b == c)
//		{
//			noEquals = false;
//			caseVal |= 2;
//			caseVal |= 4;
//		}
//		if(a == b.comp)
//		{
//			noEquals = false;
//			caseVal |= 1;
//		}
//		if(a == c.comp)
//		{
//			noEquals = false;
//			caseVal |= 1;
//			caseVal |= 4;
//		}
//		if(b == c.comp)
//		{
//			noEquals = false;
//			caseVal |= 1;
//			caseVal |= 2;
//			caseVal |= 4;
//		}
//		
//		if(noEquals)
//			return r;
//		
//		if(op == RrCSGOp.INTERSECTION)
//			caseVal |= 8;
//		if(c2.op == RrCSGOp.INTERSECTION)
//			caseVal |= 16;
//		
//		switch(caseVal)
//		{
//		case 0: 
//			r = RrCSG.union(a, c);
//			break;
//		case 1: 
//			r = RrCSG.universe();
//			break;
//		case 4: 
//			r = RrCSG.union(a, b);
//			break;
//		case 5: 
//			r = RrCSG.universe();
//			break;
//		case 6: 
//			r = RrCSG.union(b, a);
//			break;
//		case 7: 
//			r = RrCSG.universe();
//			break;
//		case 8: 
//			r = a;
//			break;
//		case 9: 
//			r = RrCSG.union(a.complement(), c);
//			break;
//		case 12: 
//			r = b;
//			break;
//		case 13: 
//			r = RrCSG.union(a, b.complement());
//			break;
//		case 14: 
//			r = RrCSG.union(b, a);
//			break;
//		case 15: 
//			r = b;
//			break;
//		case 16: 
//			r = a;
//			break;
//		case 17: 
//			r = RrCSG.intersection(a.complement(), c);
//			break;
//		case 20: 
//			r = b;
//			break;
//		case 21: 
//			r = RrCSG.intersection(a, b.complement());
//			break;
//		case 22: 
//			r = RrCSG.intersection(b, a);
//			break;
//		case 23: 
//			r = b;
//			break;
//		case 24: 
//			r = RrCSG.intersection(a, c);
//			break;
//		case 25: 
//			r = RrCSG.nothing();
//			break;
//		case 28: 
//			r = RrCSG.intersection(a, b);
//			break;
//		case 29: 
//			r = RrCSG.nothing();
//			break;
//		case 30: 
//			r = RrCSG.intersection(b, a);
//			break;
//		case 31: 
//			r = RrCSG.nothing();
//			break;		
//		default:
//			System.err.println("RrCSG.reg_3(): dud case value: " + caseVal);
//		}
//		
//		return r;
//	}
	
	/**
	 * Regularise a set with a contents of 3
	 * This assumes removeDuplicates has been run over this set
	 * @return regularised CSG object
	 */	
	private RrCSG reg_3()
	{
		RrCSG r = this;
		
		if(complexity != 3)
			return r;
		
		RrCSG a = c1;
		//RrCSG b = c2.c1;
		//RrCSG c = c2.c2;
		
		RrCSG c = c2.c1;
		RrCSG b = c2.c2;
		
		int caseVal = 0;
		boolean noEquals = true;

		if(a == b)
			noEquals = false;
		if(a == c)
		{
			noEquals = false;
			caseVal |= 4;
		}
		if(b == c)
		{
			noEquals = false;
			caseVal |= 2;
			caseVal |= 4;
		}
		if(a == b.comp)
		{
			noEquals = false;
			caseVal |= 1;
		}
		if(a == c.comp)
		{
			noEquals = false;
			caseVal |= 1;
			caseVal |= 4;
		}
		if(b == c.comp)
		{
			noEquals = false;
			caseVal |= 1;
			caseVal |= 2;
			caseVal |= 4;
		}
		
		if(noEquals)
			return r;
		
		if(op == RrCSGOp.INTERSECTION)
			caseVal |= 8;
		if(c2.op == RrCSGOp.INTERSECTION)
			caseVal |= 16;
		
		// The code in the following switch is automatically
		// generated by the program CodeGenerator.java
		
		switch(caseVal)
		{
		case 0: 
			// r = RrCSG.union(a, RrCSG.union(b, c));
			// a = b ->

			// a c 
			// 0 0 | 0 
			// 1 0 | 1 
			// 0 1 | 1 
			// 1 1 | 1 
				r = RrCSG.union(a, c);
				break;

			case 1: 
			// r = RrCSG.union(a, RrCSG.union(b, c));
			// a = !b ->

			// a c 
			// 0 0 | 1 
			// 1 0 | 1 
			// 0 1 | 1 
			// 1 1 | 1 
				r = RrCSG.universe();
				break;

			case 4: 
			// r = RrCSG.union(a, RrCSG.union(b, c));
			// a = c ->

			// a b 
			// 0 0 | 0 
			// 1 0 | 1 
			// 0 1 | 1 
			// 1 1 | 1 
				r = RrCSG.union(a, b);
				break;

			case 5: 
			// r = RrCSG.union(a, RrCSG.union(b, c));
			// a = !c ->

			// a b 
			// 0 0 | 1 
			// 1 0 | 1 
			// 0 1 | 1 
			// 1 1 | 1 
				r = RrCSG.universe();
				break;

			case 6: 
			// r = RrCSG.union(a, RrCSG.union(b, c));
			// b = c ->

			// a b 
			// 0 0 | 0 
			// 1 0 | 1 
			// 0 1 | 1 
			// 1 1 | 1 
				r = RrCSG.union(b, a);
				break;

			case 7: 
			// r = RrCSG.union(a, RrCSG.union(b, c));
			// b = !c ->

			// a b 
			// 0 0 | 1 
			// 1 0 | 1 
			// 0 1 | 1 
			// 1 1 | 1 
				r = RrCSG.universe();
				break;

			case 8: 
			// r = RrCSG.union(a, RrCSG.intersection(b, c));
			// a = b ->

			// a c 
			// 0 0 | 0 
			// 1 0 | 1 
			// 0 1 | 0 
			// 1 1 | 1 
				r = a;
				break;

			case 9: 
			// r = RrCSG.union(a, RrCSG.intersection(b, c));
			// a = !b ->

			// a c 
			// 0 0 | 0 
			// 1 0 | 1 
			// 0 1 | 1 
			// 1 1 | 1 
				r = RrCSG.union(a, c);
				break;

			case 12: 
			// r = RrCSG.union(a, RrCSG.intersection(b, c));
			// a = c ->

			// a b 
			// 0 0 | 0 
			// 1 0 | 1 
			// 0 1 | 0 
			// 1 1 | 1 
				r = a;
				break;

			case 13: 
			// r = RrCSG.union(a, RrCSG.intersection(b, c));
			// a = !c ->

			// a b 
			// 0 0 | 0 
			// 1 0 | 1 
			// 0 1 | 1 
			// 1 1 | 1 
				r = RrCSG.union(a, b);
				break;

			case 14: 
			// r = RrCSG.union(a, RrCSG.intersection(b, c));
			// b = c ->

			// a b 
			// 0 0 | 0 
			// 1 0 | 1 
			// 0 1 | 1 
			// 1 1 | 1 
				r = RrCSG.union(b, a);
				break;

			case 15: 
			// r = RrCSG.union(a, RrCSG.intersection(b, c));
			// b = !c ->

			// a b 
			// 0 0 | 0 
			// 1 0 | 1 
			// 0 1 | 0 
			// 1 1 | 1 
				r = a;
				break;

			case 16: 
			// r = RrCSG.intersection(a, RrCSG.union(b, c));
			// a = b ->

			// a c 
			// 0 0 | 0 
			// 1 0 | 1 
			// 0 1 | 0 
			// 1 1 | 1 
				r = a;
				break;

			case 17: 
			// r = RrCSG.intersection(a, RrCSG.union(b, c));
			// a = !b ->

			// a c 
			// 0 0 | 0 
			// 1 0 | 0 
			// 0 1 | 0 
			// 1 1 | 1 
				r = RrCSG.intersection(a, c);
				break;

			case 20: 
			// r = RrCSG.intersection(a, RrCSG.union(b, c));
			// a = c ->

			// a b 
			// 0 0 | 0 
			// 1 0 | 1 
			// 0 1 | 0 
			// 1 1 | 1 
				r = a;
				break;

			case 21: 
			// r = RrCSG.intersection(a, RrCSG.union(b, c));
			// a = !c ->

			// a b 
			// 0 0 | 0 
			// 1 0 | 0 
			// 0 1 | 0 
			// 1 1 | 1 
				r = RrCSG.intersection(a, b);
				break;

			case 22: 
			// r = RrCSG.intersection(a, RrCSG.union(b, c));
			// b = c ->

			// a b 
			// 0 0 | 0 
			// 1 0 | 0 
			// 0 1 | 0 
			// 1 1 | 1 
				r = RrCSG.intersection(b, a);
				break;

			case 23: 
			// r = RrCSG.intersection(a, RrCSG.union(b, c));
			// b = !c ->

			// a b 
			// 0 0 | 0 
			// 1 0 | 1 
			// 0 1 | 0 
			// 1 1 | 1 
				r = a;
				break;

			case 24: 
			// r = RrCSG.intersection(a, RrCSG.intersection(b, c));
			// a = b ->

			// a c 
			// 0 0 | 0 
			// 1 0 | 0 
			// 0 1 | 0 
			// 1 1 | 1 
				r = RrCSG.intersection(a, c);
				break;

			case 25: 
			// r = RrCSG.intersection(a, RrCSG.intersection(b, c));
			// a = !b ->

			// a c 
			// 0 0 | 0 
			// 1 0 | 0 
			// 0 1 | 0 
			// 1 1 | 0 
				r = RrCSG.nothing();
				break;

			case 28: 
			// r = RrCSG.intersection(a, RrCSG.intersection(b, c));
			// a = c ->

			// a b 
			// 0 0 | 0 
			// 1 0 | 0 
			// 0 1 | 0 
			// 1 1 | 1 
				r = RrCSG.intersection(a, b);
				break;

			case 29: 
			// r = RrCSG.intersection(a, RrCSG.intersection(b, c));
			// a = !c ->

			// a b 
			// 0 0 | 0 
			// 1 0 | 0 
			// 0 1 | 0 
			// 1 1 | 0 
				r = RrCSG.nothing();
				break;

			case 30: 
			// r = RrCSG.intersection(a, RrCSG.intersection(b, c));
			// b = c ->

			// a b 
			// 0 0 | 0 
			// 1 0 | 0 
			// 0 1 | 0 
			// 1 1 | 1 
				r = RrCSG.intersection(b, a);
				break;

			case 31: 
			// r = RrCSG.intersection(a, RrCSG.intersection(b, c));
			// b = !c ->

			// a b 
			// 0 0 | 0 
			// 1 0 | 0 
			// 0 1 | 0 
			// 1 1 | 0 
				r = RrCSG.nothing();
				break;
		default:
			System.err.println("RrCSG.reg_3(): dud case value: " + caseVal);
		}
		
		return r;
	}
	
	
	/**
	 * FIXME: there's a bug in reg_4 somewhere.  So it's not called at present.
	 * Regularise a set with a contents of 4
	 * This assumes simplify has been run over the set
	 * @return regularised CSG object
	 */	
	private RrCSG reg_4()
	{            
		RrCSG result = this;
		
		if(complexity != 4)
			return result;
		
		RrCSG temp;	
		
		if(c1.complexity == 1)
		{
			temp = c2.reg_3();
			if(temp.complexity <= 2)
			{
				if(op == RrCSGOp.UNION)
					result = union(c1, temp).reg_3();
				else
					result = intersection(c1, temp).reg_3();
			}else
			{
				// c1 can only equal at most one leaf of c2 as all three c2 leaves
				// must be distinct because reg_3() didn't simplify c2.
				
				if(c1 == c2.c1)
				{
					result = c1;
				}
				else if(c1 == c2.c2.c1 || c1 == c2.c2.c2)
				{
					if(c1 == c2.c2.c2)
						c2.c2.c2 = c2.c2.c1;
					int ops = 0;
					if(op == RrCSGOp.UNION)
						ops++;
					if(c2.op == RrCSGOp.UNION)
						ops += 2;
					if(c2.c2.op == RrCSGOp.UNION)
						ops += 4;
					switch(ops)
					{
					case 0:
						result = c2;
						break;
					case 1:
					case 6:
						result = c1;
						break;
					case 2:
					case 5:
					case 7:
						result.c2.c2 = c2.c2.c2;
						break;                            
					case 3:
					case 4:
						result.c2 = c2.c1;
						break;
					default:
						System.err.println("reg_4() 1: addition doesn't work...");
					}  
				}
			}
		} else
		{
			int type = 0;
			if(c1.c1 == c2.c1)
				type++;
			else if(c1.c1 == c2.c2)
			{
				type++;
				temp = c2.c2;
				c2.c2 = c2.c1;
				c2.c1 = temp;
			}
			if(c1.c2 == c2.c2)
				type++;
			else if(c1.c2 == c2.c1)
			{
				type++;
				temp = c1.c2;
				c1.c2 = c1.c1;
				c1.c1 = temp;
			}
			
			int ops = 0;
			if(op == RrCSGOp.UNION)
				ops += 4;
			if(c1.op == RrCSGOp.UNION)
				ops++;
			if(c2.op == RrCSGOp.UNION)
				ops += 2;
			
			switch(type)
			{
			case 0:
				break;
				
			case 1:
				switch(ops)
				{
				case 0:
					result = intersection(c1, c2.c2);
					break;
				case 1:
					result = intersection(c1.c1, c2.c2);
					break;
				case 2:
				case 5:
					result = c1;
				case 3:
					result = union(c1.c1, intersection(c1.c2, c2.c2));
					break;
				case 4:
					result = intersection(c1.c1, union(c1.c2, c2.c2));
					break;
				case 6:
					result = union(c1.c1, c2.c2);
					break;
				case 7:
					result = union(c1, c2.c2);
					break;
				default:
					System.err.println("reg_4() 2: addition doesn't work...");
				}
				break;
				
			case 2:		// Pick the child that's an intersection (if there is one)		
				if(c1.op == RrCSGOp.UNION)
					result = c2;
				else
					result = c1;
				break;
				
			default:
				System.err.println("reg_4() 4: addition doesn't work...");
			}
		}
		
		return result;
	}
	
	/**
	 * Regularise a set with simple contents ( < 4 )
	 * This assumes simplify has been run over the set
	 * @return regularised CSG object
	 */	
	public RrCSG regularise()
	{
		RrCSG result = this;

		switch(complexity)
		{
		case 0:
		case 1:
		case 2:
			break;
		case 3:
			result = reg_3();
//			if(result.complexity < complexity)
//				System.out.println("regularise: \n" + toString() + " > " + 
//						result.toString());
			break;
			
		case 4:
			//result = reg_4();
//			if(result.complexity < complexity)
//				System.out.println("regularise: \n" + toString() + " > " + 
//						result.toString());
			break;
			
		default:
			System.err.println("regularise(): set too complicated.");
		}
		
		return result;
	}
	
	/**
	 * Replace duplicate of leaf with leaf itself
	 * TODO: this should also use known complements
	 * @param leaf
	 * @param tolerance
	 */		
	private void replace_all_same_leaves(RrCSG leaf, double tolerance)
	{	
		switch(op)
		{
		case LEAF:
		case NULL:   
		case UNIVERSE:
			//System.out.println("replace_all_same_leaves(): at a leaf!");
			break;
			
		case UNION:
		case INTERSECTION:    
			RrHalfPlane hp = leaf.hp;
			if(c1.op == RrCSGOp.LEAF)
			{
				if(RrHalfPlane.same(hp, c1.hp, tolerance))
					c1 = leaf;
			} else
				c1.replace_all_same_leaves(leaf, tolerance);
			
			if(c2.op == RrCSGOp.LEAF)
			{
				if(RrHalfPlane.same(hp, c2.hp, tolerance))
					c2 = leaf;                        
			} else
				c2.replace_all_same_leaves(leaf, tolerance);
			break;
			
		default:
			System.err.println("replace_all_same(): invalid operator.");		
		}
	}
	
	/**
	 * Replace duplicate of all leaves with the last instance of each
	 * @param root
	 * @param tolerance
	 * @return simplified CSG object
	 */		
	private void simplify_r(RrCSG root, double tolerance)
	{
		switch(op)
		{
		case LEAF:
		case NULL:   
		case UNIVERSE:
			//System.out.println("simplify_r(): at a leaf!");
			break;
			
		case UNION:
		case INTERSECTION:    
			if(c1.op == RrCSGOp.LEAF)
				root.replace_all_same_leaves(c1, tolerance);
			else
				c1.simplify_r(root, tolerance);
			
			if(c2.op == RrCSGOp.LEAF)
				root.replace_all_same_leaves(c2, tolerance);
			else
				c2.simplify_r(root, tolerance);
			
			break;
			
		default:
			System.err.println("simplify_r(): invalid operator.");
		
		}
	}
	
	/**
	 * Replace duplicate of all leaves with the last instance of each
	 * @param tolerance
	 * @return simplified CSG object
	 */		
	public RrCSG simplify(double tolerance)
	{
		RrCSG root = new RrCSG(this);
		simplify_r(root, tolerance);
		return root;
	}
	
	/**
	 * For each half plane remove any existing crossing list.
     */
    public void clearCrossings()
    {
    	if(complexity() > 1)
    	{
    		c_1().clearCrossings();
    		c_2().clearCrossings();
    	} else
    	{
    		if(operator() == RrCSGOp.LEAF)
    			plane().removeCrossings();
    	}
    }
    
    
    /**
	 * For each half plane sort the crossing list.
     */
    public void sortCrossings(boolean up, RrCSGPolygon q)
    {
    	if(complexity() > 1)
    	{
    		c_1().sortCrossings(up, q);
    		c_2().sortCrossings(up, q);
    	} else
    	{
    		if(operator() == RrCSGOp.LEAF)
    			plane().sort(up, q);
    	}	
    }
	
	/**
	 * Offset by a distance (+ve or -ve)
	 * TODO: this should keep track of complements
	 * @param d
	 * @return offset CSG object by distance d
	 */
	public RrCSG offset(double d)
	{
		RrCSG result;
		
		switch(op)
		{
		case LEAF:
			result = new RrCSG(hp.offset(d));
			break;
			
		case NULL:
		case UNIVERSE:
			result = this;
			break;
			
		case UNION:
			result = union(c1.offset(d), c2.offset(d));
			break;
			
		case INTERSECTION:
			result = intersection(c1.offset(d), c2.offset(d));
			break;
			
		default:
			System.err.println("offset(): invalid operator.");
			result = nothing();
		}
		return result;
	}
	
	
	/**
	 * leaf find the half-plane that generates the value for a point
	 * @param p
	 * @return leaf?
	 */
	public RrCSG leaf(Rr2Point p)
	{
		RrCSG result, r1, r2;
		
		switch(op)
		{
		case LEAF:
			result = this;
			break;
			
		case NULL:
			result = this;
			break;
			
		case UNIVERSE:
			result = this;
			break;
			
		case UNION:
			r1 = c1.leaf(p);
			r2 = c2.leaf(p);
			if(r1.value(p) < r2.value(p))
				return r1;
			else
				return r2;
			
		case INTERSECTION:
			r1 = c1.leaf(p);
			r2 = c2.leaf(p);
			if(r1.value(p) > r2.value(p))
				return r1;
			else
				return r2;
			
		default:
			System.err.println("leaf(Rr2Point): invalid operator.");
			result = nothing();
		}
		return result;
	}
	
	/**
	 * "Potential" value of a point; i.e. a membership test
	 * -ve means inside; 0 means on the surface; +ve means outside
	 * TODO - this should work independently of a call to leaf(); that's more efficient
	 * @param p
	 * @return value of a point
	 */
	public double value(Rr2Point p)
	{
		double result = 1;
		RrCSG c = leaf(p);
		switch(c.op)
		{
		case LEAF:
			result = c.hp.value(p);
			break;
			
		case NULL:
			result = 1;
			break;
			
		case UNIVERSE:
			result = -1;
			break;
			
		case UNION:
		case INTERSECTION:
			
		default:
			System.err.println("value(Rr2Point): non-leaf operator.");
		}
		return result;
	}
	
	/**
	 * The interval value of a box (analagous to point)
	 * @param b
	 * @return value of a box
	 */
	public RrInterval value(RrBox b)
	{
		RrInterval result;
		
		switch(op)
		{
		case LEAF:
			result = hp.value(b);
			break;
			
		case NULL:
			result = new RrInterval(1, 1.01);  // Is this clever?  Or dumb?
			break;
			
		case UNIVERSE:
			result = new RrInterval(-1.01, -1);  // Ditto.
			break;
			
		case UNION:
			result = RrInterval.min(c1.value(b), c2.value(b));
			break;
			
		case INTERSECTION:
			result = RrInterval.max(c1.value(b), c2.value(b));
			break;
			
		default:
			System.err.println("value(RrBox): invalid operator.");
			result = new RrInterval();
		}
		
		return result;
	}
	
	/**
	 * Prune the set to a box
	 * @param b
	 * @return pruned box as new CSG object
	 */
	public RrCSG prune(RrBox b)
	{
		RrCSG result = this;
		
		switch(op)
		{
		case LEAF:            
			RrInterval i = hp.value(b);
			if (i.empty())
				System.err.println("RrCSG.prune(RrBox): empty interval!");
			else if(i.neg())
				result = universe();
			else if (i.pos())
				result = nothing();
			break;
			
		case NULL:
		case UNIVERSE:
			break;
			
		case UNION:
			result =  union(c1.prune(b), c2.prune(b));
			break;
			
		case INTERSECTION:
			result = intersection(c1.prune(b), c2.prune(b));
			break;
			
		default:
			System.err.println("RrCSG.prune(RrBox): dud op value!");
		}
		
		return result;
	}
}
