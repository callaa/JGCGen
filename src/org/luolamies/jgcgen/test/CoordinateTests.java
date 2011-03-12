/*
 * This file is part of JGCGen.
 *
 * JGCGen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JGCGen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGCGen.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.luolamies.jgcgen.test;

import org.junit.Test;
import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.Coordinate;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.SymbolicCoordinate;

import static org.junit.Assert.*;

public class CoordinateTests {

	@Test public void TestNumeric() {
		NumericCoordinate test = new NumericCoordinate();
		
		test.set(Axis.X, 10.0);
		test.set("Y", 5.0);
		
		assertEquals(10.0, test.getValue(Axis.X), 0);
		assertEquals(5.0, test.getValue(Axis.Y), 0);
		assertNull(test.get(Axis.Z));
		
		assertEquals("X10.000 Y5.000", test.toGcode());
	}
	
	@Test public void testSymbolic() {
		SymbolicCoordinate test = new SymbolicCoordinate();
		test.set(Axis.X, "[10+#1]");
		test.set(Axis.Y, "[5+#2]");
		test.set(Axis.Z, "#3");
		
		assertEquals("[10+#1]", test.get(Axis.X));
		assertEquals("[5+#2]", test.get(Axis.Y));
		assertEquals("#3", test.get(Axis.Z));
		assertNull(test.get(Axis.A));
		
		assertEquals("X[10+#1] Y[5+#2] Z#3", test.toGcode());
	}
	
	@Test public void testNumericOffset() {
		NumericCoordinate c1 = new NumericCoordinate();
		NumericCoordinate c2 = new NumericCoordinate();
		c1.set(Axis.X, 10.0);
		c1.set(Axis.Y, 5.0);
		
		c2.set(Axis.X, 5.0);
		c2.set(Axis.Z, 5.0);
		
		NumericCoordinate c3 = (NumericCoordinate)c1.offset(c2);
		assertEquals(15.0, c3.getValue(Axis.X), 0);
		assertEquals(5.0, c3.getValue(Axis.Y), 0);
		assertNull(c3.get(Axis.Z));
	}
	
	@Test public void testSymbolicOffset() {
		SymbolicCoordinate c1 = new SymbolicCoordinate();
		SymbolicCoordinate c2 = new SymbolicCoordinate();
		
		c1.set(Axis.X, "[10+#1]");
		c1.set(Axis.Y, "[5+#2]");
		c1.set(Axis.Z, "0");
		
		c2.set(Axis.X, "#3");
		c2.set(Axis.Z, "#<_safe>");
		c2.set(Axis.A, "[0]");
		
		SymbolicCoordinate c3 = (SymbolicCoordinate)c1.offset(c2);
		assertEquals("[[10+#1]+#3]", c3.get(Axis.X));
		assertEquals("[5+#2]", c3.get(Axis.Y));
		assertEquals("[0+#<_safe>]", c3.get(Axis.Z));
		assertNull(c3.get(Axis.A));
	}
	
	@Test public void testMixedOffset() {
		NumericCoordinate c1 = new NumericCoordinate();
		SymbolicCoordinate c2 = new SymbolicCoordinate();
		
		c1.set(Axis.X, 10.0);
		c1.set(Axis.Y, 5.0);
		
		c2.set(Axis.X, "#1");
		c2.set(Axis.Y, "#<_safe>");
		
		SymbolicCoordinate c3 = (SymbolicCoordinate)c1.offset(c2);
		assertEquals("[10.000+#1]", c3.get(Axis.X));
		assertEquals("[5.000+#<_safe>]", c3.get(Axis.Y));
		assertNull(c3.get(Axis.Z));
	}
	
	@Test public void testNumericScale() {
		NumericCoordinate c1 = new NumericCoordinate(2.0, 3.0, 4.0);
		c1 = (NumericCoordinate)c1.scale(2.0);
		assertEquals(4.0, c1.getValue(Axis.X), 0);
		assertEquals(6.0, c1.getValue(Axis.Y), 0);
		assertEquals(8.0, c1.getValue(Axis.Z), 0);
		assertFalse(c1.isDefined(Axis.A));
		
		c1 = (NumericCoordinate)c1.scale("0.5");
		assertEquals(2.0, c1.getValue(Axis.X), 0);
		assertEquals(3.0, c1.getValue(Axis.Y), 0);
		assertEquals(4.0, c1.getValue(Axis.Z), 0);
		
		c1 = (NumericCoordinate)c1.scale("x2 y0.5");
		assertEquals(4.0, c1.getValue(Axis.X), 0);
		assertEquals(1.5, c1.getValue(Axis.Y), 0);
		assertEquals(4.0, c1.getValue(Axis.Z), 0);
	}
	
	@Test public void testSymbolicScale() {
		SymbolicCoordinate c1 = new SymbolicCoordinate("2", "3", "4");
		SymbolicCoordinate t1 = (SymbolicCoordinate) c1.scale(2.0);
		assertEquals("[[2]*2.000]", t1.get(Axis.X));
		assertEquals("[[3]*2.000]", t1.get(Axis.Y));
		assertEquals("[[4]*2.000]", t1.get(Axis.Z));
		
		SymbolicCoordinate t2 = (SymbolicCoordinate) c1.scale("0.5");
		assertEquals("[[2]*0.500]", t2.get(Axis.X));
		assertEquals("[[3]*0.500]", t2.get(Axis.Y));
		assertEquals("[[4]*0.500]", t2.get(Axis.Z));
		
		SymbolicCoordinate t3 = (SymbolicCoordinate) c1.scale("x2y3");
		assertEquals("[[2]*2.000]", t3.get(Axis.X));
		assertEquals("[[3]*3.000]", t3.get(Axis.Y));
		assertEquals("4", t3.get(Axis.Z));
		
		SymbolicCoordinate t4 = (SymbolicCoordinate) c1.scale("x#1y#2");
		assertEquals("[[2]*#1]", t4.get(Axis.X));
		assertEquals("[[3]*#2]", t4.get(Axis.Y));
		assertEquals("4", t4.get(Axis.Z));
		
		SymbolicCoordinate t5 = (SymbolicCoordinate) c1.scale("#1");
		assertEquals("[[2]*#1]", t5.get(Axis.X));
		assertEquals("[[3]*#1]", t5.get(Axis.Y));
		assertEquals("[[4]*#1]", t5.get(Axis.Z));
	}
	
	@Test public void testMixedScale() {
		NumericCoordinate c1 = new NumericCoordinate(2.0, 3.0, 4.0);
		SymbolicCoordinate c2 = new SymbolicCoordinate("#2", "#3", "#4");
		
		Coordinate t1 = c1.scale("#1");
		assertEquals("[[2.000]*#1]", t1.get(Axis.X));
		assertEquals("[[3.000]*#1]", t1.get(Axis.Y));
		assertEquals("[[4.000]*#1]", t1.get(Axis.Z));
		
		Coordinate t2 = c1.scale(c2.toGcode());
		assertEquals("[[2.000]*#2]", t2.get(Axis.X));
		assertEquals("[[3.000]*#3]", t2.get(Axis.Y));
		assertEquals("[[4.000]*#4]", t2.get(Axis.Z));
	}
	
	@Test public void testParse() {
		NumericCoordinate c1 = (NumericCoordinate) Coordinate.parse("X0.00 y1.1 z-3");
		assertEquals(0.0, c1.getValue(Axis.X), 0);
		assertEquals(1.1, c1.getValue(Axis.Y), 0);
		assertEquals(-3.0, c1.getValue(Axis.Z), 0);
		
		SymbolicCoordinate c2 = (SymbolicCoordinate) Coordinate.parse("X#1 y[[sin[#1]*2]+1.1] Z [-3*#<_scale>] A#<_rotate> B-[-10]");
		assertEquals("#1", c2.get(Axis.X));
		assertEquals("[[sin[#1]*2]+1.1]", c2.get(Axis.Y));
		assertEquals("[-3*#<_scale>]", c2.get(Axis.Z));
		assertEquals("#<_rotate>", c2.get(Axis.A));
		assertEquals("-[-10]", c2.get(Axis.B));
	}
}
