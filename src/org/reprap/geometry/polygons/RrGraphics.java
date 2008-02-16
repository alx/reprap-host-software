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
import java.awt.event.*;
import javax.swing.*;
import java.util.List;
import org.reprap.gui.*;

public class RrGraphics 
{
	static final Color background = Color.white;
	static final Color boxes = Color.blue;
	static final Color polygon1 = Color.red;
	static final Color polygon0 = Color.black;	
	static final Color infill = Color.pink;
	static final Color hatch1 = Color.magenta;
	static final Color hatch0 = Color.orange;
	
	/**
	 * Pixels 
	 */
	private final int frame = 600;
	
	/**
	 * 
	 */
	private int frameWidth;
	
	/**
	 * 
	 */
	private int frameHeight;
	
	/**
	 * 
	 */
	private RrPolygonList p_list = null;
	
	/**
	 * 
	 */
	private RrCSGPolygon csg_p = null;
	
	/**
	 * 
	 */
	private boolean csgSolid = true;
	
	/**
	 * 
	 */
	private STLSlice stlc = null;
	
	/**
	 * 
	 */
	private List<RrHalfPlane> hp = null;
	
	/**
	 * 
	 */
	private double scale;
	
	/**
	 * 
	 */
	private Rr2Point p_0;
	
	/**
	 * 
	 */
	private Rr2Point pos;
	
	private RrBox scaledBox, originalBox;
	
	/**
	 * 
	 */
	private static Graphics2D g2d;
	private static JFrame jframe;
	/**
	 * 
	 */
	private boolean plot_box = false;
	
	private void setScales(RrBox b)
	{
		scaledBox = b.scale(1.2);
		
		double width = scaledBox.x().length();
		double height = scaledBox.y().length();
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
		
		// God alone knows why the 5 and 20 are needed next...
		
		p_0 = new Rr2Point((frameWidth - (width + 2*scaledBox.x().low())*scale)*0.5,
				(frameHeight - (height + 2*scaledBox.y().low())*scale)*0.5);
		
		pos = new Rr2Point(width*0.5, height*0.5);
	}
	
	/**
	 * @param b
	 */
	private void init(RrBox b)
	{
		originalBox = b;
		setScales(b);
		
		jframe = new JFrame();
		jframe.setSize(frameWidth, frameHeight);
		jframe.getContentPane().add(new MyComponent());
		jframe.setVisible(true);
		jframe.setCursor(Cursor.CROSSHAIR_CURSOR);
		jframe.addMouseListener(new myMouse());
		jframe.addKeyListener(new myKB());
		
		StatusMessage statusWindow = new StatusMessage(new JFrame());
		statusWindow.setButton("Continue");
		statusWindow.setMessage("Left mouse - magnify\n" +
				"Middle mouse - evaluate\n" +
				"Right mouse - full image\n" +
				"b - toggle boxes\n" + 
				"s - toggle solid shading\n\n" 
				);
		statusWindow.setLocation(new Point(frameWidth + 20, 0));
		statusWindow.setVisible(true);
		
		boolean loop = true;
		while(loop)
		{
			try {
				Thread.sleep(100);
				loop = !statusWindow.isCancelled();
			} catch (InterruptedException e) 
			{
				
			}
		}
		jframe.dispose();
	}
	
	/**
	 * Constructor for point-list polygon
	 * @param pl
	 * @param pb
	 */
	public RrGraphics(RrPolygonList pl) 
	{
		if(pl.size() <= 0)
		{
			System.err.println("Attempt to plot a null polygon list!");
			return;
		}
		
		p_list = pl;
		hp = null;
		csg_p = null;
		stlc = null;
		
		init(pl.getBox());
	}
	
	/**
	 * Constructor for CSG polygon
	 * @param cp
	 */
	public RrGraphics(RrCSGPolygon cp) 
	{
		p_list = null;
		hp = null;
		csg_p = cp;
		stlc = null;
		
		init(csg_p.box());
	}
	
	/**
	 * Constructor for CSG polygon and crossing lines
	 * @param cp
	 * @param pb
	 */
	public RrGraphics(RrCSGPolygon cp, List<RrHalfPlane> h) 
	{
		p_list = null;
		csg_p = cp;
		hp = h;
		stlc = null;
		
		init(csg_p.box());
	}
	
	/**
	 * Constructor for STL polygons
	 * @param s
	 * @param pb
	 */
	public RrGraphics(STLSlice s) 
	{
		p_list = null;
		csg_p = null;
		hp = null;
		stlc = s;
		
		init(stlc.box());
	}
	
	/**
	 * Constructor for just a box - add stuff later
	 * @param b
	 * @param pb
	 */
	public RrGraphics(RrBox b) 
	{
		p_list = null;
		csg_p = null;
		stlc = null;
		hp = null;
		
		init(b);
	}
	
	/**
	 * @param pl
	 */
	public void add(RrPolygonList pl)
	{
		p_list = pl;
	}
	
	/**
	 * @param cp
	 */
	public void add(RrCSGPolygon cp)
	{
		csg_p = cp;
	}
	
	/**
	 * @param s
	 */
	public void add(STLSlice s)
	{
		stlc = s;
	}
	
	/**
	 * @param h
	 */
	public void add(List<RrHalfPlane>h)
	{
		hp = h;
	}
	
	/**
	 * Real-world coordinates to pixels
	 * @param p
	 * @return
	 */
	private Rr2Point transform(Rr2Point p)
	{
		return new Rr2Point(p_0.x() + scale*p.x(), (double)frameHeight - 
				(p_0.y() + scale*p.y()));
	}
	
	/**
	 * Pixels to real-world coordinates
	 * @param p
	 * @return
	 */
	private Rr2Point iTransform(int x, int y)
	{
		return new Rr2Point(((double)x - p_0.x())/scale, ((double)(frameHeight - y)
				- p_0.y())/scale);
	}
	
	/**
	 * Move invisibly to a point
	 * @param p
	 */
	private void move(Rr2Point p)
	{
		pos = transform(p);
	}
		
	/**
	 * Draw a straight line to a point
	 * @param p
	 */
	private void plot(Rr2Point p)
	{
		Rr2Point a = transform(p);
		g2d.drawLine((int)Math.round(pos.x()), (int)Math.round(pos.y()), 
				(int)Math.round(a.x()), (int)Math.round(a.y()));
		pos = a;
	}
	
	
	/**
	 * Plot a box
	 * @param b
	 */
	private void plot(RrBox b)
	{
		if(RrBox.intersection(b, scaledBox).empty())
			return;
		
		g2d.setColor(boxes);
		move(b.sw());
		plot(b.nw());
		plot(b.ne());
		plot(b.se());
		plot(b.sw());
	}
	
	/**
	 * Plot the half-plane lust
	 * @param b
	 */
	private void plot(List<RrHalfPlane> hl)
	{
		for(int i = 0; i < hl.size(); i++)
		{
			RrHalfPlane h = hl.get(i);
			if(!scaledBox.wipe(h.pLine(), RrInterval.bigInterval()).empty())
			{
				if(h.size() > 0)
				{
					move(h.getPoint(0));
					boolean even = false;
					for(int j = 1; j < h.size(); j++)
					{
						even = !even;
						if(even)
							g2d.setColor(hatch1);
						else
							g2d.setColor(hatch0);
						plot(h.getPoint(j));
					}
				}
			}
		}
	}
	
	/**
	 * Plot a polygon
	 * @param p
	 */
	private void plot(RrPolygon p)
	{
		if(RrBox.intersection(p.getBox(), scaledBox).empty())
			return;
		
		move(p.point(0));
		g2d.setColor(polygon1);
		for(int i = 1; i < p.size(); i++)	
				plot(p.point(i));
		g2d.setColor(polygon0);
		plot(p.point(0));
	}
	
	/**
	 * Plot a section of parametric line
	 * @param a
	 * @param i
	 */
	private void plot(RrLine a, RrInterval i)
	{
		if(i.empty()) return;
		move(a.point(i.low()));
		plot(a.point(i.high()));
	}
	
	/**
	 * Recursively fill a CSG quad where it's solid.
	 * @param q
	 */
	private void fillCSG(RrCSGPolygon q)
	{
		if(RrBox.intersection(q.box(), scaledBox).empty())
			return;
		
		if(q.c1() != null)
		{
			fillCSG(q.c1());
			fillCSG(q.c2());
			fillCSG(q.c3());
			fillCSG(q.c4());
			return;
		}
		
		if(q.csg().operator() == RrCSGOp.NULL)
			return;
			
		g2d.setColor(infill);
		Rr2Point sw = transform(q.box().sw());
		Rr2Point ne = transform(q.box().ne());
		int x0 = (int)Math.round(sw.x());
		int y0 = (int)Math.round(sw.y());
		int x1 = (int)Math.round(ne.x());
		int y1 = (int)Math.round(ne.y());
		
		if(q.csg().operator() == RrCSGOp.UNIVERSE)
		{
			g2d.fillRect(x0, y1, x1 - x0 + 1, y0 - y1 + 1);
			return;
		}
		
		for(int x = x0; x <= x1; x++)
		{
			for(int y = y1; y <= y0; y++)  // Bloody backwards coordinates...
			{
				Rr2Point p = iTransform(x, y);
				double v = q.csg().value(p);
				if(v <= 0)
					g2d.fillRect(x, y, 1, 1);
			}
		}
		
	}
	
	private void boxCSG(RrCSGPolygon q)
	{
		if(RrBox.intersection(q.box(), scaledBox).empty())
			return;
		
		if(q.c1() != null)
		{
			boxCSG(q.c1());
			boxCSG(q.c2());
			boxCSG(q.c3());
			boxCSG(q.c4());
			return;
		}
		plot(q.box());
	}
	
	/**
	 * Plot a divided CSG polygon recursively
	 * @param p
	 */
	private void plot(RrCSGPolygon q)
	{
		if(RrBox.intersection(q.box(), scaledBox).empty())
			return;		
		
		if(q.c1() != null)
		{
			plot(q.c1());
			plot(q.c2());
			plot(q.c3());
			plot(q.c4());
			return;
		}
		
		g2d.setColor(polygon1);
		if(q.csg().complexity() == 1)
			plot(q.csg().plane().pLine(), q.interval1());
		else if (q.csg().complexity() == 2)
		{
			plot(q.csg().c1().plane().pLine(), q.interval1());
			plot(q.csg().c2().plane().pLine(), q.interval2());
		}
	}
	
	/**
	 * Recursively plot the boxes for an STL object
	 * @param s
	 */
	private void boxSTL(STLSlice s)
	{
		if(RrBox.intersection(s.box(), scaledBox).empty())
			return;
		
		if(s.leaf())
		{
			g2d.setColor(boxes);
			plot(s.box());
		} else
		{
			boxSTL(s.c1());
			boxSTL(s.c2());
			boxSTL(s.c3());
			boxSTL(s.c4());
		}
	}
	
	/**
	 * Plot a divided STL recursively
	 * @param s
	 */
	private void plot(STLSlice s)
	{
		if(RrBox.intersection(s.box(), scaledBox).empty())
			return;
		
		if(s.leaf())
		{
			g2d.setColor(polygon1);
			for(int i = 0; i < s.edges().size(); i++)
			{
				move(s.segment(i).a);
				plot(s.segment(i).b);
			}
		} else
		{
			plot(s.c1());
			plot(s.c2());
			plot(s.c3());
			plot(s.c4());
		}
	}
	
	/**
	 * Master plot function - draw everything
	 */
	private void plot()
	{
		if(csg_p != null)
		{
			if(csgSolid)
				fillCSG(csg_p);
			
			if(plot_box)
				boxCSG(csg_p);
			else
				plot(csg_p.box());
			
			plot(csg_p);
		}
		
		if(p_list != null)
		{
			int leng = p_list.size();
			for(int i = 0; i < leng; i++)
				plot(p_list.polygon(i));
			if(plot_box)
			{
				for(int i = 0; i < leng; i++)
					plot(p_list.polygon(i).getBox());
			} else
				plot(p_list.getBox());
		}
		
		if(stlc != null)
		{
			if(plot_box)
				boxSTL(stlc);
			else
				plot(stlc.box());
			
			plot(stlc);
		}

		if(hp != null)
		{
			plot(hp);
		}
	}
	
	class myKB implements KeyListener
	{
		public void keyTyped(KeyEvent k)
		{
			switch(k.getKeyChar())
			{
			case 'b':
			case 'B':
				plot_box = !plot_box;
				break;
				
			case 's':
			case 'S':
				csgSolid = !csgSolid;
				
			default:
			}
			jframe.repaint();
		}
		
		public void keyPressed(KeyEvent k)
		{	
		}
		
		public void keyReleased(KeyEvent k)
		{	
		}
	}
	
	/**
	 * Clicking the mouse magnifies
	 * @author ensab
	 *
	 */
	class myMouse implements MouseListener
	{
		private RrBox magBox(RrBox b, int ix, int iy)
		{
			Rr2Point cen = iTransform(ix, iy);
			//System.out.println("Mouse: " + cen.toString() + "; box: " +  scaledBox.toString());
			Rr2Point off = new Rr2Point(b.x().length()*0.05, b.y().length()*0.05);
			return new RrBox(Rr2Point.sub(cen, off), Rr2Point.add(cen, off));
		}
		
		public void mousePressed(MouseEvent e) {
		}
	    public void mouseReleased(MouseEvent e) {
	    }
	    public void mouseEntered(MouseEvent e) {
	    }
	    public void mouseExited(MouseEvent e) {
	    }
	    
	    public void mouseClicked(MouseEvent e) 
	    {
			int ix = e.getX() - 5;  // Why needed??
			int iy = e.getY() - 25; //  "     "
			
			switch(e.getButton())
			{
			case MouseEvent.BUTTON1:
				setScales(magBox(scaledBox, ix, iy));
				break;

			case MouseEvent.BUTTON2:
				if(csg_p != null)
				{
					Rr2Point pc = iTransform(ix, iy);
					System.out.println("Potential at " + pc.toString() + " is " + csg_p.value(pc));
					System.out.println("Quad: " + csg_p.quad(pc).toString());
				}
				break;
				
			case MouseEvent.BUTTON3:

			default:
				setScales(originalBox);
			}
			jframe.repaint();
	    } 
	}
	
	/**
	 * Canvas to paint on 
	 */
	class MyComponent extends JComponent 
	{
		public MyComponent()
		{
			super();
		}
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
