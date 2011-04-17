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
 * Numeric coordinates.
 * <p>A numeric coordinates value is known at "compile time"
 *
 */
public final class NumericCoordinate extends Coordinate {
	private final EnumMap<Axis, Double> axes;

	private NumericCoordinate(EnumMap<Axis, Double> axes) {
		this.axes = axes;
	}
	
	private NumericCoordinate(NumericCoordinate copy) {
		this();
		axes.putAll(copy.axes);
	}
	
	/**
	 * The default constructor.
	 * All axises are undefined.
	 */
	public NumericCoordinate() {
		this.axes = new EnumMap<Axis, Double>(Axis.class);
	}
	
	/**
	 * A constructor with defaults for x,y and z axises.
	 * @param x
	 * @param y
	 * @param z
	 */
	public NumericCoordinate(Double x, Double y, Double z) {
		this();
		if(x!=null)
			axes.put(Axis.X, x);
		if(y!=null)
			axes.put(Axis.Y, y);
		if(z!=null)
			axes.put(Axis.Z, z);
	}
	
	/**
	 * A constructor with defaults for x, y and z axes.
	 * <p>The numbers must be either null or a valid double.
	 * @param x
	 * @param y
	 * @param z
	 */
	public NumericCoordinate(String x, String y, String z) {
		this();
		if(x!=null)
			axes.put(Axis.X, Double.valueOf(x));
		if(y!=null)
			axes.put(Axis.X, Double.valueOf(y));
		if(z!=null)
			axes.put(Axis.X, Double.valueOf(z));
	}
	
	public NumericCoordinate copy() {
		return new NumericCoordinate(this);
	}
	
	/**
	 * Get the distance to another coordinate.
	 * <p>Only X, Y and Z values are used in this calculation.
	 * @param to
	 * @return
	 */
	public double distance(NumericCoordinate to) {
		double dx = getValue(Axis.X, 0.0) - to.getValue(Axis.X, 0.0);
		double dy = getValue(Axis.Y, 0.0) - to.getValue(Axis.Y, 0.0);
		double dz = getValue(Axis.Z, 0.0) - to.getValue(Axis.Z, 0.0);
		return Math.sqrt(dx*dx + dy*dy + dz*dz);
	}
	
	/**
	 * Get the numeric value for the axis
	 * @param axis
	 * @return value or null if not defined
	 */
	public final Double getValue(Axis axis) {
		return axes.get(axis);
	}
	
	/**
	 * Get the numeruc value for the axis
	 * @param axis
	 * @param def default value if axis is not defined
	 * @return value or default
	 */
	public final Double getValue(Axis axis, double def) {
		Double d = axes.get(axis);
		return d!=null ? d : def;
	}
	
	public String get(Axis axis) {
		Double d = axes.get(axis);
		if(d==null)
			return null;
		// normalize
		if(Math.abs(d) < 0.001)
			d = 0.0;
		return String.format("%.3f", d);
	}
	
	public boolean isDefined(Axis a) {
		return axes.get(a) != null;
	}
	
	public void set(Axis a, Double value) {
		if(value==null)
			axes.remove(a);
		else
			axes.put(a, value);
	}
	
	public void set(String a, Double value) {
		set(Axis.valueOf(a.toUpperCase()), value);
	}
	
	public NumericCoordinate undefined(Axis a) {
		EnumMap<Axis, Double> axes = new EnumMap<Axis, Double>(Axis.class);
		for(Map.Entry<Axis, Double> e : this.axes.entrySet())
			if(e.getKey() != a)
				axes.put(e.getKey(), e.getValue());
		return new NumericCoordinate(axes);
	}

	public NumericCoordinate abs() {
		EnumMap<Axis, Double> axes = new EnumMap<Axis, Double>(Axis.class);
		for(Axis a : this.axes.keySet())
			axes.put(a, Math.abs(this.axes.get(a)));
		return new NumericCoordinate(axes);
	}

	public Coordinate offset(Coordinate offset, boolean invert, boolean override) {
		if(offset instanceof NumericCoordinate) {
			NumericCoordinate o = (NumericCoordinate)offset;
			EnumMap<Axis, Double> c = new EnumMap<Axis, Double>(Axis.class);
			for(Axis a : Axis.values()) {
				Double val = axes.get(a);
				Double oVal = o.axes.get(a);
				if(val!=null) {
					if(oVal!=null) {
						if(invert)
							val -= oVal;
						else
							val += oVal;
					}
					c.put(a, val);
				} else if(override && oVal!=null) {
					if(invert)
						c.put(a, -oVal);
					else
						c.put(a, oVal);
				}
			}
			return new NumericCoordinate(c);
		}

		// If the other coordinate set is symbolic,
		// the result will be too.
		return toSymbolic().offset(offset, invert, override);
	}

	/**
	 * Return a copy of this coordinate set converted
	 * to symbolic coordinates.
	 * @return this set as symbolic coordinates
	 */
	public SymbolicCoordinate toSymbolic() {
		EnumMap<Axis, String> axes = new EnumMap<Axis, String>(Axis.class);
		for(Axis a : this.axes.keySet())
			axes.put(a, get(a));
		return new SymbolicCoordinate(axes);
	}

	protected Coordinate scaleCoordinate(Coordinate scale) {
		if(scale instanceof SymbolicCoordinate)
			return toSymbolic().scaleCoordinate(scale);
		
		EnumMap<Axis, Double> axes = new EnumMap<Axis, Double>(this.axes);
		for(Axis a : this.axes.keySet()) {
			if(scale.isDefined(a))
				axes.put(a, axes.get(a) * ((NumericCoordinate)scale).getValue(a));
		}
		return new NumericCoordinate(axes);
	}

	protected NumericCoordinate scaleNumeric(double scale) {
		EnumMap<Axis, Double> axes = new EnumMap<Axis, Double>(Axis.class);
		for(Axis a : this.axes.keySet())
			axes.put(a, this.axes.get(a) * scale);
		return new NumericCoordinate(axes);
	}

	protected Coordinate scaleSymbolic(String scale) {
		return toSymbolic().scaleSymbolic(scale);
	}

	public Coordinate rotate(Coordinate angle) {
		if(angle instanceof SymbolicCoordinate)
			return toSymbolic().rotate(angle);
		NumericCoordinate a = (NumericCoordinate)angle;
		
		NumericCoordinate rotated = new NumericCoordinate(this);
		
		if(a.isDefined(Axis.X)) {
			rotate(rotated, Axis.Y, Axis.Z, a.getValue(Axis.X));
			// TODO support rotations in other than XY plane
			//if(rotated.isDefined(Axis.J) || rotated.isDefined(Axis.K))
			//	rotate(rotated, Axis.J, Axis.K, a.getValue(Axis.X));
		}
		
		if(a.isDefined(Axis.Y)) {
			rotate(rotated, Axis.X, Axis.Z, a.getValue(Axis.Y));
			// TODO support rotations in other than XY plane
			//if(rotated.isDefined(Axis.I) || rotated.isDefined(Axis.K))
			//	rotate(rotated, Axis.I, Axis.K, a.getValue(Axis.Y));
		}
		
		if(a.isDefined(Axis.Z)) {
			rotate(rotated, Axis.X, Axis.Y, a.getValue(Axis.Z));
			if(rotated.isDefined(Axis.I) || rotated.isDefined(Axis.J))
				rotate(rotated, Axis.I, Axis.J, a.getValue(Axis.Z));
		}
		
		return rotated;
	}
	
	static private void rotate(NumericCoordinate c, Axis a, Axis b, Double theta) {
		theta = Math.toRadians(theta);
		double st = Math.sin(theta);
		double ct = Math.cos(theta);
		
		Double x = c.getValue(a);
		Double y = c.getValue(b);
		if(x==null)
			x = 0.0;
		if(y==null)
			y = 0.0;
		c.set(a, x * ct - y * st);
		c.set(b, x * st + y * ct);
	}

	@Override
	public Coordinate fillIn(Coordinate c) {
		if(c instanceof SymbolicCoordinate)
			return toSymbolic().fillIn(c);
		NumericCoordinate cc = (NumericCoordinate)c;
		NumericCoordinate nc = new NumericCoordinate(this);
		
		for(Axis a : Axis.XYZ)
			if(nc.axes.get(a)==null && cc.axes.get(a)!=null)
				nc.axes.put(a, cc.axes.get(a));
		
		return nc;
	}
}