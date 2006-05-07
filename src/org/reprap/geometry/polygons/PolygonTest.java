package org.reprap.geometry.polygons;

import org.testng.Assert;

public class PolygonTest {
		
	/**
	 * @testng.test groups = "geometry,all,all-offline"
	 */
	public void testPoint() {
		Rr2Point testPoint = new Rr2Point(10, 5);
		Assert.assertEquals(testPoint.x(), 10.0, 0.0, "x");		
		Assert.assertEquals(testPoint.y(), 5.0, 0.0, "y");
		
		Rr2Point negPoint = testPoint.neg();
		Assert.assertEquals(negPoint.x(), -10.0, 0.0, "neg x");
		Assert.assertEquals(negPoint.y(), -5.0, 0.0, "neg x");
	}	
	
}
