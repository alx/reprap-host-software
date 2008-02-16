package org.reprap.gui;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * Modified extensively by Adrian - 5-2-2008
 * @author ensab
 *
 */
public class StatusMessage extends javax.swing.JDialog {
	private boolean buttonHit = false;
	private JButton button;
	private JTextPane message;
	private String buttonString = "Cancel";

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
				getContentPane().add(message);
				message.setBounds(0, 0, 280, 77);
				message.setEditable(false);
				message.setBackground(Color.white);
				message.setForeground(Color.black);
				message.setEnabled(false);
				SimpleAttributeSet set = new SimpleAttributeSet();
				StyleConstants.setAlignment(set, StyleConstants.ALIGN_CENTER);
				message.setParagraphAttributes(set, true);			}
			{
				button = new JButton();
				getContentPane().add(button);
				button.setText(buttonString);
				button.setBounds(105, 84, 91, 28);
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						buttonActionPerformed(evt);
					}
				});
			}
			
			{
					getContentPane().setLayout(null);
			}
			{
				this.setTitle("Progress");
			}
			this.setSize(288, 137);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setMessage(String text) {
		message.setText(text);
	}
	
	public void setButton(String text) {
		buttonString = text;
		button.setText(buttonString);
	}

	public boolean isCancelled() {
		return buttonHit;
	}

	public void setCancelled(boolean b) {
		buttonHit = b;
	}
	
	private void buttonActionPerformed(ActionEvent evt) {
		buttonHit = true;
		setVisible(false);
	}

}
