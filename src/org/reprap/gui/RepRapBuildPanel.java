/*
 
RepRap
------
 
The Replicating Rapid Prototyper Project
 
 
Copyright (C) 2006
Adrian Bowyer & The University of Bath
 
http://reprap.org
 
Principal author:
 
Adrian Bowyer
Department of Mechanical Engineering
Faculty of Engineering and Design
University of Bath
Bath BA2 7AY
U.K.
 
e-mail: A.Bowyer@bath.ac.uk
 
RepRap is free; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
Licence as published by the Free Software Foundation; either
version 2 of the Licence, or (at your option) any later version.
 
RepRap is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public Licence for more details.
 
For this purpose the words "software" and "library" in the GNU Library
General Public Licence are taken to mean any and all computer programs
computer files data results documents and other copyright information
available from the RepRap project.
 
You should have received a copy of the GNU Library General Public
Licence along with RepRap; if not, write to the Free
Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA,
or see
 
http://www.gnu.org/
 
=====================================================================
 
This puts up a simple control panel of clickable buttons to allow
STL objects to be loaded, 90-degree-clicked about axes and (ultimately) 
built in the RepRap machine.
 
First version 16 April 2006
This version: 16 April 2006
 
 */

package org.reprap.gui;

import java.io.File;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

//****************************************************************************

// The actionPerformed function of this class gets called 
// when you click on a button

class SimpleListener implements ActionListener 
{
    private RepRapBuildPanel stlp;
    
    public SimpleListener(RepRapBuildPanel s) 
    {
        stlp = s;
    }
    
    public void actionPerformed(java.awt.event.ActionEvent e) 
    {
        String name = e.getActionCommand();
        stlp.clickAction(name);
    }
}


//*********************************************************************************

// Main class that puts up the button panel

public class RepRapBuildPanel 
{
    final static String FILE_SEP = "/";   // There's probably a system call for this...

    // Button messages - also used to identify the button that was pressed.
    
    final static String loadButtonString = "Load an STL file";
    final static String xrString = "Rotate about X";
    final static String yrString = "Rotate about Y";
    final static String zrString = "Rotate about Z";
    final static String buildString = "Build";
    
    // Window to walk the file tree
    
    private JFileChooser chooser;
    
    // The buttons and their panel
    
    private Button loadButton, xr, yr, zr, build;
    private JPanel mainPane;
    
    // The RepRap builder that we're attached to
    
    private RepRapBuild rrb;

    
    public RepRapBuildPanel(RepRapBuild r) 
    {
        // Record the RepRap builder that is using us
        
        rrb = r;
        
        // Set up the button-click listner
        
        SimpleListener simple = new SimpleListener(this);
        
        // File-chooser GUI
        
        chooser = new JFileChooser();
        
         // Do we want just to list .stl files, or all?
        
        // Note: source for ExampleFileFilter can be found in FileChooserDemo,
        // under the demo/jfc directory in the Java 2 SDK, Standard Edition.
        //ExampleFileFilter filter = new ExampleFileFilter();
        //filter.addExtension("stl");
        //filter.setDescription("STL files");
        //chooser.setFileFilter(filter);       
        
        // The buttons
        
        loadButton = new Button(loadButtonString);
        xr = new Button(xrString);
        yr = new Button(yrString);
        zr = new Button(zrString);
        build = new Button(buildString);
        
        // simple is the thing that listens for all the buttons
        
        loadButton.addActionListener(simple);
        xr.addActionListener(simple);
        yr.addActionListener(simple);
        zr.addActionListener(simple);
        build.addActionListener(simple);
        
        // Set up the main panel
        
        mainPane = new JPanel();
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));       
        mainPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        mainPane.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPane.setPreferredSize(new Dimension(150,150));
        
        mainPane.add(loadButton);
        mainPane.add(xr);
        mainPane.add(yr);
        mainPane.add(zr);
        mainPane.add(build);
        
        mainPane.add(Box.createGlue());
    }
    
    // Callback to decide which button was pressed and act on it
    
    void clickAction(String s) 
    {
        if(s == loadButtonString)
        {
            rrb.anotherSTLFile(loadButtonClicked());
        } else if (s == xrString)
        {
            rrb.xRotate();
        } else if (s == yrString)
        {
            rrb.yRotate();
        } else if (s == zrString)
        {
            rrb.zRotate();
        } else if (s == buildString)
        {
            rrb.build();
        }
    }
    
    // Function to put up the file-selector GUI and return the selected filename 
    
    String loadButtonClicked() 
    {
        String result = null;
        int returnVal = chooser.showOpenDialog(mainPane);
        if(returnVal == JFileChooser.APPROVE_OPTION) 
        {
            File f = chooser.getSelectedFile();
            result = "file:" + f.getAbsolutePath();
        }
        return result;
    }
    
    
    // Create the GUI and show it.  For thread safety,
    // this method should be invoked from the
    // event-dispatching thread.
    
    private static void createAndShowGUI(RepRapBuild r) 
    {
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);
        
        //Create and set up the window.
        JFrame frame = new JFrame("RepRap");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        //Create and set up the content pane.
        RepRapBuildPanel RepRapBuildPanel = new RepRapBuildPanel(r);
        RepRapBuildPanel.mainPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(RepRapBuildPanel.mainPane);
        
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
    
    public static void makePanel(final RepRapBuild r) 
    {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() 
        {
            public void run() 
            {
                createAndShowGUI(r);
            }
        }
        );
    }
    
}
