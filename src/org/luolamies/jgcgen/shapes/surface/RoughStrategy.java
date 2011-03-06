package org.luolamies.jgcgen.shapes.surface;

import java.util.ArrayList;
import java.util.Collections;

import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.Path.SType;
import org.luolamies.jgcgen.tools.Tool;

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
	
	public Path toPath(ImageData img) {
		// Get the pass depth in range [0,255]
		// This is used to generate waterline masks
		double passdepth = this.passdepth;
		if(passdepth==0)
			passdepth = image.getTool().getDiameter() * 0.7;
		
		// Get the stepover size.
		// The default is tool radius.
		int stepover;
		if(img.getStepover()==0) {
			stepover = (int) Math.round((image.getTool().getDiameter() / 2.0) / img.getXYscale());
			if(stepover==0)
				stepover = 1;
		} else
			stepover = img.getStepover();
	
		// Z passes
		Path path = new Path();
		double level = 0;
		do {
			level -= passdepth;
			if(level<-img.getZscale())
				level = -img.getZscale();
			doPass(path, level, img, stepover);
			
		} while(level>-img.getZscale());
		return path;
	}
	
	private void doPass(Path path, double level, ImageData img, int stepover) {
		int min=0, max;
		if(angle==0)
			max = img.getHeight();
		else
			max = img.getWidth();

		if(dir==Dir.NEG) {
			min = max - 1;
			max = -1;
			stepover = -stepover;
		}
		
		// Scan Y or X rows depending on whether angle is 0 or 90
		NumericCoordinate last=null;
		int i=min;
		do {
			doLine(path, img, i, level, dir!=Dir.NEG, last);
			
			i = incrLine(stepover, i, max);
			if(dir==Dir.ALT && i!=max) {
				doLine(path, img, i, level, false,
						path.isEmpty() ? null :
							(NumericCoordinate)path.getSegments()
								.get(path.getSize()-1).point
								.offset(image.getOrigin(), true, false)
						);
				i = incrLine(stepover, i, max);
				
				if(!path.isEmpty()) {
					last = (NumericCoordinate) path.getSegments()
						.get(path.getSize()-1).point
						.offset(image.getOrigin(), true, false);
				}
			}
		} while(i!=max);
	}
	
	/**
	 * Increment line index by stepover so that we still
	 * do max-1 (or max) last.
	 * @param stepover
	 * @param i
	 * @param max
	 * @return
	 */
	private int incrLine(int stepover, int i, int max) {
		if(stepover>0) {
			if(i+1 != max && i+stepover>=max)
				i = max - 1;
			else
				i += stepover;
			if(i>max)
				i=max;
		} else {
			if(i-1 != max && i+stepover<max)
				i = max + 1;
			else
				i += stepover;
			if(i<max)
				i=max;
		}
		return i;
	}
	
	/** Generate the toolpath for a single X or Y line, depending on the angle. */
	private void doLine(Path path, ImageData img, int i, double level, boolean pos, NumericCoordinate last) {
		ArrayList<Integer> points = sliceLine(img, i, level, pos, image.getTool());
		
		// Check if we have anything to do
		if(points.isEmpty())
			return;
		
		double ii = i * img.getXYscale();
		if(angle==0)
			ii = -ii;

		SType firsttype = SType.MOVE;
		// Check if we can continue straight from the last coordinate without a move
		if(last!=null) {
			double p = points.get(0) * (double)img.getXYscale();
			NumericCoordinate next;
			if(angle==0)
				next = new NumericCoordinate(p, ii, level);
			else
				next = new NumericCoordinate(ii, -p, level);
			if(safeline(img, last, next))
				firsttype = SType.LINE;
		}
		
		for(int j=0;j<points.size();j+=2) {
			double jj = points.get(j) * (double)img.getXYscale();
			path.addSegment(firsttype, image.getOrigin().offset(
					angle==0 ?
							new NumericCoordinate(jj, ii, level)
						:
							new NumericCoordinate(ii, -jj, level)
					));
			firsttype = SType.MOVE;
			jj = points.get(j+1) * (double)img.getXYscale();
			path.addSegment(SType.LINE, image.getOrigin().offset(
					angle==0 ?
							new NumericCoordinate(jj, ii, level)
						:
							new NumericCoordinate(ii, -jj, level)
					));
		}
	}
	
	/**
	 * Check if a line can be drawn from <i>start</i> to <i>end</i> without
	 * hitting the shape we're engraving. 
	 * @param start
	 * @param end
	 * @return true if line from start to end is safe
	 */
	private boolean safeline(ImageData img, NumericCoordinate start, NumericCoordinate end) {
		double x1 = start.getValue(Axis.X);
		double y1 = start.getValue(Axis.Y);
		double z1 = start.getValue(Axis.Z);
		
		double len = start.distance(end);
		if(len==0)
			return true;
		
		double dx = (end.getValue(Axis.X)-x1) / len;
		double dy = (end.getValue(Axis.Y)-y1) / len;
		double dz = (end.getValue(Axis.Z)-z1) / len;
		
		int prevx = -1, prevy=-1;
		for(double d=0;d<len;d += 0.01) {
			int x = (int)Math.round((x1 + dx*d) / img.getXYscale());
			int y = (int)Math.round(-(y1 + dy*d) / img.getXYscale());
			if(x>=img.getWidth())
				x = img.getWidth()-1;
			if(y>=img.getHeight())
				y = img.getHeight()-1;
			if(x!=prevx || y!=prevy) {
				double z = z1 + dz*d;
				
				if(img.getDepthAt(x, y, image.getTool()) > z)
					return false;
				prevx = x;
				prevy = y;
			}
		}
		return true;
	}
	
	/** Cut a single line into segments */
	private ArrayList<Integer> sliceLine(ImageData image, int i, double level, boolean pos, Tool tool) {
		ArrayList<Integer> points = new ArrayList<Integer>();
		boolean bb = true;
		int max = (angle==0 ? image.getWidth() : image.getHeight());
		for(int j=0;j<max;++j) {
			boolean b;
			if(angle==0)
				b = image.getDepthAt(j, i, tool) > level;
			else
				b = image.getDepthAt(i, j, tool) > level;
			
			if(b!=bb) {				
				points.add(j - (b ? 1 : 0));
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
