package org.luolamies.jgcgen.shapes.surface;

import org.luolamies.jgcgen.JGCGenerator;
import org.luolamies.jgcgen.Logger;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.Path.SType;
import static org.luolamies.jgcgen.shapes.surface.SurfaceUtils.incr;

/**
 * Simple scanning strategy.
 * <p>Parameters are:
 * <code>simple &lt;angle&gt; &lt;pos|neg|alt&gt;</code>
 * <p>Where <var>angle</var> is the scanning angle (0 or 90&deg;)
 * and the second parameter is routing direction.
 *
 */
class SimpleStrategy implements ImageStrategy {
	private enum Dir {
		POS,
		NEG,
		ALT
	}
	private final Image image;
	private final int angle;
	private final Dir dir;
	
	public SimpleStrategy(Image image) {
		this.image = image;
		angle = 0;
		dir = Dir.ALT;
	}
	
	public SimpleStrategy(Image image, String substrategy) {
		this.image = image;
		String[] params = substrategy.split(" ");
		if(params.length!=2)
			throw new IllegalArgumentException("SimpleStrategy takes 0 or 2 parameters!");
		
		if("0".equals(params[0]))
			angle = 0;
		else if("90".equals(params[0]))
			angle = 90;
		else
			throw new IllegalArgumentException("Only 0 and 90 angles are currently supported");
		
		try {
			dir = Dir.valueOf(params[1].toUpperCase());
		} catch(IllegalArgumentException e) {
			throw new IllegalArgumentException("Direction \"" + substrategy + "\" not supported!");
		}
	}
	
	/**
	 * Do a single horizontal or vertical scanline in either positive or negative direction
	 * @param path path to build
	 * @param img image source
	 * @param origin origin coordinate
	 * @param move should the first path segment be a move instead of a line
	 * @param pos positive direction?
	 * @param i scanline position (if angle==0, this is the Y coordinate, otherwise this X)
	 * @param jmin starting point at scanline
	 * @param jmax ending point at scanline
	 * @param dj scan step
	 */
	private void scanline(Path path, Surface img, boolean move, boolean pos, double i, double jmin, double jmax, double dj) {
		double j = jmin;
		if(angle==0) {
			if(move) {
				path.addSegment(SType.MOVE, image.getOrigin().offset(new NumericCoordinate(j, i, img.getDepthAt(j, i, image.getTool()))));
				j = incr(j, jmax, dj);
			}
			while(true) {
				path.addSegment(SType.LINE, image.getOrigin().offset(new NumericCoordinate(j, i, img.getDepthAt(j, i, image.getTool()))));
				if(j==jmax)
					break;
				j = incr(j, jmax, dj);
			}
		} else {
			if(move) {
				path.addSegment(SType.MOVE, image.getOrigin().offset(new NumericCoordinate(i, j, img.getDepthAt(i, j, image.getTool()))));
				j = incr(j, jmax, -dj);
			}
			while(true) {
				path.addSegment(SType.LINE, image.getOrigin().offset(new NumericCoordinate(i, j, img.getDepthAt(i, j, image.getTool()))));
				if(j==jmax)
					break;
				j = incr(j, jmax, -dj);
			}
		}
	}
	
	public Path toPath(Surface img) {
		Path path = new Path();

		double so;
		if(image.getStepover()==0)
			so = img.getResolution(); // Default minimum
		else
			so = image.getStepover();
		
		double jmin=0, dj = img.getResolution(), jmax;
		if(angle==0)
			jmax = image.getWidth();
		else if(angle==90)
			jmax = image.getHeight();
		else
			throw new UnsupportedOperationException("Arbitrary angles are not implemented yet!");
		
		if(dir==Dir.NEG && angle!=90 || (dir!=Dir.NEG && angle==90)) {
			jmin = jmax;
			jmax = 0;
			dj = -dj;
		}

		double i=0;
		double imax;
		if(angle==0) {
			imax = -image.getHeight();
			so = -so;
		} else {
			imax = image.getWidth();
			jmin = -jmin;
			jmax = -jmax;
		}
		
		final Logger log = JGCGenerator.getLogger();
		
		boolean first=true;
		while(true) {
			log.progress("SimpleStrategy", i, imax);
			scanline(path, img, first | dir!=Dir.ALT, dir!=Dir.NEG, i, jmin, jmax, dj);
			first = false;
			
			if(i==imax)
				break;
			i = incr(i, imax, so);
			
			if(dir==Dir.ALT) {
				scanline(path, img, false, false, i, jmax, jmin, -dj);
				
				if(i==imax)
					break;
				i = incr(i, imax, so);
			}
		}
				
		return path;
	}
}
