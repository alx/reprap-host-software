package org.reprap.steppertestgui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.reprap.comms.Communicator;
import org.reprap.comms.snap.SNAPAddress;
import org.reprap.comms.snap.SNAPCommunicator;

public class Main implements ChangeListener {

	private final int localNodeNumber = 0;
	private final int baudRate = 19200;
	private final String commPortName = "1";  // Use "0" on linux, "COM1" on Windows, etc

	private final int intialSpeed = 200;
	
	JSlider speedX, speedY, speedZ;
	JCheckBox lockXYZSpeed;
	
	StepperPanel motorX, motorY, motorZ;
	
	Communicator communicator;
	
	private Main() throws Exception {
		SNAPAddress myAddress = new SNAPAddress(localNodeNumber); 
		communicator = new SNAPCommunicator(commPortName, baudRate, myAddress);
	}
	
	private void createAndShowGUI() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JFrame frame = new JFrame("Stepper Exerciser");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        frame.getContentPane().add(panel);
        
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
        
        speedX = new JSlider(JSlider.VERTICAL, 0, 255, intialSpeed);
        speedX.addChangeListener(this);
        speedX.setMajorTickSpacing(50);
        speedX.setMinorTickSpacing(10);
        speedX.setPaintTicks(true);
        speedX.setPaintLabels(true);
        c.gridx = 0;
        c.gridy = 2;
        panel.add(speedX, c);
        
        speedY = new JSlider(JSlider.VERTICAL, 1, 255, intialSpeed);
        speedY.addChangeListener(this);
        speedY.setMajorTickSpacing(50);
        speedY.setMinorTickSpacing(10);
        speedY.setPaintTicks(true);
        speedX.setPaintLabels(true);
        c.gridx = 1;
        panel.add(speedY, c);

        speedZ = new JSlider(JSlider.VERTICAL, 1, 255, intialSpeed);
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
        
        motorX = new StepperPanel("X", 2, speedX, communicator);
        positionPanel.add(motorX);
        
        positionPanel.add(Box.createVerticalStrut(20));
        
        motorY = new StepperPanel("Y", 3, speedY, communicator);
        positionPanel.add(motorY);
        
        positionPanel.add(Box.createVerticalStrut(20));
        
        motorZ = new StepperPanel("Z", 4, speedZ, communicator);
        positionPanel.add(motorZ);

        positionPanel.add(Box.createVerticalStrut(20));
       
        c.gridx = 3;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 4;
        panel.add(positionPanel, c);
        
        frame.pack();
        frame.setVisible(true);
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
    		JOptionPane.showMessageDialog(null, "Update exception: " + ex.getMessage());
		}
	}

	public static void main(String[] args) {

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	try {
	        		Main gui = new Main();
	                gui.createAndShowGUI();
            	}
            	catch (Exception ex) {
            		JOptionPane.showMessageDialog(null, "General exception: " + ex);
            	}
            }
        });
	}
}
