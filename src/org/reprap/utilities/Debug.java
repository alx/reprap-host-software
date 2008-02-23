/**
 * 
 */
package org.reprap.utilities;

import org.reprap.utilities.Timer;
import org.reprap.Preferences;

/**
 * @author Adrian
 *
 */
public class Debug {
	
	private boolean commsDebug = false;
	
	private boolean debug = false;
	
	static private Debug db = null;
	
	private Debug() {}
	
	static private void initialiseIfNeedBe()
	{
		if(db != null) return;
		db = new Debug();
		
		try {
			// Try to load debug setting from properties file
			db.debug = Preferences.loadGlobalBool("Debug");
		} catch (Exception ex) {
			// Fall back to non-debug mode if no setting is available
			db.debug = false;
		}
		
		try {
			// Try to load debug setting from properties file
			db.commsDebug = Preferences.loadGlobalBool("CommsDebug");
		} catch (Exception ex) {
			// Fall back to non-debug mode if no setting is available
			db.commsDebug = false;
		}				
	}
	
	static public void sleep(long secs)
	{
		try {
			Thread.sleep(secs);
		} catch (InterruptedException e) {
		}
	}
	
	static public void d(String s)
	{
		initialiseIfNeedBe();
		if(!db.debug) return;
		System.out.println("DEBUG: " + s + Timer.stamp());
	}
	
	static public void c(String s)
	{
		initialiseIfNeedBe();
		if(!db.commsDebug) return;
		System.out.println("comms: " + s + Timer.stamp());
	}
	

}
