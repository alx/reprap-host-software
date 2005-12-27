package org.reprap.steppertestgui;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BoxLayout;
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
	
	private GenericStepperMotor motor; 
	private boolean moving = false;   // True if (as far as we know) the motor is seeking
	private boolean waiting = false;  // True if already waiting for a timer to complete (so we don't start another one)

	private Timer updateTimer;
	
	public StepperPanel(String name, int address, JSlider externalSpeedSlider, Communicator communicator) {
		super();
		
		updateTimer = new Timer();

		motor = new GenericStepperMotor(communicator, new SNAPAddress(address)); 
		
		this.externalSpeedSlider = externalSpeedSlider;
		
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new JLabel("Set " + name + " axis position"));
        positionRequest = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
        positionRequest.addChangeListener(this);
        add(positionRequest);
        add(new JLabel("Actual " + name + " axis position"));
        positionActual = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
        positionActual.setEnabled(false);
        add(positionActual);
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
			if (moving) {
				setDisplayPosition();
				startUpdates();
			}
		} catch (IOException ex) {
			// Ignore these if they happen
			System.out.println("Ignored IO exception in update");
		}
	}

}
