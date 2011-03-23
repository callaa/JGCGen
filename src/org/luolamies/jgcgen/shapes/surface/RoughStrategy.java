package org.luolamies.jgcgen.shapes.surface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.luolamies.jgcgen.JGCGenerator;
import org.luolamies.jgcgen.Logger;
import org.luolamies.jgcgen.path.Coordinate;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;

import static org.luolamies.jgcgen.shapes.surface.SurfaceUtils.incr;
import static org.luolamies.jgcgen.shapes.surface.SurfaceUtils.safeline;

/**
 * A rough path generation strategy. This generates multiple fast passes 
 * for quickly clearing out material before a semirough or finishing pass.
 */
public class RoughStrategy implements ImageStrategy {
	private enum Dir {
		POS,
		NEG,
		ALT
	}
	
	/** Line segment */
	private class Seg {
		
		Seg(double i, double j0, double j1) {
			this.i = i;
			this.j0 = j0;
			this.j1 = j1;
		}
		/** The Y coordinate when angle is 0 and X coordinate when 90 */
		final double i;
		/** The start and end coordinates orthogonal to */
		double j0, j1;
		
		private void reverse() {
			double tmp = j0;
			j0 = j1;
			j1 = tmp;
		}
	}
	
	private final Image image;
	private final double passdepth;
	private final Dir dir;
	private final int angle;
	
	public RoughStrategy(Image image) {
		this.image = image;
		angle = 0;
		passdepth = 0;
		dir = Dir.ALT;
	}
	
	/**
	 * Parameters: <code>rough <var>passdepth</var> <var>angle</var> <var>direction</var></code>,
	 * where direction can be one of POS, NEG or ALT.
	 * @param image
	 * @param params
	 */
	public RoughStrategy(Image image, String params) {
		this.image = image;
		String[] param = params.split(" ");
		if(param.length!=3)
			throw new IllegalArgumentException("RoughStrategy takes 0 or 3 parameters!");

		passdepth = Double.parseDouble(param[0]);
		if(passdepth<0)
			throw new IllegalArgumentException("Pass depth must be positive!");

		angle = Integer.parseInt(param[1]);
		if(angle!=0 && angle!=90)
			throw new IllegalArgumentException("Only angles 0 and 90 are supported!");
		
		try {
			dir = Dir.valueOf(param[2].toUpperCase());
		} catch(IllegalArgumentException e) {
			throw new IllegalArgumentException("Direction \"" + param[1] + "\" not supported!");
		}		
	}
	
	public Path toPath(Surface img) {
		// Get the pass depth. This is used to generate waterline masks
		double passdepth = -this.passdepth;
		if(passdepth==0)
			passdepth = -image.getTool().getDiameter() * 0.7;
		
		// Get the stepover distance.
		final double stepover;
		if(image.getStepover()==0)
			stepover = image.getTool().getRadius();
		else
			stepover = image.getStepover();
	
		// Z passes
		Path path = new Path();
		final double minlevel = -img.getMaxZ();
		double level = 0;
		final Logger log = JGCGenerator.getLogger();
		
		do {
			log.progress("RoughStrategy", -level, -minlevel);
			level = incr(level, minlevel, passdepth);
			if(!doPass(path, level, img, stepover))
				break;
		} while(level>minlevel);
		
		return path;
	}
	
	private boolean doPass(Path path, double level, Surface img, double stepover) {
		double imin=0, imax;
		if(angle==0) {
			imax = -image.getHeight();
			stepover = -stepover;
		} else
			imax = image.getWidth();
		
		// Scan Y or X rows depending on whether angle is 0 or 90
		List<Seg> segments = new ArrayList<Seg>();
		double i=imin;
		while(true) {
			doLine(segments, img, i, level, dir!=Dir.NEG);
			if(i==imax)
				break;
			i = incr(i, imax, stepover);
		}
		
		// Nothing to do for this layer?
		// Then we can stop right here.
		if(segments.isEmpty())
			return false;
		
		final Coordinate o = image.getOrigin(); 
		if(dir!=Dir.ALT) {
			while(!segments.isEmpty()) {
				Seg seg = segments.remove(0);
				if(angle==0) {
					path.addSegment(Path.SType.MOVE, o.offset(new NumericCoordinate(seg.j0, -seg.i, level)));
					path.addSegment(Path.SType.LINE, o.offset(new NumericCoordinate(seg.j1, -seg.i, level)));
				} else {
					path.addSegment(Path.SType.MOVE, o.offset(new NumericCoordinate(seg.i, -seg.j0, level)));
					path.addSegment(Path.SType.LINE, o.offset(new NumericCoordinate(seg.i, -seg.j1, level)));
				}
			}
		} else {
			// Alternating directions. This is a bit smarter than that actually.
			// The segments are sorted to minimize rapids
			Seg seg = segments.remove(0);
			boolean movefirst = true;
			NumericCoordinate last;
			while(true) {
				if(angle==0) {
					path.addSegment(movefirst ? Path.SType.MOVE : Path.SType.LINE, o.offset(new NumericCoordinate(seg.j0, -seg.i, level)));
					last = new NumericCoordinate(seg.j1, -seg.i, level);
				} else {
					path.addSegment(movefirst ? Path.SType.MOVE : Path.SType.LINE, o.offset(new NumericCoordinate(seg.i, -seg.j0, level)));
					last = new NumericCoordinate(seg.i, -seg.j1, level);
				}
				path.addSegment(Path.SType.LINE, o.offset(last));
				
				if(segments.isEmpty()) {
					// If this was the last segment were done.
					break;
				} else {
					// Find the nearest next segment.
					// Check distance to both ends.
					double maxd = Double.MAX_VALUE;
					int maxs = 0;
					for(int s=0;s<segments.size();++s) {
						Seg ss = segments.get(s);
						double d = dist(seg.i-ss.i, seg.j1-ss.j0);
						double d2 = dist(seg.i-ss.i, seg.j1-ss.j1);
						if(d2 < d) {
							ss.reverse();
							d = d2;
						}
						if(d < maxd) {
							maxd = d;
							maxs = s;
						}
					}
					// Ok, we got our next segment. See if we can plow
					// straight into it without lifting the tool
					Seg next = segments.remove(maxs);
					
					NumericCoordinate nc;
					if(angle==0)
						nc = new NumericCoordinate(next.j0, -next.i, level);
					else
						nc = new NumericCoordinate(next.i, -next.j0, level);
					
					movefirst = !safeline(img, image.getTool(), last, nc);
					
					seg = next;
				}
			};
		}
		return true;
	}
	
	static private double dist(double a, double b) {
		return a*a + b*b;
	}
	
	/** Generate the toolpath for a single X or Y line, depending on the angle. */
	private void doLine(List<Seg> segments, Surface img, double i, double level, boolean pos) {
		// Get the available line segments
		ArrayList<Double> points = sliceLine(img, i, level, pos);
		
		// Check if we have anything to do
		if(points.isEmpty())
			return;
		
		if(angle==0)
			i = -i;
		
		for(int j=0;j<points.size();j+=2) {
			segments.add(new Seg(i, points.get(j), points.get(j+1)));
		}
	}
	
	/** Cut a single line into segments */
	private ArrayList<Double> sliceLine(Surface img, double i, double level, boolean pos) {
		ArrayList<Double> points = new ArrayList<Double>();
		
		boolean bb = true;
		double max = (angle==0 ? image.getWidth() : image.getHeight());
		
		for(double j=0;j<max;j+=img.getResolution()) {
			boolean b;
			if(angle==0)
				b = img.getDepthAt(j, i, image.getTool()) > level;
			else
				b = img.getDepthAt(i, -j, image.getTool()) > level;
			
			if(b!=bb) {				
				points.add(j - (b ? img.getResolution() : 0));
				bb = b;
			}
		}
		if(points.size()%2==1)
			points.add(max);
		if((angle==0 && !pos) || (angle==90 && pos))
			Collections.reverse(points);
		return points;
	}
}
