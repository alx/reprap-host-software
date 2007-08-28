/* @author Ed Sells 27 July 2007
 * 
 * Dialogue for fast head positioning (maintenance/research scenarios)
 * 
 * Status: Working on it. Appologies for bosnian code - 
 * one of my first ever coding attempts. Advice welcome!
 * en0es@bath.ac.uk 
 * 
 * Code borrowed from org.reprap.gui.steppertest.Main v818
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.reprap.Preferences;
import org.reprap.comms.Communicator;
import org.reprap.comms.snap.SNAPAddress;
import org.reprap.comms.snap.SNAPCommunicator;
import org.reprap.devices.GenericExtruder;
import org.reprap.devices.GenericStepperMotor;
import org.reprap.gui.Utility;

public class MaintenancePositionsFrame  extends JFrame {
	
	// Panel globals
	private JLabel warning;
	private JLabel area;
	private JLabel status;
	private final int fastSpeed = 245;
	
	// 'Talk to bot' globals

	private final int localNodeNumber = 0;
	private final int baudRate = 19200;
	Communicator communicator;
	
	// Operation globals
	GenericStepperMotor motorX, motorY;
	boolean homePositionAlreadyFound = false;
	
	
	
	public MaintenancePositionsFrame()
	{
		//Establish connection type
		String connection = "nullcartesian";
		try {
		connection = Preferences.loadGlobalString("Geometry");
		}
		
		catch (Exception e){
			JOptionPane.showMessageDialog(null, "Can't establish 'Geometry parameter'" + e);
			return;
		}
		
		//Initialise comms with bot
		if (connection == "cartesian"){
			try { 
				talkToBot();
			}
			catch (Exception e){
				JOptionPane.showMessageDialog(null, "Can't talk to bot: " + e);
				return;
			}
		}
		
		//Establish motors
		try { 
			motorX = new GenericStepperMotor(communicator, new SNAPAddress(Preferences.loadGlobalInt("XAxisAddress")), Preferences.getGlobalPreferences(), 1);
			motorY = new GenericStepperMotor(communicator, new SNAPAddress(Preferences.loadGlobalInt("YAxisAddress")), Preferences.getGlobalPreferences(), 2);
		}
		catch (Exception e){
			JOptionPane.showMessageDialog(null, "Couldn't initialise motors" + e);
			return;
		}
		
		//Build frame
		setTitle("Maintenance positions");
		warning = new JLabel("WARNING: Use Working Volume Probe to establish WorkingAxis(mm) preferences first.");
		area = new JLabel("Finding working area...");
		status = new JLabel("To activate the sector buttons, click home...");
		
		JPanel text = new JPanel();
		text.setLayout(new GridLayout(3,1));	
		text.add(warning);
		text.add(area);
		text.add(status);
		
		getContentPane().add(text, BorderLayout.NORTH);
		
		//Add grid of buttons for sector positioning of head
		try { 
			addSectorGrid();
		}
		catch (Exception e){
			JOptionPane.showMessageDialog(null, "Couldn't add controls" + e);
			return;
		}
		
		pack();
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		show();
	}
	
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
	}
	
	private void addSectorGrid() throws Exception {
		
		double stepsPerMMX = Preferences.loadGlobalDouble("XAxisScale(steps/mm)");
		double axisLengthX = Preferences.loadGlobalDouble("WorkingX(mm)");
		int maxStepsX = (int)Math.round(stepsPerMMX*axisLengthX);
		
		double stepsPerMMY = Preferences.loadGlobalDouble("YAxisScale(steps/mm)");
		double axisLengthY = Preferences.loadGlobalDouble("WorkingY(mm)");
		int maxStepsY = (int)Math.round(stepsPerMMY*axisLengthY);
		
		area.setText("Current working area = " + axisLengthX + " mm x " + axisLengthY + " mm ("
				+ maxStepsX + " steps x " + maxStepsY + " steps)");
		area.repaint();
		
		JPanel sectorXY = new JPanel();
		sectorXY.setLayout(new GridLayout(3,3));		
		
		JButton topLeft = makeSectorXYButton(0, maxStepsY, "");
		JButton topMiddle = makeSectorXYButton(maxStepsX/2, maxStepsY, "^");
		JButton topRight = makeSectorXYButton(maxStepsX, maxStepsY, "");
		JButton middleLeft = makeSectorXYButton(0, maxStepsY/2, "<");
		JButton middle = makeSectorXYButton(maxStepsX/2, maxStepsY/2, "X");
		JButton middleRight = makeSectorXYButton(maxStepsX, maxStepsY/2, ">");
		JButton home = homeButton();
		JButton bottomMiddle= makeSectorXYButton(maxStepsX/2, 0, "V");
		JButton bottomRight = makeSectorXYButton(maxStepsX, 0, "");
		 
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

	private JButton makeSectorXYButton(int x, int y, String pointer)
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