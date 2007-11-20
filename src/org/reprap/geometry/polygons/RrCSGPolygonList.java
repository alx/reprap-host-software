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
	
	public RrCSGPolygonList offset(double off, Extruder[] es)
	{
		RrCSGPolygonList result = new RrCSGPolygonList();
		for(int i = 0; i < size(); i++)
		{
			Attributes att = get(i).getAttributes();
			if(att == null)
				System.err.println("offset(): null attribute!");
			else
				result.add(get(i).offset(off*att.getExtruder(es).getExtrusionSize()));
		}
		return result;
	}
	
	public void divide(double res_2, double swell)
	{
		for(int i = 0; i < size(); i++)
			get(i).divide(res_2, swell);
	}
	
	public RrPolygonList megList(int fg, int fs)
	{
		RrPolygonList result = new RrPolygonList();
		for(int i = 0; i < size(); i++)
			result.add(get(i).megList(fg, fs));
		return result;
	}
	
	public RrPolygonList hatch(RrHalfPlane hp, Extruder[] es, int fg, int fs)
	{
		RrPolygonList result = new RrPolygonList();
		for(int i = 0; i < size(); i++)
		{
			Attributes att = get(i).getAttributes();
			result.add(get(i).hatch(hp, att.getExtruder(es).getExtrusionInfillWidth(), fg, fs));
		}
		return result;
	}
}
