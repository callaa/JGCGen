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
package org.luolamies.jgcgen.path;

import java.util.EnumMap;
import java.util.Map;

/**
 * Symbolic coordinates.
 * <p>A symbolic coordinate can contain variable references and arithmetic
 * expressions. The final value of a symbolic coordinate is
 * calculated when the g-code is executed and therefore cannot be easily
 * manipulated in the preprocessor.
 *
 */
public final class SymbolicCoordinate extends Coordinate {
	private EnumMap<Axis, String> axes;
	
	SymbolicCoordinate(EnumMap<Axis,String> axes) {
		this.axes = axes;
	}
	
	private SymbolicCoordinate(SymbolicCoordinate copy) {
		this();
		axes.putAll(copy.axes);
	}
	
	public SymbolicCoordinate() {
		axes = new EnumMap<Axis, String>(Axis.class);
	}
	
	public SymbolicCoordinate(String x, String y, String z) {
		this();
		if(x!=null)
			axes.put(Axis.X, x);
		if(y!=null)
			axes.put(Axis.Y, y);
		if(z!=null)
			axes.put(Axis.Z, z);
	}
	
	public SymbolicCoordinate copy() {
		return new SymbolicCoordinate(this);
	}
	public void set(Axis a, String value) {
		if(value==null)
			axes.remove(a);
		else {
			value = value.trim();
			if(value.length()==0)
				throw new IllegalArgumentException("Axis coordinate cannot be empty!");

			axes.put(a, value);
		}
	}
	
	public String get(Axis axis) {
		return axes.get(axis);
	}

	public boolean isDefined(Axis axis) {
		return axes.get(axis)!=null;
	}
	
	public SymbolicCoordinate abs() {
		EnumMap<Axis, String> axes = new EnumMap<Axis, String>(Axis.class);
		for(Axis a : this.axes.keySet()) {
			String c = this.axes.get(a);
			if(c.charAt(0)=='-')
				c = c.substring(1);
			else
				c = "[abs[" + c + "]]";
			axes.put(a, c);
		}
		return new SymbolicCoordinate(axes);
	}
	
	public Coordinate offset(Coordinate offset, boolean invert, boolean override) {
		EnumMap<Axis, String> c = new EnumMap<Axis, String>(Axis.class);
		
		for(Axis a : Axis.values()) {
			String v1 = get(a);
			String v2 = offset.get(a);
			String v3;
			if(v1!=null) {
				if(v2!=null) {
					// TODO simplify the coordinate expression
					v3 = "[" + v1 + (invert ? "-" : "+") + v2 + "]";
				} else
					v3 = v1;
			} else if(override && v2!=null) {
				if(invert)
					v3 = "-[" + v2 + "]";
				else
					v3 = v2;
			} else
				continue;
			c.put(a, v3);
		}
		
		return new SymbolicCoordinate(c);
	}

	public SymbolicCoordinate undefined(Axis a) {
		EnumMap<Axis, String> axes = new EnumMap<Axis, String>(Axis.class);
		for(Map.Entry<Axis, String> e : this.axes.entrySet())
			if(e.getKey() != a)
				axes.put(e.getKey(), e.getValue());
		return new SymbolicCoordinate(axes);
	}

	protected SymbolicCoordinate scaleCoordinate(Coordinate scale) {
		EnumMap<Axis, String> c = new EnumMap<Axis, String>(Axis.class);
		
		for(Axis a : axes.keySet()) {
			if(scale.isDefined(a))
				c.put(a, "[[" + axes.get(a) + "]*" + scale.get(a) + ']');
			else
				c.put(a, axes.get(a));
		}
		
		return new SymbolicCoordinate(c);
	}

	protected SymbolicCoordinate scaleNumeric(double scale) {
		return scaleSymbolic(String.format("%.3f", scale));
	}

	protected SymbolicCoordinate scaleSymbolic(String scale) {
		EnumMap<Axis, String> c = new EnumMap<Axis, String>(Axis.class);
		
		for(Axis a : axes.keySet())
			c.put(a, "[[" + axes.get(a) + "]*" + scale + ']');
		return new SymbolicCoordinate(c);
	}

	public SymbolicCoordinate rotate(Coordinate angle) {
		SymbolicCoordinate rotated = new SymbolicCoordinate(this);
		
		if(angle.isDefined(Axis.X)) {
			rotate(rotated, Axis.Y, Axis.Z, angle.get(Axis.X));
			// TODO support rotations in other than XY plane
			//if(rotated.isDefined(Axis.J) || rotated.isDefined(Axis.K))
			//	rotate(rotated, Axis.J, Axis.K, a.get(Axis.X));
		}
		
		if(angle.isDefined(Axis.Y)) {
			rotate(rotated, Axis.X, Axis.Z, angle.get(Axis.Y));
			// TODO support rotations in other than XY plane
			//if(rotated.isDefined(Axis.I) || rotated.isDefined(Axis.K))
			//	rotate(rotated, Axis.I, Axis.K, a.get(Axis.Y));
		}
		
		if(angle.isDefined(Axis.Z)) {
			rotate(rotated, Axis.X, Axis.Y, angle.get(Axis.Z));
			if(rotated.isDefined(Axis.I) || rotated.isDefined(Axis.J))
				rotate(rotated, Axis.I, Axis.J, angle.get(Axis.Z));
		}
		
		return rotated;
	}
	
	static private void rotate(SymbolicCoordinate c, Axis a, Axis b, String theta) {
		String x = c.get(a);
		if(x==null)
			x = "0";
		String y = c.get(b);
		if(y==null)
			y = "0";
		
		c.set(a, "[[" + x + "] * COS[" + theta + "] - [" + y + "] * SIN[" + theta + "]]");
		c.set(b, "[[" + x + "] * SIN[" + theta + "] + [" + y + "] * COS[" + theta + "]]");
	}

	@Override
	public SymbolicCoordinate fillIn(Coordinate c) {
		SymbolicCoordinate nc = new SymbolicCoordinate(this);
		
		for(Axis a : Axis.XYZ)
			if(nc.axes.get(a)==null && c.get(a)!=null)
				nc.axes.put(a, c.get(a));
		
		return nc;
		
	}

}
