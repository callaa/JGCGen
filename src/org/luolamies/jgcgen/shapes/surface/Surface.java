package org.luolamies.jgcgen.shapes.surface;

import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.Coordinate;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
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
	 * Set the target size. This should be called before using the surface. 
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
		double res = getResolution() / 2;
		final double minx = cx-rad, miny = cy-rad;
		final double maxx = cx+rad, maxy = cy+rad;
		double maxz = -getMaxZ();
		
		if(2*rad < res) {
			// Special case: resolution is too small
			res = rad / 2; 
		}
		
		final double radrad = rad*rad;
		
		//System.err.println("Get depth at " + cx + ", " + cy + " with " + tool.getRadius() + " at " + res);
		for(double y=miny;y<maxy;y+=res) {
			for(double x=minx;x<maxx;x+=res) {
				double rr = (x-cx)*(x-cx) + (y-cy)*(y-cy); 
				if(rr <= radrad) {
					// Maximum allowed depth for the tool at this pixel when centered
					// at cx, cy and taking in account the tool shape
					double v = getDepthAt(x,y) - tool.getProfile(rr);
					if(v>maxz)
						maxz = v;
				}
			}
		}
		return maxz;
	}
	
	/**
	 * Project a path onto this surface. The Z value for each point
	 * in the path will be set off by the Z value corresponding point
	 * P-offset on this surface.
	 * @param path
	 * @param offset
	 * @return path projected onto this surface
	 */
	public Path project(Path path, String offset) {
		return project(path, offset, null);
	}

	/**
	 * Project a path onto this surface taking the tool shape in account.
	 * No point of the tool will penetrate the surface.
	 * @param path
	 * @param offset
	 * @param tool
	 * @return path projected onto this surface
	 */
	public Path project(Path path, String offset, String tool) {
		Path pp = new Path();
		
		final NumericCoordinate o = (NumericCoordinate)Coordinate.parse(offset);
		Double ox = o.getValue(Axis.X);
		if(ox==null)
			ox = 0.0;
		Double oy = o.getValue(Axis.Y);
		if(oy==null)
			oy = 0.0;
		final Tool t = tool!=null ? Tool.get(tool) : null;
		
		for(Path.Segment s : path.getSegments()) {
			if(s.point!=null) {
				NumericCoordinate c = (NumericCoordinate) s.point;
				double z;
				if(t!=null)
					z = getDepthAt(c.getValue(Axis.X) + ox, c.getValue(Axis.Y) + oy, t);
				else
					z = getDepthAt(c.getValue(Axis.X) + ox, c.getValue(Axis.Y) + oy);
				pp.addSegment(s.type, c.offset(new NumericCoordinate(null, null, z), false, true));
			} else
				pp.addSegment(s.type, null);
		}
		return pp;
	}
	
	
}
