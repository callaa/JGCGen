package org.luolamies.jgcgen.shapes.surface;

import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.Path.SType;
import org.luolamies.jgcgen.tools.Tool;

class SimpleStrategy implements ImageStrategy {
	enum Substrategy {
		ROWS,
		COLS,
		ZIGZAG
	}
	
	private final Substrategy ss;
	
	public SimpleStrategy() {
		this(Substrategy.ZIGZAG.toString());
	}
	
	public SimpleStrategy(String substrategy) {
		try {
			ss = Substrategy.valueOf(substrategy.toUpperCase());
		} catch(IllegalArgumentException e) {
			throw new IllegalArgumentException("Simple strategy has no substrategy \"" + substrategy + "\"!");
		}
	}
	
	public Path toPath(NumericCoordinate origin, ImageData image, Tool tool) {
		Path path = new Path();
		path.addSegment(SType.MOVE, origin);
		
		final double scale = image.getXYscale();
		final int so = 1 + image.getStepover();
		
		if(ss==Substrategy.ROWS) {
			for(int y=0;y<image.getHeight();y+=so) {
				for(int x=0;x<image.getWidth();++x) {
					path.addSegment(SType.LINE, origin.offset(new NumericCoordinate(x*scale,-y*scale,(double)image.getDepthAt(x, y, tool))));
				}
				path.addSegment(SType.MOVE, origin.offset(new NumericCoordinate(null, (y+1)*scale, null).undefined(Axis.Z)));
			}
		} else if(ss==Substrategy.COLS) {
			for(int x=0;x<image.getWidth();x+=so) {
				for(int y=0;y<image.getHeight();++x) {
					path.addSegment(SType.LINE, origin.offset(new NumericCoordinate(x*scale,-y*scale,(double)image.getDepthAt(x, y, tool))));
				}
				path.addSegment(SType.MOVE, origin.offset(new NumericCoordinate((x+1)*scale, null, null).undefined(Axis.Z)));
			}
		} else if(ss==Substrategy.ZIGZAG) {
			for(int y=0;y<image.getHeight();y+=so) {
				int x;
				for(x=0;x<image.getWidth();++x) {
					path.addSegment(SType.LINE, origin.offset(new NumericCoordinate(x*scale,-y*scale,(double)image.getDepthAt(x, y, tool))));
				}
				y+=so; --x;
				if(y>=image.getHeight())
					break;
				for(;x>=0;--x) {
					path.addSegment(SType.LINE, origin.offset(new NumericCoordinate(x*scale,-y*scale,(double)image.getDepthAt(x, y, tool))));
				}
			}
		} else
			throw new RuntimeException("BUG! Unhandled substrategy " + ss);
		
		return path;
	}

}
