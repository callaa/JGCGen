package org.luolamies.jgcgen.shapes.pocket;

import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.PathGenerator;

/**
 * Generate a rectangular pocket
 *
 */
public class Rectangle implements PathGenerator {
	private Double x,y,w,h;
	private Double stepover;
	private Dir dir = Dir.ALT;
	
	private enum Dir {
		POS,
		NEG,
		ALT,
		CW,
		CCW
	};
	
	/**
	 * Set rectangle position
	 * @param x
	 * @param y
	 * @return
	 */
	public Rectangle pos(double x, double y) {
		this.x = x;
		this.y = y;
		return this;
	}
	
	/**
	 * Set rectangle size
	 * @param x
	 * @param y
	 * @return
	 */
	public Rectangle size(double w, double h) {
		this.w = w;
		this.h = h;
		return this;
	}
	
	/**
	 * Set rectangle position and size by corners
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @return
	 */
	public Rectangle corners(double x, double y, double x2, double y2) {
		this.x = Math.min(x, x2);
		this.y = Math.min(y, y2);
		this.w = Math.abs(x2-x);
		this.h = Math.abs(y2-y);
		return this;
	}
	
	/**
	 * Set the stepover
	 * @param stepover
	 * @return
	 */
	public Rectangle stepover(double stepover) {
		this.stepover = stepover;
		return this;
	}
	
	/**
	 * Set milling direction. Can be "pos", "neg" or "alt"
	 * @param dir
	 * @return
	 */
	public Rectangle dir(String dir) {
		this.dir = Dir.valueOf(dir.toUpperCase());
		return this;
	}
	
	/**
	 * Set the origin and size to fit this rectangle around the given path
	 * @param path
	 * @param offset
	 * @return this
	 */
	public Rectangle bounds(PathGenerator path, double offset) {
		double minx = Double.MAX_VALUE, miny = Double.MAX_VALUE;
		double maxx = -Double.MAX_EXPONENT, maxy = -Double.MAX_VALUE;
		
		try {
			for(Path.Segment s : path.toPath().getSegments()) {
				if(s.point!=null) {
					NumericCoordinate nc = (NumericCoordinate) s.point;
					Double x = nc.getValue(Axis.X);
					Double y = nc.getValue(Axis.Y);
					if(x!=null) {
						if(x<minx)
							minx = x;
						if(x>maxx)
							maxx = x;
					}
					if(y!=null) {
						if(y<miny)
							miny = y;
						if(y>maxy)
							maxy = y;
					}
				}
			}
		} catch(ClassCastException e) {
			throw new RenderException("Only numeric paths are supported!");
		}
		
		if(miny==-Double.MAX_VALUE || minx==-Double.MAX_VALUE)
			throw new RenderException("X and Y coordinates in path not defined!");
		
		x = minx-offset;
		y = miny-offset;
		w = maxx-minx+2*offset;
		h = maxy-miny+2*offset;
		
		return this;
	}

	
	public Path toPath() {
		if(x==null || y==null || w==null || h==null)
			throw new NullPointerException("You must set the rectangle corners!");
		if(stepover==null)
			throw new NullPointerException("You must set the stepover!");
		
		Path path = new Path();
		
		if(dir==Dir.CW || dir==Dir.CCW) {
			double cx = x + w/2;
			double cy = y + h/2;
			double dx, dy;
			if(h < w) {
				dx = stepover;
				dy = h/w * stepover;
			} else {
				dy = stepover;
				dx = w/h * stepover;
			}
			if(dir==Dir.CCW) {
				dy = -dy;
			}
			
			path.addSegment(Path.SType.MOVE, new NumericCoordinate(cx, cy, null));
			double x=cx, y=cy;
			for(int i=1;i<=Math.max(w,h)/stepover;i+=2) {
				y += dy*i;
				if(y>this.y+h)
					y = this.y+h;
				else if(y<this.y)
					y = this.y;
				path.addSegment(Path.SType.LINE, new NumericCoordinate(x, y, null));
				x += dx*i;
				if(x>this.x+w)
					x = this.x+w;
				path.addSegment(Path.SType.LINE, new NumericCoordinate(x, y, null));
				y -= dy*(i+1);
				if(y>this.y+h)
					y = this.y+h;
				else if(y<this.y)
					y = this.y;
				path.addSegment(Path.SType.LINE, new NumericCoordinate(x, y, null));
				x -= dx*(i+1);
				if(x<this.x)
					x = this.x;
				path.addSegment(Path.SType.LINE, new NumericCoordinate(x, y, null));
			}
			
			// Final round
			if(x>this.x)
				path.addSegment(Path.SType.LINE, new NumericCoordinate(this.x, y, null));
			
			if(dir==Dir.CW) {
				path.addSegment(Path.SType.LINE, new NumericCoordinate(this.x, this.y+h, null));
				if(y>this.y){ 
					path.addSegment(Path.SType.LINE, new NumericCoordinate(this.x+w, this.y+h, null));
					path.addSegment(Path.SType.LINE, new NumericCoordinate(this.x+w, this.y, null));
					path.addSegment(Path.SType.LINE, new NumericCoordinate(this.x, this.y, null));
					path.addSegment(Path.SType.LINE, new NumericCoordinate(this.x, y, null));
				}
			} else {
				path.addSegment(Path.SType.LINE, new NumericCoordinate(this.x, this.y, null));
				if(y<this.y+h){ 
					path.addSegment(Path.SType.LINE, new NumericCoordinate(this.x+w, this.y, null));
					path.addSegment(Path.SType.LINE, new NumericCoordinate(this.x+w, this.y+h, null));
					path.addSegment(Path.SType.LINE, new NumericCoordinate(this.x, this.y+h, null));
					path.addSegment(Path.SType.LINE, new NumericCoordinate(this.x, y, null));
				}
			}
			
		} else if(h < w) {
			double x0, x1, y0;
			if(dir==Dir.NEG) {
				x0 = x+w;
				x1 = x;
				y0 = y+h;
			} else {
				x0 = x;
				x1 = x+w;
				y0 = y;
			}
			
			if(dir==Dir.ALT)
				path.addSegment(Path.SType.MOVE, new NumericCoordinate(x0,y0,null));
			
			double j=-stepover;
			while(j<h) {
				j += stepover;
				if(j>h)
					j=h;
				
				double yy = dir==Dir.NEG ? y0-j : y0+j;
				if(dir!=Dir.ALT)
					path.addSegment(Path.SType.MOVE, new NumericCoordinate(x0,yy,null));
				path.addSegment(Path.SType.LINE, new NumericCoordinate(x1, yy, null));
				
				if(dir==Dir.ALT && j<h) {
					j += stepover;
					if(j>h)
						j=h;
					yy = y0 + j;
					path.addSegment(Path.SType.LINE, new NumericCoordinate(x1, yy, null));
					path.addSegment(Path.SType.LINE, new NumericCoordinate(x0, yy, null));
					if(j<h)
						path.addSegment(Path.SType.LINE, new NumericCoordinate(x0, Math.min(y0+h, yy+stepover), null));
				}
			}
		} else {
			double y0, y1, x0;
			if(dir==Dir.NEG) {
				x0 = x+w;
				y0 = y+h;
				y1 = y;
			} else {
				x0 = x;
				y0 = y;
				y1 = y+h;
			}
			
			if(dir==Dir.ALT)
				path.addSegment(Path.SType.MOVE, new NumericCoordinate(x0,y0,null));
			
			double j=-stepover;
			while(j<w) {
				j += stepover;
				if(j>w)
					j=w;
				
				double xx = dir==Dir.NEG ? x0-j : x0+j;
				if(dir!=Dir.ALT)
					path.addSegment(Path.SType.MOVE, new NumericCoordinate(xx,y0,null));
				path.addSegment(Path.SType.LINE, new NumericCoordinate(xx, y1, null));
				
				if(dir==Dir.ALT && j<w) {
					j += stepover;
					if(j>w)
						j=w;
					xx = x0 + j;
					path.addSegment(Path.SType.LINE, new NumericCoordinate(xx, y1, null));
					path.addSegment(Path.SType.LINE, new NumericCoordinate(xx, y0, null));
					if(j<w)
						path.addSegment(Path.SType.LINE, new NumericCoordinate(Math.min(x0+w, xx+stepover), y0, null));
				}
			}
		}
				
		return path;
	}

}
