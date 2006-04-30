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

RrInterval: 1D intervals

First version 20 May 2005
This version: 7 March 2006

*/
package org.reprap.geometry.polygons;

// Real intervals

class RrInterval
{
    private double low;
    private double high;
    private boolean empty;

    public RrInterval()
    {
	empty = true;
    }

    // Two ends...

    public RrInterval(double l, double h)
    {
	low = l;
	high = h;
	empty = (low > high);
	if(empty)
	    System.err.println("RrInterval: low value bigger than high.");
    }

    // Deep copy

    public RrInterval(RrInterval i)
    {
	low = i.low;
	high = i.high;
	empty = i.empty;
    }
    
    // Return contents
    
    public double low() { return low; }
    public double high() { return high; }
    public boolean empty() { return empty; }
    
    // The biggest possible
    
    public static RrInterval big_interval()
    {
        return new RrInterval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    // Convert to a string
    
    public String toString()
    {
	if(empty)
	    return "[empty]";
        return "[" + Double.toString(low) + ", " + Double.toString(high) + "]";
    }


    // Accomodate v

    public void expand(double v)
    {
	if(empty)
	    {
		low = v;
		high = v;
	    } else
	    {
		if(v < low)
		    low = v;
		if(v > high)
		    high = v;
	    }
    }

    // Accommodate another interval

    public void expand(RrInterval i)
    {
	expand(i.low);
	expand(i.high);
    }

    // Size

    public double length()
    {
	return high - low;
    }


    // Interval addition

    public static RrInterval add(RrInterval a, RrInterval b)
    {
	if(a.empty || b.empty)
	    System.err.println("add(...): adding empty interval(s).");	    
	return new RrInterval(a.low + b.low, a.high + b.high);
    }

    public static RrInterval add(RrInterval a, double b)
    {
	if(a.empty)
	    System.err.println("add(...): adding an empty interval.");	    
	return new RrInterval(a.low + b, a.high + b);
    }
    
    public static RrInterval add(double b, RrInterval a)
    {	    
	return add(a, b);
    }


    // Interval subtraction

    public static RrInterval sub(RrInterval a, RrInterval b)
    {
	if(a.empty || b.empty)
	    System.err.println("difference(...): subtracting empty interval(s).");
	return new RrInterval(a.low - b.high, a.high - b.low);
    }

    public static RrInterval sub(RrInterval a, double b)
    {
	if(a.empty)
	    System.err.println("difference(...): subtracting an empty interval.");
	return new RrInterval(a.low - b, a.high - b);
    }
    
    public static RrInterval sub(double b, RrInterval a)
    {
	if(a.empty)
	    System.err.println("difference(...): subtracting an empty interval.");
	return new RrInterval(b - a.high, b - a.low);
    }   

    // Interval multiplication

    public static RrInterval mul(RrInterval a, RrInterval b)
    {
	if(a.empty || b.empty)
	    System.err.println("multiply(...): multiplying empty intervals.");
	double d = a.low*b.low;
	RrInterval r = new RrInterval(d, d);
	r.expand(a.low*b.high);
	r.expand(a.high*b.low);
	r.expand(a.high*b.high);
	return r;
    }

    public static RrInterval mul(RrInterval a, double f)
    {
	if(a.empty)
	    System.err.println("multiply(...): multiplying an empty interval.");
	if(f > 0)
	    return new RrInterval(a.low*f, a.high*f);
	else
	    return new RrInterval(a.high*f, a.low*f);	    
    }
    
    public static RrInterval mul(double f, RrInterval a)
    {
	return mul(a, f);	    
    }

    // Negative, zero, or positive?

    public boolean neg()
    {
	return high <= 0;
    }

    public boolean pos()
    {
	return low > 0;
    }

    // Does the interval _contain_ zero?

    public boolean zero()
    {
	return(!neg() && !pos());
    }
    
    // In or out
    
    public boolean in(double v)
    {
        return v >= low && v <= high;
    }

    // Absolute value of an interval

    public RrInterval abs()
    {
	RrInterval result = new RrInterval(this);
        double p;

        if (low < 0)
	    {
                if (high <= 0)
		    {
			result = new RrInterval(-high, -low);
		    } else
		    {
                        result = new RrInterval(0, result.high);
			p = -low;
                        if ( p > high ) result = new RrInterval(result.low, p);
		    }
	    }
        return(result);
    }

    // Sign of an interval
    
    public static double sign(double x) 
    { 
        if (x < 0) return -1; 
        else if (x > 0) return 1; 
        else return 0;
    }
    
    public RrInterval sign()
    {
        return( new RrInterval(sign(low), sign(high)) );
    }

    // Max and min

    public static RrInterval max(RrInterval a, RrInterval b)
    {
	RrInterval result = new RrInterval(b);
        if (a.low > b.low) result = new RrInterval(a.low, result.high);
        if (a.high > b.high) result = new RrInterval(result.low, a.high);
        return(result);
    }

    public static RrInterval min(RrInterval a, RrInterval b)
    {
	RrInterval result = new RrInterval(b);
        if (a.low < b.low) result = new RrInterval(a.low, result.high);
        if (a.high < b.high) result = new RrInterval(result.low, a.high);
        return(result);
    }
}
