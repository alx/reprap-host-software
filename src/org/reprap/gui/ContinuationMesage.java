package org.reprap.gui;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JTextField;
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
public class ContinuationMesage extends javax.swing.JDialog {
	private static Point lastScreenPosition = null;
	
	private JTextField message;
	private JButton okButton;

	private JCheckBoxMenuItem layerPauseMenuCheckbox, segmentPauseMenuCheckbox;

	private JCheckBox segmentPauseCheckbox;
	private JCheckBox layerPauseCheckbox;

	public ContinuationMesage(JFrame frame, String message,
			JCheckBoxMenuItem segmentPause, JCheckBoxMenuItem layerPause) {
		super(frame);
		this.layerPauseMenuCheckbox = layerPause;
		this.segmentPauseMenuCheckbox = segmentPause;
		initGUI(message);
		
		if (lastScreenPosition != null)
			setLocation(lastScreenPosition);
		
		segmentPauseCheckbox.setSelected(segmentPauseMenuCheckbox.isSelected());
		layerPauseCheckbox.setSelected(layerPauseMenuCheckbox.isSelected());
		
	}
	
	private void initGUI(String messageContent) {
		try {
			{
				okButton = new JButton();
				getContentPane().add(okButton);
				okButton.setText("Continue...");
				okButton.setBounds(98, 91, 105, 28);
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						okButtonActionPerformed(evt);
					}
				});
			}
			{
				message = new JTextField();
				getContentPane().add(message);
				message.setBounds(0, 0, 308, 35);
				message.setOpaque(false);
				message.setEditable(false);
				message.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
				message.setHorizontalAlignment(SwingConstants.CENTER);
				message.setText(messageContent);
			}
			{
				segmentPauseCheckbox = new JCheckBox();
				getContentPane().add(segmentPauseCheckbox);
				segmentPauseCheckbox.setText("Pause before segment");
				segmentPauseCheckbox.setBounds(77, 35, 189, 28);
				segmentPauseCheckbox.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						segmentPauseCheckboxActionPerformed(evt);
					}
				});
			}
			{
				layerPauseCheckbox = new JCheckBox();
				getContentPane().add(layerPauseCheckbox);
				layerPauseCheckbox.setText("Pause before layer");
				layerPauseCheckbox.setBounds(77, 56, 189, 28);
				layerPauseCheckbox.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						layerPauseCheckboxActionPerformed(evt);
					}
				});
			}
			{
				getContentPane().setLayout(null);
				this.setModal(true);
				this.setTitle("Progress pause");
			}
			this.setSize(316, 158);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void okButtonActionPerformed(ActionEvent evt) {
		lastScreenPosition = getLocation();
		dispose();
	}
	
	private void segmentPauseCheckboxActionPerformed(ActionEvent evt) {
		segmentPauseMenuCheckbox.setSelected(segmentPauseCheckbox.isSelected());
	}
	
	private void layerPauseCheckboxActionPerformed(ActionEvent evt) {
		layerPauseMenuCheckbox.setSelected(layerPauseCheckbox.isSelected());
	}

}
