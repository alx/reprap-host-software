package org.reprap.gui.extrudertest;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Properties;

import javax.swing.JCheckBox;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.reprap.comms.snap.SNAPAddress;
import org.reprap.comms.snap.SNAPCommunicator;
import org.reprap.devices.GenericExtruder;

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
public class Main extends javax.swing.JDialog {
	private JLabel jLabel1;
	private JLabel jLabel2;
	private JLabel jLabel4;
	private JCheckBox heaterActive;
	private JTextField currentTemperature;
	private JLabel jLabel3;
	private JTextField desiredTemperature;

	private SNAPCommunicator communicator;
	private GenericExtruder extruder;
	
	private final int localNodeNumber = 0;
	private final int baudRate = 19200;
	
	private Thread pollThread;
	private boolean pollThreadExiting = false;

	/**
	* Auto-generated main method to display this JDialog
	*/
	public static void main(String[] args) throws Exception {
		JFrame frame = new JFrame();
		Main inst = new Main(frame);
		inst.setVisible(true);
	}
	
	public Main(JFrame frame) throws Exception {
		super(frame);

		Properties props = new Properties();
		URL url = ClassLoader.getSystemResource("reprap.properties");
		props.load(url.openStream());
		String commPortName = props.getProperty("Port");
		
		SNAPAddress myAddress = new SNAPAddress(localNodeNumber); 
		this.setResizable(false);
		communicator = new SNAPCommunicator(commPortName, baudRate, myAddress);

		extruder = new GenericExtruder(communicator,
				new SNAPAddress(props.getProperty("Extruder1Address")),
				Integer.parseInt(props.getProperty("Extruder1Beta")),
				Integer.parseInt(props.getProperty("Extruder1Rz"))
		);

		initGUI();

		pollThread = new Thread() {
			public void run() {
				while(!pollThreadExiting) {
					try {
						RefreshTemperature();
						Thread.sleep(500);
					}
					catch (InterruptedException ex) {
						// This is normal when shutting down, so ignore
					}
				}
			}
		};
		pollThread.start();

	}
	
	protected void RefreshTemperature() {
		int temperature = (int)Math.round(extruder.getTemperature());
		currentTemperature.setText(Integer.toString(temperature));
	}

	public void dispose() {
		pollThreadExiting = true;
		pollThread.interrupt();
		super.dispose();
		extruder.dispose();
		communicator.dispose();
	}
	
	private void initGUI() {
		try {
			{
				jLabel3 = new JLabel();
				getContentPane().add(jLabel3);
				jLabel3.setText("Desired temperature");
				jLabel3.setHorizontalAlignment(SwingConstants.RIGHT);
				jLabel3.setBounds(35, 98, 140, 28);
			}
			{
				jLabel1 = new JLabel();
				getContentPane().add(jLabel1);
				jLabel1.setText("Current temperature");
				jLabel1.setBounds(35, 63, 140, 28);
				jLabel1.setHorizontalAlignment(SwingConstants.RIGHT);
			}
			{
				desiredTemperature = new JTextField();
				getContentPane().add(desiredTemperature);
				desiredTemperature.setText("0");
				desiredTemperature.setBounds(182, 98, 63, 28);
				desiredTemperature.setHorizontalAlignment(SwingConstants.RIGHT);
				desiredTemperature.setEnabled(false);
				desiredTemperature.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						desiredTemperatureActionPerformed(evt);
					}
				});
			}
			{
				jLabel2 = new JLabel();
				getContentPane().add(jLabel2);
				jLabel2.setText("deg C");
				jLabel2.setBounds(252, 98, 63, 28);
			}
			{
				currentTemperature = new JTextField();
				getContentPane().add(currentTemperature);
				currentTemperature.setHorizontalAlignment(SwingConstants.RIGHT);
				currentTemperature.setText("0");
				currentTemperature.setEditable(false);
				currentTemperature.setBounds(182, 63, 63, 28);
			}
			{
				jLabel4 = new JLabel();
				getContentPane().add(jLabel4);
				jLabel4.setText("deg C");
				jLabel4.setBounds(252, 63, 63, 28);
			}
			{
				heaterActive = new JCheckBox();
				getContentPane().add(heaterActive);
				heaterActive.setText("Heater Active");
				heaterActive.setBounds(182, 21, 126, 28);
				heaterActive.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						heaterActiveActionPerformed(evt);
					}
				});
			}
			{
				getContentPane().setLayout(null);
				this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				this.setTitle("Extruder Exerciser");
			}
			setSize(400, 300);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void desiredTemperatureActionPerformed(ActionEvent evt) {
		setTemperature();
	}
	
	private void heaterActiveActionPerformed(ActionEvent evt) {
		desiredTemperature.setEnabled(heaterActive.isSelected());
		setTemperature();
	}

	private void setTemperature() {
		if (heaterActive.isSelected())
			extruder.setTemperature(Integer.parseInt(desiredTemperature.getText()));
		else
			extruder.setTemperature(0);
	}
	
}
