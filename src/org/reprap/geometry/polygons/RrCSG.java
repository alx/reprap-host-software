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

/**
 * RepRap Constructive Solid Geometry class
 * 
 * RrCSG: 2D polygons as boolean combinations of half-planes
 * First version 14 November 2005 
 */
public class RrCSG
{
	
	private static final RrCSG u = new RrCSG(true);  ///< Universal set
	private static final RrCSG n = new RrCSG(false); ///< Null set 
	
	private RrHalfPlane hp;    ///< Leaf half plane
	//private RrCSGOp op;      ///< Type of set
	private int op;			   ///< Will go at Java 1.5
	private RrCSG c1, c2;      ///< Non-leaf child operands
	private RrCSG comp;        ///< The complement (if there is one)
	private int complexity;    ///< How much is in here (leaf count)?
	
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
	 * @return
	 */
	public static RrCSG universe()
	{
		return u;
	}
	
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
	
	// get children, operator etc
	
	public RrCSG c_1() { return c1; }
	public RrCSG c_2() { return c2; }
	//public RrCSGOp operator() { return op; }
	public int operator() { return op; }
	public RrHalfPlane plane() { return hp; }
	public int complexity() { return complexity; }
	
	/**
	 * Convert to a string
	 * @param result
	 * @param white
	 * @return
	 */
	private String toString_r(String result, String white)
	{
		switch(op)
		{
		case RrCSGOp.LEAF:
			result = result + white + hp.toString() + "\n";
			break;
			
		case RrCSGOp.NULL:
			result = result + white + "0\n";
			break;
			
		case RrCSGOp.UNIVERSE:
			result = result + white + "1\n";
			break;
			
		case RrCSGOp.UNION:
			result = result + white + "U\n";
			white = white + " ";
			result = c1.toString_r(result, white);
			result = c2.toString_r(result, white);
			break;
			
		case RrCSGOp.INTERSECTION:
			result = result + white + "I\n";
			white = white + " ";
			result = c1.toString_r(result, white);
			result = c2.toString_r(result, white);
			break;
			
		default:
			System.err.println("toString_r(): invalid operator.");
		}
		return result;
	}
	
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
	 * @return
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
	 * @return
	 */
	public RrCSG complement()
	{		
		if(comp != null)
			return comp;
		
		RrCSG result;
		
		switch(op)
		{
		case RrCSGOp.LEAF:
			result = new RrCSG(hp.complement());
			break;
			
		case RrCSGOp.NULL:
			return universe();
			
		case RrCSGOp.UNIVERSE:
			return nothing();
			
		case RrCSGOp.UNION:
			result = intersection(c1.complement(), c2.complement());
			break;
			
		case RrCSGOp.INTERSECTION:
			result = union(c1.complement(), c2.complement());
			break;
			
		default:
			System.err.println("complement(): invalid operator.");
			return nothing();
		}
		
		// Remember, so we don't have to do it again.
		
		comp = result;
		result.comp = this;
		
		return comp;
	}
	
	/**
	 * Set difference is intersection with complement
	 * @param a
	 * @param b
	 * @return
	 */		
	public static RrCSG difference(RrCSG a, RrCSG b)
	{
		return intersection(a, b.complement());
	}
	
	
	/**
	 * Regularise a set with a contents of 3
	 * This assumes simplify has been run over this set
	 * @return
	 */	
	private RrCSG reg_3()
	{
		RrCSG result = this;
		
		if(complexity != 3)
			return result;
		
		boolean different = true;
		boolean acomp = false;
		RrCSG temp;
		if(c1 == c2.c1)
			different = false;
		if(c1 == c2.c2)
		{
			different = false;
			temp = c2.c2;
			c2.c2 = c2.c1;
			c2.c1 = temp;
		}
		
		if(c1.comp != null)
		{
			if(c1.comp == c2.c1)
			{
				different = false;
				acomp = true;
			}
			if(c1.comp == c2.c2) 
			{
				different = false;
				acomp = true;
				temp = c2.c2;
				c2.c2 = c2.c1;
				c2.c1 = temp;
			}
		}
		
		if(different)
			return result;
		
		if(acomp)
		{
			if(op == RrCSGOp.UNION) 
			{
				if(c2.op == RrCSGOp.UNION)
					result = universe();
				else
					result = RrCSG.union(c1, c2.c2);
			}else 
			{
				if(c2.op == RrCSGOp.UNION)
					result = RrCSG.intersection(c1, c2.c2);
				else
					result = nothing();
			}            
		} else
		{
			if(op == RrCSGOp.UNION) 
			{
				if(c2.op == RrCSGOp.UNION)
					result = c2;
				else
					result = c1;
			}else 
			{
				if(c2.op == RrCSGOp.UNION)
					result = c1;
				else
					result = c2;
			}
		}
		return result;
	}
	
	/**
	 * Regularise a set with a contents of 4
	 * This assumes simplify has been run over the set
	 * @return
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
			{
				type++;
				temp = c2.c2;
				c2.c1 = c2.c2;
				c2.c2 = temp;
				temp = c1.c2;
				c1.c1 = c1.c2;
				c1.c2 = temp;
			}
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
					result = RrCSG.intersection(c1, c2.c2);
					break;
				case 1:
					result = RrCSG.intersection(c1.c1, c2.c2);
					break;
				case 2:
				case 5:
					result = c1;
				case 3:
					result = RrCSG.union(c1.c1, 
							RrCSG.intersection(c1.c2, c2.c2));
					break;
				case 4:
					result = RrCSG.intersection(c1.c1, 
							RrCSG.union(c1.c2, c2.c2));
					break;
				case 6:
					result = RrCSG.union(c1.c1, c2.c2);
					break;
				case 7:
					result = RrCSG.union(c1, c2.c2);
					break;
				default:
					System.err.println("reg_4() 2: addition doesn't work...");
				}
				break;
			case 2:
				
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
	 * @return
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
			if(result.complexity < complexity)
				System.out.println("regularise: \n" + toString() + " > " + 
						result.toString());
			break;
			
		case 4:
			result = reg_4();
			if(result.complexity < complexity)
				System.out.println("regularise: \n" + toString() + " > " + 
						result.toString());
			break;
			
		default:
			System.err.println("regularise(): set too complicated.");
		}
		
		return result;
	}
	
	/**
	 * Replace duplicate of leaf with leaf itself
	 * @param leaf
	 * @param tolerance
	 */		
	private void replace_all_same(RrCSG leaf, double tolerance)
	{	
		switch(op)
		{
		case RrCSGOp.LEAF:
		case RrCSGOp.NULL:   
		case RrCSGOp.UNIVERSE:
			//System.out.println("replace_all_same(): at a leaf!");
			break;
			
		case RrCSGOp.UNION:
		case RrCSGOp.INTERSECTION:    
			if (complexity > 2)
			{
				c1.replace_all_same(leaf, tolerance);
				c2.replace_all_same(leaf, tolerance);
			} else
			{
				RrHalfPlane hp = leaf.hp;
				if(c1.op == RrCSGOp.LEAF && c1 != leaf)
				{
					if(RrHalfPlane.same(hp, c1.hp, tolerance))
						c1 = leaf;
				}
				
				if(c2.op == RrCSGOp.LEAF && c2 != leaf)
				{
					if(RrHalfPlane.same(hp, c2.hp, tolerance))
						c2 = leaf;                        
				}
				
				// If we've made the children the we become one of them
				
				if(c1 == c2)
				{
					hp = c1.hp;
					op = c1.op;
					c1 = c1.c1;
					c2 = c1.c2;
					comp = c1.comp;
					complexity = c1.complexity;
				}
			}
			break;
			
		default:
			System.err.println("replace_all_same(): invalid operator.");		
		}
	}
	
	/**
	 * Replace duplicate of all leaves with the first instance of each
	 * @param root
	 * @param tolerance
	 * @return
	 */		
	private void simplify_r(RrCSG root, double tolerance)
	{
		switch(op)
		{
		case RrCSGOp.LEAF:
		case RrCSGOp.NULL:   
		case RrCSGOp.UNIVERSE:
			//System.out.println("simplify_r(): at a leaf!");
			break;
			
		case RrCSGOp.UNION:
		case RrCSGOp.INTERSECTION:    
			if (complexity > 2)
			{
				c1.simplify_r(root, tolerance);
				c2.simplify_r(root, tolerance);
			} else
			{
				if(c1.op == RrCSGOp.LEAF)
					root.replace_all_same(c1, tolerance);
				if(c2.op == RrCSGOp.LEAF)
					root.replace_all_same(c2, tolerance);
			}
			break;
			
		default:
			System.err.println("simplify_r(): invalid operator.");
		
		}
	}
	
	/**
	 * Replace duplicate of all leaves with the first instance of each
	 * @param tolerance
	 * @return
	 */		
	public RrCSG simplify(double tolerance)
	{
		RrCSG root = new RrCSG(this);
		simplify_r(root, tolerance);
		return root;
	}
	
	/**
	 * Offset by a distance (+ve or -ve)
	 * @param d
	 * @return
	 */
	public RrCSG offset(double d)
	{
		RrCSG result;
		
		switch(op)
		{
		case RrCSGOp.LEAF:
			result = new RrCSG(hp.offset(d));
			break;
			
		case RrCSGOp.NULL:
		case RrCSGOp.UNIVERSE:
			result = this;
			break;
			
		case RrCSGOp.UNION:
			result = union(c1.offset(d), c2.offset(d));
			break;
			
		case RrCSGOp.INTERSECTION:
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
	 * @return
	 */
	public RrCSG leaf(Rr2Point p)
	{
		RrCSG result, r1, r2;
		
		switch(op)
		{
		case RrCSGOp.LEAF:
			result = this;
			break;
			
		case RrCSGOp.NULL:
			result = this;
			break;
			
		case RrCSGOp.UNIVERSE:
			result = this;
			break;
			
		case RrCSGOp.UNION:
			r1 = c1.leaf(p);
			r2 = c2.leaf(p);
			if(r1.value(p) < r2.value(p))
				return c1;
			else
				return c2;
			
		case RrCSGOp.INTERSECTION:
			r1 = c1.leaf(p);
			r2 = c2.leaf(p);
			if(r1.value(p) > r2.value(p))
				return c1;
			else
				return c2;
			
		default:
			System.err.println("leaf(Rr2Point): invalid operator.");
			result = nothing();
		}
		return result;
	}
	
	/**
	 * "Potential" value of a point; i.e. a membership test
	 * -ve means inside; 0 means on the surface; +ve means outside
	 * @param p
	 * @return
	 */
	public double value(Rr2Point p)
	{
		double result = 1;
		RrCSG c = leaf(p);
		switch(c.op)
		{
		case RrCSGOp.LEAF:
			result = c.hp.value(p);
			break;
			
		case RrCSGOp.NULL:
			result = 1;
			break;
			
		case RrCSGOp.UNIVERSE:
			result = -1;
			break;
			
		case RrCSGOp.UNION:
		case RrCSGOp.INTERSECTION:
			
		default:
			System.err.println("value(Rr2Point): non-leaf operator.");
		}
		return result;
	}
	
	/**
	 * The interval value of a box (analagous to point)
	 * @param b
	 * @return
	 */
	public RrInterval value(RrBox b)
	{
		RrInterval result;
		
		switch(op)
		{
		case RrCSGOp.LEAF:
			result = hp.value(b);
			break;
			
		case RrCSGOp.NULL:
			result = new RrInterval(1, 1.01);  // Is this clever?  Or dumb?
			break;
			
		case RrCSGOp.UNIVERSE:
			result = new RrInterval(-1.01, -1);  // Ditto.
			break;
			
		case RrCSGOp.UNION:
			result = RrInterval.min(c1.value(b), c2.value(b));
			break;
			
		case RrCSGOp.INTERSECTION:
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
	 * @return
	 */
	public RrCSG prune(RrBox b)
	{
		RrCSG result = this;
		
		switch(op)
		{
		case RrCSGOp.LEAF:            
			RrInterval i = hp.value(b);
			if (i.empty())
				System.err.println("prune(RrBox): empty interval!");
			else if(i.neg())
				result = universe();
			else if (i.pos())
				result = nothing();
			break;
			
		case RrCSGOp.NULL:
		case RrCSGOp.UNIVERSE:
			break;
			
		case RrCSGOp.UNION:
			result =  union(c1.prune(b), c2.prune(b));
			break;
			
		case RrCSGOp.INTERSECTION:
			result = intersection(c1.prune(b), c2.prune(b));
			break;
			
		default:
			System.err.println("prune(RrBox): dud op value!");
		}
		
		return result;
	}
}
