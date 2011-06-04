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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

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
	static public final class Segment {
		public Segment(SType type, Coordinate point) {
			this.type = type;
			this.point = point;
			this.label = null;
		}
		
		public Segment(SType type, Coordinate point, String label) {
			this.type = type;
			this.point = point;
			this.label = label;
		}
		
		/** The type of the segment */
		public final SType type;
		/** The segment point. This can be null when type is SEAM */
		public final Coordinate point;
		/** The segment label. This is usually with SEAM to identify subpaths,
		 * but can be used with points too as general purpose comments. 
		 */
		public final String label;
		
		@Override
		public String toString() {
			if(label==null)
				return type.name() + " " + point;
			else
				return type.name() + " " + point + '(' + label + ')';
		}

		/**
		 * Get the segment comment/label
		 * @return
		 */
		public final String getLabel() { return label; }
		
		/**
		 * Get the type of the segment
		 * @return segment type
		 */
		public final SType getType() { return type; }
		
		/**
		 * Get the segment point
		 * @return point
		 * @throws NullPointerException if segment has no point
		 */
		public final Coordinate getPoint() {
			if(point==null)
				throw new NullPointerException(type + " segment has no point!");
			return point;
		}
	}
	
	private List<Segment> segments;
	
	public Path() {
		segments = new ArrayList<Segment>();
	}
	
	private Path(List<Segment> segments) {
		this.segments = segments;
	}
	
	/**
	 * Add a new segment
	 * @param type segment type
	 * @param point segment coordinates
	 */
	public void addSegment(SType type, Coordinate point) {
		segments.add(new Segment(type, point));
	}
	
	/**
	 * Add a new labeled segment
	 * @param type
	 * @param point
	 * @param label
	 */
	public void addSegment(SType type, Coordinate point, String label) {
		segments.add(new Segment(type, point, label));
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
	public void addPath(PathGenerator pathg) {
		if(pathg==null)
			return;
		Path path = pathg.toPath();
		if(path.getSize()==0)
			return;
		
		boolean addseam = !segments.isEmpty() &&
			segments.get(segments.size()-1).type != SType.SEAM &&
			path.segments.get(0).type!=SType.SEAM;
		if(addseam)
			segments.add(new Segment(SType.SEAM, null));
		segments.addAll(path.segments);
	}

	public void merge(PathGenerator pathg) {
		if(pathg==null)
			return;
		Path path = pathg.toPath();
		if(path.getSize()==0)
			return;
		
		Iterator<Segment> i = path.segments.iterator();
		Segment s = i.next();
		if(s.type==SType.MOVE) {
			addSegment(SType.LINE, s.point);
		} else
			this.segments.add(s);
		while(i.hasNext())
			this.segments.add(i.next());
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
				if(!sp.isEmpty())
					subpaths.add(sp);
				sp = new Path();
			} else
				sp.segments.add(s);
		}
		if(!sp.isEmpty())
			subpaths.add(sp);
		return subpaths;
	}
	
	/**
	 * Split this path at move commands.
	 * @return new paths
	 */
	public List<Path> splitAtSubpaths() {
		List<Path> subpaths = new ArrayList<Path>();
		Path sp = new Path();
		for(Segment s : segments) {
			if(s.type==SType.MOVE) {
				if(!sp.isEmpty()) {
					if(sp.segments.get(0).getType() == SType.SEAM) {
						// No use starting a path with a seam
						sp.segments.remove(0);
					}
					if(!sp.isEmpty())
						subpaths.add(sp);
					sp = new Path();
				}
			}
			
			sp.segments.add(s);
		}
		if(!sp.isEmpty()) {
			if(sp.segments.get(0).getType() == SType.SEAM) {
				// No use starting a path with a seam
				sp.segments.remove(0);
			}
			if(!sp.isEmpty())
				subpaths.add(sp);
		}
		return subpaths;
	}
	
	/**
	 * Extract a subpath starting from the seam with the given label and upto the next seam or end of path.
	 * @param name
	 * @return
	 */
	public Path getNamedSubpath(String name) {
		boolean include = false;
		Path subpath = new Path();
		for(Segment s : segments) {
			if(include) {
				if(s.type==SType.SEAM)
					break;
				else
					subpath.segments.add(s);
			} else {
				if(s.type==SType.SEAM && name.equals(s.label))
					include = true;
			}
		}
		if(!include)
			throw new IllegalArgumentException("No such subpath: " + name);
		return subpath;
	}
	
	/**
	 * Get the size of the path
	 * @return path segment count
	 */
	public int getSize() {
		return segments.size();
	}
	
	/**
	 * Is this an empty path
	 * @return true if nothing has been added to the path yet
	 */
	public boolean isEmpty() {
		return segments.isEmpty();
	}
	
	/**
	 * Is this a closed path? I.e. are the XY coordinates of the first and last point the same?
	 * @return true if path is closed
	 */
	public boolean isClosed() {
		Segment first = segments.get(0);
		Segment last = segments.get(segments.size()-1);
		
		return first.point.get(Axis.X).equals(last.point.get(Axis.X)) && first.point.get(Axis.Y).equals(last.point.get(Axis.Y));
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
	 * Get a rotated version of this path. The path is rotated around the origin (0).
	 * @param rotation
	 * @return rotated path
	 */
	public Path rotate(String rotation) {
		Coordinate r = Coordinate.parse(rotation);
		Path op = new Path();
		Coordinate prev = null;
		for(Segment s : segments) {
			if(s.point!=null) {
				Coordinate p = s.point;
				if(prev!=null)
					p = p.fillIn(prev);
				prev = p;
				op.segments.add(new Segment(s.type, p.rotate(r)));
			} else
				op.segments.add(s);
		}
		return op;
	}
	
	/**
	 * Get a version of this path with all X, Y and Z values defined
	 * @return
	 */
	public Path getComplete() {
		Path p = new Path();
		Coordinate prev = null;
		for(Segment s : segments) {
			if(s.point != null) {
				Coordinate point = s.point;
				if(prev!=null)
					point = point.fillIn(prev);
				prev = point;
				p.segments.add(new Segment(s.type, point));
			} else
				p.segments.add(s);
		}
		return p;
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
	 * Return a copy of this path aligned to origin by the given coordinates. Possible alignments are:
	 * <ul>
	 * <li>-x: Leftmost point on the path will be zero
	 * <li>x: Horizontal center of path will be at zero
	 * <li>+x: Rightmost point on the path will be zero
	 * <li>And the same of y and z
	 * </ul>
	 * @param axes
	 * @return
	 */
	public Path align(String axes) {
		Integer[] align = new Integer[3];
		
		// Parse alignment string
		int state=0;
		for(int i=0;i<axes.length();++i) {
			char chr = Character.toLowerCase(axes.charAt(i));
			if(Character.isWhitespace(chr))
				continue;
			
			if(state==0) {
				// Expect [+-xyz]
				if(chr=='-')
					state = -1;
				else if(chr=='+')
					state = 1;
				else if(chr=='x'||chr=='y'||chr=='z')
					align[chr-'x'] = 0;
				else
					throw new IllegalArgumentException("Expected +, -, x, y or z. Got '" + chr + "'");
			} else if(state==-1 || state==1) {
				// Expect [xyz]
				if(chr=='x'||chr=='y'||chr=='z')
					align[chr-'x'] = state;
				else
					throw new IllegalArgumentException("Expected x, y or z. Got '" + chr + "'");
				state=0;
			}
		}
		
		// Initialize bounds variables
		Double[] min = new Double[3];
		Double[] max = new Double[3];
		
		// Find path bounds
		for(Segment s : segments) {
			if(s.point!=null) {
				NumericCoordinate nc = (NumericCoordinate)s.point;
				for(int i=0;i<3;++i) {
					Axis a = Axis.values()[i];
					Double val = nc.getValue(a);
					if(val!=null) {
						if(min[i]==null)
							min[i] = max[i] = val;
						else if(val < min[i])
							min[i] = val;
						else if(val > max[i])
							max[i] = val;
					}
				}
			}
		}
		
		// Calculate offset
		NumericCoordinate offset = new NumericCoordinate();
		for(int i=0;i<3;++i) {
			if(align[i]!=null && min[i]!=null) {
				Axis a = Axis.values()[i];
				if(align[i]<0)
					offset.set(a, -min[i]);
				else if(align[i]>0)
					offset.set(a, -max[i]);
				else
					offset.set(a, -min[i] - (max[i]-min[i])/2.0);
			}
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
		return flatten(axis, null);
	}
	
	/**
	 * Return a copy of this path flattened along one axis
	 * @param axis
	 * @param value value to set the axis to
	 * @return flattened path
	 */
	public Path flatten(String axis, String value) {
		Axis a = Axis.valueOf(axis.toUpperCase());
		Path np = new Path();
		
		Double dv = null;
		try {
			if(value!=null)
				dv = Double.valueOf(value);
		} catch(NumberFormatException e) {
			// Value is not numeric: numeric paths will be converted to symbolic.
		}
		
		for(Segment s : segments) {
			if(s.point!=null) {
				Coordinate c;
				if(value==null) {
					// Undefined value
					c = s.point.undefined(a);
				} else if(dv==null) {
					// Symbolic value
					if(s.point instanceof NumericCoordinate)
						c = ((NumericCoordinate)s.point).toSymbolic();
					else
						c = s.point.copy();
					((SymbolicCoordinate)c).set(a, value);
				} else {
					// Numeric value
					c = s.point.copy();
					if(c instanceof NumericCoordinate)
						((NumericCoordinate)c).set(a, dv);
					else
						((SymbolicCoordinate)c).set(a, value);
				}
				np.segments.add(new Segment(s.type, c));
			} else
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
	 * Subdivide the path until it is made up of segments no more than <i>min</i> long.
	 * <p>Note. Currently only linear moves (G01) are subdivided.
	 * @param min
	 * @return
	 */
	public Path subdivide(double min) {
		if(segments.isEmpty())
			return this;
		
		LinkedList<Segment> seg = new LinkedList<Segment>();
		seg.addAll(segments);
		
		ListIterator<Segment> li = seg.listIterator();
		Segment prev = li.next();
		
		while(li.hasNext()) {
			Segment s = li.next();
			if(prev.point!=null && s.point!=null && s.type == SType.LINE) {
				double d = ((NumericCoordinate)prev.point).distance((NumericCoordinate)s.point);
				if(d>min) {
					NumericCoordinate mid = new NumericCoordinate(
							(((NumericCoordinate)s.point).getValue(Axis.X, 0) - ((NumericCoordinate)prev.point).getValue(Axis.X, 0))/2,
							(((NumericCoordinate)s.point).getValue(Axis.Y, 0) - ((NumericCoordinate)prev.point).getValue(Axis.Y, 0))/2,
							(((NumericCoordinate)s.point).getValue(Axis.Z, 0) - ((NumericCoordinate)prev.point).getValue(Axis.Z, 0))/2
							); 
					li.previous();
					li.add(new Segment(SType.LINE, prev.point.offset(mid)));
					li.previous();
				} else
					prev = s;
			} else
				prev = s;
		}
		
		return new Path(seg);
	}

	/**
	 * Simplify this path. Note. This works only on numeric 3 axis paths.
	 * <p>
	 * Does the following changes:
	 * <ul>
	 * <li>Merge linear moves
	 * </ul>
	 */
	public Path reduce() {
		Path rp = new Path();
		int i=0;
		int start=-1;
		
		while(i<segments.size()) {
			Segment s = segments.get(i);
			if(s.type==SType.LINE) {
				if(start<0) {
					// Start a new line to reduce
					start = i;
					rp.segments.add(s);
				} else {
					if(i>start+1) {
						// We don't have anything to reduce until at least three points: start - X* - current.
						// Extrapolate a point based on two previous points and the distance to the current point.
						// Print current point if not close enough to the extrapolated point.
						NumericCoordinate p1 = (NumericCoordinate)segments.get(i-2).point;
						NumericCoordinate p2 = (NumericCoordinate)segments.get(i-1).point;
						NumericCoordinate p3 = (NumericCoordinate)segments.get(i).point;
						
						double dist12 = p1.distance(p2);
						double dist23 = p2.distance(p3);
						NumericCoordinate e = new NumericCoordinate(
							p2.getValue(Axis.X) + (p2.getValue(Axis.X) - p1.getValue(Axis.X)) / dist12 * dist23, 
							p2.getValue(Axis.Y) + (p2.getValue(Axis.Y) - p1.getValue(Axis.Y)) / dist12 * dist23,
							p2.getValue(Axis.Z) + (p2.getValue(Axis.Z) - p1.getValue(Axis.Z)) / dist12 * dist23
							);
						if(p3.distance(e) > 0.001) {
							rp.segments.add(segments.get(i-1));
							start = i-1;
						}
					}
				}
			} else {
				// Any other type in between resets the reduction.
				if(start>=0) {
					rp.segments.add(segments.get(i-1));
					start = -1;
				}
				// We can treat rapids as line starting points
				if(s.type==SType.MOVE)
					start = i;
				rp.segments.add(s);
			}
			++i;
		}
		if(start>=0)
			rp.segments.add(segments.get(i-1));
		
		return rp;
	}
	
	/**
	 * @return this
	 */
	public Path toPath() {
		return this;
	}
}
