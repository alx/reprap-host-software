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

/**
 * @author adrian
 *
 */
class variables
{
	
	/**
	 * 
	 */
	private boolean[] v;
	
	/**
	 * 
	 */
	private int i, max, values;
	
	/**
	 * @param n
	 * @param values
	 */
	public variables(int n, int vals)
	{
		max = n;
		values = vals;
		v = new boolean[n];
		for(int j = 0; j < max; j++)
		{
			v[j] = ((vals & 1) == 1);
			vals = vals >> 1;
		}
		reset();
	}
	
	public void reset() { i = -1; }
	
	public void eliminate(int a)
	{
		boolean[] vv = new boolean[max-1];
		int i = 0;
		for(int j = 0; j < max; j++)
		{
			if(j != a)
			{
				vv[i] = v[j];
				i++;
			}
		}
		v = vv;
		max--;
		setValues();
	}
	
	private void setValues()
	{
		values = 0;
		int k = 1;
		for(int j = 0; j < max; j++)
		{
			if(v[j])
				values = values | k;
			k = k << 1;
		}
	}
	
	/**
	 * @return
	 */
	public boolean next()
	{
		i++;
		if(i >= max)
			System.err.println("variables::next() - exhausted!");
		return v[i];
	}
	
	/**
	 * @param j
	 * @return
	 */
	public boolean get(int j)
	{
		return v[j];
	}
	
	public int getValues() { return values; }
	
	/**
	 * @param b
	 * @return
	 */
	public String toString()
	{
		String result = "";
		for(int j = 0; j < max; j++)
		{
			if(v[j])
				result += "1 ";
			else
				result += "0 ";
		}
		return result;
	}
	
	
	/**
	 * @param a
	 * @param b
	 * @return
	 */
	static boolean same(variables a, variables b)
	{
		if(a.max != b.max)
			return false;
		for(int j = 0; j < a.max; j++)
			if(a.v[j] != b.v[j])
				return false;
		return true;
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

	private bop leafOp;

	/**
	 * Operand and two operators
	 * @param a
	 * @param b
	 * @param op
	 */
	public BooleanExpression(BooleanExpression a, BooleanExpression b, bop op)
	{
		c1 = a;
		c2 = b;
		if(op == bop.LEAF || op == bop.ZERO || op == bop.ONE)
			System.out.println("BooleanExpression constructor called for leaf with two operands!");
		leafOp = op;
	}
	
	/**
	 * Leaf of known value
	 * @param v
	 */
	public BooleanExpression(boolean v)
	{
		c1 = null;
		c2 = null;
		if(v)
			leafOp = bop.ONE;
		else
			leafOp = bop.ZERO;
	}
	
	/**
	 * Variable leaf
	 */
	public BooleanExpression()
	{
		c1 = null;
		c2 = null;
		leafOp = bop.LEAF;
	}
	
	/**
	 * @return
	 */
	public int leafCount()
	{
		if(leafOp == bop.LEAF || leafOp == bop.ZERO || leafOp == bop.ONE)
			return 1;
		else
			return c1.leafCount()+c2.leafCount();
	}
	
	/**
	 * @param v
	 * @return
	 */
	public boolean generateValue_r(variables v){
		
		boolean r;
		
		switch(leafOp)
		{
		case LEAF:
			return v.next();
		
		case ZERO:
			return false;
			
		case ONE:
			return true;
			
		case LEFT:
			r = c1.generateValue_r(v);
			c2.generateValue_r(v);
			return r;
			
		case RIGHT:
			r = c1.generateValue_r(v);
			return c2.generateValue_r(v);
			
		case AND:
			r = c1.generateValue_r(v);
			return r && c2.generateValue_r(v);
			
		case OR:
			r = c1.generateValue_r(v); 
			return r || c2.generateValue_r(v);
			
		case XOR:
			r = c1.generateValue_r(v); 
			return r ^ c2.generateValue_r(v);
			
		default:
			System.err.println("generateValue_r: dud operator!");
		}
		return false;
	}
	
	public boolean generateValue(variables v)
	{
		v.reset();
		return generateValue_r(v);
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
	 * 
	 */
	variables[] vs;
	
	
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
		vs = new variables[entries];
		for(i = 0; i < entries; i++)
		{
			vs[i] = new variables(inputs, i);
			table[i] = b.generateValue(vs[i]);
		}
		sortEntries();
	}
	
	/**
	 * @param b
	 * @param a
	 * @param equal_a
	 */
	public FunctionTable(BooleanExpression b, int a, int equal_a)
	{
		int i;
		inputs = b.leafCount();
		entries = 1;
		for(i = 1; i < inputs; i++)
			entries *= 2;
		table = new boolean[entries];
		vs = new variables[entries];
		variables vsc;
		int k = 0;
		for(i = 0; i < entries*2; i++)
		{
			vsc = new variables(inputs, i);
			if(equal_a < 0)
			{
				if(vsc.get(a) == !vsc.get(-equal_a))
				{
					table[k] = b.generateValue(vsc);
					vsc.eliminate(-equal_a);
					vs[k] = vsc;
					k++;
				}
			}else
			{
				if(vsc.get(a) == vsc.get(equal_a))
				{
					table[k] = b.generateValue(vsc);
					vsc.eliminate(equal_a);
					vs[k] = vsc;
					k++;
				}
			}
		}
		sortEntries();
	}
	
	/**
	 * Nasty bubble sort, but it's only a few entries
	 */
	private void sortEntries()
	{
		for(int j = 0; j < entries - 1; j++)
		{
			for(int i = j+1; i < entries; i++)
			{
				if(vs[i].getValues() < vs[j].getValues())
				{
					variables v = vs[i];
					vs[i] = vs[j];
					vs[j] = v;
					boolean t = table[i];
					table[i] = table[j];
					table[j] = t;
				}
			}
		}		
	}
	
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
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		String result = "";
		for(int j = 0; j < entries; j++)
		{
			result = result + vs[j].toString() + "| ";
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

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		BooleanExpression a = new BooleanExpression();
		BooleanExpression b = new BooleanExpression();
		BooleanExpression c = new BooleanExpression(a, b, bop.OR);
		BooleanExpression d = new BooleanExpression();
		BooleanExpression e = new BooleanExpression(d, c, bop.AND);
		FunctionTable f = new FunctionTable(e, 0, -2);
		BooleanExpression g = new BooleanExpression(a, b, bop.AND);
		FunctionTable h = new FunctionTable(g);
		System.out.println(f.toString() + "\n\n");
		System.out.println(h.toString() + "\n");
		if(FunctionTable.same(f, h))
			System.out.println("Same");
		else
			System.out.println("Different");
	}
}

