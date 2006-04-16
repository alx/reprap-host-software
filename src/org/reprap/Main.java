/*
 * Created on Mar 29, 2006
 *
 */
package org.reprap;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import org.reprap.gui.Preferences;
import org.reprap.gui.RepRapBuild;

public class Main {

    // Window to walk the file tree
    
    private JFileChooser chooser;
    private JFrame mainFrame;
    private RepRapBuild builder;
	
	public Main() {
        chooser = new JFileChooser();
        
         // Do we want just to list .stl files, or all?
        
        // Note: source for ExampleFileFilter can be found in FileChooserDemo,
        // under the demo/jfc directory in the Java 2 SDK, Standard Edition.
        //ExampleFileFilter filter = new ExampleFileFilter();
        //filter.addExtension("stl");
        //filter.setDescription("STL files");
        //chooser.setFileFilter(filter);       

	}

	private void createAndShowGUI() {
        JFrame.setDefaultLookAndFeelDecorated(false);
        mainFrame = new JFrame("RepRap");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        
        JMenuBar menubar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menubar.add(fileMenu);
        
        JMenuItem fileOpen = new JMenuItem("Open...", KeyEvent.VK_O);
        fileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        fileOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				onOpen();
			}});
        fileMenu.add(fileOpen);
        
        JMenuItem fileProduce = new JMenuItem("Produce...", KeyEvent.VK_P);
        fileProduce.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK));
        fileProduce.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				onProduce();
			}});
        fileMenu.add(fileProduce);

        fileMenu.addSeparator();

        JMenuItem filePrefs = new JMenuItem("Preferences...", KeyEvent.VK_R);
        filePrefs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Preferences prefs = new Preferences(mainFrame);
				prefs.show();
			}});
        fileMenu.add(filePrefs);

        
        fileMenu.addSeparator();

        JMenuItem fileExit = new JMenuItem("Exit", KeyEvent.VK_X);
        fileExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
        fileExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.exit(0);
			}});
        fileMenu.add(fileExit);

        JMenu manipMenu = new JMenu("Manipulate");
        manipMenu.setMnemonic(KeyEvent.VK_M);
        menubar.add(manipMenu);

        JMenuItem manipX = new JMenuItem("Rotate X", KeyEvent.VK_X);
        manipX.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        manipX.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				onRotateX();
			}});
        manipMenu.add(manipX);

        JMenuItem manipY = new JMenuItem("Rotate Y", KeyEvent.VK_Y);
        manipY.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
        manipY.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				onRotateY();
			}});
        manipMenu.add(manipY);

        JMenuItem manipZ = new JMenuItem("Rotate Z", KeyEvent.VK_Z);
        manipZ.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
        manipZ.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				onRotateZ();
			}});
        manipMenu.add(manipZ);
        
        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setMnemonic(KeyEvent.VK_T);
        menubar.add(toolsMenu);
        
        JMenuItem toolsExerciser = new JMenuItem("Stepper exerciser", KeyEvent.VK_S);
        toolsExerciser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
          		  org.reprap.steppertestgui.Main gui = new org.reprap.steppertestgui.Main();
                  gui.createAndShowGUI(false);
				}
              	catch (Exception ex) {
             		JOptionPane.showMessageDialog(null, "General exception: " + ex);
         			ex.printStackTrace();
             	}
			}});
        toolsMenu.add(toolsExerciser);
        
        JMenu diagnosticsMenu = new JMenu("Diagnostics");
        toolsMenu.add(diagnosticsMenu);
        JMenuItem diagnosticsCommsTest = new JMenuItem("Basic comms test");
        diagnosticsMenu.add(diagnosticsCommsTest);
        
        builder = new RepRapBuild();
        builder.setPreferredSize(new Dimension(600, 400));
        
        mainFrame.getContentPane().add(builder);
        
        mainFrame.setJMenuBar(menubar);
        
        mainFrame.pack();
        mainFrame.setVisible(true);
        
	}
	
	private void onProduce() {
		JOptionPane.showMessageDialog(null, "Produce not implemented yet");
	}
	
    private void onOpen() 
    {
        String result = null;
        int returnVal = chooser.showOpenDialog(mainFrame);
        if(returnVal == JFileChooser.APPROVE_OPTION) 
        {
            File f = chooser.getSelectedFile();
            result = "file:" + f.getAbsolutePath();
        }
        
        builder.anotherSTLFile(result);
    }
    
    private void onRotateX() {
    	  builder.xRotate();
    }

    private void onRotateY() {
  	  builder.yRotate();
    }

    private void onRotateZ() {
  	  builder.zRotate();
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
	        			ex.printStackTrace();
	            	}
            }
        });

	}

}
