package org.reprap.geometry.polygons;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Collections;

import javax.print.attribute.Size2DSyntax;

/**

/**
 * @author adrian
 * 
 * This is a program to automatically generate the Java for dealing with
 * the simplification of CSG expressions.  That is to say that it generates
 * simplified expressions when two operands in a more complicated expression
 * are equal, or are complements.
 *
 */

/**
 * Boolean operators and similar
 * 
 */
enum Bop 
{
	ZERO("zero"),
	ONE("one"),
	LEAF("leaf"),
	NOT("not"),
	LEFT("left"),
	RIGHT("right"),
	AND("and"),
	OR("or"),
	XOR("xor");
	
    private String name;
    
    Bop(String name)
    {
        this.name = name;
    }
    
    public String toString() { return name; }
    
    /**
     * All above NOT are diadic; all including and below monadic
     * @return
     */
    public boolean diadic() { return compareTo(NOT) > 0; }
}

/**
 * A single boolean variable with a name
 * @author ensab
 *
 */
class Variable implements Comparator
{
	boolean bv;
	boolean init;
	String n;
	
	public Variable(String s) { init = false; n = s;}
	public boolean value() { if(!init) System.err.println("Variable undefined!"); return bv; }
	public boolean isSet() { return init; }
	public void set(boolean b) { bv = b; init = true;}
	public String name() { return n; }
	public void clean() { init = false; }
	
	public Variable(Variable v)
	{
		if(!v.init) 
			System.err.println("Variable(Variable v): input Variable undefined!");
		bv = v.bv;
		init = v.init;
		n = new String(v.n);
	}
	
	public static boolean same(Variable a, Variable b)
	{
		return(a.compare(a, b) == 0);
	}
		
	/**
	 * Compare means compare the lexical order of the names.
	 */
	public final int compare(Object a, Object b)
	{
		return(((Variable)a).n.compareTo(((Variable)b).n));
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
	private Bop leafOp;
	
	/**
	 * 
	 */
	private Variable leaf;
	
	/**
	 * 
	 */
	private Variable[] variables;
	
	/**
	 * 
	 */
	private int leafCount;
	
	/**
	 * Make an expression from three variables in an array.
	 * exp decides the expression.
	 * @param variables
	 * @param exp
	 */
	public BooleanExpression(Variable [] variables, int exp)
	{
		if(variables.length != 3)
			System.err.println("BooleanExpression(...): variable array not length 3!");
		leafCount = -1;
		c1 = new BooleanExpression(variables[0]);
		if((exp & 1) == 1)
			c2 = new BooleanExpression(new BooleanExpression(variables[1]), 
					new BooleanExpression(variables[2]), Bop.AND);
		else
			c2 = new BooleanExpression(new BooleanExpression(variables[1]), 
					new BooleanExpression(variables[2]), Bop.OR);
		if((exp & 2) == 2)
			leafOp = Bop.AND;
		else
			leafOp = Bop.OR;
		recordVariables();
	}

	/**
	 * Operand and two operators
	 * @param a
	 * @param b
	 * @param op
	 */
	public BooleanExpression(BooleanExpression a, BooleanExpression b, Bop op)
	{
		leafCount = -1;		
		if(!op.diadic())
			System.err.println("BooleanExpression(a, b): leaf operator or NOT!");
		
		leafOp = op;
		leaf = null;
		c1 = a;
		c2 = b;
		recordVariables();
	}
	
	/**
	 * Monadic operator
	 * @param a
	 * @param op
	 */
	public BooleanExpression(BooleanExpression a, Bop op)
	{
		leafCount = -1;		
		if(op != Bop.NOT)
			System.err.println("BooleanExpression(..., NOT): op not NOT!");
		
		leafOp = op;
		leaf = null;
		c1 = a;
		c2 = null;
		recordVariables();
	}
	
	/**
	 * Variable leaf
	 */
	public BooleanExpression(Variable v)
	{
		leafCount = -1;
		c1 = null;
		c2 = null;
		leafOp = Bop.LEAF;
		leaf = v;
		recordVariables();
	}
	
	/**
	 * @return
	 */
	public int leafCount()
	{
		if(leafCount < 0)
		{
			if(leafOp == Bop.LEAF) // || leafOp == bop.ZERO || leafOp == bop.ONE)
			{
				leafCount = 1;
			}
			else if(leafOp == Bop.NOT)
			{
				leafCount = c1.leafCount();
			} else
				leafCount = c1.leafCount()+c2.leafCount();
		}

		return leafCount;		
	}
		
	
	private void recordVariables()
	{
		int vc = leafCount();
		variables = new Variable[vc];
		int i = 0;
		int k;
		if(leafOp == Bop.LEAF) // || leafOp == bop.ZERO || leafOp == bop.ONE)
			variables[i++] = leaf;
		else if(leafOp == Bop.NOT)
		{
			for(k = 0; k < c1.variables.length; k++)
				variables[i++] = c1.variables[k];			
		} else
		{
			for(k = 0; k < c1.variables.length; k++)
				variables[i++] = c1.variables[k];
			for(k = 0; k < c2.variables.length; k++)
				variables[i++] = c2.variables[k];
		}		
	}
	
	public void setAll(int i)
	{
		TableRow.setAll(variables, i);
	}
	
	public Variable [] getVariables()
	{
		return variables;
	}
	
	public int getIndex(Variable v)
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
	public boolean value()
	{
		
		boolean r;
		
		switch(leafOp)
		{
		case LEAF:
			return leaf.value();
		
		case ZERO:
			return false;
			
		case ONE:
			return true;
			
		case NOT:
			return !c1.value();
			
		case LEFT:
			return c1.value();
			
		case RIGHT:
			return c2.value();
			
		case AND:
			r = c1.value();
			return r & c2.value(); // &&
			
		case OR:
			r = c1.value(); 
			return r | c2.value(); // ||
			
		case XOR:
			r = c1.value(); 
			return r ^ c2.value();
			
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
		
		case ZERO:
			return r + "RrCSG.nothing()";
			
		case ONE:
			return r + "RrCSG.universe()";
			
		case NOT:
			return c1.toJava_r(r) + ".complement()";
			
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
 * A row of variables in a function table, and the table value.
 * Also contains useful functions for variable arrays.
 * @author ensab
 *
 */

class TableRow implements Comparator
{
	private Variable[] vs;
	private boolean b;
	
	public TableRow() { vs = null; }
	
	public TableRow(Variable[] vin, boolean bin)
	{
		vs = sort(vin);
		b = bin;
	}
	
	public int length() { return vs.length; }
	public boolean value() { return b; } 
	public Variable get(int i) { return vs[i]; }
	public Variable[] all() { return vs; }
	
	public String toString()
	{
		String result = "";
		for(int i = 0; i < vs.length; i++)
			result += vs[i].name() + " ";
		return result;
	}
	
	/**
	 * Set all the variables in a list according to the corresponding
	 * bits in an integer.
	 * @param vs
	 * @param v
	 */
	public static void setAll(Variable[] vars, int v)
	{
		int k = 1;
		for(int i = 0; i < vars.length; i++)
		{
			if((v & k) == 0)
				vars[i].set(false);
			else
				vars[i].set(true);
			k *= 2;	
		}
	}
	
	/**
	 * Remove one variable from a list to make a shorter list
	 * @param vars
	 * @param remove
	 * @return
	 */
	public static Variable[] eliminateVariable(Variable[] vars, Variable remove)
	{
		Variable[] result = new Variable[vars.length - 1];
		int k = 0;
		
		for(int i = 0; i < vars.length; i++)
		{
			if(vars[i] != remove)
			{
				result[k] = new Variable(vars[i]);
				k++;
			}
		}
		
		return result;
	}
	
	/**
	 * Take a list of variables and return a copy lexically sorted by name
	 * @param v
	 * @return
	 */
	private static Variable[] sort(Variable[] vins)
	{
		Variable[] result = new Variable[vins.length];
		for(int i = 0; i < vins.length; i++)
			result[i] = new Variable(vins[i]);
		java.util.Arrays.sort(result, new Variable(""));
		return result;
	}
	
	/**
	 * Check if two lists of variables have the same variables in the same order
	 * @param a
	 * @param b
	 */
	public static boolean sameOrder(Variable [] a, Variable [] b)
	{
		if(a.length != b.length)
			return false;

		for(int i = 0; i < a.length; i++)
		{
			if(!Variable.same(a[i], b[i]))
				return false;
		}
		
		return true;
	}
	
	/**
	 * Find the binary number represented by the list
	 * @param a
	 * @return
	 */
	public int number()
	{
		int result = 0;
		
		for(int i = length() - 1; i >= 0; i--)
		{
			if(get(i).value())
				result |= 1;
			result = result << 1;
		}
		
		return result;
	}
	
	/**
	 * Compare the binary numbers represented by two lists
	 * @param a
	 * @param b
	 */
	public final int compare(Object a, Object b)
	{
		int va = ((TableRow)a).number();
		int vb = ((TableRow)b).number();
		
		if(va < vb)
			return -1;
		else if(va > vb)
			return 1;
		
		return 0;
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
	List<TableRow> rows;
	
	/**
	 * 
	 */
	boolean allFalse, allTrue;
	
	/**
	 *
	 */
	public FunctionTable()
	{
		rows = new ArrayList<TableRow>();
		allFalse = true;
		allTrue = true;
	}
	
	/**
	 * Add a new row to the function table
	 * @param v
	 * @param b
	 */
	public void addRow(Variable[] v, boolean b)
	{
		if(b)
			allFalse = false;
		else
			allTrue = false;

		TableRow newOne = new TableRow(v, b);
		
//		 Check that each has the same variables as the first
		
		if(rows.size() > 0)
		{
			if(!TableRow.sameOrder(newOne.all(), rows.get(0).all()))
				System.err.println("FunctionTable.addRow() - variable lists different!");
		}
		
		rows.add(newOne);
	}
	
	public void tableCheck()
	{
		// Check we have the right number of entries
		
		int vars = rows.get(0).all().length;
		int leng = 1;
		for(int j = 0; j < vars; j++)
			leng *= 2;
		
		if(leng != rows.size())
			System.err.println("FunctionTable.tableCheck() - incorrect entry count: " + rows.size() +
					"(should be " + leng + ")");
		Collections.sort(rows, new TableRow());
		for(int i = 1; i < rows.size(); i++)
			if(rows.get(i-1).number() == rows.get(i).number())
				System.err.println("FunctionTable.tableDone() - identical rows: " + rows.get(i-1).toString() +
						rows.get(i).toString());
	}
		
	/**
	 * @param b
	 */
	public FunctionTable(BooleanExpression b)
	{
		this();
		
		int i;
		int inputs = b.leafCount();
		
		int entries = 1;
		for(i = 0; i < inputs; i++)
			entries *= 2;

		for(i = 0; i < entries; i++)
		{
			b.setAll(i);
			addRow(b.getVariables(), b.value());
		}
		
		tableCheck();
	}	

	/**
	 * @param b
	 * @param a
	 * @param equal_a
	 */
	public FunctionTable(BooleanExpression b, Variable v, Variable equal_v, boolean opposite)
	{
		this();
		
		int i;
		int inputs = b.leafCount() - 1;
		
		int entries = 1;
		for(i = 0; i < inputs; i++)
			entries *= 2;

		for(i = 0; i < entries*2; i++)
		{
			b.setAll(i);
			if(opposite ^ (equal_v.value() == v.value()))
				addRow(TableRow.eliminateVariable(b.getVariables(), equal_v), b.value());
		}
		
		tableCheck();		
	}
	
	public boolean allOnes() { return allTrue;}
	
	public boolean allZeros() { return allFalse;}
	
	public int entries() { return rows.size(); }
	
	/**
	 * @param a
	 * @param b
	 * @return
	 */
	static boolean same(FunctionTable a, FunctionTable b)
	{
		if(!TableRow.sameOrder(a.rows.get(0).all(), b.rows.get(0).all()))
			return false;
		
		if(a.entries() != b.entries())
			return false;
		if(a.allFalse && b.allFalse)
			return true;
		if(a.allTrue && b.allTrue)
			return true;		
		for(int i = 0; i < a.entries(); i++)
			if(a.rows.get(i).value() != b.rows.get(i).value())
				return false;
		return true;
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		String result = "\n\t// " + rows.get(0).toString();
		for(int i = 0; i < entries(); i++)
		{
			TableRow tr = rows.get(i);
			Variable[] vs = tr.all();
			result += "\n\t// ";
			for(int j = 0; j < vs.length; j++)
			{
				if(vs[j].value())
					result += "1 ";
				else
					result += "0 ";
			}
			
			result += "| ";
			if(tr.value())
				result += "1 ";
			else
				result += "0 ";
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
	
	static BooleanExpression findEqualTwo(FunctionTable f, Variable a, Variable b)
	{
		Bop[] bopValues = Bop.values();
		for(int i = 0; i < bopValues.length; i++)
		{
			if(bopValues[i].diadic())
			{
				BooleanExpression be = new BooleanExpression(new BooleanExpression(a), 
						new BooleanExpression(b), bopValues[i]);
				FunctionTable g = new FunctionTable(be);
				if(FunctionTable.same(f, g))
					return be;
				BooleanExpression bf = new BooleanExpression(be, Bop.NOT);
				g = new FunctionTable(bf);
				if(FunctionTable.same(f, g))
					return bf;
				BooleanExpression bg = new BooleanExpression(new BooleanExpression(new BooleanExpression(a),
						Bop.NOT), 
						new BooleanExpression(b), bopValues[i]);
				g = new FunctionTable(bg);
				if(FunctionTable.same(f, g))
					return bg;				
				BooleanExpression bh = new BooleanExpression(new BooleanExpression(a), 
						new BooleanExpression(new BooleanExpression(b),Bop.NOT), bopValues[i]);
				g = new FunctionTable(bh);
				if(FunctionTable.same(f, g))
					return bh;
				BooleanExpression bi = new BooleanExpression(new BooleanExpression(new BooleanExpression(a),
						Bop.NOT), 
						new BooleanExpression(new BooleanExpression(b),Bop.NOT), bopValues[i]);
				g = new FunctionTable(bi);
				if(FunctionTable.same(f, g))
					return bi;					
			}
		}
		return null;
	}
	
	private static void oneCase3(Variable [] variables, int exp, int j, int k, boolean opposite, boolean fts)
	{
		BooleanExpression a = new BooleanExpression(variables, exp);
		
//		FunctionTable tt = new FunctionTable(a);
//		System.out.println(tt.toString()+"\n\n");
		
		FunctionTable f = new FunctionTable(a, variables[j], variables[k], opposite);
		
//		int jj = j;
//		int kk = 3-(j+k);
//		if(jj > kk)
//		{
//			int swp = kk;
//			kk = jj;
//			jj = swp;
//		}
		
		BooleanExpression g = findEqualTwo(f, variables[j], variables[3-(j+k)]);
		
		int caseVal = 0;
		if(opposite)
			caseVal |= 1;
		if(j == 1)
			caseVal |= 2;
		if(k == 2)
			caseVal |= 4;
		caseVal |= exp << 3;

		System.out.println("\tcase " + caseVal + ": ");
		if(fts)
		{
			System.out.println("\t// " + a.toJava());
			System.out.print("\t// " + variables[j].name() + " = ");
			if(opposite)
				System.out.print("!");	
			System.out.println(variables[k].name() + " ->");
			System.out.println(f.toString());
		}

		if(g != null || f.allOnes() || f.allZeros())
		{
			if(f.allOnes())
				System.out.println("\t\tr = RrCSG.universe();");
			else if(f.allZeros())
				System.out.println("\t\tr = RrCSG.nothing();");
			else
				System.out.println("\t\t" + g.toJava());
//			if(g != null && fts)
//			{
//				FunctionTable h = new FunctionTable(g);
//				System.out.println(h.toString());
//			}
		} else
			System.out.println("\t\t// No equivalence." + "\n");
		System.out.println("\t\tbreak;\n");
	}
	
	private static void allCases(Variable [] variables)
	{	
		for(int exp = 0; exp < 4; exp++)
		{
			for(int j = 0; j < 2; j++)
				for(int k = j+1; k < 3; k++)
				{
					oneCase3(variables, exp, j, k, false, true);
					oneCase3(variables, exp, j, k, true, true);
				}
		}		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		Variable [] variables = new Variable[3];
		variables[0] = new Variable("a");
		variables[1] = new Variable("b");
		variables[2] = new Variable("c");
		//variables[3] = new variable("d");
		
		//oneCase3(variables, 2, 0, 2, false, true);
		allCases(variables);
	}	

}

