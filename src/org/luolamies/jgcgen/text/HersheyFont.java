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
package org.luolamies.jgcgen.text;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;

/**
 * Read Hershey fonts.
 * <p>
 * Format described here: link http://emergent.unpythonic.net/software/hershey
 *
 */
public class HersheyFont extends Font {

	static private class Point {
		Point(Integer x,Integer y) { this.x = x; this.y = y; }
		Integer x, y;
	}
	
	private ArrayList<List<Point>> characters = new ArrayList<List<Point>>();
	
	public HersheyFont(InputStream ins) throws IOException {
		InputStreamReader in = new InputStreamReader(ins, "ISO-8859-1");
		
		// Parser state:
		// 0-3: Expect line number
		// 4-6: Expect vertex count
		// 7: Done
		// 7+n: Expect n more vertices
		int state=0;
		int chr;
		StringBuilder sb = new StringBuilder();
		
		List<Point> points=null;
		while((chr=in.read())>=0) {
			if(chr=='\n' || chr=='\r')
				continue;
			if(state<0)
				throw new IllegalStateException();
			else if(state<=4) {
				// Line number (ignored)
				++state;
			} else if(state <=7) {
				// Vertext count
				if(!Character.isWhitespace(chr))
					sb.append((char)chr);
				if(state==7) {
					int vertices = Integer.parseInt(sb.toString());
					sb.delete(0, sb.length());
					state = 8 + vertices;
					if(state==8) {
						//System.err.println("Font warning: No vertexes on line ");
						state=0;
					} else
						points = new ArrayList<Point>();
				} else
					++state;
			} else {
				sb.append((char)chr);
				if(sb.length()==2) {
					if(" R".equals(sb.toString())) {
						points.add(new Point(null, null));
					} else {
						points.add(new Point(sb.charAt(0) - 'R', sb.charAt(1) - 'R'));
					}
					sb.delete(0, 2);
					--state;
					if(state==8) {
						characters.add(points);
						points = null;
						state = 0;				
					}
				}
			}
		}
		if(state!=0)
			System.err.println("Warning: Font ended with parser not in state 0: " + state);
		
		postprocess();
	}
	
	/**
	 * Adjust paths. Align every character so their leftmost point is at X0.
	 */
	private void postprocess() {
		Iterator<List<Point>> i = characters.iterator();
		// skip first (space)
		i.next();
		while(i.hasNext()) {
			List<Point> points = i.next();
			int minx=Integer.MAX_VALUE;
			boolean first=true;
			for(Point p : points) {
				if(first) { first=false; continue; }
				if(p.x!=null && p.x<minx)
					minx = p.x;
			}
			first=true;
			for(Point p : points) {
				if(first) { first=false; continue; }
				if(p.x!=null)
					p.x -= minx;
			}
		}
	}

	private List<Point> charmap(char c) {
		int i;
		
		i = c - '!' + 1;
		
		try {
			return characters.get(i);
		} catch(IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	protected int getSpaceWidth() {
		return characters.get(0).get(0).y - characters.get(0).get(0).x;
	}
	
	public Path getChar(char c) {
		List<Point> chr = charmap(c);
		Path path = new Path();
		if(chr!=null) {
			if(chr.size()==1) {
				path.addSegment(Path.SType.POINT, new NumericCoordinate((double)chr.get(0).x, (double)chr.get(0).y, null));
			} else {
				Iterator<Point> i = chr.iterator();
				// The first coordinate pair is actually the left and right positions.
				i.next();
				
				Point p = i.next();
				if(p.x!=null && p.y!=null)
					path.addSegment(Path.SType.MOVE, new NumericCoordinate((double)p.x, (double)-p.y, null));
				else
					System.err.println("Font warning: First point is pen-up!");
				boolean penup = false;

				while(i.hasNext()) {
					p = i.next();
					if(p.x!=null && p.y!=null) {
						Path.SType type;
						if(penup) {
							// If we were instructed to lift the pen and there are more points to come,
							// move to the next coordinates. If this is the last coordinate, just dab down.
							if(i.hasNext())
								type = Path.SType.MOVE;
							else
								type = Path.SType.POINT;
							penup = false;
						} else
							type = Path.SType.LINE;
						
						path.addSegment(type, new NumericCoordinate((double)p.x, (double)-p.y, null));
						penup = false;
					} else {
						// Pen up: next we'll have to move over to the next coordinate
						penup = true;
					}
				}
			}
		}
		return path;
	}
}
