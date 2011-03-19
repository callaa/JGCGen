package org.luolamies.jgcgen.shapes.outline;

import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.Coordinate;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.PathGenerator;
import org.luolamies.jgcgen.path.Path.SType;

/**
 * A circle outline by helical milling.
 *
 */
public class Helix implements PathGenerator {
	private NumericCoordinate origin = new NumericCoordinate(0.0,0.0,0.0);
	private double radius;
	private double depth;
	private double passdepth;
	private boolean ccw;
	
	/**
	 * Set the top-center coordinates for the helix.
	 * If Z coordinate is omitted, 0 is used for the top.
	 * @param origin
	 * @return
	 */
	public Helix origin(String origin) {
		Coordinate c = Coordinate.parse(origin);
		if(!(c instanceof NumericCoordinate))
			throw new IllegalArgumentException("Helix supports only numeric coordinates!");
		this.origin = (NumericCoordinate)c;
		if(!this.origin.isDefined(Axis.Z))
			this.origin.set(Axis.Z, 0.0);
		return this;
	}
	
	/**
	 * Set the depth of the helix. The final depth will be origin Z - depth
	 * @param depth
	 * @return
	 */
	public Helix depth(double depth) {
		if(depth<=0)
			throw new IllegalArgumentException("Depth should be greater than zero!");
		this.depth = depth;
		return this;
	}
	
	/**
	 * Set the depth of a single pass
	 * @param depth
	 * @return
	 */
	public Helix pass(double depth) {
		this.passdepth = depth;
		return this;
	}
	
	/**
	 * Set the radius of the helix
	 * @param radius
	 * @return
	 */
	public Helix radius(double radius) {
		if(radius<=0)
			throw new IllegalArgumentException("Radius should be greater than zero!");
		this.radius = radius;
		return this;
	}
	
	public Helix cw() {
		ccw = false;
		return this;
	}
	
	public Helix ccw() {
		ccw = true;
		return this;
	}
	
	public Path toPath() {
		if(radius<=0)
			throw new RenderException("Radius not set!");
		if(depth<=0)
			throw new RenderException("Depth not set!");
		if(passdepth<=0)
			throw new RenderException("Pass depth not set!");
		
		Path path = new Path();
		
		path.addSegment(SType.MOVE, origin.offset(new NumericCoordinate(null, radius, null)));
		double z = origin.getValue(Axis.Z);
		final double targz = z - depth;
		
		final SType arctype = ccw ? SType.CCWARC : SType.CWARC;
		
		while(z>targz) {
			z -= passdepth;
			if(z<targz)
				z = targz;
			NumericCoordinate nc = (NumericCoordinate) origin.offset(new NumericCoordinate(null, radius, null));
			nc.set(Axis.J, -radius);
			nc.set(Axis.Z, z);
			path.addSegment(arctype, nc);
		}
		
		// Finish the last circle
		NumericCoordinate nc = (NumericCoordinate) origin.offset(new NumericCoordinate(null, radius, null));
		nc.set(Axis.J, -radius);
		nc.set(Axis.Z, z);
		path.addSegment(arctype, nc);
		
		return path;
	}

}
