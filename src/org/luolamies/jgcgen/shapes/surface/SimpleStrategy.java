package org.luolamies.jgcgen.shapes.surface;

import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.Path.SType;

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
	
	public Path toPath(ImageData img) {
		Path path = new Path();
		//path.addSegment(SType.MOVE, origin);
		
		final double scale = img.getXYscale();
		final int so;
		if(img.getStepover()==0)
			so = 1; // Default minimum
		else
			so = img.getStepover();

		final NumericCoordinate o = image.getOrigin();
		
		if(angle==0) {
			// Zero angle: Scan row by row
			if(dir!=Dir.ALT) {
				int x0, dx;
				if(dir==Dir.POS) {
					x0 = 0;
					dx = 1;
				} else {
					x0 = img.getWidth()-1;
					dx = -1;
				}
				for(int y=0;y<img.getHeight();y+=so) {
					path.addSegment(SType.MOVE, o.offset(new NumericCoordinate(x0*scale, -y*scale, (double)img.getDepthAt(x0, y, image.getTool()))));
					for(int x=1,xx=x0+dx;x<img.getWidth();++x,xx+=dx) {
						path.addSegment(SType.LINE, o.offset(new NumericCoordinate(xx*scale,-y*scale,(double)img.getDepthAt(xx, y, image.getTool()))));
					}
				}
			} else {
				path.addSegment(SType.MOVE, o.offset(new NumericCoordinate(null, null, (double)img.getDepthAt(0, 0, image.getTool()))));
				for(int y=0;y<img.getHeight();y+=so) {
					int x;
					for(x=0;x<img.getWidth();++x) {
						path.addSegment(SType.LINE, o.offset(new NumericCoordinate(x*scale,-y*scale,(double)img.getDepthAt(x, y, image.getTool()))));
					}
					y+=so; --x;
					if(y>=img.getHeight())
						break;
					for(;x>=0;--x) {
						path.addSegment(SType.LINE, o.offset(new NumericCoordinate(x*scale,-y*scale,(double)img.getDepthAt(x, y, image.getTool()))));
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
					y0 = img.getHeight()-1;
					dy = -1;
				}
				for(int x=0;x<img.getWidth();x+=so) {
					path.addSegment(SType.MOVE, o.offset(new NumericCoordinate(x*scale, -y0*scale, (double)img.getDepthAt(x, y0, image.getTool()))));
					for(int y=1,yy=y0+dy;y<img.getHeight();++y,yy+=dy) {
						path.addSegment(SType.LINE, o.offset(new NumericCoordinate(x*scale,-yy*scale,(double)img.getDepthAt(x, yy, image.getTool()))));
					}
				}
			} else {
				path.addSegment(SType.MOVE, o.offset(new NumericCoordinate(null, null, (double)img.getDepthAt(0, 0, image.getTool()))));
				for(int x=0;x<img.getWidth();x+=so) {
					int y;
					for(y=0;y<img.getHeight();++y) {
						path.addSegment(SType.LINE, o.offset(new NumericCoordinate(x*scale,-y*scale,(double)img.getDepthAt(x, y, image.getTool()))));
					}
					x+=so; --y;
					if(x>=img.getWidth())
						break;
					for(;y>=0;--y) {
						path.addSegment(SType.LINE, o.offset(new NumericCoordinate(x*scale,-y*scale,(double)img.getDepthAt(x, y, image.getTool()))));
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
