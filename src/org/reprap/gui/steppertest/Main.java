package org.reprap.gui.steppertest;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.reprap.Preferences;
import org.reprap.comms.Communicator;
import org.reprap.comms.snap.SNAPAddress;
import org.reprap.comms.snap.SNAPCommunicator;
import org.reprap.devices.GenericExtruder;
import org.reprap.gui.Utility;

public class Main extends javax.swing.JDialog implements ChangeListener {

	private final int localNodeNumber = 0;
	private final int baudRate = 19200;
	
	private final int intialSpeed = 236;
	
	//private ShapePanel shapePanel;
	private ExtruderPanel extruderPanel;
	
	JSlider speedX, speedY, speedZ;
	JCheckBox lockXYZSpeed;
	
	StepperPanel motorX, motorY, motorZ;
	GenericExtruder extruder;
	
	Communicator communicator;
	
	public static void main(String[] args) throws Exception {
		Thread.currentThread().setName("Stepper Exerciser");
		JFrame frame = new JFrame();
		Main inst = new Main(frame);
		inst.setVisible(true);
	}
	
	public Main(JFrame frame) throws Exception {
		super(frame);

		SNAPAddress myAddress = new SNAPAddress(localNodeNumber); 
		communicator = new SNAPCommunicator(Preferences.loadGlobalString("Port"),
				baudRate, myAddress);
		
		extruder = new GenericExtruder(communicator,
				new SNAPAddress(Preferences.loadGlobalString("Extruder1Address")),
				Preferences.getGlobalPreferences(), 1);
		
		initGUI();
        Utility.centerWindowOnScreen(this);
	}
	
	public void dispose() {
		super.dispose();
		extruder.dispose();
		motorX.dispose();
		motorY.dispose();
		motorZ.dispose();
		communicator.dispose();
	}
	
	private void initGUI() throws Exception {
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("Stepper Exerciser");

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		getContentPane().add(panel);

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		panel.add(new JLabel("Motor speed"), c);
		
		c.gridy = 1;
		c.gridwidth = 1;
		panel.add(new JLabel("X"), c);
		c.gridx = 1;
		panel.add(new JLabel("Y"), c);
		c.gridx = 2;
		panel.add(new JLabel("Z"), c);
		
		speedX = new JSlider(SwingConstants.VERTICAL, 0, 255, intialSpeed);
		speedX.addChangeListener(this);
		speedX.setMajorTickSpacing(50);
		speedX.setMinorTickSpacing(10);
		speedX.setPaintTicks(true);
		speedX.setPaintLabels(true);
		c.gridx = 0;
		c.gridy = 2;
		panel.add(speedX, c);
		
		speedY = new JSlider(SwingConstants.VERTICAL, 1, 255, intialSpeed);
		speedY.addChangeListener(this);
		speedY.setMajorTickSpacing(50);
		speedY.setMinorTickSpacing(10);
		speedY.setPaintTicks(true);
		speedX.setPaintLabels(true);
		c.gridx = 1;
		panel.add(speedY, c);
		
		speedZ = new JSlider(SwingConstants.VERTICAL, 1, 255, intialSpeed);
		speedZ.addChangeListener(this);
		speedZ.setMajorTickSpacing(50);
		speedZ.setMinorTickSpacing(10);
		speedZ.setPaintTicks(true);
		c.gridx = 2;
		panel.add(speedZ, c);
		
		lockXYZSpeed = new JCheckBox("Lock X/Y/Z speed", true);
		lockXYZSpeed.addChangeListener(this);
		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 3;
		panel.add(lockXYZSpeed, c);
		
		JPanel positionPanel = new JPanel();
		positionPanel.setLayout(new BoxLayout(positionPanel, BoxLayout.Y_AXIS));
		
		positionPanel.add(Box.createVerticalStrut(20));
		
		motorX = new StepperPanel("X", 1, speedX, communicator);
		positionPanel.add(motorX);
		
		positionPanel.add(Box.createVerticalStrut(20));
		
		motorY = new StepperPanel("Y", 2, speedY, communicator);
		positionPanel.add(motorY);
		
		positionPanel.add(Box.createVerticalStrut(20));
		
		motorZ = new StepperPanel("Z", 3, speedZ, communicator);
		positionPanel.add(motorZ);
		
		positionPanel.add(Box.createVerticalStrut(20));
		
		c.gridx = 3;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 5;
		panel.add(positionPanel, c);
		
		c.gridx = 0;
		c.gridy = 5;
		c.gridwidth = 4;
		c.gridheight = 1;
		extruderPanel = new ExtruderPanel(extruder);
		panel.add(extruderPanel, c);
		
		
		JPanel buttons = new JPanel();
		c.gridx = 0;
		c.gridy = 6;
		c.gridwidth = 4;
		c.gridheight = 1;
		panel.add(buttons, c);
		
		JButton lineButton = new JButton("Line Test");
		lineButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				onLineButton();
			}
		});
		buttons.add(lineButton);
		
		//JButton squareButton = new JButton("Square Test");
		//squareButton.addActionListener(new ActionListener() {
		//	public void actionPerformed(ActionEvent evt) {
		//		onSquareButton();
		//	}
		//});
		//buttons.add(squareButton);
		
		//JButton circleButton = new JButton("Circle Test");
		//circleButton.addActionListener(new ActionListener() {
		//	public void actionPerformed(ActionEvent evt) {
		//		onCircleButton();
		//	}
		//});
		//buttons.add(circleButton);
pack();
	}

	public void stateChanged(ChangeEvent evt) {
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
	
	protected void onCircleButton() {
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
	}
	
	protected void onSquareButton() {
	}
	
	private void reloadPosition() throws IOException {
		motorX.loadPosition();
		motorY.loadPosition();
	}

}
