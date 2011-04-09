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

public class Circle implements PathGenerator {
	private String radius = "1";
	private String origin;
	private String leadin;
	private boolean ccw;
	
	private int bridges;
	private double bridgelen;
	
	public Circle origin(String origin) {
		this.origin = origin;
		return this;
	}
	
	public Circle radius(String radius) {
		this.radius = radius;
		return this;
	}
	
	public Circle cw() {
		this.ccw = false;
		return this;
	}
	
	public Circle ccw() {
		this.ccw = true;
		return this;
	}
	
	public Circle leadin(String leadin) {
		if(leadin.length()==0 || "0".equals(leadin))
			this.leadin = null;
		else
			this.leadin = leadin;
		return this;
	}
	
	public Circle bridges(int count, double len) {
		this.bridges = count;
		if(count>0) {
			if(len<=0)
				throw new IllegalArgumentException("Bridge length must be greater than zero!");
		}
		this.bridgelen = len;
		return this;
	}
	
	@Override
	public Path toPath() {
		Path p = new Path();
		
		Coordinate pos;
		
		if(bridges>0) {
			if(origin==null)
				pos = new NumericCoordinate(0.0, 0.0, null);
			else
				pos = Coordinate.parse(origin);
			
			if(leadin!=null)
				throw new RenderException("Leadin is not supported with bridges");
			final double r;
			try {
				r = Double.parseDouble(radius);
			} catch(NumberFormatException e) {
				throw new RenderException("Only numeric radius supported when using bridges");
			}
			if(bridgelen * bridges >= r*2*Math.PI)
				throw new RenderException("Bridges are longer than the circle circumference!");
			
			final double bridge = bridgelen / r;
			final double arc = Math.PI * 2 / bridges - bridge;
			double a = bridge / 2;
			for(int i=0;i<bridges;++i) {
				NumericCoordinate o0 = new NumericCoordinate(
						r * Math.cos(a),
						-r * Math.sin(a),
						null
						);
				p.addSegment(SType.MOVE, pos.offset(o0));
				
				a += arc;
				NumericCoordinate o1 = new NumericCoordinate(
						r * Math.cos(a),
						-r * Math.sin(a),
						null
						);
				o1.set(Axis.I, -o0.getValue(Axis.X));
				o1.set(Axis.J, -o0.getValue(Axis.Y));
				p.addSegment(SType.CWARC, pos.offset(o1, false, true));
				
				a += bridge;
			}
			
		} else {
			if(origin!=null)
				pos = Coordinate.parse(origin).offset(Coordinate.parse("y"+radius));
			else
				pos = Coordinate.parse("y"+radius);

			if(leadin!=null) {
				if(ccw ^ leadin.charAt(0)=='-') {
					p.addSegment(SType.MOVE, pos.offset(Coordinate.parse("x"+leadin+"y-"+leadin)));
					p.addSegment(SType.CCWARC, pos.offset(Coordinate.parse("i-"+leadin), false, true));
				} else {
					p.addSegment(SType.MOVE, pos.offset(Coordinate.parse("x-"+leadin+"y-"+leadin)));
					p.addSegment(SType.CWARC, pos.offset(Coordinate.parse("i"+leadin), false, true));
				}
			} else
				p.addSegment(SType.MOVE, pos);
			
			p.addSegment(ccw ? SType.CCWARC : SType.CWARC, pos.offset(Coordinate.parse("j-"+radius), false, true));
		}
		
		return p;
	}

}
