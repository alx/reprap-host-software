package org.reprap.geometry.polygons;

import java.util.*;

import org.reprap.Attributes;
import org.reprap.Extruder;

/**
 * It's convenient to have lists of CSG polygons (even though they
 * can be multiple polygons themselves) so that you each entry
 * can be one collection of polygons per material (attribute).
 * 
 * @author Adrian
 *
 */
public class RrCSGPolygonList {
	/**
	 * The list of polygons
	 */
	List<RrCSGPolygon> csgPolygons = null;
	
	/**
	 * Flag to prevent cyclic graphs going round forever
	 */
	private boolean beingDestroyed = false;
	
	/**
	 * Destroy me and all that I point to
	 */
	public void destroy() 
	{
		if(beingDestroyed) // Prevent infinite loop
			return;
		beingDestroyed = true;
		if(csgPolygons != null)
		{
			for(int i = 0; i < csgPolygons.size(); i++)
			{
				csgPolygons.get(i).destroy();
				csgPolygons.set(i, null);
			}
		}
		csgPolygons = null;
		beingDestroyed = false;
	}
	
	/**
	 * Destroy just me
	 */
	protected void finalize() throws Throwable
	{
		csgPolygons = null;
		super.finalize();
	}
	
	/**
	 * 
	 */
	public RrCSGPolygonList()
	{
		csgPolygons = new ArrayList<RrCSGPolygon>();
	}
	
	/**
	 * Add polygon c
	 * @param c
	 */
	public void add(RrCSGPolygon c)
	{
		csgPolygons.add(c);
	}
	
	/**
	 * Get the ith polygon in the list
	 * @param i
	 * @return
	 */
	public RrCSGPolygon get(int i)
	{
		return csgPolygons.get(i);
	}
	
	/**
	 * How many polygons?
	 * @return
	 */
	public int size()
	{
		return csgPolygons.size();
	}
	
	/**
	 * The minimum enclosing rectangle
	 * @return
	 */
	public RrBox box()
	{
		RrBox result = new RrBox();
		for(int i = 0; i < size(); i++)
			result.expand(get(i).box());
		return result;
	}
	
	/**
	 * Offset the lot by appropriate extruder widths
	 * @param es
	 * @param outline
	 * @return
	 */
	public RrCSGPolygonList offset(Extruder[] es, boolean outline)
	{
		RrCSGPolygonList result = new RrCSGPolygonList();
		for(int i = 0; i < size(); i++)
		{
			Attributes att = get(i).getAttributes();
			if(att == null)
				System.err.println("offset(): null attribute!");
			else
			{
				Extruder e = att.getExtruder(es);
				int shells = e.getShells();
				if(outline)
				{
					for(int shell = 0; shell < shells; shell++)
						result.add(get(i).offset(-((double)shell + 0.5)*e.getExtrusionSize()));
				} else
				{
					// Must be a hatch.  Only do it if the gap is +ve
					
					if (e.getExtrusionInfillWidth(0, 1) > 0)  // Z valuesn't mattere here
						result.add(get(i).offset(-((double)shells + 0.5)*e.getExtrusionSize() + 
							e.getInfillOverlap()));
				}
			}
		}
		return result;
	}
	
	/**
	 * Recursively divide the lot
	 * @param res_2
	 * @param swell
	 */
	public void divide(double res_2, double swell)
	{
		for(int i = 0; i < size(); i++)
			get(i).divide(res_2, swell);
	}
	
	/**
	 * Generate boundary lists for the lot
	 * @return
	 */
	public RrPolygonList megList()
	{
		RrPolygonList result = new RrPolygonList();
		for(int i = 0; i < size(); i++)
			result.add(get(i).megList());
		return result;
	}
	
	/**
	 * Cross hatch the lot
	 * @param hp
	 * @param es
	 * @return
	 */
	public RrPolygonList hatch(RrHalfPlane hp, Extruder[] es, double z, double zMax) 
	{
		RrPolygonList result = new RrPolygonList();
		for(int i = 0; i < size(); i++)
		{
			Attributes att = get(i).getAttributes();
			result.add(get(i).hatch(hp, att.getExtruder(es).getExtrusionInfillWidth(z, zMax)));
		}
		return result;
	}
}
