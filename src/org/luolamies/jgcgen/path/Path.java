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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.luolamies.jgcgen.RenderException;

/**
 * A toolpath.
 * <p>Paths can be divided into two types: numeric or symbolic. A numeric path contains
 * only numeric coordinates, while a symbolic path may contain a mixture of numeric and symbolic coordinates.
 * Some path manipulation functions can only be used on numeric paths. 
 */
public class Path implements PathGenerator {
	/** Segment types */
	public enum SType {
		/** A marker for splitting the path */
		SEAM("---"),
		/** Drill down at this point */
		POINT("???"),
		/** Move in a straight line */
		LINE("G01"),
		/** Clockwise arc */
		CWARC("G02"),
		/** Counter clockwise arc */
		CCWARC("G03"),
		/** Rapid to location (usually through safe height) */
		MOVE("G00")
		;
		public final String gcode;
		SType(String gc) { gcode = gc; }
	}
	
	/**
	 * Path segment
	 */
	static public class Segment {
		public Segment(SType type, Coordinate point) {
			this.type = type;
			this.point = point;
		}
		public final SType type;
		public final Coordinate point;
		
		@Override
		public String toString() {
			return type.name() + " " + point;
		}
	}
	
	private List<Segment> segments = new ArrayList<Segment>();
	
	/**
	 * Add a new segment
	 * @param type segment type
	 * @param point segment coordinates
	 */
	public void addSegment(SType type, Coordinate point) {
		segments.add(new Segment(type, point));
	}
	
	/**
	 * Template friendly addSegment.
	 * @param type segment type
	 * @param point gcode coordinates
	 */
	public void addSegment(String type, String point) {
		segments.add(new Segment(
				SType.valueOf(type.toUpperCase()),
				Coordinate.parse(point)
				));
	}
	
	/**
	 * Add another path to this path. A seam is automaticallý added to the intersection.
	 * @param path path to add
	 */
	public void addPath(Path path) {
		if(path==null || path.getSize()==0)
			return;
		
		boolean addseam = !segments.isEmpty() &&
			segments.get(segments.size()-1).type != SType.SEAM &&
			path.segments.get(0).type!=SType.SEAM;
		if(addseam)
			segments.add(new Segment(SType.SEAM, null));
		segments.addAll(path.segments);
	}
	
	public List<Segment> getSegments() {
		return Collections.unmodifiableList(segments);
	}
	
	/**
	 * Split this path at the seams.
	 * @return new paths
	 */
	public List<Path> splitAtSeams() {
		List<Path> subpaths = new ArrayList<Path>();
		Path sp = new Path();
		for(Segment s : segments) {
			if(s.type==SType.SEAM) {
				subpaths.add(sp);
				sp = new Path();
			} else
				sp.segments.add(s);
		}
		subpaths.add(sp);
		return subpaths;
	}
	
	/**
	 * Get the size of the path
	 * @return path segment count
	 */
	public int getSize() {
		return segments.size();
	}
	
	/**
	 * Offset this path by a coordinate
	 * @param offset
	 * @return copy of this path with the offset
	 */
	public Path offset(Coordinate offset) {
		Path op = new Path();
		for(Segment s : segments) {
			if(s.point!=null)
				op.segments.add(new Segment(s.type, s.point.offset(offset)));
			else
				op.segments.add(s);
		}
		return op;
	}
	
	/**
	 * Offset this path by a coordinate
	 * @param offset offset in g-code 
	 * @return copy of this path with the offset
	 */
	public Path offset(String offset) {
		return offset(Coordinate.parse(offset));
	}
	
	/**
	 * Get a scaled version of this path
	 * @param scale scale factor
	 * @return scaled path
	 */
	public Path scale(String scale) {
		Path op = new Path();
		for(Segment s : segments) {
			if(s.point!=null)
				op.segments.add(new Segment(s.type, s.point.scale(scale)));
			else
				op.segments.add(s);
		}
		return op;
	}
	
	/**
	 * Get the dimension of the path on the given axis
	 * @param axis
	 * @return dimension
	 */
	public double getDimension(Axis axis) {
		double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
		try {
			for(Segment s : segments) {
				if(s.point!=null) {
					Double d = ((NumericCoordinate)s.point).getValue(axis);
					if(d!=null) {
						if(d<min)
							min = d;
						if(d>max)
							max = d;
					}
				}
			}
		} catch(ClassCastException e) {
			throw new RenderException("getDimension can only be used with numeric paths!");
		}
		return max-min;
	}
	
	/**
	 * Return a copy of this path with coordinates on the given axes centered.
	 * <p>This method can only be called on paths where each coordinate
	 * is numeric. 
	 * @return centered path
	 */
	public Path center(String axis) {
		// Select axes
		EnumSet<Axis> axes = EnumSet.noneOf(Axis.class);
		for(int i=0;i<axis.length();++i)
			axes.add(Axis.valueOf(Character.toString(Character.toUpperCase(axis.charAt(i)))));

		if(axes.isEmpty())
			throw new IllegalArgumentException("No axes specified!");
		
		// Initialize bounds variables
		double[] min = new double[Axis.values().length];
		double[] max = new double[Axis.values().length];
		for(int i=0;i<min.length;++i) {
			min[i] = Double.MAX_VALUE;
			max[i] = -Double.MAX_VALUE;
		}
		
		// Find path bounds
		for(Segment s : segments) {
			if(s.point!=null) {
				NumericCoordinate nc = (NumericCoordinate)s.point;
				for(Axis a : axes) {
					int i = a.ordinal();
					if(nc.getValue(a) < min[i])
						min[i] = nc.getValue(a);
					if(nc.getValue(a) > max[i])
						max[i] = nc.getValue(a);
				}
			}
		}
		
		// Calculate offset
		NumericCoordinate offset = new NumericCoordinate();
		for(Axis a : axes) {
			int i = a.ordinal();
			offset.set(a, -min[i] - (max[i]-min[i])/2.0);
		}
		
		// Apply offset
		Path np = new Path();
		for(Segment s : segments) {
			if(s.point!=null)
				np.addSegment(s.type, s.point.offset(offset));
			else
				np.segments.add(s);
		}
		return np;
	}
	
	/**
	 * Return a copy of this path flattened along one axis
	 * @param axis
	 * @return flattened path
	 */
	public Path flatten(String axis) {
		Axis a = Axis.valueOf(axis.toUpperCase());
		Path np = new Path();
		for(Segment s : segments) {
			if(s.point!=null)
				np.segments.add(new Segment(s.type, s.point.undefined(a)));
			else
				np.segments.add(s);
		}
		
		return np;
	}
	
	/**
	 * Debugging method: Dump this path to stderr
	 * @return this
	 */
	public Path dump() {
		System.err.println("Path: " + segments.size() + " segments.");
		for(Segment s : segments)
			System.err.println("\t" + s);
		return this;
	}

	/**
	 * @return this
	 */
	public Path toPath() {
		return this;
	}
}
