package org.luolamies.jgcgen.test;

import java.util.Iterator;

import org.junit.Test;
import org.luolamies.jgcgen.math.Range;

import static org.junit.Assert.*;

public class TestMathRange {	
	@Test public void TestPositiveRange() {
		Range range = new Range(0, 10, 0.5);
		int pos=0;
		for(Iterator<Double> i = range.iterator();i.hasNext();++pos) {
			Double d = i.next();
			assertEquals(pos/2.0, d, 0.001);
			if(pos==19)
				assertFalse(i.hasNext());
		}
	}
	
	@Test public void TestNegativeRange() {
		Range range = new Range(10, 0, 1);
		int pos=10;
		for(Iterator<Double> i = range.iterator();i.hasNext();--pos) {
			Double d = i.next();
			assertEquals(pos, d, 0);
			if(pos==1)
				assertFalse(i.hasNext());
		}
	}
}
