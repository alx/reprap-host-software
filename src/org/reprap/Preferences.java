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
	 * This section deals with internal (i.e. not RepRap machine, but this code)
	 * precisions and accuracies - it should probably
	 * get its data from the properties file...
	 */
	
	private static final int grid = 100;             // Click outline polygons to a...
	private static final double gridRes = 1.0/grid;  // ...10 micron grid
	private static final double lessGridSquare = gridRes*gridRes*0.01;  // Small squared size of a gridsquare
	private static final double tiny = 1.0e-10;      // A small number
	private static final double swell = 1.01;        // Quad tree swell factor
	private static final double machineResolution = 0.1; // RepRap step size in mm - should 
	                                                     // derive this from Axis1Scale and Axis2Scale
	
	public static int grid() { return grid; }
	public static double gridRes() { return gridRes; }
	public static double lessGridSquare() { return lessGridSquare; }
	public static double tiny() { return tiny; }
	public static double swell() { return swell; }
	public static double inchesToMillimetres() { return 25.4; }
	public static double machineResolution() { return machineResolution; }
	
	
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
