package org.luolamies.jgcgen.shapes.outline;

import org.luolamies.jgcgen.path.Coordinate;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.PathGenerator;

/**
 * A line from point a to point b.
 */
public class Line implements PathGenerator {
	private Coordinate a, b;
	private Double z0, z1, dz;
	
	/**
	 * Set starting coordinates
	 * @param a
	 * @return
	 */
	public Line from(String a) {
		this.a = Coordinate.parse(a);
		return this;
	}
	
	/**
	 * Set ending coordinate
	 */
	public Line to(String b) {
		this.b = Coordinate.parse(b);
		return this;
	}
	
	/**
	 * Set cutting depth offset.
	 * @param z0
	 * @param z1
	 * @param dz
	 * @return
	 */
	public Line depth(double z0, double z1, double dz) {
		if(z1 > z0)
			throw new IllegalArgumentException("Target depth should be below starting depth!");
		if(dz==0)
			throw new IllegalArgumentException("Pass depth must be nonzero!");
		this.z0 = z0;
		this.z1 = z1;
		this.dz = Math.abs(dz);
		return this;
	}
	
	public Line depth(double z1, double dz) {
		return depth(0.0, z1, dz);
	}
	
	public Line noDepth() {
		z0 = null;
		z1 = null;
		dz = null;
		return this;
	}
	
	@Override
	public Path toPath() {
		Path path = new Path();
		
		if(z0!=null) {
			path.addSegment(Path.SType.MOVE, a.offset(new NumericCoordinate(null, null, z0), false, true));	
			double z = z0;
			boolean incomplete=false;
			while(z>z1) {
				z -= dz;
				if(z<z1)
					z = z1;
				path.addSegment(Path.SType.LINE, b.offset(new NumericCoordinate(null, null, z), false, true));
				
				incomplete = z>z1;
				z -= dz;
				if(z<z1)
					z = z1;
				path.addSegment(Path.SType.LINE, a.offset(new NumericCoordinate(null, null, z), false, true));
			}
			if(incomplete)
				path.addSegment(Path.SType.LINE, b.offset(new NumericCoordinate(null, null, z), false, true));
			
		} else {
			path.addSegment(Path.SType.MOVE, a);
			path.addSegment(Path.SType.LINE, b);
		}
		
		return path;
	}

}
