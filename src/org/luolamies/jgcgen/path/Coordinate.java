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
import java.util.Stack;
import java.util.regex.Pattern;

public abstract class Coordinate {
	static private final Pattern isnumeric = Pattern.compile("-?\\d+(?:\\.\\d+)?");
	/**
	 * Extract coordinates from a g-code fragment.
	 * Coordinates can be in forms like:
	 * <ul>
	 * <li>A0.0
	 * <li>A[0.0]
	 * <li>A#1
	 * <li>A#&lt;_var&gt;
	 * <li>A[#1+1.0]
	 * <li>A[sin[#0]+#3]
	 * <li>A-[-1]
	 * </ul>
	 * @param gcode
	 */
	static public Coordinate parse(String gcode) {
		if(gcode.length()==0)
			return new NumericCoordinate();
		
		EnumMap<Axis, String> coords = new EnumMap<Axis, String>(Axis.class);
		Axis axis = null;
		StringBuilder cb = new StringBuilder();
		
		/* states:
		 * 0 - expect variable or number or block start ([)
		 * 1 - expect variable (numeric or named)
		 * 2 - expect end of numeric constant
		 * 3 - expect end of named variable (>)
		 */
		Stack<Integer> states = new Stack<Integer>();
		
		int i=-1;
		while(++i<gcode.length()) {
			char chr = gcode.charAt(i);
			if(Character.isWhitespace(chr))
				continue;
			
			if(states.isEmpty()) {
				if(axis!=null) {
					char last = cb.charAt(cb.length()-1); 
					if(last!=']' && last!='>')
						cb.delete(cb.length()-1, cb.length());
					coords.put(axis, cb.toString());
					cb.delete(0, cb.length());
				}
				// Expect axis
				Axis a = Axis.get(chr);
				if(a!=null)
					axis = a;
				else
					throw new IllegalArgumentException(gcode + " (" + chr + "): Expected axis");
				states.push(0);
			} else {
				cb.append(chr);
				int state = states.peek();
				switch(state) {
				case 0:
					states.pop();
					// axis value start
					if(chr=='[')
						states.push(4);
					else if(chr=='#')
						states.push(1);
					else if(Character.isDigit(chr) || chr=='-')
						states.push(2);
					else
						throw new IllegalArgumentException(chr + ": Expected number, variable or parenthesis");
					break;
				case 1:
					// Expect variable start
					states.pop();
					if(chr=='<')
						states.push(3);
					else if(Character.isDigit(chr))
						states.push(2);
					else
						throw new IllegalArgumentException("Expected numeric constant or named variable");
					break;
				case 2:
					// Expect end of numeric constant
					if(!(chr=='-' || chr=='.' || Character.isDigit(chr))) {
						states.pop();
						// - sign can come before [ too
						if(chr=='[' && gcode.charAt(i-1)=='-')
							states.push(4);
						else
							--i;
					}
					break;
				case 3:
					// Expect end of named variable
					if(chr=='>') {
						states.pop();
					}
					break;
				case 4:
					// Expect end of parenthetical block (])
					if(chr=='[')
						states.push(4);
					else if(chr==']') {
						states.pop();
					}
					break;
				default: throw new RuntimeException("BUG: Unhandled state " + state);
				}
			}
		}
		
		// Add the last axis
		coords.put(axis, cb.toString());

		boolean allnumeric = true;
		for(String s : coords.values()) {
			if(!isnumeric.matcher(s).matches()) {
				allnumeric = false;
				break;
			}
		}
		
		if(allnumeric) {
			NumericCoordinate c = new NumericCoordinate();
			for(Map.Entry<Axis, String> a : coords.entrySet())
				c.set(a.getKey(), Double.valueOf(a.getValue()));
			return c;
		} else
			return new SymbolicCoordinate(coords);
	}
	
	/**
	 * Construct a commonly used 3 axis coordinate set.
	 * <p>If only numeric values are used, a NumericCoordinate
	 * will be constructed
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	static public Coordinate xyz(String x, String y, String z) {
		if(
			isnumeric.matcher(x).matches() &&
			isnumeric.matcher(y).matches() &&
			isnumeric.matcher(z).matches()
			)
			return new NumericCoordinate(x, y, z);
		else
			return new SymbolicCoordinate(x, y, z);
	}
	
	/**
	 * Get the coordinate for the given axis.
	 * If this the coordinate is symbolic, it will be wrapped in "[]"
	 * @param axis
	 * @return coordinate string or null if not defined for this axis
	 */
	public abstract String get(Axis axis);
	
	/**
	 * Is a coordinate defined for the given axis?
	 * @param axis axis to check
	 * @return true if a value is set
	 */
	public abstract boolean isDefined(Axis axis);
	
	/**
	 * Return a copy of this coordinate with the given axis undefined.
	 * @param axis axis to undefine
	 * @return copy of this with axis value set to null
	 */
	public abstract Coordinate undefined(Axis axis);
	
	/**
	 * Shorthand for <code>get(Axis.valueOf(axis))</code>
	 * @param axis
	 * @return
	 */
	public final String get(String axis) {
		return get(Axis.valueOf(axis.toUpperCase()));
	}
	
	/**
	 * Get a copy of this coordinate set with an added offset.
	 * If either this or the offset is symbolic, the returned
	 * coordinates will be symbolic also.
	 * <p>Offsets, even if defined, are not added to axes that are
	 * undefined in this coordinate set.
	 * @param offset offset
	 * @param invert subtract offset instead of adding
	 * @param override offset even undefined axes
	 * @return this -/+ offset
	 */
	public abstract Coordinate offset(Coordinate offset, boolean invert, boolean override);
	
	/**
	 * Get a copy of this coordinate set with an added offset.
	 * If either this or the offset is symbolic, the returned
	 * coordinates will be symbolic also.
	 * <p>Offsets, even if defined, are not added to axes that are
	 * undefined in this coordinate set.
	 * @param offset
	 * @return this + offset
	 */
	public final Coordinate offset(Coordinate offset) {
		return offset(offset, false, false);
	}
	
	/**
	 * Get a copy of this coordinate with the unset axes set from the
	 * given coordinate.
	 * @param c
	 */
	public abstract Coordinate fillIn(Coordinate c);
	
	/**
	 * Return a copy of this coordinate with a positive sign.
	 * @return abs(this)
	 */
	public abstract Coordinate abs();
	
	protected abstract Coordinate scaleNumeric(double scale);
	protected abstract Coordinate scaleSymbolic(String scale);
	protected abstract Coordinate scaleCoordinate(Coordinate scale);
	
	/**
	 * Get a copy of this coordinate with the axes multiplied by the
	 * scale values.
	 * <p>The scale value can be a number, or an expression (e.g. 2.0 or #1),
	 * in which case each axis is multiplied by that value.
	 * <p>Multiple axes can also be defined. E.g. "x2.0 y1.5" or "x#1 y[#2-1.0]",
	 * in which case only the defined axes will be scaled.
	 * @param scale
	 * @return
	 */
	public final Coordinate scale(String scale) {
		// First, identify what sort of scale we are dealing with
		scale = scale.trim();
		int i=0;
		while(scale.charAt(i)=='-') ++i;
		// Starts with a digit: This is a numeric scale to all axes
		if(Character.isDigit(scale.charAt(i)))
			return scaleNumeric(Double.parseDouble(scale));
		
		// A variable reference or an expression. This is a symbolic
		// scale to all axes
		if(scale.charAt(i)=='#' || scale.charAt(i)=='[')
			return scaleSymbolic(scale);
		
		// Otherwise this must be a coordinate scale
		Coordinate coord = Coordinate.parse(scale);
		
		return scaleCoordinate(coord);
	}
	
	/**
	 * Get a copy of this coordinate with each axis multiplied by the
	 * scale value.
	 * @param scale
	 * @return
	 */
	public final Coordinate scale(double scale) {
		return scaleNumeric(scale);
	}
	
	/**
	 * Rotate the coordinate around origin (0).
	 * @param angle angles around axes to rotate
	 * @return rotated coordinate
	 */
	public abstract Coordinate rotate(Coordinate angle);
	
	/**
	 * Convert this coordinate set to G-code.
	 * E.g. If X and Y coordinates are set, this might produce
	 * <kbd>X10.20 Y4.01</kbd>
	 * @return coordinates
	 */
	public String toGcode() {
		StringBuilder sb = new StringBuilder();
		boolean first=true;
		for(Axis a : Axis.values()) {
			String val = get(a);
			if(val!=null) {
				if(!first)
					sb.append(' ');
				else
					first = false;
				sb.append(a.toString());
				sb.append(val);
			}
		}
		return sb.toString();
	}

	/**
	 * Return a copy of this coordinate set.
	 * @return copy of this
	 */
	public abstract Coordinate copy();
	
	public String toString() {
		return toGcode();
	}
}
