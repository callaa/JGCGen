package org.luolamies.jgcgen.shapes.surface;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.luolamies.jgcgen.JGCGenerator;
import org.luolamies.jgcgen.Logger;
import org.luolamies.jgcgen.path.Path;

/**
 * Trace waterlevel outlines
 *
 */
class OutlineStrategy implements ImageStrategy {
	private final Image image;
	private final double minpass, maxpass;
	private final double resolution;
	
	public OutlineStrategy(Image image) {
		this.image = image;
		this.minpass = 0;
		this.maxpass = 0;
		this.resolution = 0;
	}
	
	/**
	 * Outline strategy with parameters.
	 * Parameters are: <code>pass depth</code> <code>[resolution]</code>.
	 * Pass depth can be a range: e.g. 0.05-2. In this case, 0.05 is the minimum pass depth and 2 is the largest pass that will be taken.
	 * @param image
	 * @param params
	 */
	public OutlineStrategy(Image image, String params) {
		this.image = image;
		String[] param = params.split(" ");
		if(param.length!=1 && param.length!=2)
			throw new IllegalArgumentException("RoughStrategy takes 0, 1 or 2 parameters!");

		Matcher passdepth = passpattern.matcher(param[0]);
		if(passdepth.matches()) {
			minpass = Double.parseDouble(passdepth.group(1));
			if(passdepth.group(2)==null)
				maxpass = minpass;
			else
				maxpass = Double.parseDouble(passdepth.group(2));
			if(minpass<=0)
				throw new IllegalArgumentException("Pass depth must be greater than zero!");
			if(maxpass < minpass)
				throw new IllegalArgumentException("Max pass must be greater than minimum pass");			
		} else
			throw new IllegalArgumentException("Invalid pass depth!");
		
		if(param.length==2) {
			resolution = Double.parseDouble(param[1]);
			if(resolution<0)
				throw new IllegalArgumentException("Resolution must be positive!");
		} else
			resolution = 0;
	}
	
	static private final Pattern passpattern = Pattern.compile("(\\d+(?:\\.\\d+)?)(?:\\s*-\\s*(\\d+(?:\\.\\d+)?))?");
	
	public Path toPath(Surface img) {
		double res = resolution;
		if(res==0)
			res = img.getResolution();
		
		double mind = minpass, maxd = maxpass;
		if(mind==0)
			mind = res;
		if(maxd==0)
			maxd = image.getTool().getRadius();
		
		Path path = new Path();
		
		final Logger log = JGCGenerator.getLogger();
		
		Plane plane = new Plane(img, image.getTool(), image.getWidth(), image.getHeight(), res);
		
		final double minz = -img.getMaxZ();
		double z=0;
		double skipped = 0;
		while(z>minz) {
			z -= mind;
			if(z<minz)
				z = minz;
			log.progress("OutlineStrategy", -z, -minz);
			if(plane.init(z)) {
				// Encountered last plane?
				if(skipped>0)
					plane.restorePrevious();
				else
					break;
			} else if(skipped < maxd){
				// Is the new plane same as the old plane?
				log.status("Checking equality at Z" + z);
				if(plane.isIdentical()) {
					log.status("OutlineStrategy: Skipping identical plane at Z" + z);
					skipped += mind;
					continue;
				} else {
					if(skipped>0) {
						// When we encounter a different plane, the previous plane shouldn't be skipped.
						plane.restorePrevious();
						z += mind;
					}
				}
			}
			
			skipped = 0;
			
			path.addPath(plane.trace());
		}
		
		return path;
	}
}
