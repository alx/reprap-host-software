package org.reprap.steppertestgui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Main implements ChangeListener {

	private final int intialSpeed = 200;
	
	JSlider speedX, speedY, speedZ;
	JCheckBox lockXYZSpeed;
	
	JSlider positionRequestX, positionRequestY, positionRequestZ;
	JSlider positionActualX, positionActualY, positionActualZ;
	
	Controller controller;
	
	Timer updateTimer;
	boolean waiting = false;
	
	private Main() throws Exception {
		controller = new Controller(this, intialSpeed, intialSpeed, intialSpeed);
		updateTimer = new Timer();
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
        positionPanel.add(Box.createVerticalStrut(20));

        positionPanel.add(new JLabel("Set X axis position"));
        positionPanel.setLayout(new BoxLayout(positionPanel, BoxLayout.Y_AXIS));
        positionRequestX = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
        positionRequestX.addChangeListener(this);
        positionPanel.add(positionRequestX);
        positionPanel.add(new JLabel("Actual X axis position"));
        positionActualX = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
        positionActualX.setEnabled(false);
        positionPanel.add(positionActualX);
        
        positionPanel.add(Box.createVerticalStrut(20));
        
        positionPanel.add(new JLabel("Set Y axis position"));
        positionRequestY = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
        positionRequestY.addChangeListener(this);
        positionPanel.add(positionRequestY);
        positionPanel.add(new JLabel("Actual Y axis position"));
        positionActualY = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
        positionActualY.setEnabled(false);
        positionPanel.add(positionActualY);
        
        positionPanel.add(Box.createVerticalStrut(20));
        
        positionPanel.add(new JLabel("Set Z axis position"));
        positionRequestZ = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
        positionRequestZ.addChangeListener(this);
        positionPanel.add(positionRequestZ);
        positionPanel.add(new JLabel("Actual Z axis position"));
        positionActualZ = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
        positionActualZ.setEnabled(false);
        positionPanel.add(positionActualZ);

        positionPanel.add(Box.createVerticalStrut(20));
       
        c.gridx = 3;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 4;
        panel.add(positionPanel, c);
        
        frame.pack();
        frame.setVisible(true);
    }
	
	protected void updatePositions()
	{
		try {
			if (controller.isMovingX()) {
				positionActualX.setValue(controller.getPositionX());
				startUpdates();
			}
			if (controller.isMovingY()) {
				positionActualY.setValue(controller.getPositionY());
				startUpdates();
			}
			if (controller.isMovingZ()) {
				positionActualZ.setValue(controller.getPositionZ());
				startUpdates();
			}
		} catch (IOException ex) {
			// Ignore these if they happen
			System.out.println("Ignored IO exception in update");
		}
	}
	
	private void startUpdates()
	{
		if (!waiting && (controller.isMovingX() || controller.isMovingY() ||
				controller.isMovingZ())) {
			waiting = true;
			TimerTask task = new TimerTask() {
				public void run() {
					waiting = false;
					updatePositions();
				}			
			};
			updateTimer.schedule(task, 200);
		}
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
					controller.updateSpeeds(speedX.getValue(),
							speedY.getValue(), speedZ.getValue());
				} else if (src == positionRequestX) {
					controller.setPositionX(src.getValue());
					startUpdates();
				} else if (src == positionRequestY) {
					controller.setPositionY(src.getValue());
					startUpdates();
				} else if (src == positionRequestZ) {
					controller.setPositionZ(src.getValue());
					startUpdates();
				}
				
			} else if (srcObj instanceof JCheckBox) {
				JCheckBox src = (JCheckBox)srcObj;
				if (src.isSelected())
					speedY.setValue(speedX.getValue());
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
            		JOptionPane.showMessageDialog(null, "General exception: " + ex.getMessage());
            	}
            }
        });
	}
}
