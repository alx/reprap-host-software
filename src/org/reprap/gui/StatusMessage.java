package org.reprap.gui;
import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

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
public class StatusMessage extends javax.swing.JDialog {
	private JTextPane message;

	/**
	* Auto-generated main method to display this JDialog
	*/
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		StatusMessage inst = new StatusMessage(frame);
		inst.setVisible(true);
	}
	
	public StatusMessage(JFrame frame) {
		super(frame);
		initGUI();
	}
	
	private void initGUI() {
		try {
			{
				message = new JTextPane();
				getContentPane().add(message, BorderLayout.CENTER);
				message.setEditable(false);
				message.setBackground(new java.awt.Color(225,225,225));
				message.setEnabled(false);
				SimpleAttributeSet set = new SimpleAttributeSet();
				StyledDocument doc = message.getStyledDocument();
				StyleConstants.setAlignment(set, StyleConstants.ALIGN_CENTER);
				message.setParagraphAttributes(set, true);
			}
			{
				this.setTitle("Progress");
			}
			this.setSize(287, 115);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setMessage(String text) {
		message.setText(text);
	}

}
