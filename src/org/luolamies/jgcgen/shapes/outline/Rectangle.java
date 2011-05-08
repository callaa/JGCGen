/*
 * This file is part of JGCGen.
 *
 * JGCGen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JGCGen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGCGen.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.luolamies.jgcgen.shapes.outline;

import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.Coordinate;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.PathGenerator;
import org.luolamies.jgcgen.path.Path.SType;

import static org.luolamies.jgcgen.path.Coordinate.parse;

/**
 * A rectangle outline
 *
 */
public class Rectangle implements PathGenerator {
	private String x,y;
	private String w;
	private String h;
	private String round;
	private String leadin;
	private boolean concave=false;
	private boolean ccw;
	
	/**
	 * Set the coordinates for the top left corner
	 * @param x
	 * @param y
	 * @return
	 */
	public Rectangle pos(String x, String y) {		
		this.x = x;
		this.y = y;
		return this;
	}
	
	/**
	 * Set the width and height
	 * @param w
	 * @param h
	 * @return
	 */
	public Rectangle size(String w, String h) {		
		this.w = w;
		this.h = h;
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
		
		x = Double.toString(minx-offset);
		y = Double.toString(maxy+offset);
		w = Double.toString(maxx-minx+2*offset);
		h = Double.toString(maxy-miny+2*offset);
		
		return this;
	}

	/**
	 * Clockwise
	 */
	public Rectangle cw() {
		this.ccw = false;
		return this;
	}
	
	/**
	 * Counter clockwise
	 */
	public Rectangle ccw() {
		this.ccw = true;
		return this;
	}
	
	/**
	 * Set leadin
	 * @param leadin
	 * @return
	 */
	public Rectangle leadin(String leadin) {
		if(leadin.length()==0 || leadin.equals("0"))
			this.leadin = null;
		else
			this.leadin = leadin;
		return this;
	}
	
	/**
	 * Set corner radius.
	 * If radius is negative, the corners will be concave.
	 * @param radius
	 * @return this
	 */
	public Rectangle round(String radius) {
		this.round = radius;
		if(round.length()>0 && round.charAt(0)=='-') {
			concave = true;
			round = round.substring(1);
		} else
			concave = false;
		return this;
	}
	
	@Override
	public Path toPath() {
		if(x==null || y==null)
			throw new NullPointerException("You must set the rectangle position!");
		if(w==null || h==null)
			throw new NullPointerException("You must set the rectangle size!");
		
		Path path = new Path();
		
		Coordinate topleft = parse("x"+x + "y"+y);
		
		Coordinate cw = parse("x" + w);
		Coordinate ch = parse("y-" + h);
		
		if(round!=null && round.length()>0 && !"0".equals(round)) {
			// Rounded rectangle
			Coordinate xr = parse("x"+round);
			Coordinate yr = parse("y"+round);
			
			if(leadin!=null) {
				path.addSegment(SType.MOVE, topleft.offset(xr).offset(parse("x"+leadin+"y-"+leadin)));
				path.addSegment(SType.CWARC, topleft.offset(xr).offset(parse("j"+leadin), false, true));
			} else {
				path.addSegment(SType.MOVE, topleft.offset(xr));
			}
			
			if(concave) {
				Coordinate btmleft = topleft.offset(ch);
				
				if(!ccw) {
					path.addSegment(SType.LINE, topleft.offset(cw).offset(xr,true,false));
					path.addSegment(SType.CCWARC,
							topleft.offset(cw).offset(parse("y-"+round+"i"+round), false, true));
					
					path.addSegment(SType.LINE, btmleft.offset(cw).offset(yr));
					path.addSegment(SType.CCWARC,
							btmleft.offset(cw).offset(parse("x-"+round+"j-"+round), false, true));
					
					path.addSegment(SType.LINE, btmleft.offset(xr));
					path.addSegment(SType.CCWARC,
							btmleft.offset(parse("y"+round+"i-"+round), false, true));
					
					path.addSegment(SType.LINE, topleft.offset(yr,true,false));
					path.addSegment(SType.CCWARC,
							topleft.offset(parse("x"+round+"j"+round), false, true));
					
				} else {
					path.addSegment(SType.CWARC,
							topleft.offset(parse("y-"+round+"i-"+round), false, true)
							);
					
					path.addSegment(SType.LINE, btmleft.offset(yr));
					path.addSegment(SType.CWARC, btmleft.offset(parse("x"+round+"j-"+round), false, true));
					
					Coordinate btmright = btmleft.offset(cw);
					path.addSegment(SType.LINE, btmright.offset(xr,true,false));
					path.addSegment(SType.CWARC, btmright.offset(parse("y"+round+"i"+round),false,true));
					
					Coordinate topright = topleft.offset(cw);
					path.addSegment(SType.LINE, topright.offset(yr,true,false));
					path.addSegment(SType.CWARC, topright.offset(parse("x-"+round+"j"+round),false,true));
					
					path.addSegment(SType.LINE, topleft.offset(xr));
				}
				
			} else {
				Coordinate topright = topleft.offset(cw);
				
				if(ccw) {
					path.addSegment(SType.CCWARC,
							topleft.offset(parse("y-"+round+"j-"+round), false, true)
							);
					
					path.addSegment(SType.LINE, topleft.offset(ch).offset(yr));
					path.addSegment(SType.CCWARC,
							topleft.offset(ch).offset(parse("x"+round+"i"+round), false, true));
					
					path.addSegment(SType.LINE, topright.offset(ch).offset(xr, true, false));
					path.addSegment(SType.CCWARC,
							topright.offset(ch).offset(parse("y"+round+"j"+round), false, true));
					
					path.addSegment(SType.LINE, topright.offset(yr, true, false));
					path.addSegment(SType.CCWARC,
							topright.offset(parse("x-"+round+"i-"+round), false, true));
					
					path.addSegment(SType.LINE, topleft.offset(xr));
				} else {
					path.addSegment(SType.LINE, topright.offset(xr, true, false));
					
					path.addSegment(SType.CWARC,
							topright.offset(parse("y-"+round+"j-"+round), false, true)
							);
					
					path.addSegment(SType.LINE, topright.offset(ch).offset(yr));
					path.addSegment(SType.CWARC,
							topright.offset(ch).offset(parse("x-"+round+"i-"+round), false, true));
					
					path.addSegment(SType.LINE, topleft.offset(ch).offset(xr));
					path.addSegment(SType.CWARC,
							topleft.offset(ch).offset(parse("y"+round+"j"+round), false, true));
					
					path.addSegment(SType.LINE, topleft.offset(yr, true, false));
					path.addSegment(SType.CWARC,
							topleft.offset(parse("x"+round+"i"+round), false, true));
				}
			}
			
		} else {
			// Sharp cornered rectangle			
			Coordinate topright = topleft.offset(cw);

			if(leadin!=null) {
				path.addSegment(SType.MOVE, topleft.offset(parse("x"+leadin+"y-"+leadin)));
				path.addSegment(SType.CWARC, topleft.offset(parse("j"+leadin), false, true));
			} else {
				path.addSegment(SType.MOVE, topleft);
			}

			if(ccw) {
				path.addSegment(SType.LINE, topleft.offset(ch));
				path.addSegment(SType.LINE, topright.offset(ch));
				path.addSegment(SType.LINE, topright);
			} else {
				path.addSegment(SType.LINE, topright);
				path.addSegment(SType.LINE, topright.offset(ch));
				path.addSegment(SType.LINE, topleft.offset(ch));
			}
			path.addSegment(SType.LINE, topleft);
		}
		
		return path;
	}
}
