package org.luolamies.jgcgen.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.luolamies.jgcgen.tools.Ballnose;

public class TestTool {
	/**
	 * The optimized version is less accurate. Make sure it stays within some tolerances.
	 */
	@Test public void TestBallnoseOptimization() {
		final double radius = 6.5;
		Ballnose tool = new Ballnose(radius*2);
		
		for(double d=0;d<radius;d+=0.05) {
			double expected = radius-Math.sin(Math.acos(d/radius))*radius;
			assertEquals(expected, tool.getProfile(d*d), 0.05);
		}
	}
}
