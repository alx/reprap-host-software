package org.reprap.steppertestgui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.reprap.devices.GenericExtruder;

public class ExtruderPanel extends JPanel {
	
	private GenericExtruder extruder;
	private boolean extruding = false;
	private JButton extrudeButton;
	private JSlider speed;
	
	public ExtruderPanel(GenericExtruder extruder) {
		super();
		this.extruder = extruder;
		
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		c.gridx = 0;
		c.gridy = 0;
		add(new JLabel("Extrusion speed: "), c);
		
		speed = new JSlider(JSlider.HORIZONTAL, 0, 255, 255);
		speed.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				onExtrudeSpeed();
			}
		});
		c.gridx = 1;
		c.gridy = 0;
		add(speed, c);
		
		extrudeButton = new JButton("Extrude");
		extrudeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				onExtrudeButton();
			}
		});
		
		c.gridx = 2;
		c.gridy = 0;
		c.insets.left = 5;
		add(extrudeButton, c);
		
	}
	
	protected void onExtrudeSpeed() {
		if (extruding)
			setExtruderSpeed();
	}

	private void setExtruderSpeed() {
		try {
			extruder.setExtrusion(extruding?speed.getValue():0);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Extruder exception: " + ex);
			ex.printStackTrace();
		}
	}

	protected void onExtrudeButton() {
		if (extruding) {
			extruding = false;
			extrudeButton.setText("Extrude");
		} else {
			extrudeButton.setPreferredSize(extrudeButton.getSize());
			extruding = true;
			extrudeButton.setText("Stop");
		}
		setExtruderSpeed();
	}

	public int getSpeed() {
		return speed.getValue();
	}
	
}
