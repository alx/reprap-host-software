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
 
Wrapper class for STL objects that allows them easily to be moved about
by the mouse.  The STL object itself is a Shape3D loaded by the STL loader.

First version 14 April 2006
This version: 14 April 2006
 
 */

package org.reprap.gui;

import java.util.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.picking.*;
import com.sun.j3d.loaders.Scene;
import org.j3d.renderer.java3d.loaders.STLLoader;
import org.reprap.Attributes;
import org.reprap.Preferences;
import org.reprap.devices.NullExtruder;

/**
 * Class for holding a group (maybe just 1) of 3D objects for RepRap to make.
 * They can be moved around on the build platform en mass, but not moved
 * relative to each other, so they can represent an assembly made from several
 * different materials.
 * 
 * @author adrian
 * 
 */

public class STLObject
{
    private MouseObject mouse = null;  // The mouse, if it is controlling us
    public BranchGroup top = null;     // The thing that links us to the world
    public BranchGroup handle = null;  // Internal handle for the mouse to grab
    public TransformGroup trans = null;// Static transform for when the mouse is away
    public BranchGroup stl = null;     // The actual STL geometry
    public Vector3d size = null;       // X, Y and Z extent
    private BoundingBox bbox = null;   // Temporary storage for the bounding box while loading
    

    public STLObject()
    {
    	stl = new BranchGroup(); 
        
        // No mouse yet
        
        mouse = null;
        
        // Set up our bit of the scene graph
        
        top = new BranchGroup();
        handle = new BranchGroup();
        trans = new TransformGroup();
        
        top.setCapability(BranchGroup.ALLOW_DETACH);
        top.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        top.setCapability(Group.ALLOW_CHILDREN_WRITE);
        top.setCapability(Group.ALLOW_CHILDREN_READ);
        top.setCapability(Node.ALLOW_AUTO_COMPUTE_BOUNDS_READ);
        top.setCapability(Node.ALLOW_BOUNDS_READ);
        
        handle.setCapability(BranchGroup.ALLOW_DETACH);
        handle.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        handle.setCapability(Group.ALLOW_CHILDREN_WRITE);
        handle.setCapability(Group.ALLOW_CHILDREN_READ);
        
        trans.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        trans.setCapability(Group.ALLOW_CHILDREN_WRITE);
        trans.setCapability(Group.ALLOW_CHILDREN_READ);
        trans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        trans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        
        trans.addChild(stl);
        handle.addChild(trans);
        top.addChild(handle);
        
        Attributes nullAtt = new Attributes(null, this, null, null);
        top.setUserData(nullAtt);
        handle.setUserData(nullAtt);
        trans.setUserData(nullAtt);
        stl.setUserData(nullAtt);
        
        bbox = null;
    }

    /**
     * Actually load the stl file and set its attributes
     * @param location
     * @param att
     * @return
     */
    private BranchGroup loadSingleSTL(String location, Attributes att)
    {
    	BranchGroup result = null;
        STLLoader loader = new STLLoader();
        Scene scene;
        try 
        {
            scene = loader.load(location);
            if (scene != null) 
            {
                result = scene.getSceneGroup();
                result.setCapability(Node.ALLOW_BOUNDS_READ);
                result.setCapability(Group.ALLOW_CHILDREN_READ);
                
                att.setPart(result);
                result.setUserData(att);
        		stl.addChild(result);
                
                // Recursively add its attribute
                
                Hashtable namedObjects = scene.getNamedObjects( );
                java.util.Enumeration enumValues = namedObjects.elements( );
                
                if( enumValues != null ) 
                {
                    while(enumValues.hasMoreElements( )) 
                    {
                    	Shape3D value = (Shape3D)enumValues.nextElement();
                        bbox = (BoundingBox)value.getBounds();
                        
                        value.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE );
                        GeometryArray g = (GeometryArray)value.getGeometry();
                        g.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
                        
                        recursiveSetUserData(value, att);
                    }
                }
            } 

        } catch ( Exception e ) 
        {
            System.err.println("loadSingelSTL(): Exception loading STL file from: " 
                    + location);
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * Move the object by actually changing all its coordinates (i.e. don't just add a
     * transform).  Also record its size.
     * @param child
     * @param offset
     */
    private void applyOffset(BranchGroup child, Vector3d offset) 
    {
    	if(child != null && bbox != null)
    	{
            javax.vecmath.Point3d p0 = new javax.vecmath.Point3d();
            javax.vecmath.Point3d p1 = new javax.vecmath.Point3d();
            bbox.getLower(p0);
            bbox.getUpper(p1);
            
            // If no offset requested, set it to bottom-left-at-origin
            
            if(offset == null) 
            {
                offset = new Vector3d();
                offset.x = -p0.x;  // Generally offset to but bottom left at the origin
                offset.y = -p0.y;
                offset.z = -p0.z;
            }
            
            // How big?
            
            size = new Vector3d(p1.x - p0.x, p1.y - p0.y, p1.z - p0.z);
            
            // Position us centre at origin:
            
            offset = add(offset, neg(scale(size, 0.5)));
            
            // Recursively apply that.  N.B. we do not apply a transform to the
            // loaded object; we actually shift all its points to put it in this
            // standard place.
            
            setOffset(offset);

            // Now shift us to have bottom left at origin using our transform.
            
            Transform3D temp_t = new Transform3D();
            temp_t.set(scale(size, 0.5));
            trans.setTransform(temp_t);
            
            restoreAppearance();
            
        } else
            System.err.println("applyOffset(): no bounding box or child.");
    }
    
    /**
     * Load an STL object from a file with a known offset (set that null to put
     * the object bottom-left-at-origin) and set its material (and hence its
     * appearance).
     * 
     * @param location
     * @param offset
     * @param material
     */
    public Attributes addSTL(String location, Vector3d offset, String material) 
    {
    	Attributes att = new Attributes(material, this, null,
    			NullExtruder.getAppearanceFromNumber(NullExtruder.getNumberFromMaterial(material)));
    	BranchGroup child = loadSingleSTL(location, att);
    	if(child == null)
    		return null;
    	applyOffset(child, offset);
    	return att;
    }
    
    /**
     * Load an STL object from a file with a known offset (set that null to put
     * the object bottom-left-at-origin) and set its appearance
     * 
     * @param location
     * @param offset
     * @param app
     */
    public Attributes addSTL(String location, Vector3d offset, Appearance app) 
    {
    	Attributes att = new Attributes(null, this, null, app);
    	BranchGroup child = loadSingleSTL(location, att);
    	if(child == null)
    		return null;
    	applyOffset(child, offset);
    	return att;
    }
    
    /**
     * Make an STL object from an existing BranchGroup
     */
    public STLObject(BranchGroup s, String n) 
    {
    	this();
  
        stl.addChild(s);
        size = new Vector3d(1, 1, 1);  // Should never be needed.
        
        Transform3D temp_t = new Transform3D();
        trans.setTransform(temp_t); 
    }

    // method to recursively set the user data for objects in the scenegraph tree
    // we also set the capabilites on Shape3D objects required by the PickTool

    void recursiveSetUserData(Object value, Object me) 
    {
        if( value instanceof SceneGraphObject != false ) 
        {
            // set the user data for the item
            SceneGraphObject sg = (SceneGraphObject) value;
            sg.setUserData( me );
            
            // recursively process group
            if( sg instanceof Group ) 
            {
                Group g = (Group) sg;
                
                // recurse on child nodes
                java.util.Enumeration enumKids = g.getAllChildren( );
                
                while(enumKids.hasMoreElements( ))
                    recursiveSetUserData( enumKids.nextElement( ), me );
            } else if ( sg instanceof Shape3D ) 
            {
                ((Shape3D)sg).setUserData(me);
                PickTool.setCapabilities( (Node) sg, PickTool.INTERSECT_FULL );
            }
        }
    }
    
    // Move the object by p permanently (i.e. don't just apply a transform).
    
    void recursiveSetOffset( Object value, Vector3d p) 
    {
        if( value instanceof SceneGraphObject != false ) 
        {
            // set the user data for the item
            SceneGraphObject sg = (SceneGraphObject) value;
            
            // recursively process group
            if( sg instanceof Group ) 
            {
                Group g = (Group) sg;
                
                // recurse on child nodes
                java.util.Enumeration enumKids = g.getAllChildren( );
                
                while(enumKids.hasMoreElements( ))
                    recursiveSetOffset( enumKids.nextElement( ), p );
            } else if ( sg instanceof Shape3D ) 
            {
                    s3dOffset((Shape3D)sg, p);
            }
        }
    }
    
    void setOffset(Vector3d p)
    {
    	recursiveSetOffset(stl, p);
    }
    
    // Shift a Shape3D permanently by p
    
    void s3dOffset(Shape3D shape, Tuple3d p)
    {
        GeometryArray g = (GeometryArray)shape.getGeometry();
        Point3d p3d = new Point3d();
        if(g != null)
        {
            for(int i = 0; i < g.getVertexCount(); i++) 
            {
                g.getCoordinate(i, p3d);
                p3d.add(p);
                g.setCoordinate(i, p3d);
            }
        }
    }
    
    // Scale the object by s permanently (i.e. don't just apply a transform).
    
    void recursiveSetScale( Object value, double s) 
    {
        if( value instanceof SceneGraphObject != false ) 
        {
            // set the user data for the item
            SceneGraphObject sg = (SceneGraphObject) value;
            
            // recursively process group
            if( sg instanceof Group ) 
            {
                Group g = (Group) sg;
                
                // recurse on child nodes
                java.util.Enumeration enumKids = g.getAllChildren( );
                
                while(enumKids.hasMoreElements( ))
                    recursiveSetScale( enumKids.nextElement( ), s );
            } else if ( sg instanceof Shape3D ) 
            {
                    s3dScale((Shape3D)sg, s);
            }
        }
    }
    
   // Scale a Shape3D permanently by s
    
    void s3dScale(Shape3D shape, double s)
    {
        GeometryArray g = (GeometryArray)shape.getGeometry();
        Point3d p3d = new Point3d();
        if(g != null)
        {
            for(int i = 0; i < g.getVertexCount(); i++) 
            {
                g.getCoordinate(i, p3d);
                p3d.scale(s);
                g.setCoordinate(i, p3d);
            }
        }
    }
    


    // Set my transform
    
    public void setTransform(Transform3D t3d)
    {
        trans.setTransform(t3d);
    }
    
    // Get my transform
    
    public Transform3D getTransform()
    {
    	Transform3D result = new Transform3D();
        trans.getTransform(result);
        return result;
    }
    
    // Get the actual object
    
    public BranchGroup getSTL()
    {
    	return stl;
    }
    
    // The mouse calls this to tell us it is controlling us
    
    public void setMouse(MouseObject m)
    {
        mouse = m;
    }
    
    // Change colour etc. - recursive private call to walk the tree
    
    private static void setAppearance_r(Object gp, Appearance a) 
    {
        if( gp instanceof Group ) 
        {
            Group g = (Group) gp;
            
            // recurse on child nodes
            java.util.Enumeration enumKids = g.getAllChildren( );
            
            while(enumKids.hasMoreElements( )) 
            {
                Object child = enumKids.nextElement( );
                if(child instanceof Shape3D) 
                {
                    Shape3D lf = (Shape3D) child;
                    lf.setAppearance(a);
                } else
                    setAppearance_r(child, a);
            }
        }
    }
    
    // Change colour etc. - call the internal fn to do the work.
    
    public void setAppearance(Appearance a)
    {
        setAppearance_r(stl, a);     
    }
    
    /**
     * Restore the appearances to the correct colour.
     */
    public void restoreAppearance()
    {
    	java.util.Enumeration enumKids = stl.getAllChildren( );
        
        while(enumKids.hasMoreElements( ))
        {
        	Object b = enumKids.nextElement();
        	if(b instanceof BranchGroup)
        	{
        		Attributes att = (Attributes)((BranchGroup)b).getUserData();
        		if(att != null)
        			setAppearance_r(b, att.getAppearance());
        		else
        			System.err.println("restoreAppearance(): no Attributes!");
        	}
        }
    }
    
    // Why the !*$! aren't these in Vector3d???
    
    public static Vector3d add(Vector3d a, Vector3d b)
    {
        Vector3d result = new Vector3d();
        result.x = a.x + b.x;
        result.y = a.y + b.y;
        result.z = a.z + b.z;
        return result;
    }
    
    public static Vector3d neg(Vector3d a)
    {
        Vector3d result = new Vector3d(a);
        result.negate();
        return result;
    }
    
    public static Vector3d scale(Vector3d a, double s)
    {
        Vector3d result = new Vector3d(a);
        result.scale(s);
        return result;
    }
    
    // Put a vector in the positive octant (sort of abs for vectors)
    
    Vector3d posOct(Vector3d v)
    {
        Vector3d result = new Vector3d();
        result.x = Math.abs(v.x);
        result.y = Math.abs(v.y);
        result.z = Math.abs(v.z);
        return result;
    }
    
    // Apply a 90 degree click transform about one of the coordinate axes,
    // which should be set in t.  This can only be done if we're being controlled
    // by the mouse, making us the active object.
    
    private void rClick(Transform3D t)
    {
        if(mouse == null)
            return;
        
        // Get the mouse transform and split it into a rotation and a translation
        
        Transform3D mtrans = new Transform3D();
        mouse.getTransform(mtrans);
        Vector3d mouseTranslation = new Vector3d();
        Matrix3d mouseRotation = new Matrix3d();
        mtrans.get(mouseRotation, mouseTranslation);
        
        // Subtract the part of the translation that puts the bottom left corner
        // at the origin.
        
        Vector3d zero = scale(size, 0.5);
        mouseTranslation = add(mouseTranslation, neg(zero));       
        
        // Click the size record round by t
        
        t.transform(size);
        size = posOct(size); 
        
        // Apply the new rotation to the existing one
        
        Transform3D spin = new Transform3D();
        spin.setRotation(mouseRotation);
        t.mul(spin);
        
        // Add a new translation to put the bottom left corner
        // back at the origin.
        
        zero = scale(size, 0.5);
        mouseTranslation = add(mouseTranslation, zero);
        
        // Then slide us back where we were
        
        Transform3D fromZeroT = new Transform3D();
        fromZeroT.setTranslation(mouseTranslation);

        fromZeroT.mul(t);
        
        // Apply the whole new transformation
        
        mouse.setTransform(fromZeroT);       
    }
    
   // Rescale the STL object (for inch -> mm conversion)
    
    void rScale(double s)
    {
        if(mouse == null)
            return;
        
        // Get the mouse transform and split it into a rotation and a translation
        
        Transform3D mtrans = new Transform3D();
        mouse.getTransform(mtrans);
        Vector3d mouseTranslation = new Vector3d();
        Matrix3d mouseRotation = new Matrix3d();
        mtrans.get(mouseRotation, mouseTranslation);
        
        // Subtract the part of the translation that puts the bottom left corner
        // at the origin.
        
        Vector3d zero = scale(size, 0.5);
        mouseTranslation = add(mouseTranslation, neg(zero));       
        
        // Rescale the box
        
       	size.scale(s);
        
        // Add a new translation to put the bottom left corner
        // back at the origin.
        
        zero = scale(size, 0.5);
        mouseTranslation = add(mouseTranslation, zero);
        
        // Then slide us back where we were
        
        Transform3D fromZeroT = new Transform3D();
        fromZeroT.setTranslation(mouseTranslation);
        
        // Apply the whole new transformation
        
        mouse.setTransform(fromZeroT);
        
        // Rescale the object
 
        Enumeration things;

        things = stl.getAllChildren();
        while(things.hasMoreElements()) 
        {
        	Object value = things.nextElement();
        	recursiveSetScale(value, s);
        }


    }
    
    // Apply X, Y or Z 90 degree clicks to us if we're the active (i.e. mouse
    // controlled) object.
    
    public void xClick()
    {
        if(mouse == null)
            return;
        
        Transform3D x90 = new Transform3D();
        x90.set(new AxisAngle4d(1, 0, 0, 0.5*Math.PI));
        
        rClick(x90);
    }
    
    public void yClick()
    {
        if(mouse == null)
            return;
        
        Transform3D x90 = new Transform3D();
        x90.set(new AxisAngle4d(0, 1, 0, 0.5*Math.PI));
        
        rClick(x90);
    }
    
    public void zClick()
    {
        if(mouse == null)
            return;
        
        Transform3D x90 = new Transform3D();
        x90.set(new AxisAngle4d(0, 0, 1, 0.5*Math.PI));
        
        rClick(x90);
    } 
    
    // This is called when the user wants to convert the object from
    // inches to mm.
    
    public void inToMM()
    {
        if(mouse == null)
            return;
        
        rScale(Preferences.inchesToMillimetres());
    } 
}

//********************************************************************************
