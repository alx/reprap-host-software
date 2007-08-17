/* @author Ed Sells 27 July 2007
 * 
 * Dialogue for fast head positioning (maintenance/research scenarios)
 * 
 * Status: Working on it. Appologies for bosnian code - 
 * one of my first ever coding attempts. Advice welcome!
 * en0es@bath.ac.uk 
 */

package org.reprap.gui.steppertest;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import org.reprap.Printer;
import java.io.IOException;
import java.util.Timer;

import javax.swing.event.ChangeEvent;

import org.reprap.Preferences;
import org.reprap.comms.Communicator;
import org.reprap.comms.snap.SNAPAddress;
import org.reprap.comms.snap.SNAPCommunicator;
import org.reprap.devices.GenericExtruder;
import org.reprap.devices.GenericStepperMotor;
import org.reprap.gui.Utility;

import javax.swing.event.ChangeListener;

public class MaintenancePositionsFrame  extends JFrame /*implements ChangeListener*/ {
	
	private JLabel warning;
	private JLabel status;
	
	private final int fastSpeed = 245;

	GenericStepperMotor motorX, motorY;
	
	boolean homePositionAlreadyFound = false;
	
	public MaintenancePositionsFrame()
	{
		try { 
			talkToBot();
		}
		catch (Exception e){
			JOptionPane.showMessageDialog(null, "Can't talk to bot: " + e);
			return;
		}
		
		try { 
			motorX = new GenericStepperMotor(communicator, new SNAPAddress(Preferences.loadGlobalInt("XAxisAddress")), Preferences.getGlobalPreferences(), 1);
			motorY = new GenericStepperMotor(communicator, new SNAPAddress(Preferences.loadGlobalInt("YAxisAddress")), Preferences.getGlobalPreferences(), 2);
		}
		catch (Exception e){
			JOptionPane.showMessageDialog(null, "Couldn't initialise motors" + e);
			return;
		}
		


		warning = new JLabel("WARNING: Use Working Volume Probe to establish WorkingAxis(mm) preferences first.");
		status = new JLabel("To activate the sector buttons, click home...");
		
		JPanel text = new JPanel();
		text.setLayout(new GridLayout(2,1));	
		text.add(warning);
		text.add(status);
		
		getContentPane().add(text, BorderLayout.NORTH);
		
		try { 
			addControls();
		}
		catch (Exception e){
			JOptionPane.showMessageDialog(null, "Couldn't add controls" + e);
			return;
		}
		
		pack();
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		show();
	}
	
	/*
	 * 
	 * Code cribbed from org.reprap.gui.steppertest.Main v818
	 * Needs review
	 * 
	 */
	
	private final int localNodeNumber = 0;
	private final int baudRate = 19200;
	private final int intialSpeed = 236;
	
	//private ShapePanel  shapePanel;
	private ExtruderPanel extruderPanel;
	

	GenericExtruder extruder = null;
	
	Communicator communicator;
	
	public void talkToBot() throws Exception {
		
		SNAPAddress myAddress = new SNAPAddress(localNodeNumber);
		
		String port = Preferences.loadGlobalString("Port(name)");
		String err = "";
		
		try {
			communicator = new SNAPCommunicator(port,baudRate, myAddress);
		}
		catch (gnu.io.NoSuchPortException e)
		{
			err = "There was an error opening " + port + ".\n\n";
			err += "Check to make sure that is the right path.\n";
			err += "Check that you have your serial connector plugged in.";
			
			throw new Exception(err);
		}
		catch (gnu.io.PortInUseException e)
		{
			err = "The " + port + " port is already in use by another program.";
			
			throw new Exception(err);
		}
		
		if (err.length() == 0)
		{
			extruder = new GenericExtruder(communicator,
					new SNAPAddress(Preferences.loadGlobalString("Extruder0_Address")),
					Preferences.getGlobalPreferences(), 0);
		

	        Utility.centerWindowOnScreen(this);
		}
	}
	
	private void addControls() throws Exception {
		
		setTitle("Maintenance positions");
		
		double stepsPerMMX = Preferences.loadGlobalDouble("XAxisScale(steps/mm)");
		double axisLengthX = Preferences.loadGlobalDouble("WorkingX(mm)");
		int maxStepsX = (int)Math.round(stepsPerMMX*axisLengthX);
		
		double stepsPerMMY = Preferences.loadGlobalDouble("YAxisScale(steps/mm)");
		double axisLengthY = Preferences.loadGlobalDouble("WorkingY(mm)");
		int maxStepsY = (int)Math.round(stepsPerMMY*axisLengthY);
		
		JPanel sectorXY = new JPanel();
		sectorXY.setLayout(new GridLayout(3,3));		
		
		JButton topLeft = sectorXYButton(0, maxStepsY, "");
		JButton topMiddle = sectorXYButton(maxStepsX/2, maxStepsY, "^");
		JButton topRight = sectorXYButton(maxStepsX, maxStepsY, "");
		JButton middleLeft = sectorXYButton(0, maxStepsY/2, "<");
		JButton middle = sectorXYButton(maxStepsX/2, maxStepsY/2, "X");
		JButton middleRight = sectorXYButton(maxStepsX, maxStepsY/2, ">");
		JButton home = homeButton();
		JButton bottomMiddle= sectorXYButton(maxStepsX/2, 0, "V");
		JButton bottomRight = sectorXYButton(maxStepsX, 0, "");
		 
		sectorXY.add(topLeft);
		sectorXY.add(topMiddle);
		sectorXY.add(topRight);
		sectorXY.add(middleLeft);
		sectorXY.add(middle);
		sectorXY.add(middleRight);
		sectorXY.add(home);
		sectorXY.add(bottomMiddle);
		sectorXY.add(bottomRight);
		
		getContentPane().add(sectorXY, BorderLayout.SOUTH);
		
	}

	private JButton sectorXYButton(int x, int y, String pointer)
	{
		final int xCoord = x;
		final int yCoord = y;
		
		JButton sector = new JButton(pointer);
		sector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				if (homePositionAlreadyFound) 
				{
					status.setText("Travelling...");
					status.repaint();
					try {
							motorX.seek(fastSpeed, xCoord);
							motorX.seek(fastSpeed, yCoord);
	
						
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(null, "Could not position motor: " + ex);
					}
					try {
					
						status.setText("Step position: " + motorX.getPosition()
								+ ", " + motorY.getPosition()); 
						status.repaint();
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(null, "Could not find motor position " + ex);
					}
				}
				else {
					status.setText("Find the home position before sectors can activate."); 
					status.repaint();
				}
			}
		});
		return sector;
	}

	private JButton homeButton()
	{
		JButton home = new JButton("Home");
		home.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				status.setText("Homing... ");
				status.repaint();
				try {
					motorX.homeReset(fastSpeed);
					motorY.homeReset(fastSpeed);
					homePositionAlreadyFound = true;
				} 
				catch (Exception ex) {
					homePositionAlreadyFound = false;
					JOptionPane.showMessageDialog(null, "Could not home motor: " + ex);
				}
				if (homePositionAlreadyFound = true)
				{
				status.setText("Axes homed. You may now click any sector button...");
				status.repaint();
				}
			}
		});
		return home;
	}
}