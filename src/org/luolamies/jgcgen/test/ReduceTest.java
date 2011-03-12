package org.luolamies.jgcgen.test;

import org.junit.Test;
import org.luolamies.jgcgen.path.Path;

import static org.junit.Assert.*;

/**
 * Path reduction tests
 */
public class ReduceTest {
	/** test with a square pattern */
	public @Test void testReduceSimple() {
		Path p = new Path();
		p.addSegment("move", "x0y0z0");
		p.addSegment("line", "x1y0z0"); p.addSegment("line", "x2y0z0"); p.addSegment("line", "x3y0z0");
		p.addSegment("line", "x3y1z0"); p.addSegment("line", "x3y2z0"); p.addSegment("line", "x3y3z0");
		p.addSegment("line", "x2y3z0"); p.addSegment("line", "x1y3z0"); p.addSegment("line", "x0y3z0");
		p.addSegment("line", "x0y2z0"); p.addSegment("line", "x0y1z0"); p.addSegment("line", "x0y0z0");
		
		Path simple = new Path();
		simple.addSegment("move", "x0y0z0");
		simple.addSegment("line", "x3y0z0");
		simple.addSegment("line", "x3y3z0");
		simple.addSegment("line", "x0y3z0");
		simple.addSegment("line", "x0y0z0");
		
		p = p.reduce();
		p.dump();
		
		checkPath(simple, p);
	}
	
	/** Test with a zig zag pattern */
	public @Test void testReduceSimple2() {
		Path p = new Path();
		p.addSegment("move", "x0y0z0");
		p.addSegment("line", "x1y0z0"); p.addSegment("line", "x2y0z0"); p.addSegment("line", "x3y0z0");
		p.addSegment("line", "x3y1z0");
		p.addSegment("line", "x2y1z0"); p.addSegment("line", "x1y1z0"); p.addSegment("line", "x0y1z0");
		p.addSegment("line", "x0y2z0");
		p.addSegment("line", "x1y2z0"); p.addSegment("line", "x2y2z0"); p.addSegment("line", "x3y2z0");
		
		Path simple = new Path();
		simple.addSegment("move", "x0y0z0");
		simple.addSegment("line", "x3y0z0");
		simple.addSegment("line", "x3y1z0");
		simple.addSegment("line", "x0y1z0");
		simple.addSegment("line", "x0y2z0");
		simple.addSegment("line", "x3y2z0");
		
		p = p.reduce();
		p.dump();
		
		checkPath(simple, p);
	}
	
	public @Test void testReducePartial() {
		Path p = new Path();
		p.addSegment("move", "x0y0z0");
		p.addSegment("line", "x1"); p.addSegment("line", "x2"); p.addSegment("line", "x3");
		p.addSegment("line", "y1"); p.addSegment("line", "y2"); p.addSegment("line", "y3");
		p.addSegment("line", "x2"); p.addSegment("line", "x1"); p.addSegment("line", "x0");
		p.addSegment("line", "y2"); p.addSegment("line", "y1"); p.addSegment("line", "y0");
		
		Path simple = new Path();
		simple.addSegment("move", "x0y0z0");
		simple.addSegment("line", "x3");
		simple.addSegment("line", "y3");
		simple.addSegment("line", "x0");
		simple.addSegment("line", "y0");
		
		p = p.reduce();
				
		checkPath(simple, p);
	}
	
	private void checkPath(Path expected, Path path) {
		assertEquals(expected.getSize(), path.getSize());
		for(int i=0;i<expected.getSize();++i) {
			assertEquals(expected.getSegments().get(i).toString(), path.getSegments().get(i).toString());
		}
	}
}
