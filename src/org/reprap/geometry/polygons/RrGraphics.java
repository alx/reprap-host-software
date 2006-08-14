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
 This version: 1 May 2006 (Now in CVS - no more comments here)
 
 */

package org.reprap.geometry.polygons;

import java.awt.*;
import javax.swing.*;

public class RrGraphics 
{
	private final int frame = 600; // Pixels
	private int frameWidth;
	private int frameHeight;
	private RrPolygonList p_list;
	private RrCSGPolygon csg_p;
	private STLSlice stlc;
	private double scale;
	private Rr2Point p_0;
	private Rr2Point pos;
	private Graphics2D g2d;
	private boolean plot_box;
	
	private void setScales(RrBox b)
	{
		RrBox big = b.scale(1.2);
		
		double width = big.x().length();
		double height = big.y().length();
		if(width > height)
		{
			frameWidth = frame;
			frameHeight = (int)(0.5 + (frameWidth*height)/width);
		} else
		{
			frameHeight = frame;
			frameWidth = (int)(0.5 + (frameHeight*width)/height);
		}
		double xs = (double)frameWidth/width;
		double ys = (double)frameHeight/height;
		
		if (xs < ys)
			scale = xs;
		else
			scale = ys;
		
		// God alone knows why the 5 and 10 are needed next...
		
		p_0 = new Rr2Point((frameWidth - (width + 2*big.x().low())*scale)*0.5 - 5,
				10 + (frameHeight - (height + 2*big.y().low())*scale)*0.5);
		
		pos = new Rr2Point(width*0.5, height*0.5);
		
		JFrame frame = new JFrame();
		frame.setSize(frameWidth, frameHeight);
		frame.getContentPane().add(new MyComponent());
		frame.setVisible(true);
	}
	
	// Constructor for point-list polygon
	
	public RrGraphics(RrPolygonList pl, boolean pb) 
	{
		if(pl.size() <= 0)
		{
			System.err.println("Attempt to plot a null polygon list!");
			return;
		}
		
		p_list = pl;
		csg_p = null;
		stlc = null;
		plot_box = pb;
		
		setScales(pl.box);
	}
	
	// Constructor for CSG polygon
	
	public RrGraphics(RrCSGPolygon cp, boolean pb) 
	{
		p_list = null;
		csg_p = cp;
		stlc = null;
		plot_box = pb;
		
		setScales(csg_p.box());
	}
	
// Constructor for STL polygons
	
	public RrGraphics(STLSlice s, boolean pb) 
	{
		p_list = null;
		csg_p = null;
		stlc = s;
		plot_box = pb;
		
		setScales(stlc.box());
	}
	
	
	// Constructor for just a box - add stuff later
	
	public RrGraphics(RrBox b, boolean pb) 
	{
		p_list = null;
		csg_p = null;
		stlc = null;
		plot_box = pb;
		
		setScales(b);
	}
	
	public void addPol(RrPolygonList pl)
	{
		p_list = pl;
	}
	
	public void addCSG(RrCSGPolygon cp)
	{
		csg_p = cp;
	}
	
	public void addSTL(STLSlice s)
	{
		stlc = s;
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
			
		case 5:
			g2d.setColor(Color.magenta);
			break;
			
		case 6:
			g2d.setColor(Color.pink);
			
		default:
			g2d.setColor(Color.orange);
		break;
		
		}
	}
	
	// Plot a box
	
	private void plot(RrBox b)
	{
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
		{
			colour(5);
			plot(p.box);
		}
		
//		colour(4);
//		plot(p.point(0));
		
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
				if(j != leng)
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
	
	private void plotLeaf(RrCSGPolygon q)
	{
		if(plot_box)
		{
			colour(4);
			plot(q.box());
		}
		
		colour(2);
		
		if(q.csg().complexity() == 1)
			plot(q.csg().plane().pLine(), q.interval1());
		else if (q.csg().complexity() == 2)
		{
			plot(q.csg().c_1().plane().pLine(), q.interval1());
			plot(q.csg().c_2().plane().pLine(), q.interval2());
		}
	}
	
	// Plot a divided CSG polygon recursively
	
	private void plot(RrCSGPolygon p)
	{
		if(p.c_1() == null)
		{
			plotLeaf(p);
		} else
		{
			plot(p.c_1());
			plot(p.c_2());
			plot(p.c_3());
			plot(p.c_4());
		}
	}
	
// Plot a divided STL recursively
	
	private void plot(STLSlice s)
	{
		if(s.c_1() == null)
		{
			if(plot_box)
			{
				colour(4);
				plot(s.box());
			}
			colour(1);
			for(int i = 0; i < s.edges().size(); i++)
			{
				move(s.segment(i).a);
				plot(s.segment(i).b);
			}
		} else
		{
			plot(s.c_1());
			plot(s.c_2());
			plot(s.c_3());
			plot(s.c_4());
		}
	}
	
	
	// Master plot function - draw everything
	
	private void plot()
	{
		if(csg_p != null)
			plot(csg_p);
		if(p_list != null)
		{
			int leng = p_list.size();
			for(int i = 0; i < leng; i++)
				plot(p_list.polygon(i));
		}
		if(stlc != null)
		{
			plot(stlc);
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
