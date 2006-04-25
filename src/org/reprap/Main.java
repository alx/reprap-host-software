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

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;

import org.reprap.geometry.Producer;
import org.reprap.gui.Preferences;
import org.reprap.gui.PreviewPanel;
import org.reprap.gui.RepRapBuild;

public class Main {

    // Window to walk the file tree
    
    private JFileChooser chooser;
    private JFrame mainFrame;
    private RepRapBuild builder;
    private PreviewPanel preview;
    private JCheckBoxMenuItem viewBuilder;
    private JCheckBoxMenuItem viewPreview;
    private JCheckBoxMenuItem segmentPause;
    private JCheckBoxMenuItem layerPause;
    
    private JMenuItem cancelMenuItem;
    private JMenuItem produceProduce;
	
    private JSplitPane panel;
	
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

        // Required so menus float over Java3D
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        
        // Create menus
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

        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        menubar.add(viewMenu);

        JMenuItem viewToggle = new JMenuItem("Toggle view", KeyEvent.VK_V);
        viewToggle.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
        viewToggle.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				onViewToggle();
			}});
        viewMenu.add(viewToggle);
        
        viewBuilder = new JCheckBoxMenuItem("Builder");
        viewBuilder.setSelected(true);
        viewBuilder.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				onViewBuilder();
			}});
        viewMenu.add(viewBuilder);
        
        viewPreview = new JCheckBoxMenuItem("Preview");
        viewPreview.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				onViewPreview();
			}});
        viewMenu.add(viewPreview);

        
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

        JMenu produceMenu = new JMenu("Produce");
        produceMenu.setMnemonic(KeyEvent.VK_P);
        menubar.add(produceMenu);

        produceProduce = new JMenuItem("Produce...", KeyEvent.VK_P);
        produceProduce.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK));
        produceProduce.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				onProduce();
			}});
        produceMenu.add(produceProduce);

        cancelMenuItem = new JMenuItem("Cancel", KeyEvent.VK_P);
        cancelMenuItem.setEnabled(false);
        cancelMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				preview.setCancelled(true);
			}});
        produceMenu.add(cancelMenuItem);
        
        produceMenu.addSeparator();

        segmentPause = new JCheckBoxMenuItem("Pause before segment");
        produceMenu.add(segmentPause);

        layerPause = new JCheckBoxMenuItem("Pause before layer");
        produceMenu.add(layerPause);
                
        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setMnemonic(KeyEvent.VK_T);
        menubar.add(toolsMenu);
        
        JMenuItem toolsExerciser = new JMenuItem("Stepper exerciser", KeyEvent.VK_S);
        toolsExerciser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					org.reprap.gui.steppertest.Main.main(null);
				}
              	catch (Exception ex) {
             		JOptionPane.showMessageDialog(null, "Stepper exerciser exception: " + ex);
         			ex.printStackTrace();
             	}
			}});
        toolsMenu.add(toolsExerciser);

        JMenuItem toolsExtruderExerciser = new JMenuItem("Extruder exerciser", KeyEvent.VK_E);
        toolsExtruderExerciser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					org.reprap.gui.extrudertest.Main.main(null);
				}
              	catch (Exception ex) {
             		JOptionPane.showMessageDialog(null, "Extruder exerciser exception: " + ex);
         			ex.printStackTrace();
             	}
			}});
        toolsMenu.add(toolsExtruderExerciser);

        
        JMenu diagnosticsMenu = new JMenu("Diagnostics");
        toolsMenu.add(diagnosticsMenu);
        JMenuItem diagnosticsCommsTest = new JMenuItem("Basic comms test");
        diagnosticsMenu.add(diagnosticsCommsTest);

        // Create the main window area
        // This is a horizontal box layout that includes
        // both the builder and preview screens, one of
        // which may be invisible.

        Box builderFrame = new Box(BoxLayout.Y_AXIS);
        builderFrame.add(new JLabel("Builder"));
        builder = new RepRapBuild();
        builderFrame.setMinimumSize(new Dimension(0,0));
        builderFrame.add(builder);
        
        panel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
        panel.setPreferredSize(new Dimension(600, 400));
        panel.setMinimumSize(new Dimension(0, 0));
        panel.setResizeWeight(0.5);
        panel.setOneTouchExpandable(true);
        panel.setContinuousLayout(true);
        panel.setLeftComponent(builderFrame);
        panel.setRightComponent(createPreviewPanel());
        panel.setDividerLocation(panel.getPreferredSize().width);
        
        mainFrame.getContentPane().add(panel);
                
        mainFrame.setJMenuBar(menubar);
        
        mainFrame.pack();
        mainFrame.setVisible(true);
        
	}

	private Box createPreviewPanel() {
		/*JSplitPane splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitter.setMinimumSize(new Dimension(0, 0));
		splitter.setResizeWeight(1.0);
		splitter.setOneTouchExpandable(true);
		splitter.setContinuousLayout(true);
		splitter.setDividerLocation(1.0);*/
		
        Box pane = new Box(BoxLayout.Y_AXIS);
        pane.add(new JLabel("Progress Preview"));
		preview = new PreviewPanel();
		pane.setMinimumSize(new Dimension(0,0));
		pane.add(preview);
		
		/*JPanel controls = new JPanel(new FlowLayout());
		controls.setMinimumSize(new Dimension(0, 0));

		JCheckBox pauseSegment = new JCheckBox("Segment pause");
		pauseSegment.setMinimumSize(new Dimension(0, 0));
		controls.add(pauseSegment);

		JCheckBox pauseLayer = new JCheckBox("Layer pause");
		pauseLayer.setMinimumSize(new Dimension(0, 0));
		controls.add(pauseLayer);

		JButton continueButton = new JButton("Next");
		continueButton.setMinimumSize(new Dimension(0, 0));
		continueButton.setEnabled(false);
		controls.add(continueButton);

		pane.add(controls);*/
		
		//splitter.setTopComponent(pane);
		//splitter.setBottomComponent(controls);
		
		return pane;
	}
	
	private void onProduce() {
        cancelMenuItem.setEnabled(true);
        produceProduce.setEnabled(false);
		Thread t = new Thread() {
			public void run() {
				try {
					// TODO Some kind of progress indicator would be good
					
					if (!viewPreview.isSelected()) {
						viewPreview.setSelected(true);
						updateView();
					}

					preview.setSegmentPause(segmentPause);
					preview.setLayerPause(layerPause);
					
					Producer producer = new Producer(preview, builder);
					producer.produce();
					producer.dispose();
			        cancelMenuItem.setEnabled(false);
			        produceProduce.setEnabled(true);
					JOptionPane.showMessageDialog(mainFrame, "Production complete");
				}
				catch (Exception ex) {
					JOptionPane.showMessageDialog(mainFrame, "Production exception: " + ex);
					ex.printStackTrace();
				}
			}
		};
		t.start();
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

    private void onViewBuilder() {
    		if (!viewBuilder.isSelected() && !viewPreview.isSelected())
    			viewPreview.setSelected(true);
        	updateView();
    }

    private void onViewPreview() {
		if (!viewPreview.isSelected() && !viewBuilder.isSelected())
			viewBuilder.setSelected(true);
		updateView();
    }
    
    private void onViewToggle() {
    		if (viewBuilder.isSelected()) {
    			viewPreview.setSelected(true);
    			viewBuilder.setSelected(false);
    		} else {
    			viewPreview.setSelected(false);
    			viewBuilder.setSelected(true);
    		}
        	updateView();
    }
    
    private void updateView() {
    	    if (viewBuilder.isSelected() && viewPreview.isSelected())
    	    	  panel.setDividerLocation(0.5);
    	    else if (viewBuilder.isSelected())
  	    	  panel.setDividerLocation(1.0);
    	    else
    	    	  panel.setDividerLocation(0.0);
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
