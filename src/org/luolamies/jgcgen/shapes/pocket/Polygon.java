package org.luolamies.jgcgen.shapes.pocket;

import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.Coordinate;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.PathGenerator;
import org.luolamies.jgcgen.path.Path.SType;

public class Polygon implements PathGenerator {
	private int vertices = 3;
	private double rotate=0;
	private double radius=1;
	private double tooldia;
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
	 * Set the diamater of the tool.
	 * @param radius
	 * @return this
	 */
	public Polygon tool(double dia) {
		if(dia<=0)
			throw new IllegalArgumentException("Tool diameter must be greater than zero!");
		this.tooldia = dia;
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

	public Path toPath() {
		Path path = new Path();
		
		path.addSegment(SType.MOVE, origin);
		
		double r = 0;
		
		// Spiral outwards
		double dr = tooldia/vertices;
		int finish=0;
		while(r<radius) {
			for(int i=0;i<vertices;++i) {
				path.addSegment(Path.SType.LINE, getPoint(i, r));
				if(r<radius) {
					r += dr;
					if(r>radius || Math.abs(r-radius) < 0.0001) {
						r = radius;
						finish = i+1;
					}
				}
			}
		}
		
		// Finish
		for(int i=0;i<=finish;++i)
			path.addSegment(Path.SType.LINE, getPoint(i, r));
		return path;
	}

	/**
	 * Get coordinates for point <var>v</var> at radius <var>r</var>
	 * @param v point index
	 * @param r distance from origin
	 * @return coordinates
	 */
	private NumericCoordinate getPoint(int v, double r) {
		double a = (v / (double)vertices * 2.0 * Math.PI) + rotate;
		return new NumericCoordinate(
				origin.getValue(Axis.X) + Math.sin(a) * r,
				origin.getValue(Axis.Y) + Math.cos(a) * r,
				null
				);
	}
}
