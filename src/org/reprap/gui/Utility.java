package org.reprap.gui;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JFrame;

public class Utility {

	  public static void centerWindowOnScreen(Window w) {
	  	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	  	w.setLocation((screenSize.width - w.getSize().width) / 2,
	  			(screenSize.height - w.getSize().height) / 2);
	  }

	public static Dimension getDefaultAppSize() {
	  	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		return new Dimension(4 * screenSize.width / 5, 4 * screenSize.height / 5);
	}

	public static void centerWindowOnParent(Window w, JFrame parent) {
		Rectangle bounds = parent.getBounds();
		int cx = bounds.x + bounds.width / 2;
		int cy = bounds.y + bounds.height / 2;
		Dimension mySize = w.getSize();
		w.setLocation(cx - mySize.width / 2, cy - mySize.height / 2);
	}
	
}
