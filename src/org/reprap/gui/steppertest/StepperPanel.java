package org.reprap.steppertestgui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.reprap.comms.Communicator;
import org.reprap.comms.snap.SNAPAddress;
import org.reprap.devices.GenericStepperMotor;

public class StepperPanel extends JPanel implements ChangeListener {

	private static final long serialVersionUID = 6262697694879478425L;

	private JSlider externalSpeedSlider;  // Externally maintained speed slider
	private JSlider positionRequest;      // Slider to control position
	private JSlider positionActual;       // Slider to indicate position
	
	private JCheckBox torque;            // If motor is driving or not
	
	private GenericStepperMotor motor; 
	private boolean moving = false;   // True if (as far as we know) the motor is seeking
	private boolean waiting = false;  // True if already waiting for a timer to complete (so we don't start another one)

	private Timer updateTimer;
	
	public StepperPanel(String name, int address, JSlider externalSpeedSlider, Communicator communicator) {
		super();
		
		updateTimer = new Timer();

		motor = new GenericStepperMotor(communicator, new SNAPAddress(address)); 
		
		this.externalSpeedSlider = externalSpeedSlider;
	
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
                
        add(new JLabel("Set " + name + " axis position"), c);
        positionRequest = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
        positionRequest.addChangeListener(this);

        c.gridy = 1;
        add(positionRequest, c);
        c.gridy = 2;
        add(new JLabel("Actual " + name + " axis position"), c);
        positionActual = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
        positionActual.setEnabled(false);
        c.gridy = 3;
        add(positionActual, c);
        
        c.gridx = 1;
        c.gridy = 1;
        JButton calibrate = new JButton("Calibrate");
        calibrate.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent evt) {
        		onCalibrate();
        	}
        });
        //add(calibrate, c);
        
        c.gridy = 2;
        torque = new JCheckBox("Torque");
        torque.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent evt) {
        		onTorqueUpdate();
        	}
        });
        add(torque, c);
	}
	
	protected void onCalibrate() {
		JOptionPane.showMessageDialog(null, "Calibrate not implemented yet");
	}
	
	protected void onTorqueUpdate()
	{
		try {
			if (torque.isSelected()) {
				// You can't currently do this
				torque.setSelected(false);
			} else {
				motor.setIdle();
				moving = false;
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Could not idle motor: " + ex);
		}
			
	}

	public void stateChanged(ChangeEvent evt) {
		try {
			Object srcObj = evt.getSource();
			
			if (srcObj instanceof JSlider) {
				JSlider src = (JSlider)srcObj;
				if (src == positionRequest)
					seekToSelectedPosition();
			}
		} catch (Exception ex) {
    		JOptionPane.showMessageDialog(null, "Update exception: " + ex);
		} 
	}

	private void startUpdates() {
		if (!waiting && moving) {
			waiting = true;
			TimerTask task = new TimerTask() {
				public void run() {
					waiting = false;
					updatePosition();
				}			
			};
			updateTimer.schedule(task, 200);
		}
	}
	
	private void setDisplayPosition() throws IOException {
		int position = motor.getPosition();
		positionActual.setValue(position);
		if (position == positionRequest.getValue())
			moving = false;
	}	

	private void seekToSelectedPosition() throws IOException {
		motor.seek(externalSpeedSlider.getValue(), positionRequest.getValue());
		torque.setSelected(true);
		moving = true;
		startUpdates();
	}

	public void updateSpeed() throws IOException {
		if (moving)
			seekToSelectedPosition();
	}

	protected void updatePosition()
	{
		try {
			setDisplayPosition();
			if (moving)
				startUpdates();
		} catch (IOException ex) {
			// Ignore these if they happen
			System.out.println("Ignored IO exception in update: " + ex);
		}
	}

}
