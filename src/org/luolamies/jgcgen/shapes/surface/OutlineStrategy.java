package org.luolamies.jgcgen.shapes.surface;

import java.util.Iterator;
import java.util.List;

import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;

/**
 * Trace waterlevel outlines
 *
 */
class OutlineStrategy implements ImageStrategy {
	private final Image image;
	private final double passdepth;
	private final double resolution;
	
	public OutlineStrategy(Image image) {
		this.image = image;
		this.passdepth = 0;
		this.resolution = 0;
	}
	
	public OutlineStrategy(Image image, String params) {
		this.image = image;
		String[] param = params.split(" ");
		if(param.length!=1 && param.length!=2)
			throw new IllegalArgumentException("RoughStrategy takes 0, 1 or 2 parameters!");

		passdepth = Double.parseDouble(param[0]);
		if(passdepth<0)
			throw new IllegalArgumentException("Pass depth must be positive!");
		
		if(param.length==2) {
			resolution = Double.parseDouble(param[1]);
			if(resolution<0)
				throw new IllegalArgumentException("Resolution must be positive!");
		} else
			resolution = 0;
	}
	
	public Path toPath(Surface img) {
		double pd = passdepth;
		if(pd==0)
			pd = image.getTool().getRadius();
		
		double res = resolution;
		if(res==0)
			res = img.getResolution();
		
		Path path = new Path();
		
		Plane plane = new Plane(img, image.getTool(), image.getWidth(), image.getHeight(), res);
		
		final double minz = -img.getMaxZ();
		double z=0;
		while(z>minz) {
			z -= pd;
			if(z<minz)
				z = minz;
			if(plane.init(z))
				break;
			
			path.addPath(plane.trace());
			/*
			for(List<Plane.Point> ppath : paths) {
				//plane.straighten(ppath);
				
				Iterator<Plane.Point> i = ppath.iterator();
				Plane.Point p = i.next();
				path.addSegment(Path.SType.MOVE, new NumericCoordinate((double)p.x, (double)-p.y, z));
				while(i.hasNext()) {
					p = i.next();
					path.addSegment(Path.SType.LINE, new NumericCoordinate((double)p.x, (double)-p.y, z));
				}
			}
			*/
		}
		
		return path;
	}
}
