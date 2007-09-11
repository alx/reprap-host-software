/**
 * 
 */
package org.reprap.utilities;

import java.util.*;

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
	
	public Timer(String e)
	{
		clean(e);
	}
	
	/**
	 * Start again
	 * @param e
	 */
	public void clean(String e)
	{
		times = new ArrayList();
		events = new ArrayList();
		
		Date d = new Date();
		times.add(new Long(d.getTime()));
		events.add(e);		
	}
	
	/**
	 * Record an event silently
	 * @param e
	 */
	public void event(String e)
	{
		Date d = new Date();
		times.add(new Long(d.getTime()));
		events.add(e);		
	}
	
	/**
	 * Report an event
	 * @param e
	 * @return
	 */
	public String report(String e)
	{
		Date d = new Date();
		long t = d.getTime();
		long tBefore = times.get(times.size()-1);
		String eBefore = events.get(events.size()-1);
		times.add(new Long(t));
		events.add(e);
		return e + " | at " + t + "ms (" + (t - tBefore) + "ms since " + eBefore + ")";
	}
	
	/**
	 * Report all the events
	 */
	public String toString()
	{
		String r = "";
		for(int i = 0; i < times.size(); i++)
		{
			String e = events.get(i);
			Long t = times.get(i);
			int j = i - 1;
			String since;
			if(j >= 0)
				since = "ms (" + (t - (long)times.get(j)) + "ms since " + events.get(i) + ")\n";
			else
				since = "ms\n";
			r = r + e + " | at " + t + since;
		}
		
		return r;
	}
}
