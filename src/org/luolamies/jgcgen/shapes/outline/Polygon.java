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
package org.luolamies.jgcgen.shapes.outline;

import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.Coordinate;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.PathGenerator;

/**
 * Generate a polygon
 *
 */
public class Polygon implements PathGenerator {
	private int vertices = 3;
	private double rotate=0;
	private double radius=1;
	private double leadin=0;
	private boolean ccw;
	private NumericCoordinate origin = new NumericCoordinate(0.0,0.0,null);
	
	/**
	 * Set the number of sides in the polygon
	 * @param v vertice count
	 * @return this
	 */
	public Polygon sides(int v) {
		if(v<3)
			throw new IllegalArgumentException("A polygon must have at least three vertices");
		vertices = v;
		return this;
	}
	
	/**
	 * Rotate the polygon
	 * @param angle angle in degrees
	 * @return this
	 */
	public Polygon rotate(double angle) {
		rotate = angle / 180.0 * Math.PI;
		return this;
	}
	
	/**
	 * Set the radius of the polygon
	 * @param radius
	 * @return this
	 */
	public Polygon radius(double radius) {
		this.radius = radius;
		return this;
	}

	/**
	 * Set the origin
	 * @param origin
	 * @return
	 */
	public Polygon origin(String origin) {
		Coordinate o = Coordinate.parse(origin);
		if(!(o instanceof NumericCoordinate))
			throw new IllegalArgumentException("Only numeric coordinates supported for the origin!");
		if(!o.isDefined(Axis.X) || !o.isDefined(Axis.Y))
			throw new IllegalArgumentException("Both X and Y axes must be defined for polygon origin!");
		this.origin = (NumericCoordinate)o;
		return this;
	}
	
	public Polygon cw() {
		this.ccw = false;
		return this;
	}
	
	public Polygon ccw() {
		this.ccw = true;
		return this;
	}
	
	public Polygon leadin(double leadin) {
		this.leadin = leadin;
		return this;
	}
	
	public Path toPath() {
		Path path = new Path();
		
		double ox = origin.getValue(Axis.X);
		double oy = origin.getValue(Axis.Y);
		Double oz = origin.getValue(Axis.Z);
		
		if(leadin!=0) {
			path.addSegment(Path.SType.MOVE, new NumericCoordinate(
					ox + Math.sin(rotate) * (radius-leadin),
					oy + Math.cos(rotate) * (radius-leadin),
					oz));
			path.addSegment(Path.SType.LINE, new NumericCoordinate(
					ox + Math.sin(rotate) * radius,
					oy + Math.cos(rotate) * radius,
					oz));
			
		} else {
			path.addSegment(Path.SType.MOVE,
					new NumericCoordinate(
							ox + Math.sin(rotate) * radius,
							oy + Math.cos(rotate) * radius,
							oz
							));
		}
		
		for(int i=1;i<=vertices;++i) {
			int j;
			if(ccw)
				j = vertices-i;
			else
				j=i;
			
			double a = (j / (double)vertices * 2.0 * Math.PI) + rotate;
			path.addSegment(Path.SType.LINE,
					new NumericCoordinate(
							ox + Math.sin(a) * radius,
							oy + Math.cos(a) * radius,
							oz
							));
		}
		
		return path;
	}

}
