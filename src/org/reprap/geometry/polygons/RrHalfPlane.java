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

RrHalfPlane: 2D planar half-spaces

First version 20 May 2005
This version: 9 March 2006

*/

package org.reprap.geometry.polygons;

// Exception for when trying to intersect parallel lines

class rr_ParallelLineException extends Exception
{
    public rr_ParallelLineException(String s)
    {
	super(s);
    }
}

// Class to hold and manipulate linear half-planes

class RrHalfPlane
{

    // The half-plane is normal*(x, y) + offset <= 0

    private Rr2Point normal; 
    private double offset;


    // Convert a parametric line

    public RrHalfPlane(RrLine l)
    {
	double rsq = 1/l.direction().mod();
	normal = new Rr2Point(-l.direction().y()*rsq, l.direction().x()*rsq);
	offset = -Rr2Point.mul(l.origin(), normal());
    }


    // Make one from two points on its edge

    public RrHalfPlane(Rr2Point a, Rr2Point b)
    {
	this(new RrLine(a, b));
    }   

    // Deep copy

    public RrHalfPlane(RrHalfPlane a)
    {
	normal = new Rr2Point(a.normal);
	offset = a.offset;
    }


    // Convert to a string

    public String toString()
    {
	return "|" + normal.toString() + ", " + Double.toString(offset) + "|";
    } 

    // Get the components
    
    public Rr2Point normal() { return normal; }
    public double offset() { return offset; }
    
    // Is another line the same within a tolerance?
    
    public static boolean same(RrHalfPlane a, RrHalfPlane b, double tolerance)
    {
        if(Math.abs(a.normal.x() - b.normal.x()) > tolerance)
            return false;
        if(Math.abs(a.normal.y() - b.normal.y()) > tolerance)
            return false;
        double rms = Math.sqrt((a.offset*a.offset + b.offset*b.offset)*0.5);
        if(Math.abs(a.offset - b.offset) > tolerance*rms)
            return false;
        
        return true;
    }
    

    // Change the sense

    public RrHalfPlane complement()
    {
	RrHalfPlane r = new RrHalfPlane(this);
	r.normal = r.normal.neg();
	r.offset = -r.offset;
	return r;
    }
    
    // Move
    
    public RrHalfPlane offset(double d)
    {
	RrHalfPlane r = new RrHalfPlane(this);
	r.offset = r.offset - d;
	return r;
    }


    // Find the potential value of a point

    public double value(Rr2Point p)
    {
	return offset + Rr2Point.mul(normal, p);
    }


    // Find the potential interval of a box

    public RrInterval value(RrBox b)
    {
	return RrInterval.add(RrInterval.add((RrInterval.mul(b.x(), normal.x())), 
                (RrInterval.mul(b.y(), normal.y()))), offset);
    }


    // The point where another line crosses

    public Rr2Point cross_point(RrHalfPlane a) throws rr_ParallelLineException
    {
	double det = Rr2Point.op(normal, a.normal);
	if(det == 0)
	    throw new rr_ParallelLineException("cross_point: parallel lines.");
	det = 1/det;
	double x = normal.y()*a.offset - a.normal.y()*offset;
	double y = a.normal.x()*offset - normal.x()*a.offset;
	return new Rr2Point(x*det, y*det);
    }

    // Parameter value where a line crosses
    
    public double cross_t(RrLine a) throws rr_ParallelLineException 
    {
	double det = Rr2Point.mul(a.direction(), normal);
        if (det == 0)
	    throw new rr_ParallelLineException("cross_t: parallel lines.");  
	return -value(a.origin())/det;
    }

    // Point where a parametric line crosses
    
    public Rr2Point cross_point(RrLine a) throws rr_ParallelLineException
    {
	return a.point(cross_t(a));
    }
    
    // Take a range of parameter values and a line, and find
    // the intersection of that range with the part of the line
    // (if any) on the solid side of the half-plane.
    
    public RrInterval wipe(RrLine a, RrInterval range)
    {
        if(range.empty()) return range;
        
        // Which way is the line pointing relative to our normal?
        
        boolean wipe_down = (Rr2Point.mul(a.direction(), normal) >= 0);
        
        double t;
        
        try
        {
            t = cross_t(a);
            if (t >= range.high())
            {
                if(wipe_down)
                    return range;
                else
                    return new RrInterval();
            } else if (t <= range.low())
            {
                if(wipe_down)
                    return new RrInterval();
                else
                    return range;                
            } else
            {
                if(wipe_down)
                    return new RrInterval(range.low(), t);
                else
                    return new RrInterval(t, range.high());                 
            }
        } catch (rr_ParallelLineException ple)
        {
            t = value(a.origin());
            if(t <= 0)
                return range;
            else
                return new RrInterval();  
        }
    }

}
