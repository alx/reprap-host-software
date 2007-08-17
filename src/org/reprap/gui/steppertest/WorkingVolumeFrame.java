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

public class WorkingVolumeFrame  extends JFrame /*implements ChangeListener*/ {
	
	private JLabel status;
	
	private final int fastSpeed = 235;
	private final int slowSpeed = 180;
	
	public WorkingVolumeFrame()
	{
		setTitle("Working Volume Probe");
		
		try { 
			talkToBot();
		}
		catch (Exception e){
			JOptionPane.showMessageDialog(null, e.getMessage());
			return;
		}
		
		status = new JLabel("Pick an axis, then zero it by clicking on the home button.");
		
		getContentPane().add(status, BorderLayout.CENTER);
		
		try { 
			addAxisControls();
		}
		catch (Exception e){
			JOptionPane.showMessageDialog(null, e.getMessage());
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
	
	JPanel motorX, motorY, motorZ;
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
	
/*	public void dispose() {

		super.dispose();
		if (extruder != null)
			extruder.dispose();
		if (motorX != null)
			motorX.dispose();
		if (motorY != null)
			motorY.dispose();
		if (motorZ != null)
			motorZ.dispose();
		if (communicator != null)
			communicator.dispose();
	}*/
	
	private void addAxisControls() throws Exception {
		
		JPanel dialogue = new JPanel();
		
		dialogue.setLayout(new GridLayout(3,1));
		
		motorX = axisPanel("X", 1, communicator);
		dialogue.add(motorX);
		
		motorY = axisPanel("Y", 2, communicator);
		dialogue.add(motorY);
		
		motorZ = axisPanel("Z", 3, communicator);
		dialogue.add(motorZ);
			
		getContentPane().add(dialogue, BorderLayout.SOUTH);
	}

	/*public void stateChanged(ChangeEvent evt) {
		try {
			Object srcObj = evt.getSource();
			
			if (srcObj instanceof JSlider) {
				JSlider src = (JSlider)srcObj;
				if (src == speedX || src == speedY || src == speedZ) {
					if (src.getValue() < 1)
						src.setValue(1);
					if (lockXYZSpeed.isSelected()) {
						if (src == speedX) {
							speedY.setValue(speedX.getValue());
							speedZ.setValue(speedX.getValue());
						} else if (src == speedY) {
							speedX.setValue(speedY.getValue());
							speedZ.setValue(speedY.getValue());
						} else if (src == speedZ) {
							speedX.setValue(speedZ.getValue());
							speedY.setValue(speedZ.getValue());
						}
					}
					motorX.updateSpeed();
					motorY.updateSpeed();
					motorZ.updateSpeed();
				}
				
			} else if (srcObj instanceof JCheckBox) {
				JCheckBox src = (JCheckBox)srcObj;
				if (src.isSelected()) {
					speedY.setValue(speedX.getValue());
					speedZ.setValue(speedZ.getValue());
				}
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Update exception: " + ex);
			ex.printStackTrace();
		}
	}
		
	private void reloadPosition() throws IOException {
		motorX.loadPosition();
		motorY.loadPosition();
	}

	protected void onLineButton() {
		try {
			JFrame frame = new JFrame();
			LineTest inst = new LineTest(frame,
					motorX.getMotor(),
					motorY.getMotor(),
					extruder, speedX.getValue(), extruderPanel.getSpeed());
			inst.setVisible(true);
			reloadPosition();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Line test exception: " + ex);
			ex.printStackTrace();
		
		}
	}*/
	
	/* 
	 * 
	 * End of Cribbing from org.reprap.gui.steppertest.Main v818
	 * 
	 */



	
	public JPanel axisPanel(String name, int motorId, Communicator communicator) throws IOException 
	{
	    int maxValue = 30000;
		int startingPosition = 5000;
		Timer updateTimer;
		final GenericStepperMotor motor;
		
		/* 
		 * 
		 * Start of Cribbing from org.reprap.gui.steppertest.StepperPanel v673
		 * 
		 */
		
//super();
		

		final String axis;
		switch(motorId)
		{
		case 1:
			axis = "X";
			break;
		case 2:
			axis = "Y";
			break;
		case 3:
			axis = "Z";
			break;
		default:
			axis = "X";
			System.err.println("StepperPanel - dud axis id: " + motorId);
				
		}
		int address = Preferences.loadGlobalInt(axis + "Axis" + "Address");
		
		final double stepsPerMM = Preferences.loadGlobalDouble(axis + "AxisScale(steps/mm)");
		

		
		updateTimer = new Timer();
		
		motor = new GenericStepperMotor(communicator, new SNAPAddress(address), Preferences.getGlobalPreferences(), motorId);
		
		
		/* 
		 * 
		 * End of Cribbing from org.reprap.gui.steppertest.StepperPanel v673
		 * 
		 */
		
		
		JButton home = new JButton("Home");
		home.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				status.setText("Homing... ");
				status.repaint();
				try {
					motor.homeReset(fastSpeed);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, "Could not home motor: " + ex);
				}
				try {
					status.setText("Axis homed @ " + motor.getPosition() + ". Push an 'Advance' button to move towards the end of the axis...");
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, "Could not get motor position: " + ex);
				}
				
				status.repaint();

				//Set position in memory to zero?

			}
		});
		
		JButton advanceFast = new JButton("Advance FAST");
		advanceFast.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				status.setText("Advancing quickly... be ready to push the STOP button when the axis nears the end.");
				status.repaint();
				try 
				{
					motor.seek(fastSpeed, 30000);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, "Could not advance at fastSpeed: " + ex);
				}
			}
		});
		
		JButton advanceSlow = new JButton("Advance SLOW");
		advanceSlow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				status.setText("Advancing slowly... be ready to push the STOP button when the axis nears the end.");
				status.repaint();
				try {
					motor.seek(slowSpeed, 30000);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, "Could not advance at slowSpeed: " + ex);
				}
			}
		});
		
		JButton stop = new JButton("STOP!");
		stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {

				try {
					motor.setIdle();
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, "Could not stop motor: " + ex);
				}
				try {
					status.setText("Axis stopped @ " + motor.getPosition() + " steps. To save this position as the endstop, click 'Set as Limit'.");
					
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, "Could not get motor position: " + ex);
				}
				status.repaint();
			}
		});
		
		JButton set = new JButton("Calculate Limit");
		set.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				
				
				try {
					//org.reprap.Preferences.setGlobalString("Working" + axis + "(mm)", Integer.toString(motor.getPosition()));
					//doesn't work. Manual workaround displayed next..
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, "Either could not get position or set preferences: " + ex);
				}
				
				try {

					double axisLength = (int)Math.round(motor.getPosition()/stepsPerMM);
					
					status.setText("Steps: " + motor.getPosition() + " @ "
							+ stepsPerMM + " 'steps/mm'. Update 'Working" + axis + "(mm)' to: "
							+ axisLength + " mm.");
					
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, "Could not get position, or unable to load AxisScale preference: " + ex);
				}
				status.repaint();
				
			}
		});

		JPanel panel = new JPanel();
		
		panel.setBorder(new TitledBorder(new EtchedBorder(), name +"-Axis"));
		panel.add(home);
		panel.add(advanceFast);
		panel.add(advanceSlow);
		panel.add(stop);
		panel.add(set);
		return panel;
		
	}
}
