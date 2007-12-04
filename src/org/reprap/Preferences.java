package org.reprap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import javax.media.j3d.Appearance;
import javax.media.j3d.Material;
import javax.vecmath.Color3f;

/**
 * A single centralised repository of the current preference settings.  This also
 * implements (almost) a singleton for easy global access.  If there are no current
 * preferences fallback distribution defaults are used. 
 */
public class Preferences {
	
	private static final String propsFile = "reprap.properties";
	private static final String propsFolder = ".reprap";
	private static final String propsFileDist = "reprap.properties.dist";
	
	private static Preferences globalPrefs = null; 
	
	Properties fallbackPreferences;
	Properties mainPreferences;
	
	/*
	 * This section deals with internal (i.e. not RepRap machine, but this code or
	 * physics) precisions and accuracies - it should probably
	 * get its data from the properties file...
	 */
	
	private static final int grid = 100;             // Click outline polygons to a...
	private static final double gridRes = 1.0/grid;  // ...10 micron grid
	private static final double lessGridSquare = gridRes*gridRes*0.01;  // Small squared size of a gridsquare
	private static final double tiny = 1.0e-12;      // A small number
	private static final double swell = 1.01;        // Quad tree swell factor
	private static final double machineResolution = 0.1; // RepRap step size in mm - should 
	                                                     // derive this from Axis1Scale and Axis2Scale

	private static final double absoluteZero = -273;
	private static final double inToMM = 25.4;
	
	private static final Color3f black = new Color3f(0, 0, 0);
	
	public static int grid() { return grid; }
	public static double gridRes() { return gridRes; }
	public static double lessGridSquare() { return lessGridSquare; }
	public static double tiny() { return tiny; }
	public static double swell() { return swell; }
	public static double inchesToMillimetres() { return inToMM; }
	public static double machineResolution() { return machineResolution; }
	public static double absoluteZero() { return absoluteZero; }
	public static Appearance unselectedApp()
	{
		Color3f unselectedColour = null;
		try
		{
			unselectedColour = new Color3f((float)Preferences.loadGlobalDouble("UnselectedColourR(0..1)"), 
				(float)Preferences.loadGlobalDouble("UnselectedColourG(0..1)"), 
				(float)Preferences.loadGlobalDouble("UnselectedColourB(0..1)"));
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
		Appearance unselectedApp = new Appearance();
		unselectedApp.setMaterial(new 
				Material(unselectedColour, black, unselectedColour, black, 0f));
		return unselectedApp;
	}
	
	// Main preferences constructor
	
	public Preferences() throws IOException {
		fallbackPreferences = new Properties();
		mainPreferences = new Properties();
		URL fallbackUrl = ClassLoader.getSystemResource(propsFileDist);

		// Construct URL of user properties file
		String path = new String(System.getProperty("user.home") + File.separatorChar + 
			propsFolder + File.separatorChar + propsFile);
		File mainFile = new File(path);
		URL mainUrl = mainFile.toURL();
		
		if (fallbackUrl == null && !mainFile.exists())
			throw new IOException("Cannot load RepRap properties file or default "+propsFileDist);
		
		if (fallbackUrl != null)
			fallbackPreferences.load(fallbackUrl.openStream());
		
		if (mainFile.exists())
			mainPreferences.load(mainUrl.openStream());
		else
		{
			// If we don't have a local preferences file copy the default
			// file into it.
			mainPreferences.load(fallbackUrl.openStream());
			save();
		}

	}

	public void save() throws FileNotFoundException, IOException {
		String savePath = new String(System.getProperty("user.home") + File.separatorChar + 
			propsFolder + File.separatorChar);
		File f = new File(savePath + File.separatorChar + propsFile);
		if (!f.exists()) {
			// No properties file exists, so we will create one and try again
			// We'll put the properties file in the .reprap folder,
			// under the user's home folder.
			File p = new File(savePath);
			if (!p.isDirectory())		// Create .reprap folder if necessary
				   p.mkdirs();
		}
		
		OutputStream output = new FileOutputStream(f);
		mainPreferences.store(output, "Reprap properties http://reprap.org/");
	}
		
	public String loadString(String name) {
		if (mainPreferences.containsKey(name))
			return mainPreferences.getProperty(name);
		if (fallbackPreferences.containsKey(name))
			return fallbackPreferences.getProperty(name);
		System.err.println("RepRap preference: " + name + " not found in either preference file.");
		return null;
	}
	
	public int loadInt(String name) {
		String strVal = loadString(name);
		return Integer.parseInt(strVal);
	}
	
	public double loadDouble(String name) {
		String strVal = loadString(name);
		return Double.parseDouble(strVal);
	}
	
	public boolean loadBool(String name) {
		String strVal = loadString(name);
		if (strVal == null) return false;
		if (strVal.length() == 0) return false;
		if (strVal.compareToIgnoreCase("true") == 0) return true;
		return false;
	}

	synchronized private static void initIfNeeded() throws IOException {
		if (globalPrefs == null)
			globalPrefs = new Preferences();
	}

	public static String loadGlobalString(String name) throws IOException {
		initIfNeeded();
		return globalPrefs.loadString(name);
	}

	public static int loadGlobalInt(String name) throws IOException {
		initIfNeeded();
		return globalPrefs.loadInt(name);
	}
	
	public static double loadGlobalDouble(String name) throws IOException {
		initIfNeeded();
		return globalPrefs.loadDouble(name);
	}
	
	public static boolean loadGlobalBool(String name) throws IOException {
		initIfNeeded();
		return globalPrefs.loadBool(name);
	}
	
	public static void saveGlobal() throws IOException {		
		initIfNeeded();
		globalPrefs.save();
	}

	public static Preferences getGlobalPreferences() throws IOException {
		initIfNeeded();
		return globalPrefs;
	}

	/**
	 * Set a new value
	 * @param name
	 * @param value
	 * @throws IOException
	 */
	public static void setGlobalString(String name, String value) throws IOException {
		initIfNeeded();
		globalPrefs.setString(name, value);
	}

	public static void setGlobalBool(String name, boolean value) throws IOException {
		initIfNeeded();
		globalPrefs.setString(name, value ? "true" : "false");
	}

	/**
	 * @param name
	 * @param value
	 */
	private void setString(String name, String value) {
		mainPreferences.setProperty(name, value);
	}
	
	/**
	 * @return an array of all the names of all the materials in extruders
	 * @throws IOException
	 */
	public static String[] allMaterials() throws IOException
	{
		int extruderCount = globalPrefs.loadInt("NumberOfExtruders");
		String[] result = new String[extruderCount];
		
		for(int i = 0; i < extruderCount; i++)
		{
			String prefix = "Extruder" + i + "_";
			result[i] = globalPrefs.loadString(prefix + "MaterialType(name)");	
		}
		
		return result;
	}
	
	public static String[] startsWith(String prefix) throws IOException 
	{
		initIfNeeded();
		Enumeration allOfThem = globalPrefs.mainPreferences.propertyNames();
		List r = new ArrayList();
		
		while(allOfThem.hasMoreElements())
		{
			String next = (String)allOfThem.nextElement();
			if(next.startsWith(prefix))
				r.add(next);
		}
		String[] result = new String[r.size()];
		
		for(int i = 0; i < r.size(); i++)
			result[i] = (String)r.get(i);
		
		return result;		
	}
	
	public static String[] notStartsWith(String prefix) throws IOException 
	{
		initIfNeeded();
		Enumeration allOfThem = globalPrefs.mainPreferences.propertyNames();
		List r = new ArrayList();
		
		while(allOfThem.hasMoreElements())
		{
			String next = (String)allOfThem.nextElement();
			if(!next.startsWith(prefix))
				r.add(next);
		}
		
		String[] result = new String[r.size()];
		
		for(int i = 0; i < r.size(); i++)
			result[i] = (String)r.get(i);
		
		return result;
	}
	
}
