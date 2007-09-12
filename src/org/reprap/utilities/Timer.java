/**
 * 
 */
package org.reprap.utilities;

import java.util.*;
import java.text.DecimalFormat;

/**
 * @author Adrian
 *
 * I bet there's a system utility somewhere to do this...
 * 
 */
public class Timer {

	/**
	 * Times in ms
	 */
	private List <Long>times;
	
	/**
	 * Names of events
	 */
	private List <String>events;
	
	/**
	 * Time at the start
	 */
	private long t0;
	
	/**
	 * for 3 d.p.
	 */
	private DecimalFormat threeDec;
	
	/**
	 * Static single instance to hold all times
	 */
	static private Timer log = null;
	
	/**
	 * Constructor just needs to create a single 
	 * instance for initialiseIfNeedBe(String e)
	 *
	 */
	private Timer()
	{
	}
	
	/**
	 * Start (again)
	 * @param e
	 */
	static private void clean(String e)
	{
		log.times = new ArrayList();
		log.events = new ArrayList();
		
		Date d = new Date();
		log.t0 = d.getTime();
		log.times.add(new Long(log.t0));
		log.events.add(e);
		log.threeDec = new DecimalFormat("0.000");
		log.threeDec.setGroupingUsed(false);
	}
	
	/**
	 * Check if we've been initialised and initialise if needed
	 * @param e
	 */
	static public void initialiseIfNeedBe(String e)
	{
		if(log != null) return;
		
		log = new Timer();
		log.clean(e);
	}
	
	/**
	 * Get a double as a 3 d.p. string
	 * @param v
	 * @return
	 */
	static public String d3dp(double v)
	{
		return log.threeDec.format(v);
	}
	
	/**
	 * Since the beginning in seconds
	 * @param t
	 * @return
	 */
	static private double elapsed(long t)
	{
		initialiseIfNeedBe("");
		return ((double)(t - log.t0)*0.001);
	}
	
	static public String time()
	{
		initialiseIfNeedBe("");
		Date d = new Date();
		return d3dp(elapsed(d.getTime())) + "s";
	}
	
	/**
	 * Record an event silently
	 * @param e
	 */
	static public void event(String e)
	{
		Boolean first = log == null;
		initialiseIfNeedBe(e);
		if(first) return;
		Date d = new Date();
		log.times.add(new Long(d.getTime()));
		log.events.add(e);		
	}
	
	/**
	 * Report an event
	 * @param e
	 * @return
	 */
	static public String report(String e)
	{
		Boolean first = log == null;
		initialiseIfNeedBe(e);
		if(first)
			return e + " | at 0s";
		Date d = new Date();
		long t = d.getTime();
		long tBefore = log.times.get(log.times.size()-1);
		String eBefore = log.events.get(log.events.size()-1);
		log.times.add(new Long(t));
		log.events.add(e);
		return e + " | at " + d3dp(elapsed(t)) + "s (" + (t - tBefore) + "ms since " + eBefore + ")";
	}
	
	/**
	 * Report all the events
	 */
	static public String reportAll()
	{
		initialiseIfNeedBe("");
		String r = "";
		for(int i = 0; i < log.times.size(); i++)
		{
			String e = log.events.get(i);
			Long t = log.times.get(i);
			int j = i - 1;
			String since;
			if(j >= 0)
				since = "s (" + (t - log.times.get(j)) + "ms since " + log.events.get(i) + ")\n";
			else
				since = "s\n";
			r = r + e + " | at " + d3dp(elapsed(t)) + since;
		}
		
		return r;
	}
}
