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

import org.luolamies.jgcgen.path.Coordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.PathGenerator;
import org.luolamies.jgcgen.path.Path.SType;

public class Circle implements PathGenerator {
	private String radius = "1";
	private String origin;
	private boolean ccw;
	
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
	
	@Override
	public Path toPath() {
		Path p = new Path();
		
		Coordinate pos;
		if(origin!=null)
			pos = Coordinate.parse(origin).offset(Coordinate.parse("y"+radius));
		else
			pos = Coordinate.parse("y"+radius);
		
		p.addSegment(SType.MOVE, pos);
		p.addSegment(ccw ? SType.CCWARC : SType.CWARC, pos.offset(Coordinate.parse("j-"+radius), false, true));
		
		return p;
	}

}
