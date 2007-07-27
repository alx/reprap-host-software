/* @author Ed Sells 27 July 2007
 * 
 * Status: Actively working on it.
 *  
 * Designed to walk the axes to their extents
 * thus setting the working volume of the printer.
 * 
 */

package org.reprap.gui.steppertest;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
//import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
//import org.reprap.Printer;
//import java.io.IOException;

public class WorkingVolumeFrame  extends JFrame {

	private JLabel status;
	
	public WorkingVolumeFrame()
	{
		status = new JLabel("Pick an axis, then zero it by clicking on the home button.");
		getContentPane().add(status, BorderLayout.CENTER);
		
		createControlPanel();
		pack();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		show();
	}
	
	public void createControlPanel()
	{
		JPanel xPanel = createXPanel();
		//JPanel yPanel = createYPanel();
		//JPanel zPanel = createZPanel();

		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new GridLayout(3,1));
		controlPanel.add(xPanel);
		//controlPanel.add(yPanel);
		//controlPanel.add(zPanel);
		
		getContentPane().add(controlPanel, BorderLayout.SOUTH);
	
	}
	
	public JPanel createXPanel() 
	{
		JButton home = new JButton("Home");
		home.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				status.setText("Homing...");
				status.repaint();
				//onHomeReset();
				//When homed, reset JLabel to "When homed, push an 'Advance' button to move towards the end of the axis..."
			}
		});
		
		JButton advanceFast = new JButton("Advance FAST");
		advanceFast.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				status.setText("Advancing... be ready to push the STOP button when the axis nears the end.");
				status.repaint();
				//go fast;
			}
		});
		
		JButton advanceSlow = new JButton("Advance SLOW");
		advanceSlow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				status.setText("Advancing... be ready to push the STOP button when the axis nears the end.");
				status.repaint();
				//go slow;
			}
		});
		
		JButton stop = new JButton("STOP!");
		stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				status.setText("Axis stopped. To save this position as the endstop, click 'Set as Limit'.");
				status.repaint();
				//stop;
			}
		});
		
		JButton set = new JButton("Set as Limit");
		set.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				status.setText("Limit set. Returning home...");
				status.repaint();
				//update preferences
				//go home
				//when home reset JLabel
			}
		});

		JPanel panel = new JPanel();
		
		panel.setBorder(new TitledBorder(new EtchedBorder(), "X-Axis"));
		panel.add(home);
		panel.add(advanceFast);
		panel.add(advanceSlow);
		panel.add(stop);
		panel.add(set);
		return panel;
		
	}
}

