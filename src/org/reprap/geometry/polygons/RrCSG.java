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
 
 RrCSG: 2D polygons as boolean combinations of half-planes
 
 First version 14 November 2005
 
 */

package org.reprap.geometry.polygons;

class RrCSG
{
	
	// Universal and null set
	
	private static final RrCSG u = new RrCSG(true);
	private static final RrCSG n = new RrCSG(false);
	
	private RrHalfPlane hp;  // Leaf half plane
	//private RrCSGOp op;      // Type of set
	private int op;			  // Will go at Java 1.5
	private RrCSG c1, c2;     // Non-leaf child operands
	private RrCSG comp;       // The complement (if there is one)
	private int complexity;    // How much is in here (leaf count)?
	
	// Make a leaf from a single half-plane
	
	public RrCSG(RrHalfPlane h)
	{
		hp = new RrHalfPlane(h);
		op = RrCSGOp.LEAF;
		c1 = null;
		c2 = null;
		comp = null;
		complexity = 1;
	}
	
	// One off constructor for the universal and null sets
	
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
	
	// Universal or null set
	
	public static RrCSG universe()
	{
		return u;
	}
	
	public static RrCSG nothing()
	{
		return n;
	}
	
	// Deep copy
	
	public RrCSG(RrCSG c)
	{
		if(this == u || this == n)
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
		
		if(c.comp != null)
		{
			comp = new RrCSG(comp);
			comp.comp = comp;
		} else
			comp = null;
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
	
	// Convert to a string
	
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
			result = c1.toString_r(result, white + " ");
			result = c2.toString_r(result, white + " ");
			break;
			
		case RrCSGOp.INTERSECTION:
			result = result + white + "I\n";
			result = c1.toString_r(result, white + " ");
			result = c2.toString_r(result, white + " ");
			break;
			
		default:
			System.err.println("toString_r(): invalid operator.");
		}
		return result;
	}
	
	public String toString()
	{
		String result = "RrCSG: complexity = " + Integer.toString(complexity) + "\n";
		result = toString_r(result, " ");
		return result;
	}
	
	// Private constructor for common work setting up booleans
	
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
	
	// Boolean operations, with de Morgan simplifications
	
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
	
	public RrCSG complement()
	{
		RrCSG result;
		
		switch(op)
		{
		case RrCSGOp.LEAF:
			if(comp == null)
			{
				result = new RrCSG(hp.complement());
				comp = result;
				result.comp = this;
			} else
				result = comp;
			break;
			
		case RrCSGOp.NULL:
			result = universe();
			break;
			
		case RrCSGOp.UNIVERSE:
			result = nothing();
			break;
			
		case RrCSGOp.UNION:
			result = intersection(c1.complement(), c2.complement());
			break;
			
		case RrCSGOp.INTERSECTION:
			result = union(c1.complement(), c2.complement());
			break;
			
		default:
			System.err.println("complement(): invalid operator.");
		result = nothing();
		}
		return result;
	}
	
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
	
	private RrCSG reg_4()
	{            
		RrCSG result = this;
		
		if(complexity != 4)
			return result;
		
		if(c1.complexity == 1)
		{
			result = c2.reg_3();
			if(result.complexity <= 2)
			{
				if(op == RrCSGOp.UNION)
					result = union(c1, result).reg_3();
				else
					result = intersection(c1, result).reg_3();
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
			RrCSG temp;
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
	
	public RrCSG regularise()
	{
		RrCSG result = this;
		
		/*switch(complexity)
		 {
		 case 0:
		 case 1:
		 case 2:
		 break;
		 case 3:
		 result = reg_3();
		 break;
		 case 4:
		 result = reg_4();
		 break;
		 
		 default:
		 System.err.println("regularise(): set too complicated.");
		 }*/
		
		return result;
	}
	
	private RrCSG replace_all_same(RrCSG leaf, double tolerance)
	{
		RrCSG result = this;
		
		switch(op)
		{
		case RrCSGOp.LEAF:
		case RrCSGOp.NULL:   
		case RrCSGOp.UNIVERSE:
			//System.err.println("replace_all_same(): at a leaf!");
			break;
			
		case RrCSGOp.UNION:
		case RrCSGOp.INTERSECTION:    
			if (complexity > 2)
			{
				result.c1 = c1.replace_all_same(leaf, tolerance);
				result.c2 = c2.replace_all_same(leaf, tolerance);
			} else
			{
				RrHalfPlane hp = leaf.hp;
				if(c1.op == RrCSGOp.LEAF && c1 != leaf)
				{
					if(RrHalfPlane.same(hp, c1.hp, tolerance))
						result.c1 = leaf;
				}
				
				if(c2.op == RrCSGOp.LEAF && c2 != leaf)
				{
					if(RrHalfPlane.same(hp, c2.hp, tolerance))
						result.c2 = leaf;                        
				}
				if(c1 == c2)
					result = c1;
			}
			break;
			
		default:
			System.err.println("replace_all_same(): invalid operator.");
		
		}
		return result;
	}
	
	public RrCSG simplify(double tolerance)
	{
		RrCSG result = this;
		switch(op)
		{
		case RrCSGOp.LEAF:
		case RrCSGOp.NULL:   
		case RrCSGOp.UNIVERSE:
			//System.err.println("simplify(): at a leaf!");
			break;
			
		case RrCSGOp.UNION:
		case RrCSGOp.INTERSECTION:    
			if (complexity > 2)
			{
				result.c1 = c1.simplify(tolerance);
				result.c2 = c2.simplify(tolerance);
			} else
			{
				if(c1.op == RrCSGOp.LEAF)
					result = replace_all_same(c1, tolerance);
				if(c2.op == RrCSGOp.LEAF)
					result = replace_all_same(c2, tolerance);
			}
			break;
			
		default:
			System.err.println("simplify(): invalid operator.");
		
		}
		return result;
	}
	
	
	public static RrCSG difference(RrCSG a, RrCSG b)
	{
		return intersection(a, b.complement());
	}
	
	// Offset by a distance
	
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
	
	
	// "Potential" value of a point; i.e. a membership test
	// -ve means inside; 0 means on the surface; +ve means outside
	// Leaf find the half-plane that generates the value for a point
	
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
	
	// The interval value of a box (analagous to point)
	
	public RrInterval value(RrBox b)
	{
		RrInterval result;
		
		switch(op)
		{
		case RrCSGOp.LEAF:
			result = hp.value(b);
			break;
			
		case RrCSGOp.NULL:
			result = new RrInterval(1, 1.01);
			break;
			
		case RrCSGOp.UNIVERSE:
			result = new RrInterval(-1.01, -1);
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
	
	// Prune the set to a box
	
	public RrCSG prune(RrBox b)
	{
		RrCSG result;
		switch(op)
		{
		case RrCSGOp.LEAF:            
			RrInterval i = hp.value(b);
			if (i.empty())
			{
				System.err.println("prune(RrBox): empty interval!");
				result = this;
			} else if(i.neg())
				result = universe();
			else if (i.pos())
				result = nothing();
			else
				result = this;
			break;
			
		case RrCSGOp.NULL:
		case RrCSGOp.UNIVERSE:
			result = this;
			break;
			
		case RrCSGOp.UNION:
			result =  union(c1.prune(b), c2.prune(b));
			break;
			
		case RrCSGOp.INTERSECTION:
			result = intersection(c1.prune(b), c2.prune(b));
			break;
			
		default:
			System.err.println("prune(RrBox): dud op value!");
		result = this;
		}
		
		return result;
	}
}
