package org.luolamies.jgcgen.shapes.surface;

import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.tools.Tool;

/**
 * Various useful helper functions.
 *
 */
class SurfaceUtils {
	/**
	 * Increment <i>i</i> by <i>d</i>, while making sure it doesn't go above (or below if <i>d</i> is negative) <i>max</i>.
	 * @param i value to increment
	 * @param max maximum value
	 * @param d increment
	 * @return i+d
	 */
	static public double incr(double i, double max, double d) {
		i += d;
		if((d<0 & i<max) || (d>0 & i>max))
			i = max;
		return i;
	}
	
	/**
	 * Check if a line can be drawn from <i>start</i> to <i>end</i> without
	 * hitting the shape we're engraving. 
	 * @param start
	 * @param end
	 * @return true if line from start to end is safe
	 */
	static public boolean safeline(Surface img, Tool tool, NumericCoordinate start, NumericCoordinate end) {
		double x1 = start.getValue(Axis.X);
		double y1 = start.getValue(Axis.Y);
		double z1 = start.getValue(Axis.Z);
		
		double len = start.distance(end);
		if(len<0.000001)
			return true;
		
		double dx = (end.getValue(Axis.X)-x1) / len;
		double dy = (end.getValue(Axis.Y)-y1) / len;
		double dz = (end.getValue(Axis.Z)-z1) / len;
		
		for(double d=0;d<len;d += img.getResolution()) {				
			if(img.getDepthAt(x1 + dx*d, y1 + dy*d, tool) > z1 + dz*d)
				return false;
		}
		return true;
	}
}
