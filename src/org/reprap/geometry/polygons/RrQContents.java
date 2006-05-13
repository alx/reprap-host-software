
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

This holds the contents of a simple CSG quad.

First version 11 May 2006

*/

package org.reprap.geometry.polygons;

/**
 * Small class for describing simple quad contents
 * @author Adrian Bowyer
 *
 */
public class RrQContents 
{
		public int count;
		public boolean corner;
		public RrLine l1, l2;
		public RrInterval i1, i2;
		public Rr2Point vertex;
		
		public RrQContents(RrCSGPolygon q)
		{
			RrCSG c = q.csg();
			RrBox b = q.box();
			
			count = 0;
			corner = false;
			l1 = null;
			l2 = null;
			vertex = null;
			
			switch(c.complexity())
			{
			case 0:
				return;
				
				// One half-plane in the box:
				
			case 1:
				l1 = new RrLine(c.plane());
				i1 = RrInterval.big_interval();
				i1 = b.wipe(l1, i1);
				if(i1.empty()) 
				{
					l1 = null;
					return;
				}
				count = 1;
				return;
				
				// Two - maybe a corner, or they may not intersect
				
			case 2:
				l1 = new RrLine(c.c_1().plane());
				i1 = RrInterval.big_interval();
				i1 = b.wipe(l1, i1);
				
				l2 = new RrLine(c.c_2().plane());
				i2 = RrInterval.big_interval();
				i2 = b.wipe(l2, i2);
				
				RrInterval oldR1 = new RrInterval(i1);
				RrInterval oldR2 = new RrInterval(i2);
				
				if(c.operator() == RrCSGOp.INTERSECTION)
				{
					i2 = c.c_1().plane().wipe(l2, i2);
					i1 = c.c_2().plane().wipe(l1, i1);
				} else
				{
					i2 = c.c_1().plane().complement().wipe(l2, i2);
					i1 = c.c_2().plane().complement().wipe(l1, i1);                    
				}
				
				Rr2Point[] p = new Rr2Point[] { null, null, null, null };
				
				if(!i1.empty())
				{
					corner = !RrInterval.same(i1, oldR1, 
							Math.sqrt(q.resolution()));
					count++;
					p[0] = l1.point(i1.low());
					p[1] = l1.point(i1.high());
				} else
					l1 = null;
				
				if(!i2.empty())
				{
					corner = corner || !RrInterval.same(i2, oldR2, 
							Math.sqrt(q.resolution()));
					count++;
					p[2] = l2.point(i2.low());
					p[3] = l2.point(i2.high());
				} else
					l2 = null;
				
				for(int i = 0; i < 3; i++)
				{
					if(p[i] != null)
					{
						for(int j = i+1; j < 4; j++)
						{
							if(p[j] != null)
								if(Rr2Point.same(p[j], p[i], q.resolution()))
									vertex = p[i];
						}
					}
				}
				if(corner && vertex == null)
					System.err.println("RrQContents(): can't find cross point!");
				return;
				
			default:
				System.err.println("RrQContents(): complexity > 2.");
			}
		}
	}
