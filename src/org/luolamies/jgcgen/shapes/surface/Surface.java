package org.luolamies.jgcgen.shapes.surface;

import org.luolamies.jgcgen.tools.Tool;

/**
 * Base class for 2.5D surfaces
 *
 */
public abstract class Surface {

	/**
	 * Get the aspect ratio of the surface.
	 * @return width / height
	 */
	abstract public double getAspectRatio();
	
	/**
	 * Get the Z value at the given coordinates.
	 * @param x
	 * @param y
	 * @return
	 */
	abstract public double getDepthAt(double x, double y);
	
	/**
	 * Set the target size. This usually sets the scaling factors for finite surfaces.
	 * @param width the target width
	 * @param height the target height
	 * @param depth the target depth
	 */
	abstract public void setTargetSize(double width, double height, double depth);
	
	/**
	 * Get the resolution. This is the distance covered by one pixel. If not applicable,
	 * some suitably small nonzero value should be returned.
	 * <p>This is not guaranteed to return anything sensible before {@link #setTargetSize(double, double, double)} has been called.
	 * @return smallest useful distance between two points
	 */
	abstract public double getResolution();
	
	/**
	 * Get Z maximum Z value (absolute). This can be though of as the Z scaling factor
	 * if internal values are all in the range [-1, 0].
	 * <p>
	 * This is usually the depth value passed in {@link #setTargetSize(double, double, double)}.
	 * @return Z scaling factor
	 */
	abstract public double getMaxZ();
	
	public double getDepthAt(double cx, double cy, Tool tool) {
		final double rad = tool.getRadius();
		final double res = getResolution();
		double minx = cx-rad, miny = cy-rad;
		double maxx = cx+rad, maxy = cy+rad;
		double maxz = -getMaxZ();
		
		//System.err.println("Get depth at " + cx + ", " + cy + " with " + tool.getRadius() + " at " + res);
		for(double y=miny;y<maxy;y+=res) {
			for(double x=minx;x<maxx;x+=res) {
				double r = Math.hypot(x-cx, y-cy); 
				if(r <= rad) {
					// Maximum allowed depth for the tool at this pixel when centered
					// at cx, cy and taking in account the tool shape
					double v = getDepthAt(x,y) - tool.getProfile(r);
					if(v>maxz)
						maxz = v;
				}
			}
		}
		return maxz;
	}
}
