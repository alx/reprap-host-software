package org.reprap.geometry.polygons.code_generator;


/**

/**
 * @author adrian
 *
 */
class bop {
	public static final int LEAF = 0;
	public static final int ZERO = 1;
	public static final int ONE = 2;
	public static final int LEFT = 3;
	public static final int RIGHT = 4;
	public static final int AND = 5;
	public static final int OR = 6;
	public static final int XOR = 7;
	}

class BooleanExpression
{
	private BooleanExpression c1, c2;
	private int leafOp;
	private static int vCount; // This is nasty - it stops the code being re-entrant
	
	public BooleanExpression(BooleanExpression a, BooleanExpression b, int op)
	{
		c1 = a;
		c2 = b;
		if(op == bop.LEAF)
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
		if(leafOp == bop.LEAF)
			return 1;
		else
			return c1.leafCount()+c2.leafCount();
	}
	
	private boolean generateValue_r(boolean variables[])
	{
		boolean r;
		
		switch(leafOp)
		{
		case bop.LEAF:
			vCount++;
			return variables[vCount];
		
		case bop.ZERO:
			return false;
			
		case bop.ONE:
			return true;
			
		case bop.LEFT:
			r = c1.generateValue_r(variables);
			c2.generateValue_r(variables);
			return r;
			
		case bop.RIGHT:
			r = c1.generateValue_r(variables);
			return c2.generateValue_r(variables);
			
		case bop.AND:
			return c1.generateValue_r(variables) && c2.generateValue_r(variables);
			
		case bop.OR:
			return c1.generateValue_r(variables) || c2.generateValue_r(variables);
			
		case bop.XOR:
			return c1.generateValue_r(variables) ^ c2.generateValue_r(variables);
			
		default:
			System.err.println("generateValue_r: dud operator!");
		}
		return false;
	}	
	
	public boolean generateValue(boolean v[])
	{
		vCount = -1;
		return generateValue_r(v);
	}
}

class FunctionTable
{
	private int inputs, entries;
	boolean table[];
	
	private void setVariables(boolean variables[], int values)
	{
		for(int i = 0; i < inputs; i++)
		{
			variables[i] = ((values & 1) == 1);
			values = values >> 1;
		}
	}
	
	public FunctionTable(BooleanExpression b)
	{
		int i;
		inputs = b.leafCount();
		boolean variables[] = new boolean[inputs];
		entries = 1;
		for(i = 0; i < inputs; i++)
			entries *= 2;
		table = new boolean[entries];
		for(i = 0; i < entries; i++)
		{
			setVariables(variables, i);
			table[i] = b.generateValue(variables);
		}
	}
	
	public FunctionTable(BooleanExpression b, int a, int equal_a)
	{
		int i;
		inputs = b.leafCount();
		boolean variables[] = new boolean[inputs];
		entries = 1;
		for(i = 1; i < inputs; i++)
			entries *= 2;
		table = new boolean[entries];
		int k = 0;
		for(i = 0; i < entries*2; i++)
		{
			setVariables(variables, i);
			if(equal_a < 0)
			{
				if(variables[a] == !variables[-equal_a])
				{
					table[k] = b.generateValue(variables);
					k++;
				}
			}else
			{
				if(variables[a] == variables[equal_a])
				{
					table[k] = b.generateValue(variables);
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
	
}

public class CodeGenerator 
{

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		// TODO Auto-generated method stub

	}

}

