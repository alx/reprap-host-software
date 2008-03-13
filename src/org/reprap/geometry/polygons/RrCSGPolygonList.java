package org.reprap.geometry.polygons;

import java.util.*;

import org.reprap.Attributes;
import org.reprap.Extruder;

public class RrCSGPolygonList {
	List<RrCSGPolygon> csgPolygons;
	
	public RrCSGPolygonList()
	{
		csgPolygons = new ArrayList<RrCSGPolygon>();
	}
	
	public void add(RrCSGPolygon c)
	{
		csgPolygons.add(c);
	}
	
	public RrCSGPolygon get(int i)
	{
		return csgPolygons.get(i);
	}
	
	public int size()
	{
		return csgPolygons.size();
	}
	
	public RrBox box()
	{
		RrBox result = new RrBox();
		for(int i = 0; i < size(); i++)
			result.expand(get(i).box());
		return result;
	}
	
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
	
	public void divide(double res_2, double swell)
	{
		for(int i = 0; i < size(); i++)
			get(i).divide(res_2, swell);
	}
	
	public RrPolygonList megList()
	{
		RrPolygonList result = new RrPolygonList();
		for(int i = 0; i < size(); i++)
			result.add(get(i).megList());
		return result;
	}
	
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
