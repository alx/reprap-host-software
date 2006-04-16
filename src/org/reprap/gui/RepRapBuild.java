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
 
This program loads STL files of objects, orients them, and builds them
in the RepRap machine.
 
It is based on one of the open-source examples in Daniel Selman's excellent
Java3D book, and his notice is immediately below.
 
First version 2 April 2006
This version: 16 April 2006
 
 */



/**********************************************************
 * VrmlPickingTest.java Copyright (C) 2001 	Daniel Selman
 *
 * First distributed with the book "Java 3D Programming"
 * by Daniel Selman and published by Manning Publications.
 * http://manning.com/selman
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, version 2.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * The license can be found on the WWW at:
 * http://www.fsf.org/copyleft/gpl.html
 *
 * Or by writing to:
 * Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Authors can be contacted at:
 * Daniel Selman: daniel@selman.org
 *
 * If you make changes you think others would like, please
 * contact one of the authors or someone at the
 * www.j3d.org web site.
 **************************************************************/

package org.reprap.gui;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.*;
import javax.media.j3d.*;
import javax.swing.JPanel;
import javax.vecmath.*;

import com.sun.j3d.utils.picking.*;
import com.sun.j3d.audioengines.javasound.JavaSoundMixer;

//************************************************************************

// This is the main public class that creates a virtual world of the RepRap
// working volume, allows you to put STL-file objects in it, move them about
// to arrange them, (and builds them in the machine - one day soon).

public class RepRapBuild extends JPanel implements MouseListener 
{
    // ------------------------
    // Most of the stuff that follows will be read from
    // a configuration file ultimately.
    
    private static final double xwv = 300;  // The RepRap machine...
    private static final double ywv = 300;  // ...working volume in mm.
    private static final double zwv = 300;
    
    // The relative location of the STL model of the working volume
    // And the offset of the origin in it.
    
    private static final String wv_location = "RepRap-data/RepRap-wv.stl";
    private static final String worldName = "RepRap World";
    private static final Vector3d wv_offset = new Vector3d(-17.3, -24.85, -2);
    
    // Translate and zoom scaling factors
    
    private static final double mouse_tf = 10;
    private static final double mouse_zf = 10;
    
    // Factors for front and back clipping planes and so on
    
    private static final double RADFAC = 0.7;
    private static final double BACKFAC = 2.0;
    private static final double FRONTFAC = 0.025;
    private static final double BOUNDFAC = 3.0;
    
    // Black, the background, and other colours
    
    private static final Color3f black = new Color3f(0, 0, 0);
    private static final Color3f bgColour = new Color3f( 0.9f, 0.9f, 0.9f );
    private static final Color3f rrRed = new Color3f( 0.6f, 0.2f, 0.2f );
    private static final Color3f rrGreen = new Color3f( 0.3f, 0.4f, 0.3f );
    private static final Color3f rrGrey = new Color3f( 0.3f, 0.3f, 0.3f );
          
    private static int m_kWidth = 600;                    // Window size
    private static int m_kHeight = 400;
    
    //---- End of stuff to be loaded from config file
    
    private PickCanvas pickCanvas = null;   // The thing picked by a mouse click
    
    private Appearance default_app = null;  // Colour for unselected parts
    private Appearance picked_app = null;   // Colour for the selected part
    private Appearance wv_app = null;       // Colour for the working volume
    
    private MouseObject mouse = null;
    private BranchGroup wv_and_stls = new BranchGroup();  // Where in the scene the 
                                                          // working volume and STLs
                                                          // are joined on.
                                                          
    private STLObject world = null;                      // Everything  
    private STLObject workingVolume = null;              // The RepRap machine itself.
    private STLObject lastPicked = null;                 // The last thing picked
    private java.util.List stls = new ArrayList();       // All the STLs to be built
    private int objectIndex = 0;                         // Counter for STLs as they
                                                         // are loaded

    // The world in the Applet
    
    protected VirtualUniverse m_Universe = null;
    protected BranchGroup m_SceneBranchGroup = null;
    protected Bounds m_ApplicationBounds = null;

    
    // Constructors
    
    public RepRapBuild( ) 
    {
    	    initialise();
    	
    }
    
    // (About) how big is the world?
    
    protected float getViewPlatformActivationRadius( ) 
    {
        return (float)(RADFAC*Math.sqrt(xwv*xwv + ywv*ywv + zwv*zwv));
    }
    
    // How far away is the back?
    
    protected double getBackClipDistance( ) 
    {
        return BACKFAC*getViewPlatformActivationRadius( );
    }
    
    // How close is the front?
    
    protected double getFrontClipDistance( ) 
    {
        return FRONTFAC*getViewPlatformActivationRadius( );
    }
    
    // Set up the size of the world
    
    protected Bounds createApplicationBounds( ) 
    {
        m_ApplicationBounds = new BoundingSphere(new
                Point3d(xwv*0.5, ywv*0.5, zwv*0.5),
                BOUNDFAC*getViewPlatformActivationRadius( )
                );
        return m_ApplicationBounds;
    }
    
    // Running as an applet?
    
    public boolean isApplet( ) 
    {
        try 
        {
            System.getProperty( "user.dir" );
            return false;
        } catch( Exception e ) 
        {
        }
        
        return true;
    }
    
    
    // Where are we in the file system?
    
    public static URL getWorkingDirectory( ) throws java.net.MalformedURLException 
    {
        URL url = null;
        
        try 
        {
            File file = new File( System.getProperty("user.dir") );
            return file.toURL( );
        } catch( Exception e ) 
        {
            System.err.println("getWorkingDirectory( ): can't get user dir.");
        }
                
        //return getCodeBase( );
        return null;
    }
    
    // Return handles on big things above where we are interested
    
    public VirtualUniverse getVirtualUniverse( ) 
    {
        return m_Universe;
    }
    
    
    public javax.media.j3d.Locale getFirstLocale( ) 
    {
        java.util.Enumeration en = m_Universe.getAllLocales( );
        
        if( en.hasMoreElements( ) != false )
            return ( javax.media.j3d.Locale ) en.nextElement( );
        
        return null;
    }
    
    // The size of the world
    
    protected Bounds getApplicationBounds( ) 
    {
        if( m_ApplicationBounds == null )
            m_ApplicationBounds = createApplicationBounds( );
        
        return m_ApplicationBounds;
    }
    
    // Set bg light grey
    
    protected Background createBackground( ) 
    {
        Background back = new Background( bgColour );
        back.setApplicationBounds( createApplicationBounds( ) );
        return back;
    }
    
    // Fire up Java3D
    
    public void initJava3d( ) 
    {
        m_Universe = createVirtualUniverse( );
        
        javax.media.j3d.Locale locale = createLocale( m_Universe );
        
        BranchGroup sceneBranchGroup = createSceneBranchGroup( );
        
        ViewPlatform vp = createViewPlatform( );
        BranchGroup viewBranchGroup = createViewBranchGroup( getViewTransformGroupArray( ), vp );
        
        createView( vp );
        
        Background background = createBackground( );
        
        if( background != null )
            sceneBranchGroup.addChild( background );
        
        locale.addBranchGraph( sceneBranchGroup );
        addViewBranchGroup( locale, viewBranchGroup );
        
        //onDoneInit( );
    }
    
//    protected void onDoneInit( ) 
//    {
//    }
    
    protected double getScale( ) 
    {
        return 1.0;
    }
    
    
    protected void addViewBranchGroup( javax.media.j3d.Locale locale, BranchGroup bg ) 
    {
        locale.addBranchGraph( bg );
    }
    
    protected javax.media.j3d.Locale createLocale( VirtualUniverse u ) 
    {
        return new javax.media.j3d.Locale( u );
    }

    
    protected View createView( ViewPlatform vp ) 
    {
        View view = new View( );
        
        PhysicalBody pb = createPhysicalBody( );
        PhysicalEnvironment pe = createPhysicalEnvironment( );
        
        AudioDevice audioDevice = createAudioDevice( pe );
        
        if( audioDevice != null ) 
        {
            pe.setAudioDevice( audioDevice );
            audioDevice.initialize( );
        }
        
        view.setPhysicalEnvironment( pe );
        view.setPhysicalBody( pb );
        
        if( vp != null )
            view.attachViewPlatform( vp );
        
        view.setBackClipDistance( getBackClipDistance( ) );
        view.setFrontClipDistance( getFrontClipDistance( ) );
        
        Canvas3D c3d = createCanvas3D( );
        view.addCanvas3D( c3d );
        addCanvas3D( c3d );
        
        return view;
    }
    
    protected PhysicalBody createPhysicalBody( ) 
    {
        return new PhysicalBody( );
    }
    
    protected AudioDevice createAudioDevice( PhysicalEnvironment pe ) 
    {
        JavaSoundMixer javaSoundMixer = new JavaSoundMixer( pe );
        
        if (javaSoundMixer == null)
            System.out.println( "create of audiodevice failed" );
        
        return javaSoundMixer;
    }
    
    protected PhysicalEnvironment createPhysicalEnvironment( ) 
    {
        return new PhysicalEnvironment( );
    }
    
    
    protected ViewPlatform createViewPlatform( ) 
    {
        ViewPlatform vp = new ViewPlatform( );
        vp.setViewAttachPolicy( View.RELATIVE_TO_FIELD_OF_VIEW );
        vp.setActivationRadius( getViewPlatformActivationRadius( ) );
        
        return vp;
    }
    
    protected Canvas3D createCanvas3D( ) 
    {
        GraphicsConfigTemplate3D gc3D = new GraphicsConfigTemplate3D( );
        gc3D.setSceneAntialiasing( GraphicsConfigTemplate.PREFERRED );
        GraphicsDevice gd[] = GraphicsEnvironment.getLocalGraphicsEnvironment( ).getScreenDevices( );
        
        Canvas3D c3d = new Canvas3D( gd[0].getBestConfiguration( gc3D ) );
        c3d.setSize( getCanvas3dWidth( c3d ), getCanvas3dHeight( c3d ) );
        
        return c3d;
    }
    
    // These two are probably wrong.
    
    protected int getCanvas3dWidth( Canvas3D c3d ) 
    {
        return m_kWidth;
    }
    
    protected int getCanvas3dHeight( Canvas3D c3d ) 
    {
        return m_kHeight;
    }
    
        
    protected BranchGroup createViewBranchGroup( TransformGroup[] tgArray, 
            ViewPlatform vp ) 
    {
        BranchGroup vpBranchGroup = new BranchGroup( );
        
        if( tgArray != null && tgArray.length > 0 ) 
        {
            Group parentGroup = vpBranchGroup;
            TransformGroup curTg = null;
            
            for( int n = 0; n < tgArray.length; n++ ) 
            {
                curTg = tgArray[n];
                parentGroup.addChild( curTg );
                parentGroup = curTg;
            }
            
            tgArray[tgArray.length-1].addChild( vp );
        } else
            vpBranchGroup.addChild( vp );
        
        return vpBranchGroup;
    }
    
    
    protected VirtualUniverse createVirtualUniverse( ) 
    {
        return new VirtualUniverse( );
    }
        
    // Set stuff up for the constructors - called by all of them that actually
    // do anything.
    
    private void initialise() 
    {
        default_app = new Appearance();
        default_app.setMaterial(new Material(rrGrey, black, rrGrey, black, 0f));
        
        picked_app = new Appearance();
        picked_app.setMaterial(new Material(rrRed, black, rrRed, black, 0f));
        
        wv_app = new Appearance();
        wv_app.setMaterial(new Material(rrGreen, black, rrGreen, black, 0f));
                
        initJava3d( );
        
    }
    
    // Are we live?  If not, vivfy.
    
    public void start( ) 
    {
        if( pickCanvas == null )
            initialise( );
    }
    
    protected void addCanvas3D( Canvas3D c3d ) 
    {
        setLayout( new BorderLayout( ) );
        add( c3d, BorderLayout.CENTER );
        doLayout( );
        
        if ( m_SceneBranchGroup != null ) {
            c3d.addMouseListener( this );
            
            pickCanvas = new PickCanvas( c3d, m_SceneBranchGroup );
            pickCanvas.setMode( PickTool.GEOMETRY_INTERSECT_INFO );
            pickCanvas.setTolerance( 4.0f );
        }
        
        c3d.setCursor( new Cursor( Cursor.DEFAULT_CURSOR ) );
    }
    
    public TransformGroup[] getViewTransformGroupArray( ) 
    {
        TransformGroup[] tgArray = new TransformGroup[1];
        tgArray[0] = new TransformGroup( );
        
        Transform3D viewTrans = new Transform3D( );
        Transform3D eyeTrans = new Transform3D( );
        
        BoundingSphere sceneBounds = (BoundingSphere) m_SceneBranchGroup.getBounds( );
        
        // point the view at the center of the object
        
        Point3d center = new Point3d( );
        sceneBounds.getCenter( center);
        double radius = sceneBounds.getRadius( );
        Vector3d temp = new Vector3d( center );
        viewTrans.set( temp );
        
        // pull the eye back far enough to see the whole object
        
        double eyeDist = radius / Math.tan( Math.toRadians( 40 ) / 2.0 );
        temp.x = 0.0;
        temp.y = 0.0;
        temp.z = eyeDist;
        eyeTrans.set( temp );
        viewTrans.mul( eyeTrans );
        
        // set the view transform
        
        tgArray[0].setTransform( viewTrans );
        
        return tgArray;
    }
    
    // Set up the RepRap working volume
    
    protected BranchGroup createSceneBranchGroup( ) 
    {       
        m_SceneBranchGroup = new BranchGroup( );
        
        BranchGroup objRoot = m_SceneBranchGroup;
        
        Bounds lightBounds = getApplicationBounds( );
        
        AmbientLight ambLight = new AmbientLight( true, new Color3f( 1.0f, 1.0f, 1.0f ) );
        ambLight.setInfluencingBounds( lightBounds );
        objRoot.addChild( ambLight );
        
        DirectionalLight headLight = new DirectionalLight( );
        headLight.setInfluencingBounds( lightBounds );
        objRoot.addChild( headLight );
        
        mouse = new MouseObject(getApplicationBounds( ), mouse_tf, mouse_zf);
        
        wv_and_stls.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        
        // Load the STL file for the working volume
        
        world = new STLObject(wv_and_stls, worldName);
        
        URL codebase = null;
        
        String stlFile = null;
        
        try 
        {
            codebase = RepRapBuild.getWorkingDirectory();
            stlFile = codebase.toExternalForm( ) + wv_location;
        } catch( Exception e ) 
        {
            System.err.println( "createSceneBranchGroup(): Exception finding working directory: " +
                    codebase.toExternalForm( ));
            e.printStackTrace();
        }
        
        workingVolume = new STLObject( stlFile, wv_offset, objectIndex, wv_app );
        wv_and_stls.addChild(workingVolume.top);
        
        // Set the mouse to move everything
        
        mouse.move(world, false);
        objRoot.addChild(world.top);
        
        return objRoot;
    }
    

    
    // Find the stl object in the scene with the given name
    
    private STLObject findSTL(String name)
    {
        STLObject stl;
        for (int i = 0; i < stls.size(); i++)
        {
            stl = (STLObject)stls.get(i);
            if(stl.name == name)
                return stl;
        }
        return null;
    }
    
    // Action on mouse click
    
    public void mouseClicked( MouseEvent e ) 
    {        
        pickCanvas.setShapeLocation( e );
        
        PickResult pickResult = pickCanvas.pickClosest( );
        boolean valid_object = false;
        STLObject picked = null;
        
        if( pickResult != null ) // Got anything?
        {    
            Node actualNode = pickResult.getObject( );
            String name = (String)actualNode.getUserData( );
            if( name != null ) // Really got something?
            {
                if( name != workingVolume.name ) // STL object picked?
                {
                    picked = findSTL(name);
                    if(picked != null)
                    {
                        picked.setAppearance(picked_app); // Highlight it
                        if(lastPicked != null)
                            lastPicked.setAppearance(default_app); // lowlight the last one
                        mouse.move(picked, true);  // Set the mouse to move it
                        lastPicked = picked;  // Remember it
                    }
                } else
                {   // Picked the working volume - deselect all and...
                    if(lastPicked != null)
                            lastPicked.setAppearance(default_app);
                    mouse.move(world, false); // ...switch the mouse to moving the world
                    lastPicked = null;
                }
            }
        }
    }
    
    public void mouseEntered( MouseEvent e ) 
    {
    }
    
    public void mouseExited( MouseEvent e ) 
    {
    }
    
    public void mousePressed( MouseEvent e ) 
    {
    }
    
    public void mouseReleased( MouseEvent e ) 
    {
    }
    
    // Callback for when the user selects an STL file to load
    
    public void anotherSTLFile(String s) 
    {
        if (s == null)
            return;
        objectIndex++;
        STLObject stl = new STLObject(s, null, objectIndex, default_app);
        if(stl != null) 
        {
            wv_and_stls.addChild( stl.top );
            stls.add(stl);
        }
    }
    
    // Callbacks for when the user rotates the selected object
    
    public void xRotate() 
    {
        if(lastPicked != null)
            lastPicked.xClick();
    }
    
    public void yRotate() 
    {
        if(lastPicked != null)
            lastPicked.yClick();
    }
    
    public void zRotate() 
    {    
        if(lastPicked != null)
            lastPicked.zClick();
    }
    
    // Callback to build the objects - work needed here...
    
    public void build()
    {
        System.out.println(
                "Now build the STL object(s); a bit more needed than this message...");
    }
    
}
