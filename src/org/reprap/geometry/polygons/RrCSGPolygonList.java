package org.reprap.geometry.polygons;

import java.util.*;

import org.reprap.Attributes;
import org.reprap.Extruder;

/**
 * It's convenient to have lists of CSG polygons (even though they
 * can be multiple polygons themselves) so that you each entry
 * can be one collection of polygons per material (attribute).
 * 
 * @author ensab
 *
 */
public class RrCSGPolygonList 
{
	/**
	 * 
	 */
	List<RrCSGPolygon> csgPolygons;
	
	/**
	 * 
	 *
	 */
	public RrCSGPolygonList()
	{
		csgPolygons = new ArrayList<RrCSGPolygon>();
	}
	
	/**
	 * 
	 * @param c
	 */
	public void add(RrCSGPolygon c)
	{
		csgPolygons.add(c);
	}
	
	/**
	 * 
	 * @param i
	 * @return
	 */
	public RrCSGPolygon get(int i)
	{
		return csgPolygons.get(i);
	}
	
	/**
	 * 
	 * @return
	 */
	public int size()
	{
		return csgPolygons.size();
	}
	
	/**
	 * 
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
	 * 
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
				if(outline)
					result.add(get(i).offset(-0.5*e.getExtrusionSize()));
				else
					result.add(get(i).offset(-1.5*e.getExtrusionSize() + e.getInfillOverlap()));
			}
		}
		return result;
	}
		
	/**
	 * 
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
	 * 
	 * @param hp
	 * @param es
	 * @return
	 */
	public RrPolygonList hatch(RrHalfPlane hp, Extruder[] es)
	{
		RrPolygonList result = new RrPolygonList();
		for(int i = 0; i < size(); i++)
		{
			Attributes att = get(i).getAttributes();
			result.add(get(i).hatch(hp, att.getExtruder(es).getExtrusionInfillWidth()));
		}
		return result;
	}
}
