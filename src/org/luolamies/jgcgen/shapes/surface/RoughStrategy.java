package org.luolamies.jgcgen.shapes.surface;

import java.util.ArrayList;
import java.util.Collections;

import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.Path.SType;

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
		do {
			level = incr(level, minlevel, passdepth);
			doPass(path, level, img, stepover);			
		} while(level>minlevel);
		
		return path;
	}
	
	private void doPass(Path path, double level, Surface img, double stepover) {
		double imin=0, imax;
		if(angle==0) {
			imax = -image.getHeight();
			stepover = -stepover;
		} else
			imax = image.getWidth();
		
		// Scan Y or X rows depending on whether angle is 0 or 90
		NumericCoordinate last=null;
		double i=imin;
		while(true) {
			doLine(path, img, i, level, dir!=Dir.NEG, last);
			if(i==imax)
				break;
			i = incr(i, imax, stepover);
			
			if(dir==Dir.ALT && i!=imax) {
				doLine(path, img, i, level, false,
						path.isEmpty() ? null :
							(NumericCoordinate)path.getSegments()
								.get(path.getSize()-1).point
								.offset(image.getOrigin(), true, false)
						);
				i = incr(i, imax, stepover);
				
				if(!path.isEmpty()) {
					last = (NumericCoordinate) path.getSegments()
						.get(path.getSize()-1).point
						.offset(image.getOrigin(), true, false);
				}
			}
		}
	}
	
	/** Generate the toolpath for a single X or Y line, depending on the angle. */
	private void doLine(Path path, Surface img, double i, double level, boolean pos, NumericCoordinate last) {
		// Get the available line segments
		ArrayList<Double> points = sliceLine(img, i, level, pos);
		
		// Check if we have anything to do
		if(points.isEmpty())
			return;
		
		if(angle==0)
			i = -i;

		SType firsttype = SType.MOVE;
		// Check if we can continue straight from the last coordinate without a move
		if(last!=null) {
			double jj = points.get(0);
			NumericCoordinate next;
			if(angle==0)
				next = new NumericCoordinate(jj, -i, level);
			else
				next = new NumericCoordinate(i, -jj, level);
			if(safeline(img, image.getTool(), last, next))
				firsttype = SType.LINE;
		}
		
		for(int j=0;j<points.size();j+=2) {
			double jj = points.get(j);
			path.addSegment(firsttype, image.getOrigin().offset(
					angle==0 ?
							new NumericCoordinate(jj, -i, level)
						:
							new NumericCoordinate(i, -jj, level)
					));
			firsttype = SType.MOVE;
			jj = points.get(j+1);
			path.addSegment(SType.LINE, image.getOrigin().offset(
					angle==0 ?
							new NumericCoordinate(jj, -i, level)
						:
							new NumericCoordinate(i, -jj, level)
					));
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
