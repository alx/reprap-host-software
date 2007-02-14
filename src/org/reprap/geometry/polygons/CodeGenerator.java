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
class bop 
{
	public static final int ZERO = 0;
	public static final int ONE = 1;
	public static final int LEAF = 2;
	public static final int LEFT = 3;
	public static final int RIGHT = 4;
	public static final int AND = 5;
	public static final int OR = 6;
	public static final int XOR = 7;
}

class variables
{
	private boolean[] v;
	private int i, max;
	
	public variables(int n, int values)
	{
		i = -1;
		max = n;
		v = new boolean[n];
		for(int j = 0; j < max; j++)
		{
			v[j] = ((values & 1) == 1);
			values = values >> 1;
		}		
	}
	
	public boolean next()
	{
		i++;
		if(i >= max)
			System.err.println("variables::next() - exhausted!");
		return v[i];
	}
	
	public boolean get(int j)
	{
		return v[j];
	}
	
	public String toString(int b)
	{
		String result = "";
		for(int j = 0; j < max; j++)
		{
			if(b == 0 || Math.abs(b) != j)
			{
				if(v[j])
					result += "1 ";
				else
					result += "0 ";
			}
		}
		return result;
	}
	
	public String toString()
	{
		return toString(0);
	}
	
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

class BooleanExpression
{
	private BooleanExpression c1, c2;
	private int leafOp;
	
	public BooleanExpression(BooleanExpression a, BooleanExpression b, int op)
	{
		c1 = a;
		c2 = b;
		if(op == bop.LEAF || op == bop.ZERO || op == bop.ONE)
			System.out.println("BooleanExpression constructor called for leaf with two operands!");
		leafOp = op;
	}
	
	public BooleanExpression(boolean v)
	{
		c1 = null;
		c2 = null;
		if(v)
			leafOp = bop.ONE;
		else
			leafOp = bop.ZERO;
	}
	
	public BooleanExpression()
	{
		c1 = null;
		c2 = null;
		leafOp = bop.LEAF;
	}
	
	public int leafCount()
	{
		if(leafOp == bop.LEAF || leafOp == bop.ZERO || leafOp == bop.ONE)
			return 1;
		else
			return c1.leafCount()+c2.leafCount();
	}
	
	public boolean generateValue(variables v)
	{
		boolean r;
		
		switch(leafOp)
		{
		case bop.LEAF:
			return v.next();
		
		case bop.ZERO:
			return false;
			
		case bop.ONE:
			return true;
			
		case bop.LEFT:
			r = c1.generateValue(v);
			c2.generateValue(v);
			return r;
			
		case bop.RIGHT:
			r = c1.generateValue(v);
			return c2.generateValue(v);
			
		case bop.AND:
			r = c1.generateValue(v);
			return r && c2.generateValue(v);
			
		case bop.OR:
			r = c1.generateValue(v); 
			return r || c2.generateValue(v);
			
		case bop.XOR:
			r = c1.generateValue(v); 
			return r ^ c2.generateValue(v);
			
		default:
			System.err.println("generateValue_r: dud operator!");
		}
		return false;
	}	
}

class FunctionTable
{
	private int inputs, entries;
	boolean[] table;
	variables[] vs;
	int equal_2;
	
	public FunctionTable(BooleanExpression b)
	{
		equal_2 = 0;
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
	}
	
	public FunctionTable(BooleanExpression b, int a, int equal_a)
	{
		equal_2 = equal_a;
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
					vs[k] = vsc;
					k++;
				}
			}else
			{
				if(vsc.get(a) == vsc.get(equal_a))
				{
					table[k] = b.generateValue(vsc);
					vs[k] = vsc;
					k++;
				}
			}
		}
	}
	
	static boolean same(FunctionTable a, FunctionTable b)
	{
		if(a.entries != b.entries)
			return false;
		for(int i = 0; i < a.entries; i++)
			if(a.table[i] != b.table[i])
				return false;
		return true;
	}
	
	public String toString()
	{
		String result = "";
		for(int j = 0; j < entries; j++)
		{
			result = result + vs[j].toString(equal_2) + "| ";
			if(table[j])
				result += "1 \n";
			else
				result += "0 \n";
		}
		return result;
	}
}

public class CodeGenerator 
{

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		BooleanExpression a = new BooleanExpression(true);
		BooleanExpression b = new BooleanExpression();
		BooleanExpression c = new BooleanExpression(a, b, bop.AND);
		BooleanExpression d = new BooleanExpression();
		BooleanExpression e = new BooleanExpression(d, c, bop.AND);
		FunctionTable f = new FunctionTable(e, 0, -2);
		System.out.println(f.toString());
	}
}

