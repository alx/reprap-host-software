
package org.reprap.geometry.polygons;

public class TestMain
{
	public static void fred(String[] args)
	{
		Rr2Point p = new Rr2Point(0.1, 0.15);
		Rr2Point q = new Rr2Point(0.2, 0.85);
		Rr2Point r = new Rr2Point(1, 0.89);
		
		Rr2Point s = new Rr2Point(0.95, 0.03);
		Rr2Point pp = new Rr2Point(0.35, 0.62);
		Rr2Point qq = new Rr2Point(0.55, 0.65);
		Rr2Point rr = new Rr2Point(0.45, 0.5);
		Rr2Point ss = new Rr2Point(0.4, 0.3);    
		RrLine x = new RrLine(new Rr2Point(-1, -1), new Rr2Point(1, 1));
		
		RrPolygon a = new RrPolygon();
		a.append(p, 1);
		a.append(q, 1);
		a.append(r, 1);
		a.append(s, 1);
		
		RrPolygonList c = new RrPolygonList();
		c.append(a);
		
		a = new RrPolygon();
		a.append(rr, 2);
		a.append(qq, 2);
		a.append(pp, 2);
		a.append(ss, 2);
		c.append(a);
		
		RrPolygonList d = c.offset(0.03);
		
		RrPolygon  e = d.hatch(x, 0.03, 3, 4);
		//d = d.offset(0.003);
		//e = e.join_up(d);
		//c.append(d); 
		c.append(e);
		
		new RrGraphics(c, false);
	}
	
	public static void main(String[] args)
	{
		Rr2Point p = new Rr2Point(0.1, 0.15);
		Rr2Point q = new Rr2Point(0.2, 0.85);
		Rr2Point r = new Rr2Point(1, 0.89);
		Rr2Point s = new Rr2Point(0.95, 0.03);
		
		Rr2Point pp = new Rr2Point(0.35, 0.62);
		Rr2Point qq = new Rr2Point(0.55, 0.95);
		Rr2Point rr = new Rr2Point(0.45, 0.5);
		RrLine x = new RrLine(new Rr2Point(-1, -1), new Rr2Point(1, 1));
		
		RrHalfPlane ph = new RrHalfPlane(p, q);
		RrHalfPlane qh = new RrHalfPlane(q, r);
		RrHalfPlane rh = new RrHalfPlane(r, s);
		RrHalfPlane sh = new RrHalfPlane(s, p);
		
		RrHalfPlane pph = new RrHalfPlane(pp, qq);
		RrHalfPlane qqh = new RrHalfPlane(qq, rr);
		RrHalfPlane rrh = new RrHalfPlane(rr, pp);
		
		RrCSG pc = new RrCSG(ph);
		RrCSG qc = new RrCSG(qh);
		RrCSG rc = new RrCSG(rh);
		RrCSG sc = new RrCSG(sh);
		
		pc = RrCSG.intersection(pc, qc);
		rc = RrCSG.intersection(sc, rc);		
		pc = RrCSG.intersection(pc, rc);
		//pc = RrCSG.union(pc, rc); // !!
		
		RrCSG ppc = new RrCSG(pph);
		RrCSG qqc = new RrCSG(qqh);
		RrCSG rrc = new RrCSG(rrh);
		
		
		ppc = RrCSG.intersection(ppc, qqc);
		ppc = RrCSG.intersection(ppc, rrc);
		ppc = RrCSG.difference(pc, ppc);
		
		pc = ppc.offset(-0.15);
		ppc = RrCSG.difference(ppc, pc);
		
		//ppc = RrCSG.union(rc, ppc);
		ppc = RrCSG.union(new RrCSG(ppc), ppc);
		//System.out.println(ppc.toString());
		ppc = ppc.simplify(1.0e-6);
		//System.out.println(ppc.toString());
		//ppc = ppc.regularise();
		//System.out.println(ppc.toString());
		RrCSGPolygon cp = new RrCSGPolygon(ppc, new 
				RrBox(new Rr2Point(0,0), new Rr2Point(1,1)));
		
		cp.divide(1.0e-6, 1.0);
		//System.out.println(cp.toString());
		//new RrGraphics(cp, true);
		
		RrPolygon  h = cp.hatch_join(x, 0.005, 1, 3);
		RrPolygonList hp = new RrPolygonList();
		RrPolygon a = new RrPolygon();
		a.append(p, 5);
		a.append(q, 5);
		a.append(r, 5);
		a.append(s, 5);
		hp.append(h);
		//hp.append(a);
		new RrGraphics(hp, false);  
		
	}
}
