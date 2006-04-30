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


RrGraphics: Simple 2D graphics

First version 20 May 2005
This version: 9 October 2005 (translation to Java)

*/

package org.reprap.geometry.polygons;

import java.awt.*;
import javax.swing.*;
    
public class RrGraphics 
{
    private final int frameWidth = 600; // Pixels
    private int frameHeight;
    private RrPolygonList p_list;
    private RrCSGPolygon csg_p;
    private double scale;
    private Rr2Point p_0;
    private Rr2Point pos;
    private Graphics2D g2d;
    private boolean plot_box;

    // Constructor for point-list polygon
    
    RrGraphics(RrPolygonList pl, boolean pb) 
    {
	p_list = pl;
        csg_p = null;
        plot_box = pb;

	RrBox big = pl.box.scale(1.1);

        double width = big.x().length();
        double height = big.y().length();
	frameHeight = (int)(0.5 + (frameWidth*height)/width);
        double xs = (double)frameWidth/width;
        double ys = (double)frameHeight/height;

        if (xs < ys)
            scale = xs;
        else
            scale = ys;
        p_0 = new Rr2Point((frameWidth - (width + 2*big.x().low())*scale)*0.5,
			   (frameHeight - (height + 2*big.y().low())*scale)*0.5);

	pos = new Rr2Point(width*0.5, height*0.5);

	// Display the frame

	JFrame frame = new JFrame();
	frame.setSize(frameWidth, frameHeight);
	frame.getContentPane().add(new MyComponent());
	frame.setVisible(true);
    }
    
    // Constructor for CSG polygon
    
    RrGraphics(RrCSGPolygon cp, boolean pb) 
    {
	p_list = null;
        csg_p = cp;
        plot_box = pb;

	RrBox big = csg_p.box().scale(1.1);

        double width = big.x().length();
        double height = big.y().length();
	frameHeight = (int)(0.5 + (frameWidth*height)/width);
        double xs = (double)frameWidth/width;
        double ys = (double)frameHeight/height;

        if (xs < ys)
            scale = xs;
        else
            scale = ys;
        p_0 = new Rr2Point((frameWidth - (width + 2*big.x().low())*scale)*0.5,
			   (frameHeight - (height + 2*big.y().low())*scale)*0.5);

	pos = new Rr2Point(width*0.5, height*0.5);

	// Display the frame

	JFrame frame = new JFrame();
	frame.setSize(frameWidth, frameHeight);
	frame.getContentPane().add(new MyComponent());
	frame.setVisible(true);
    }

    // Real-world coordinates to pixels
    
    private Rr2Point transform(Rr2Point p)
    {
        return new Rr2Point(p_0.x() + scale*p.x(), (double)frameHeight - 
                (p_0.y() + scale*p.y()));
    }

    // Move invisibly to a point
    
    private void move(Rr2Point p)
    {
        pos = transform(p);
    }

    // Draw a straight line to a point
    
    private void plot(Rr2Point p)
    {
        Rr2Point a = transform(p);
        g2d.drawLine((int)(pos.x() + 0.5), (int)(pos.y() + 0.5), 
		     (int)(a.x() + 0.5), (int)(a.y() + 0.5));
	pos = a;
    }

    // Set the plotting colour
    
    private void colour(int c)
    {
	switch(c)
	    {
	    case 0:
		g2d.setColor(Color.white);
		break;

	    case 1:
		g2d.setColor(Color.black);
		break;

	    case 2:
		g2d.setColor(Color.red);
		break;

	    case 3:
		g2d.setColor(Color.green);
		break;

	    case 4:
		g2d.setColor(Color.blue);
		break;

	    default:
		g2d.setColor(Color.orange);
		break;

	    }
    }
    
    // Plot a box
    
    private void plot(RrBox b)
    {
        colour(4);
        move(b.sw());
        plot(b.nw());
        plot(b.ne());
        plot(b.se());
        plot(b.sw());
    }

    // Plot a polygon
    
    private void plot(RrPolygon p)
    {
        if(plot_box)
            plot(p.box);
        
	int leng = p.size();
	for(int j = 0; j <= leng; j++)
	    {
		int i = j%leng;
		int f = p.flag(i);
		if(f != 0 && j != 0)
		    {
			colour(f);
			plot(p.point(i));
		    } else
		    move(p.point(i)); 
	    }
    }
    
    // Plot a section of parametric line
    
    private void plot(RrLine a, RrInterval i)
    {
        if(i.empty()) return;
        move(a.point(i.low()));
        plot(a.point(i.high()));
    }
    
    // Plot a set in a box
    
    private void plot(RrCSG c, RrBox b)
    {
        if(plot_box)
            plot(b);
        
        colour(1);
        
        switch(c.complexity())
        {
            case 0:
                return;
                
            // One half-plane in the box
                
            case 1:
                if(c.plane() == null)
                    System.err.println("plot(RrCSG, RrBox): hp not set.");
                RrLine ln = new RrLine(c.plane());
                RrInterval range = RrInterval.big_interval();
                range = b.wipe(ln, range);
                if(range.empty()) return;
                plot(ln, range);
                break;
                
            // Two - maybe a corner, or they may not intersect
                
            case 2:
                RrLine ln1 = new RrLine(c.c_1().plane());
                RrInterval range1 = RrInterval.big_interval();
                range1 = b.wipe(ln1, range1);
                RrLine ln2 = new RrLine(c.c_2().plane());
                RrInterval range2 = RrInterval.big_interval();
                range2 = b.wipe(ln2, range2);              
                if(c.operator() == RrCSGOp.INTERSECTION)
                {
                    range2 = c.c_1().plane().wipe(ln2, range2);
                    range1 = c.c_2().plane().wipe(ln1, range1);
                } else
                {
                    range2 = c.c_1().plane().complement().wipe(ln2, range2);
                    range1 = c.c_2().plane().complement().wipe(ln1, range1);                    
                }

                plot(ln1, range1);
                plot(ln2, range2);
                break;
                
            default:
                System.err.println("plot(RrCSG, RrBox): complexity > 2.");
        }
    }
    
    // Plot a divided CSG polygon recursively
    
    private void plot(RrCSGPolygon p)
    {
	if(p.c_1() == null)
        {
            plot(p.csg(), p.box());
        } else
        {
            plot(p.c_1());
            plot(p.c_2());
            plot(p.c_3());
            plot(p.c_4());
        }
    }


    // Master plot function - draw everything
    
    private void plot()
    {
        if(p_list == null)
            plot(csg_p);
        else
        {
            int leng = p_list.size();
            for(int i = 0; i < leng; i++)
                plot(p_list.polygon(i));
        }
    }
    
    class MyComponent extends JComponent 
    {
	// This method is called whenever the contents needs to be painted
	public void paint(Graphics g) 
	{
	    // Retrieve the graphics context; this object is used to paint shapes
	    g2d = (Graphics2D)g;
            // Draw everything
	    plot();
	}
    }
}
