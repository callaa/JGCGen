package org.luolamies.jgcgen.shapes.surface;

import java.util.ArrayList;

import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.Path.SType;
import org.luolamies.jgcgen.tools.Tool;

public class RoughStrategy implements ImageStrategy {
	private final Image image;
	private final double passdepth;
	
	public RoughStrategy(Image image) {
		this.image = image;
		passdepth = 0;
	}
	
	public Path toPath(ImageData img) {
		// Get the pass depth in range [0,255]
		// This is used to generate waterline masks
		double passdepth = this.passdepth;
		if(passdepth==0)
			passdepth = image.getTool().getDiameter() * 0.7;
		
		int stepover;
		if(img.getStepover()==0) {
			stepover = (int) Math.round((image.getTool().getDiameter() / 2.0) / img.getXYscale());
			if(stepover==0)
				stepover = 1;
		} else
			stepover = img.getStepover();
		
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
	
	private void doPass(Path path, double level, ImageData img, int dy) {
		int y=0;
		do {
			doLine(path, img, y, level);
			
			// Increment y, while making sure the we do height-1
			// even if we have to do one thinner pass.
			if(y+1 != img.getHeight() && y+dy>=img.getHeight())
				y = img.getHeight() - 1;
			else
				y += dy;
		} while(y<img.getHeight());
	}
	
	private void doLine(Path path, ImageData img, int y, double level) {
		ArrayList<Integer> points = sliceLine(img, y, level, image.getTool());
		double yy = -y * img.getXYscale();
		for(int i=0;i<points.size();i+=2) {
			path.addSegment(SType.MOVE, image.getOrigin().offset(
					new NumericCoordinate(
							points.get(i) * (double)img.getXYscale(),
							yy,
							level
							)
					));
			path.addSegment(SType.LINE, image.getOrigin().offset(
					new NumericCoordinate(
							points.get(i+1) * (double)img.getXYscale(),
							yy,
							level
							)
					));
		}
	}
	
	private ArrayList<Integer> sliceLine(ImageData image, int y, double level, Tool tool) {
		ArrayList<Integer> points = new ArrayList<Integer>();
		boolean bb = true;
		for(int x=0;x<image.getWidth();++x) {
			boolean b = image.getDepthAt(x, y,tool) > level;
			if(b!=bb) {
				points.add(x + (b ? -1 : 0));
				bb = b;
			}
		}
		if(points.size()%2==1)
			points.add(image.getWidth());
		return points;
	}
}
