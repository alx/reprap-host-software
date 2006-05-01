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

/**
 * CSG operators; Null and Universal sets, and leaf nodes
 * First version 6 March 2006
 */
package org.reprap.geometry.polygons;
/*

// This is the proper one for Java 1.5...
 
public enum RrCSGOp 
{
    LEAF("LEAF SET"), 
    NULL("NULL SET"), 
    UNIVERSE("UNIVERSAL SET"), 
    UNION("UNION"), 
    INTERSECTION("INTERSECTION");
    
    private String name;
    
    RrCSGOp(String name)
    {
        this.name = name;
    }
    
    public String toString() { return name; }
}*/

// This is the nasty one for Java 1.4

public class RrCSGOp {
	public static final int LEAF = 0;
	public static final int NULL = 1;
	public static final int UNIVERSE = 2;
	public static final int UNION = 3;
	public static final int INTERSECTION = 4; 
	}

