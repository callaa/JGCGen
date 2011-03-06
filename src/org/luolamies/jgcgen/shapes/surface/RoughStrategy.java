package org.luolamies.jgcgen.shapes.surface;

import java.util.ArrayList;

import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.Path.SType;
import org.luolamies.jgcgen.tools.Tool;

public class RoughStrategy implements ImageStrategy {
	private final double passdepth;
	
	public RoughStrategy() {
		passdepth = 0;
	}
	
	public Path toPath(NumericCoordinate origin, ImageData image, Tool tool) {
		// Get the pass depth in range [0,255]
		// This is used to generate waterline masks
		double passdepth = this.passdepth;
		if(passdepth==0)
			passdepth = tool.getDiameter() * 0.7;
		
		int stepover;
		if(image.getStepover()==0) {
			stepover = (int) Math.round((tool.getDiameter() / 2.0) / image.getXYscale());
			if(stepover==0)
				stepover = 1;
		} else
			stepover = image.getStepover();
		
		Path path = new Path();
		double level = 0;
		do {
			level -= passdepth;
			if(level<-image.getZscale())
				level = -image.getZscale();
			doPass(path, level, origin, image, tool, stepover);
			
		} while(level>-image.getZscale());
		return path;
	}
	
	private void doPass(Path path, double level, NumericCoordinate origin, ImageData image, Tool tool,int dy) {
		int y=0;
		do {
			ArrayList<Integer> points = sliceLine(image, y, level, tool);
			double yy = -y * image.getXYscale();
			for(int i=0;i<points.size();i+=2) {
				path.addSegment(SType.MOVE, origin.offset(
						new NumericCoordinate(
								points.get(i) * (double)image.getXYscale(),
								yy,
								level
								)
						));
				path.addSegment(SType.LINE, origin.offset(
						new NumericCoordinate(
								points.get(i+1) * (double)image.getXYscale(),
								yy,
								level
								)
						));
			}
			
			// Increment y, while making sure the we do height-1
			// even if we have to do one thinner pass.
			if(y+1 != image.getHeight() && y+dy>=image.getHeight())
				y = image.getHeight() - 1;
			else
				y += dy;
		} while(y<image.getHeight());
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
