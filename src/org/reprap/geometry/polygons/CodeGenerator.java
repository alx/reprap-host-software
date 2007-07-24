package org.reprap.geometry.polygons;

import java.util.*;

/**

/**
 * @author adrian
 * 
 * This is a program to automatically generate the Java for dealing with
 * the simplification of CSG expressions.
 *
 */

/**
 * Boolean operators and similar
 * 
 */
enum bop 
{
	ZERO("zero"),
	ONE("one"),
	LEAF("leaf"),
	LEFT("left"),
	RIGHT("right"),
	AND("and"),
	OR("or"),
	XOR("xor");
	
    private String name;
    
    bop(String name)
    {
        this.name = name;
    }
    
    public String toString() { return name; }
}


class variable
{
	boolean bv;
	boolean init;
	String n;
	
	public variable(String s) { init = false; n = s;}
	public boolean get() { if(!init) System.err.println("Variable undefined!"); return bv; }
	public boolean isSet() { return init; }
	public void set(boolean b) { bv = b; init = true;}
	public String name() { return n; }
	
	public static void setAll(variable [] vs, int v)
	{
		int k = 1;
		for(int i = 0; i < vs.length; i++)
		{
			if((v & k) == 0)
				vs[i].set(false);
			else
				vs[i].set(true);
			k *= 2;	
		}
	}
}

/**
 * @author adrian
 *
 */
class BooleanExpression
{	
	/**
	 * 
	 */
	private BooleanExpression c1, c2;

	/**
	 * 
	 */
	private bop leafOp;
	
	/**
	 * 
	 */
	private variable leaf;
	
	private variable [] variables;
	
	
//	public BooleanExpression(int exp)
//	{
//		c1 = new BooleanExpression('a');
//		if((exp & 1) == 1)
//			c2 = new BooleanExpression(new BooleanExpression('b'), new BooleanExpression('c'), bop.AND);
//		else
//			c2 = new BooleanExpression(new BooleanExpression('b'), new BooleanExpression('b'), bop.OR);
//		if((exp & 2) == 2)
//			leafOp = bop.AND;
//		else
//			leafOp = bop.OR;
//	}

	/**
	 * Operand and two operators
	 * @param a
	 * @param b
	 * @param op
	 */
	public BooleanExpression(BooleanExpression a, BooleanExpression b, bop op)
	{
		
		if(op == bop.LEAF || op == bop.ZERO || op == bop.ONE)
			System.out.println("BooleanExpression(...): leaf operator!");
		
		leafOp = op;
		leaf = null;
		c1 = a;
		c2 = b;
		recordVariables();
	}
	
//	/**
//	 * Leaf of known value
//	 * @param v
//	 */
//	public BooleanExpression(boolean v)
//	{
//		c1 = null;
//		c2 = null;
//		if(v)
//			leafOp = bop.ONE;
//		else
//			leafOp = bop.ZERO;
//	}
	
	/**
	 * Variable leaf
	 */
	public BooleanExpression(variable v)
	{
		c1 = null;
		c2 = null;
		leafOp = bop.LEAF;
		leaf = v;
		recordVariables();
	}
	
	/**
	 * @return
	 */
	public int leafCount()
	{
		if(leafOp == bop.LEAF) // || leafOp == bop.ZERO || leafOp == bop.ONE)
			return 1;
		else
			return c1.leafCount()+c2.leafCount();
	}
	
	private int recordVariables_r(int i)
	{
		if(leafOp == bop.LEAF) // || leafOp == bop.ZERO || leafOp == bop.ONE)
			variables[i++] = leaf;
		else
		{
			int k;
			for(k = 0; k < c1.variables.length; k++)
				variables[i++] = c1.variables[k];
			for(k = 0; k < c2.variables.length; k++)
				variables[i++] = c2.variables[k];
		}
		return i;
	}	
	
	private void recordVariables()
	{
		int vc = leafCount();
		variables = new variable[vc];
		if(recordVariables_r(0) != vc)
			System.err.println("setVariables(): dud recursion!");
	}
	
	public void setAll(int i)
	{
		variable.setAll(variables, i);
	}
	
	public int getIndex(variable v)
	{
		for(int i = 0; i < variables.length; i++)
		{
			if(v == variables[i])
				return i;
		}
		System.err.println("getIndex(): variable not found!");
		return -1;
	}
		
	/**
	 * @param v
	 * @return
	 */
	public boolean generateValue()
	{
		
		boolean r;
		
		switch(leafOp)
		{
		case LEAF:
			return leaf.get();
		
//		case ZERO:
//			return false;
//			
//		case ONE:
//			return true;
			
		case LEFT:
			return c1.generateValue();
			
		case RIGHT:
			return c2.generateValue();
			
		case AND:
			r = c1.generateValue();
			return r && c2.generateValue();
			
		case OR:
			r = c1.generateValue(); 
			return r || c2.generateValue();
			
		case XOR:
			r = c1.generateValue(); 
			return r ^ c2.generateValue();
			
		default:
			System.err.println("generateValue_r: dud operator!");
		}
		return false;
	}

	
	private String toJava_r(String r)
	{		
		switch(leafOp)
		{
		case LEAF:
			return r + leaf.name();
		
//		case ZERO:
//			return r + "RrCSG.nothing()";
//			
//		case ONE:
//			return r + "RrCSG.nothing()";
			
		case LEFT:
			return c1.toJava_r(r);
			
		case RIGHT:
			return c2.toJava_r(r);
			
		case AND:
			r += "RrCSG.intersection(";
			r = c1.toJava_r(r) + ", ";
			r = c2.toJava_r(r) + ")";
			return r;
			
		case OR:
			r += "RrCSG.union(";
			r = c1.toJava_r(r) + ", ";
			r = c2.toJava_r(r) + ")";
			return r;
			
		case XOR:
			System.err.println("toJava(): got to an XOR...");
			break;
			
		default:
			System.err.println("toJava(): dud operator");
		}
		
		return r;
	}
	
	public String toJava()
	{
		String r = "r = ";
		return toJava_r(r) + ";";
	}
}

/**
 * @author adrian
 *
 */
class FunctionTable
{
	/**
	 * 
	 */
	private int inputs, entries;
	
	/**
	 * 
	 */
	boolean[] table;
		
	/**
	 * @param b
	 */
	public FunctionTable(BooleanExpression b)
	{
		int i;
		inputs = b.leafCount();
		
		entries = 1;
		for(i = 0; i < inputs; i++)
			entries *= 2;
		table = new boolean[entries];
		for(i = 0; i < entries; i++)
		{
			b.setAll(i);
			table[i] = b.generateValue();
		}
		//sortEntries();
	}
	
	private static boolean notOne(int i, int v)
	{
		return ((i >> v) & 1) == 1;
	}
	
	/**
	 * @param b
	 * @param a
	 * @param equal_a
	 */
	public FunctionTable(BooleanExpression b, variable v, variable equal_v, boolean opposite)
	{
		int i;
		inputs = b.leafCount();	
		
		entries = 1;
		for(i = 1; i < inputs; i++)
			entries *= 2;
		table = new boolean[entries];
		int k = 0;
		for(i = 0; i < entries*2; i++)
		{
			b.setAll(i);
			if(opposite ^ (equal_v.get() == v.get()))
			{
				table[k] = b.generateValue();
				k++;
			}
		}
		//sortEntries();
	}
	
//	/**
//	 * Nasty bubble sort, but it's only a few entries
//	 */
//	private void sortEntries()
//	{
//		for(int j = 0; j < entries - 1; j++)
//		{
//			for(int i = j+1; i < entries; i++)
//			{
//				if(vs[i].getValues() < vs[j].getValues())
//				{
//					variables v = vs[i];
//					vs[i] = vs[j];
//					vs[j] = v;
//					boolean t = table[i];
//					table[i] = table[j];
//					table[j] = t;
//				}
//			}
//		}		
//	}
	
	/**
	 * @param a
	 * @param b
	 * @return
	 */
	static boolean same(FunctionTable a, FunctionTable b)
	{
		if(a.entries != b.entries)
			return false;
		for(int i = 0; i < a.entries; i++)
			if(a.table[i] != b.table[i])
				return false;
		return true;
	}
	
	public String bitString(int v, int bits)
	{
		String result = "";
		for(int i = 0; i < bits; i++)
		{
			if((v & 1) == 1)
				result = result + "1 ";
			else
				result = result + "0 ";
			v = v >> 1;
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		int bits = 0;
		int k = entries/2;
		while(k > 0)
		{
			k = k >> 1;
			bits++;
		}
		String result = "";
		for(int j = 0; j < entries; j++)
		{
			result = result + bitString(j, bits) + "| ";
			if(table[j])
				result += "1 \n";
			else
				result += "0 \n";
		}
		return result;
	}
}

/**
 * @author adrian
 *
 */
public class CodeGenerator 
{
	
//	static BooleanExpression findEqualTwo(FunctionTable f)
//	{
//		bop[] bopValues = bop.values();
//		for(int i = 0; i < bopValues.length; i++)
//		{
//			BooleanExpression a = new BooleanExpression(new BooleanExpression(), 
//					new BooleanExpression(), bopValues[i]);
//			FunctionTable g = new FunctionTable(a);
//			if(FunctionTable.same(f, g))
//				return a;
//		}
//		return null;
//	}

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		variable a = new variable("a");
		variable b = new variable("b");
		variable c = new variable("c");
		//variable d = new variable("d");
		
		BooleanExpression aa = new BooleanExpression(a);
		BooleanExpression bb = new BooleanExpression(b);
		BooleanExpression cc = new BooleanExpression(c);
		//BooleanExpression dd = new BooleanExpression(d);
		
		BooleanExpression xx = new BooleanExpression(aa, bb, bop.OR);
		BooleanExpression yy = new BooleanExpression(xx, cc, bop.AND);
				
		FunctionTable f = new FunctionTable(yy, a, c, true);
		
		BooleanExpression g = new BooleanExpression(aa, bb, bop.AND);

		
		FunctionTable h = new FunctionTable(g);
		
		System.out.println(f.toString() + "\n\n");
		System.out.println(h.toString() + "\n");
		if(FunctionTable.same(f, h))
			System.out.println("Same");
		else
			System.out.println("Different");
		System.out.println(g.toJava());
		System.out.println(yy.toJava());	
		
//		for(int i = 0; i < 4; i++)
//		{
//			BooleanExpression a = new BooleanExpression(i);
//			for(int j = 0; j < 2; j++)
//				for(int k = j+1; k < 3; k++)
//				{
//					FunctionTable f = new FunctionTable(a, j, k);
//					BooleanExpression b = findEqualTwo(f);
//					if(b != null)
//					{
//						System.out.println(a.toJava());
//						System.out.println("equal0: " + j + ", equal1: " + k + " => ");
//						System.out.println(b.toJava() + "\n");
//					}
//				}
//		}
	}
}

