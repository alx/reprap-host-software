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

public class STLObject
{
    private MouseObject mouse = null;  // The mouse, if it is controlling us
    public BranchGroup top = null;     // The thing that links us to the world
    public BranchGroup handle = null;  // Internal handle for the mouse to grab
    public TransformGroup trans = null;// Static transform for when the mouse is away
    public BranchGroup stl = null;     // The actual STL geometry
    public Vector3d size = null;       // X, Y and Z extent
    public String name = null;         // Name is the file name plus a digit indicating
                                       // the order of loading
    
    // Load an STL object from a file with a known offset (set that null to put
    // the object bottom-left-at-origin), an index count in the scene, 
    // and set its appearance
    
    public STLObject( String location, Vector3d offset,
            int objectIndex, Appearance app) 
    {
        Scene scene = null;
        BoundingBox bbox = null;
        
        STLLoader loader = new STLLoader( );
        
        try 
        {
            scene = loader.load( location );
        } catch ( Exception e ) 
        {
            System.err.println( "STLObject(): Exception loading STL file from: " 
                    + location );
            e.printStackTrace( );
        }
        
        name = location + " " + Integer.toString(objectIndex);
        
        
        // Recurse down the object setting its characteristics
        
        Hashtable namedObjects = null;
        java.util.Enumeration enumValues = null;
        java.util.Enumeration enumKeys = null;
        
        if (scene != null) 
        {
            // get the scene group - that is the stl
            
            stl = scene.getSceneGroup( );
            stl.setCapability( Node.ALLOW_BOUNDS_READ );
            stl.setCapability( Group.ALLOW_CHILDREN_READ );
            
            // Set the appearance of the object and recursively add its name
            
            namedObjects = scene.getNamedObjects( );
            enumValues = namedObjects.elements( );
            enumKeys = namedObjects.keys( );
            
            
            if( enumValues != null ) 
            {
                while( enumValues.hasMoreElements( ) != false ) 
                {
                    Object value = enumValues.nextElement( );
                    bbox = (BoundingBox)((Shape3D)value).getBounds();
                    
                    ((Shape3D)value).setCapability(Shape3D.ALLOW_APPEARANCE_WRITE );
                    ((Shape3D)value).setAppearance(app);
                    
                    Object key = enumKeys.nextElement( );
                    recursiveSetUserData( value, key, name );
                }
            }
        }
        
        // If we've got a live one, the bounding box will be set; find its limits.
        
        if(bbox != null) 
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
            
            enumValues = namedObjects.elements( );
            
            if( enumValues != null ) 
            {
                while( enumValues.hasMoreElements( ) != false ) 
                {
                    Object value = enumValues.nextElement( );
                    recursiveSetOffset( value, offset );
                }
            }
            
            // Now shift us to have bottom left at origin using our transform.
            
            Transform3D temp_t = new Transform3D();
            temp_t.set(scale(size, 0.5));
            trans = new TransformGroup(temp_t);
            
            // No mouse yet
            
            mouse = null;
            
            // Set up our bit of the scene graph
            
            top = new BranchGroup();
            handle = new BranchGroup();
            
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
            
            // Tell everything our name (N.B. everything under stl should have had that
            // done for it by the recursive call above).
            
            top.setUserData(name);
            handle.setUserData(name);
            trans.setUserData(name);
            stl.setUserData(name);
        } else
            System.err.println("STLObject(): cannot create a valid STL object.");
    }

    // method to recursively set the user data for objects in the scenegraph tree
    // we also set the capabilites on Shape3D and Morph objects required by the PickTool

    void recursiveSetUserData( Object value, Object key , String name) 
    {
        if( value instanceof SceneGraphObject != false ) 
        {
            // set the user data for the item
            SceneGraphObject sg = (SceneGraphObject) value;
            sg.setUserData( key );
            
            // recursively process group
            if( sg instanceof Group ) 
            {
                Group g = (Group) sg;
                
                // recurse on child nodes
                java.util.Enumeration enumKids = g.getAllChildren( );
                
                while( enumKids.hasMoreElements( ) != false )
                    recursiveSetUserData( enumKids.nextElement( ), key, name );
            } else if ( sg instanceof Shape3D || sg instanceof Morph ) 
            {
                if ( sg instanceof Shape3D)
                    ((Shape3D)sg).setUserData(name);
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
                
                while( enumKids.hasMoreElements( ) != false )
                    recursiveSetOffset( enumKids.nextElement( ), p );
            } else if ( sg instanceof Shape3D ) 
            {
                    s3dOffset((Shape3D)sg, p);
            }
        }
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
    
    // Make an STL object from an existing BranchGroup
    
    public STLObject(BranchGroup s, String n) 
    {
        stl = s;
        size = new Vector3d(1, 1, 1);  // Should never be needed.
        name = n;
        
        Transform3D temp_t = new Transform3D();
        trans = new TransformGroup(temp_t);
        
        // No mouse yet
        
        mouse = null;
        
        // Set up our bit of the scene graph
        
        top = new BranchGroup();
        handle = new BranchGroup();
        
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
        
        // Tell everything our name 
        
        top.setUserData(name);
        handle.setUserData(name);
        trans.setUserData(name);
        stl.setUserData(name);
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
}

//********************************************************************************
