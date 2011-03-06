package org.luolamies.jgcgen.shapes.surface;

import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.Path.SType;
import org.luolamies.jgcgen.tools.Tool;

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
	private final int angle;
	private final Dir dir;
	
	public SimpleStrategy() {
		angle = 0;
		dir = Dir.ALT;
	}
	
	public SimpleStrategy(String substrategy) {
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
	
	public Path toPath(NumericCoordinate origin, ImageData image, Tool tool) {
		Path path = new Path();
		//path.addSegment(SType.MOVE, origin);
		
		final double scale = image.getXYscale();
		final int so;
		if(image.getStepover()==0)
			so = 1; // Default minimum
		else
			so = image.getStepover();

		if(angle==0) {
			// Zero angle: Scan row by row
			if(dir!=Dir.ALT) {
				int x0, dx;
				if(dir==Dir.POS) {
					x0 = 0;
					dx = 1;
				} else {
					x0 = image.getWidth()-1;
					dx = -1;
				}
				for(int y=0;y<image.getHeight();y+=so) {
					path.addSegment(SType.MOVE, origin.offset(new NumericCoordinate(x0*scale, -y*scale, (double)image.getDepthAt(x0, y, tool))));
					for(int x=1,xx=x0+dx;x<image.getWidth();++x,xx+=dx) {
						path.addSegment(SType.LINE, origin.offset(new NumericCoordinate(xx*scale,-y*scale,(double)image.getDepthAt(xx, y, tool))));
					}
				}
			} else {
				path.addSegment(SType.MOVE, origin.offset(new NumericCoordinate(null, null, (double)image.getDepthAt(0, 0, tool))));
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
			}
		} else if(angle==90) {
			// 90° angle: Scan column by column
			if(dir!=Dir.ALT) {
				int y0, dy;
				if(dir==Dir.NEG) {
					y0 = 0;
					dy = 1;
				} else {
					y0 = image.getHeight()-1;
					dy = -1;
				}
				for(int x=0;x<image.getWidth();x+=so) {
					path.addSegment(SType.MOVE, origin.offset(new NumericCoordinate(x*scale, -y0*scale, (double)image.getDepthAt(x, y0, tool))));
					for(int y=1,yy=y0+dy;y<image.getHeight();++y,yy+=dy) {
						path.addSegment(SType.LINE, origin.offset(new NumericCoordinate(x*scale,-yy*scale,(double)image.getDepthAt(x, yy, tool))));
					}
				}
			} else {
				path.addSegment(SType.MOVE, origin.offset(new NumericCoordinate(null, null, (double)image.getDepthAt(0, 0, tool))));
				for(int x=0;x<image.getWidth();x+=so) {
					int y;
					for(y=0;y<image.getHeight();++y) {
						path.addSegment(SType.LINE, origin.offset(new NumericCoordinate(x*scale,-y*scale,(double)image.getDepthAt(x, y, tool))));
					}
					x+=so; --y;
					if(x>=image.getWidth())
						break;
					for(;y>=0;--y) {
						path.addSegment(SType.LINE, origin.offset(new NumericCoordinate(x*scale,-y*scale,(double)image.getDepthAt(x, y, tool))));
					}
				}
			}
		} else {
			// Other angles are not so simple
			throw new UnsupportedOperationException("Arbitrary angles are not implemented yet!");
		}
		
		return path;
	}

}
