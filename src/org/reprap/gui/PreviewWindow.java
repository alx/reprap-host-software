package org.reprap.gui;
import java.awt.BorderLayout;

import javax.swing.WindowConstants;

public class PreviewWindow extends javax.swing.JFrame implements Previewer {

	private PreviewPanel panel;
	
	public PreviewWindow() {
		super();
		initGUI();
	}
	
	private void initGUI() {
		try {
			panel = new PreviewPanel();
			getContentPane().add(panel, BorderLayout.CENTER);
			setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			pack();
			setSize(500, 350);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setMaterial(int index) {
		panel.setMaterial(index);
	}
	
	public void addSegment(double x1, double y1, double z1,
			double x2, double y2, double z2) {
		panel.addSegment(x1, y1, z1, x2, y2, z2);
	}
	
	public void setMessage(String message) {
		panel.setMessage(message);
	}

	public boolean isCancelled() {
		return panel.isCancelled();
	}
	
	public void reset() {
		panel.reset();
	}

	public void setCancelled(boolean isCancelled) {
		panel.setCancelled(isCancelled);
	}
	
}
