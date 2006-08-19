package org.reprap.gui;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

/**
 * This code was edited or generated using CloudGarden's Jigloo
 * SWT/Swing GUI Builder, which is free for non-commercial
 * use. If Jigloo is being used commercially (ie, by a corporation,
 * company or business for any purpose whatever) then you
 * should purchase a license for each developer using Jigloo.
 * Please visit www.cloudgarden.com for details.
 * Use of Jigloo implies acceptance of these licensing terms.
 * A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
 * THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
 * LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
 */
public class Preferences extends javax.swing.JDialog {
	private JButton jButtonOK;
	private JButton jButtonCancel;
	private JComboBox geometry;
	private JLabel jLabel2;
	private JLabel jLabel4;
	private JLabel jLabel8;
	private JTextField motorScale3;
	private JTextField motorScale2;
	private JTextField motorScale1;
	private JLabel jLabel22;
	private JLabel jLabel21;
	private JLabel jLabel20;
	private JLabel jLabel19;
	private JTextField extrusionSize;
	private JTextField extruderMaxSpeed1;
	private JCheckBox idleZMotor;
	private JCheckBox rememberWindowPosition;
	private JTextField coolingPeriod;
	private JLabel jLabel5;
	private JTextField extrudert01;
	private JLabel jLabel31;
	private JLabel jLabel30;
	private JLabel jLabel29;
	private JTextField extruderOffsetZ1;
	private JTextField extruderOffsetY1;
	private JLabel jLabel28;
	private JTextField extruderOffsetX1;
	private JLabel jLabel27;
	private JLabel jLabel26;
	private JLabel jLabel25;
	private JTextField hb;
	private JTextField hm;
	private JLabel jLabel24;
	private JLabel jLabel23;
	private JTextField extrusionHeight;
	private JPanel jPanelProduction;
	private JTextField extrusionTemp;
	private JLabel jLabel18;
	private JTextField extrusionSpeed;
	private JLabel jLabel17;
	private JLabel jLabel16;
	private JTextField movementSpeedXY;
	private JTextField movementSpeedZ;
	private JTextField extruderRz1;
	private JTextField extruderAddress1;
	private JTextField extruderBeta1;
	private JLabel jLabel15;
	private JLabel jLabel14;
	private JLabel jLabel13;
	private JLabel jLabel12;
	private JLabel jLabel11;
	private JPanel jPanelExtruders;
	private JLabel jLabel10;
	private JLabel jLabel9;
	private JTextField motorTorque1;
	private JTextField motorTorque2;
	private JTextField motorTorque3;
	private JTextField motorAddress1;
	private JTextField motorAddress2;
	private JTextField motorAddress3;
	private JLabel jLabel7;
	private JLabel jLabel6;
	private JLabel speedLbl;
	private JLabel jLabel3;
	private JPanel jPanelMotors;
	private JTextPane jTextPane1;
	private JTextField serialPort;
	private JLabel jLabel1;
	private JPanel jPanelGeneral;
	private JTabbedPane jTabbedPane1;
	
	private String [][] geometries =
	{
			{ "cartesian", "Cartesian" },
			{ "nullcartesian", "Null cartesian" }
	};
	
	/**
	 * Auto-generated main method to display this JDialog
	 */
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		Preferences inst = new Preferences(frame);
		inst.setVisible(true);
	}
	
	private String loadString(String name) throws IOException {
		return org.reprap.Preferences.loadGlobalString(name);
	}
	
	private boolean loadBool(String name) throws IOException {
		return org.reprap.Preferences.loadGlobalBool(name);
	}
	
	private void saveString(String name, String value) throws IOException {
		org.reprap.Preferences.setGlobalString(name, value);
	}
	
	private void saveBool(String name, boolean value) throws IOException {
		org.reprap.Preferences.setGlobalBool(name, value);
	}
	
	public void loadPreferences() {
		try {
			serialPort.setText(loadString("Port"));
			motorAddress1.setText(loadString("Axis1Address"));
			motorAddress2.setText(loadString("Axis2Address"));
			motorAddress3.setText(loadString("Axis3Address"));
			motorTorque1.setText(loadString("Axis1Torque"));
			motorTorque2.setText(loadString("Axis2Torque"));
			motorTorque3.setText(loadString("Axis3Torque"));
			motorScale1.setText(loadString("Axis1Scale"));
			motorScale2.setText(loadString("Axis2Scale"));
			motorScale3.setText(loadString("Axis3Scale"));
			
			extruderAddress1.setText(loadString("Extruder1Address"));
			extruderBeta1.setText(loadString("Extruder1Beta"));
			extruderRz1.setText(loadString("Extruder1Rz"));
			extruderMaxSpeed1.setText(loadString("Extruder1MaxSpeed"));
			extruderOffsetX1.setText(loadString("Extruder1OffsetX"));
			extruderOffsetY1.setText(loadString("Extruder1OffsetY"));
			extruderOffsetZ1.setText(loadString("Extruder1OffsetZ"));
			
			hm.setText(loadString("Extruder1hm"));
			hb.setText(loadString("Extruder1hb"));
			extrudert01.setText(loadString("Extruder1t0"));
			
			extrusionSpeed.setText(loadString("ExtrusionSpeed"));
			extrusionTemp.setText(loadString("ExtrusionTemp"));
			extrusionSize.setText(loadString("ExtrusionSize"));
			extrusionHeight.setText(loadString("ExtrusionHeight"));
			movementSpeedXY.setText(loadString("MovementSpeed"));
			movementSpeedZ.setText(loadString("MovementSpeedZ"));
			
			coolingPeriod.setText(loadString("CoolingPeriod"));
			
			idleZMotor.setSelected(loadBool("IdleZAxis"));
			rememberWindowPosition.setSelected(loadBool("RememberWindowPosition"));
			
			String geometryName = loadString("Geometry");
			for(int i = 0; i < geometries.length; i++)
				if (geometries[i][0].compareToIgnoreCase(geometryName) == 0)
					geometry.setSelectedIndex(i);
			
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Loading preferences: " + ex);
			ex.printStackTrace();
		}
		
	}

	public void savePreferences() {
		try {
			saveString("Port", serialPort.getText());
			saveString("Axis1Address", motorAddress1.getText());
			saveString("Axis2Address", motorAddress2.getText());
			saveString("Axis3Address", motorAddress3.getText());
			saveString("Axis1Torque", motorTorque1.getText());
			saveString("Axis2Torque", motorTorque2.getText());
			saveString("Axis3Torque", motorTorque3.getText());
			saveString("Axis1Scale", motorScale1.getText());
			saveString("Axis2Scale", motorScale2.getText());
			saveString("Axis3Scale", motorScale3.getText());
			
			saveString("Geometry", geometries[geometry.getSelectedIndex()][0]);

			saveString("Extruder1Address", extruderAddress1.getText());
			saveString("Extruder1Beta", extruderBeta1.getText());
			saveString("Extruder1Rz", extruderRz1.getText());
			saveString("Extruder1MaxSpeed", extruderMaxSpeed1.getText());
			saveString("Extruder1OffsetX", extruderOffsetX1.getText());
			saveString("Extruder1OffsetY", extruderOffsetY1.getText());
			saveString("Extruder1OffsetZ", extruderOffsetZ1.getText());
			saveString("Extruder1hm", hm.getText());
			saveString("Extruder1hb", hb.getText());
			saveString("Extruder1t0", extrudert01.getText());
			
			saveString("ExtrusionSpeed", extrusionSpeed.getText());
			saveString("ExtrusionTemp", extrusionTemp.getText());
			saveString("ExtrusionSize", extrusionSize.getText());
			saveString("ExtrusionHeight", extrusionHeight.getText());
			saveString("MovementSpeed", movementSpeedXY.getText());
			saveString("MovementSpeedZ", movementSpeedZ.getText());

			saveString("CoolingPeriod", coolingPeriod.getText());
			saveBool("IdleZAxis", idleZMotor.isSelected());
			saveBool("RememberWindowPosition", rememberWindowPosition.isSelected());
			
			org.reprap.Preferences.saveGlobal();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Saving preferences: " + ex);
			ex.printStackTrace();
		}
	}
	
	public Preferences(JFrame frame) {
		super(frame);
		initGUI();
		loadPreferences();
        Utility.centerWindowOnParent(this, frame);
	}
	
	private void initGUI() {
		try {
			{
				jButtonOK = new JButton();
				getContentPane().add(jButtonOK);
				jButtonOK.setText("OK");
				jButtonOK.setBounds(357, 274, 77, 28);
				jButtonOK.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent evt) {
						jButtonOKMouseClicked(evt);
					}
				});
			}
			{
				jButtonCancel = new JButton();
				getContentPane().add(jButtonCancel);
				jButtonCancel.setText("Cancel");
				jButtonCancel.setBounds(266, 274, 77, 28);
				jButtonCancel.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent evt) {
						jButtonCancelMouseClicked(evt);
					}
				});
			}
			{
				jTabbedPane1 = new JTabbedPane();
				getContentPane().add(jTabbedPane1);
				jTabbedPane1.setBounds(7, 7, 427, 264);
				{
					jPanelGeneral = new JPanel();
					jTabbedPane1.addTab("General", null, jPanelGeneral, null);
					jPanelGeneral.setPreferredSize(new java.awt.Dimension(373, 198));
					jPanelGeneral.setLayout(null);
					{
						jLabel1 = new JLabel();
						jPanelGeneral.add(jLabel1);
						jLabel1.setText("Serial port");
						jLabel1.setBounds(7, 14, 84, 28);
					}
					{
						serialPort = new JTextField();
						jPanelGeneral.add(serialPort);
						serialPort.setBounds(91, 14, 154, 28);
					}
					{
						jTextPane1 = new JTextPane();
						jPanelGeneral.add(jTextPane1);
						jTextPane1.setText("For linux use a number such as \"0\", \"1\" or alternatively use the full path to your serial device.  For Windows use \"COM1\", \"COM2\" etc.");
						jTextPane1.setBounds(91, 49, 273, 63);
						jTextPane1.setEnabled(false);
						jTextPane1.setEditable(false);
						jTextPane1.setOpaque(false);
					}
					/*{
						rememberWindowPosition = new JCheckBox();
						jPanelGeneral.add(rememberWindowPosition);
						rememberWindowPosition.setText("Remember window position at startup");
						rememberWindowPosition.setBounds(35, 105, 260, 28);
					}*/
				}
				{
					jPanelMotors = new JPanel();
					jTabbedPane1.addTab("Axes", null, jPanelMotors, null);
					jPanelMotors.setLayout(null);
					{
						String [] geometryList = new String[geometries.length];
						for(int i = 0; i < geometries.length; i++)
							geometryList[i] = geometries[i][1];
						ComboBoxModel geometryModel = new DefaultComboBoxModel(
								geometryList);
						geometry = new JComboBox();
						jPanelMotors.add(geometry);
						geometry.setModel(geometryModel);
						geometry.setBounds(91, 14, 182, 28);
					}
					{
						jLabel2 = new JLabel();
						jPanelMotors.add(jLabel2);
						jLabel2.setText("Geometry");
						jLabel2.setBounds(14, 14, 63, 28);
					}
					{
						jLabel3 = new JLabel();
						jPanelMotors.add(jLabel3);
						jLabel3.setText("Motor 1 (X)");
						jLabel3.setBounds(14, 81, 84, 28);
					}
					{
						jLabel4 = new JLabel();
						jPanelMotors.add(jLabel4);
						jLabel4.setText("Motor 2 (Y)");
						jLabel4.setBounds(14, 109, 84, 28);
					}
					{
						speedLbl = new JLabel();
						jPanelMotors.add(speedLbl);
						speedLbl.setText("Speed (XY)");
						speedLbl.setBounds(14, 168, 70, 28);
					}
					{
						speedLbl = new JLabel();
						jPanelMotors.add(speedLbl);
						speedLbl.setText("Speed  (Z)");
						speedLbl.setBounds(14, 198, 70, 28);
					}
					{
						jLabel16 = new JLabel();
						jPanelMotors.add(jLabel16);
						jLabel16.setText("Motor 3 (Z)");
						jLabel16.setBounds(14, 137, 84, 28);
					}
					{
						jLabel6 = new JLabel();
						jPanelMotors.add(jLabel6);
						jLabel6.setText("Address");
						jLabel6.setBounds(105, 56, 63, 28);
						jLabel6.setHorizontalAlignment(SwingConstants.CENTER);
					}
					{
						jLabel22 = new JLabel();
						jPanelMotors.add(jLabel22);
						jLabel22.setText("Max torque");
						jLabel22.setBounds(182, 56, 77, 28);
					}
					{
						jLabel7 = new JLabel();
						jPanelMotors.add(jLabel7);
						jLabel7.setText("Scale");
						jLabel7.setBounds(259, 56, 77, 28);
					}
					{
						motorAddress3 = new JTextField();
						jPanelMotors.add(motorAddress3);
						motorAddress3.setBounds(112, 140, 49, 21);
					}
					{
						motorAddress1 = new JTextField();
						jPanelMotors.add(motorAddress1);
						motorAddress1.setBounds(112, 84, 49, 21);
					}
					{
						motorAddress2 = new JTextField();
						jPanelMotors.add(motorAddress2);
						motorAddress2.setBounds(112, 112, 49, 21);
					}
					{
						motorTorque1 = new JTextField();
						jPanelMotors.add(motorTorque1);
						motorTorque1.setBounds(182, 84, 35, 21);
					}
					{
						motorTorque3 = new JTextField();
						jPanelMotors.add(motorTorque3);
						motorTorque3.setBounds(182, 140, 35, 21);
					}
					{
						motorTorque2 = new JTextField();
						jPanelMotors.add(motorTorque2);
						motorTorque2.setBounds(182, 112, 35, 21);
					}
					{
						motorScale1 = new JTextField();
						jPanelMotors.add(motorScale1);
						motorScale1.setBounds(259, 84, 91, 21);
					}
					{
						motorScale2 = new JTextField();
						jPanelMotors.add(motorScale2);
						motorScale2.setBounds(259, 112, 91, 21);
					}
					{
						motorScale3 = new JTextField();
						jPanelMotors.add(motorScale3);
						motorScale3.setBounds(259, 140, 91, 21);
					}
					{
						jLabel8 = new JLabel();
						jPanelMotors.add(jLabel8);
						jLabel8.setText("%");
						jLabel8.setBounds(224, 140, 21, 21);
					}
					{
						jLabel9 = new JLabel();
						jPanelMotors.add(jLabel9);
						jLabel9.setText("%");
						jLabel9.setBounds(224, 84, 21, 21);
					}
					{
						jLabel10 = new JLabel();
						jPanelMotors.add(jLabel10);
						jLabel10.setText("%");
						jLabel10.setBounds(224, 112, 21, 21);
					}
					{
						movementSpeedXY = new JTextField();
						jPanelMotors.add(movementSpeedXY);
						movementSpeedXY.setBounds(112, 170, 105, 21);
					}
					{
						movementSpeedZ = new JTextField();
						jPanelMotors.add(movementSpeedZ);
						movementSpeedZ.setBounds(112, 200, 105, 21);
					}
				}
				{
					jPanelExtruders = new JPanel();
					jTabbedPane1.addTab("Extruders", null, jPanelExtruders, null);
					jPanelExtruders.setLayout(null);
					jPanelExtruders.setPreferredSize(new java.awt.Dimension(
						420,
						196));
					{
						jLabel11 = new JLabel();
						jPanelExtruders.add(jLabel11);
						jLabel11.setText("Extruder");
						jLabel11.setBounds(7, 7, 56, 21);
						jLabel11.setHorizontalAlignment(SwingConstants.RIGHT);
					}
					{
						jLabel14 = new JLabel();
						jPanelExtruders.add(jLabel14);
						jLabel14.setText("Rz");
						jLabel14.setBounds(224, 7, 42, 21);
					}
					{
						jLabel12 = new JLabel();
						jPanelExtruders.add(jLabel12);
						jLabel12.setText("1");
						jLabel12.setBounds(35, 28, 21, 28);
						jLabel12.setHorizontalAlignment(SwingConstants.RIGHT);
					}
					{
						jLabel15 = new JLabel();
						jPanelExtruders.add(jLabel15);
						jLabel15.setText("Beta");
						jLabel15.setBounds(154, 7, 42, 21);
					}
					{
						jLabel13 = new JLabel();
						jPanelExtruders.add(jLabel13);
						jLabel13.setText("Address");
						jLabel13.setBounds(77, 7, 63, 21);
					}
					{
						extruderBeta1 = new JTextField();
						jPanelExtruders.add(extruderBeta1);
						extruderBeta1.setBounds(140, 28, 63, 28);
					}
					{
						extruderAddress1 = new JTextField();
						jPanelExtruders.add(extruderAddress1);
						extruderAddress1.setBounds(70, 28, 63, 28);
					}
					{
						extruderRz1 = new JTextField();
						jPanelExtruders.add(extruderRz1);
						extruderRz1.setBounds(210, 28, 63, 28);
					}
					{
						jLabel19 = new JLabel();
						jPanelExtruders.add(jLabel19);
						jLabel19.setText("Extrusion size");
						jLabel19.setBounds(14, 105, 105, 28);
					}
					{
						jLabel24 = new JLabel();
						jPanelExtruders.add(jLabel24);
						jLabel24.setText("t0");
						jLabel24.setBounds(357, 7, 35, 21);
					}
					{
						extruderMaxSpeed1 = new JTextField();
						jPanelExtruders.add(extruderMaxSpeed1);
						extruderMaxSpeed1.setBounds(280, 28, 63, 28);
					}
					{
						jLabel31 = new JLabel();
						jPanelExtruders.add(jLabel31);
						jLabel31.setText("Max Speed");
						jLabel31.setBounds(280, 7, 77, 21);
					}
					{
						extrudert01 = new JTextField();
						jPanelExtruders.add(extrudert01);
						extrudert01.setBounds(350, 28, 63, 28);
					}
					{
						extrusionSize = new JTextField();
						jPanelExtruders.add(extrusionSize);
						extrusionSize.setBounds(119, 105, 70, 21);
					}
					{
						jLabel18 = new JLabel();
						jPanelExtruders.add(jLabel18);
						jLabel18.setText("Temperature");
						jLabel18.setBounds(14, 130, 84, 28);
					}
					{
						extrusionHeight = new JTextField();
						jPanelExtruders.add(extrusionHeight);
						extrusionHeight.setBounds(266, 105, 70, 21);
					}
					{
						extrusionTemp = new JTextField();
						jPanelExtruders.add(extrusionTemp);
						extrusionTemp.setBounds(119, 133, 70, 21);
					}
					{
						jLabel17 = new JLabel();
						jPanelExtruders.add(jLabel17);
						jLabel17.setText("Speed");
						jLabel17.setBounds(14, 158, 84, 28);
					}
					{
						extrusionSpeed = new JTextField();
						jPanelExtruders.add(extrusionSpeed);
						extrusionSpeed.setBounds(119, 161, 70, 21);
					}
					{
						jLabel20 = new JLabel();
						jPanelExtruders.add(jLabel20);
						jLabel20.setText("mm high");
						jLabel20.setBounds(343, 105, 63, 21);
					}
					{
						jLabel21 = new JLabel();
						jPanelExtruders.add(jLabel21);
						jLabel21.setText("C");
						jLabel21.setBounds(196, 133, 28, 21);
					}
					{
						jLabel23 = new JLabel();
						jPanelExtruders.add(jLabel23);
						jLabel23.setText("mm  by");
						jLabel23.setBounds(196, 105, 49, 21);
					}
					{
						hm = new JTextField();
						jPanelExtruders.add(hm);
						hm.setBounds(266, 133, 70, 21);
					}
					{
						jLabel26 = new JLabel();
						jPanelExtruders.add(jLabel26);
						jLabel26.setText("hb");
						jLabel26.setHorizontalAlignment(SwingConstants.RIGHT);
						jLabel26.setBounds(217, 161, 42, 21);
					}
					{
						hb = new JTextField();
						jPanelExtruders.add(hb);
						hb.setBounds(266, 161, 70, 21);
					}
					{
						jLabel25 = new JLabel();
						jPanelExtruders.add(jLabel25);
						jLabel25.setText("hm");
						jLabel25.setBounds(217, 133, 42, 21);
						jLabel25.setHorizontalAlignment(SwingConstants.RIGHT);
					}
					{
						jLabel27 = new JLabel();
						jPanelExtruders.add(jLabel27);
						jLabel27.setText("Offsets:   X");
						jLabel27.setBounds(7, 70, 98, 21);
						jLabel27.setHorizontalAlignment(SwingConstants.RIGHT);
					}
					{
						extruderOffsetX1 = new JTextField();
						jPanelExtruders.add(extruderOffsetX1);
						extruderOffsetX1.setBounds(119, 70, 49, 21);
					}
					{
						extruderOffsetZ1 = new JTextField();
						jPanelExtruders.add(extruderOffsetZ1);
						extruderOffsetZ1.setBounds(273, 70, 49, 21);
					}
					{
						jLabel28 = new JLabel();
						jPanelExtruders.add(jLabel28);
						jLabel28.setText("Z");
						jLabel28.setBounds(245, 70, 21, 21);
						jLabel28.setHorizontalAlignment(SwingConstants.RIGHT);
					}
					{
						extruderOffsetY1 = new JTextField();
						jPanelExtruders.add(extruderOffsetY1);
						extruderOffsetY1.setBounds(196, 70, 49, 21);
					}
					{
						jLabel29 = new JLabel();
						jPanelExtruders.add(jLabel29);
						jLabel29.setText("Y");
						jLabel29.setHorizontalAlignment(SwingConstants.RIGHT);
						jLabel29.setBounds(168, 70, 21, 21);
					}
					{
						jLabel30 = new JLabel();
						jPanelExtruders.add(jLabel30);
						jLabel30.setText("mm");
						jLabel30.setBounds(329, 70, 35, 21);
					}
				}
				{
					jPanelProduction = new JPanel();
					jTabbedPane1.addTab("Production", null, jPanelProduction, null);
					jPanelProduction.setLayout(null);
					{
						jLabel5 = new JLabel();
						jPanelProduction.add(jLabel5);
						jLabel5.setText("Cooling period");
						jLabel5.setBounds(14, 35, 98, 28);
						jLabel5.setHorizontalAlignment(SwingConstants.RIGHT);
					}
					{
						coolingPeriod = new JTextField();
						jPanelProduction.add(coolingPeriod);
						coolingPeriod.setBounds(126, 35, 63, 28);
					}
					{
						idleZMotor = new JCheckBox();
						jPanelProduction.add(idleZMotor);
						idleZMotor.setText("Idle Z motor between layers");
						idleZMotor.setBounds(35, 77, 203, 28);
					}
				}
			}
			{
				getContentPane().setLayout(null);
				this.setTitle("RepRap Preferences");
			}
			this.setSize(449, 338);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void jButtonOKMouseClicked(MouseEvent evt) {
		// Update all preferences
		savePreferences();
		dispose();
	}
	
	private void jButtonCancelMouseClicked(MouseEvent evt) {
		// Close without saving
		dispose();
	}
	
}
