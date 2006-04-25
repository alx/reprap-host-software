package org.reprap.gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;

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
	
}
