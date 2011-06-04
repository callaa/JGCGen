package org.luolamies.jgcgen.importer.svg;

import java.util.Iterator;
import java.util.List;

import org.luolamies.jgcgen.path.Path;
import org.w3c.dom.Element;

/**
 * Functions for parsing and converting SVG paths to g-code
 *
 */
class SvgPath {
	static public void toPath(Path path, Element el, Transform matrix, ZMap zmap) {
		new SvgPath(new Style(el), el.getAttribute("d"), path, matrix, zmap).parse();
	}
	
	/** Depth mapper */
	private final ZMap zmap;
	
	/** Element style */
	private final Style style;
	
	/** (Sub)path starting coordinates */
	private Double startx, starty;
	
	/** The current "pen" position */
	private double posx, posy;
	/** Previous control point (curves) */
	private CP oldctrl;
	
	/** The current position in data string */
	private int pos;
	/** The path description */
	private final String data;
	
	private final Transform matrix;
	private final Path path;
	
	private SvgPath(Style style, String data, Path path, Transform matrix, ZMap zmap) {
		this.data = data;
		this.path = path;
		this.matrix = matrix;
		this.zmap = zmap;
		this.style = style;
	}
	
	private void parse() {
		while(pos<data.length()) {
			char d = data.charAt(pos);
			++pos;
			
			if(Character.isWhitespace(d))
				continue;
			else if(d=='m' || d=='M')
				moveto(d=='M');
			else if(d=='l' || d=='L')
				lineto(d=='L');
			else if(d=='v' || d=='V')
				lineto(true, d=='V');
			else if(d=='h' || d=='H')
				lineto(false, d=='H');
			else if(d=='c' || d=='C')
				cubicbezier(d=='C');
			else if(d=='s' || d=='S')
				shortcubicbezier(d=='S');
			else if(d=='q' || d=='Q')
				quadbezier(d=='Q');
			else if(d=='t' || d=='T')
				shortquadbezier(d=='T');
			else if(d=='a' || d=='A')
				ellipsearc(d=='A');
			else if(d=='z' || d=='Z') {
				// Return home
				if(Math.abs(posx-startx) + Math.abs(posy-starty) > 0.00001) {
					posx = startx;
					posy = starty;
					path.addSegment(Path.SType.LINE, zmap.mapZ(matrix.apply(posx, posy), style, matrix));
				}
			} else {
				throw new IllegalArgumentException("Unsupported command: '" + d + "' at index " + (pos-1));
			}
		}
	}
	
	/**
	 * Move to a new point. This maps directly to our MOVE path segment type.
	 * Coordinates after a move are implicitly of line type.
	 * @param abs
	 */
	private void moveto(boolean abs) {
		CP move = coordinatepair(abs);
		posx = move.x;
		posy = move.y;
		oldctrl = null;
		
		path.addSegment(Path.SType.MOVE, zmap.mapZ(matrix.apply(posx, posy), style, matrix));
		
		startx = posx;
		starty = posy;
		
		// See if lines follow this
		while(skip_ws()) {
			if(Character.isDigit(data.charAt(pos)) || data.charAt(pos)=='-') {
				CP line = coordinatepair(abs);
				posx = line.x;
				posy = line.y;
				path.addSegment(Path.SType.LINE, matrix.apply(posx, posy));
			} else {
				break;
			}
		}
	}
	
	/**
	 * Line to new coordinates.
	 * @param abs
	 */
	private void lineto(boolean abs) {
		while(skip_ws()) {
			if(Character.isDigit(data.charAt(pos)) || data.charAt(pos)=='-') {
				CP line = coordinatepair(abs);
				posx = line.x;
				posy = line.y;
				oldctrl = null;
				path.addSegment(Path.SType.LINE, matrix.apply(posx, posy));
			} else {
				break;
			}
		}
	}
	
	/**
	 * A vertical or horizontal line
	 * @param vertical
	 * @param abs
	 */
	private void lineto(boolean vertical, boolean abs) {
		while(skip_ws()) {
			if(Character.isDigit(data.charAt(pos)) || data.charAt(pos)=='-') {
				int start = pos;
				skip_num();
				double val = Double.parseDouble(data.substring(start,pos));
				
				if(abs) {
					if(vertical)
						posy = val;
					else
						posx = val;
				} else {
					if(vertical)
						posy += val;
					else
						posx += val;
				}
				oldctrl = null;
				path.addSegment(Path.SType.LINE, matrix.apply(posx, posy));
			} else {
				break;
			}
		}
	}
	
	/**
	 * A quadratic bezier curve to new coordinates
	 * @param abs
	 */
	private void quadbezier(boolean abs) {
		while(skip_ws()) {
			if(Character.isDigit(data.charAt(pos)) || data.charAt(pos)=='-') {
				CP cp0 = new CP(posx, posy);
				CP cp1 = coordinatepair(abs);
				CP cp2 = coordinatepair(abs);
				
				// Quadratic splines can be expressed as cubic
				cubicbezier(
						cp0.add(cp1.sub(cp0).mult(2.0/3.0)),
						cp1.add(cp2.sub(cp0).mult(1.0/3.0)),
						cp2
						);
			} else {
				break;
			}
		}
	}
	
	/**
	 * Quadratic bezier curve to new coordinates, shorthand
	 * @param abs
	 */
	private void shortquadbezier(boolean abs) {
		while(skip_ws()) {
			if(Character.isDigit(data.charAt(pos)) || data.charAt(pos)=='-') {
				CP cp0 = new CP(posx, posy);
				CP cp1 = cp0.mult(2).sub(oldctrl!=null ? oldctrl : cp0);
				CP cp2 = coordinatepair(abs);
				
				cubicbezier(
						cp0.add(cp1.sub(cp0).mult(2.0/3.0)),
						cp1.add(cp2.sub(cp0).mult(1.0/3.0)),
						cp2
						);
			} else {
				break;
			}
		}
	}
	
	/**
	 * Cubic bezier curve to new coordinates.
	 * @param abs
	 */
	private void cubicbezier(boolean abs) {
		while(skip_ws()) {
			if(Character.isDigit(data.charAt(pos)) || data.charAt(pos)=='-') {
				CP cp1 = coordinatepair(abs);
				CP cp2 = coordinatepair(abs);
				CP cp3 = coordinatepair(abs);
				
				cubicbezier(cp1, cp2, cp3);
			} else {
				break;
			}
		}
	}
	
	/**
	 * Cubic bezier curve to new coordinates, shorthand
	 * @param abs
	 */
	private void shortcubicbezier(boolean abs) {
		while(skip_ws()) {
			if(Character.isDigit(data.charAt(pos)) || data.charAt(pos)=='-') {
				CP cp0 = new CP(posx, posy);
				CP cp1 = cp0.mult(2).sub(oldctrl!=null ? oldctrl : cp0);
				CP cp2 = coordinatepair(abs);
				CP cp3 = coordinatepair(abs);
				
				cubicbezier(cp1, cp2, cp3);
			} else {
				break;
			}
		}
	}
	
	private void cubicbezier(CP cp1, CP cp2, CP cp3) {
		List<CP> approx = Curves.linearapproxCubicBezier(new CP(posx, posy), cp1, cp2, cp3);
		
		posx = cp3.x;
		posy = cp3.y;		
		oldctrl = cp2;

		Iterator<CP> i = approx.iterator();
		i.next(); // skip first point
		while(i.hasNext()) {
			CP cp = i.next();
			path.addSegment(Path.SType.LINE, matrix.apply(cp.x, cp.y));
		}
	}
	
	/**
	 * Elliptiacl arc to new coordinatse
	 * @param abs
	 * @see http://www.w3.org/TR/SVG/paths.html#PathDataEllipticalArcCommands
	 * @see http://www.w3.org/TR/SVG/implnote.html#ArcImplementationNotes
	 */
	private void ellipsearc(boolean abs) {
		oldctrl = null;
		while(skip_ws()) {
			if(Character.isDigit(data.charAt(pos)) || data.charAt(pos)=='-') {
				// Radii
				CP radius = coordinatepair(true).abs();
				
				// X-axis rotation
				skip_wsc();
				int start = pos;
				skip_num();
				double xrot = Double.parseDouble(data.substring(start, pos)) % 360.0 / 180.0 * Math.PI;
				
				// Large-arc flag
				skip_wsc();
				start = pos;
				skip_num();
				boolean largearc = Integer.parseInt(data.substring(start, pos)) != 0;
				
				// Sweep flag
				skip_wsc();
				start = pos;
				skip_num();
				boolean sweep = Integer.parseInt(data.substring(start, pos)) != 0;
				
				// End point
				CP arc = coordinatepair(abs);
				
				// If endpoints are  the same, arc is omitted
				if(Math.abs(arc.x-posx) < 0.0001 && Math.abs(arc.y-posy) < 0.0001)
					continue;

				CP arc0 = new CP(posx, posy);
				posx = arc.x;
				posy = arc.y;
				
				// If radius is 0, this acts like lineto 
				if(radius.x==0 || radius.y==0) {
					path.addSegment(Path.SType.LINE, matrix.apply(posx, posy));
					continue;
				}
				
				// If radii are equal, we can use a simple circular arc
				/* TODO
				if(Math.abs(radius.x - radius.y) < 0.00001) {
					path.addSegment(SType.CWARC, matrix.apply(posx, posy));
					continue;
				}
				*/
				
				List<CP> arcpoints = Curves.ellipseArc(arc0, radius, xrot, largearc, sweep, arc);
				
				for(CP cp : arcpoints)
					path.addSegment(Path.SType.LINE, matrix.apply(cp.x, cp.y));
				
			} else {
				break;
			}
		}
	}
	
	/**
	 * Consume two numbers separated by a comma or whitespace
	 * @param abs absolute coordinates? If not, pos[xy] will be added
	 * @return
	 */
	private CP coordinatepair(boolean abs) {
		double x, y;
		// Find X
		skip_wsc();
		int start = pos;
		skipto_wsc();
		x = Double.parseDouble(data.substring(start,pos));
		
		// Find Y
		skip_wsc();
		start = pos;
		skip_num();
		y = Double.parseDouble(data.substring(start,pos));
		
		if(abs)
			return new CP(x,y);
		else
			return new CP(posx+x, posy+y);
	}
	
	/**
	 * Increment position until a character that is not part of a number [0-9.-e] is encountered
	 */
	private void skip_num() {
		while(pos < data.length() && (Character.isDigit(data.charAt(pos)) || data.charAt(pos)=='-' || data.charAt(pos)=='.' || data.charAt(pos)=='e')) ++pos;
	}
	
	/**
	 * Increment position until a character that is not whitespace encountered
	 * @return false if end of data reached
	 */
	private boolean skip_ws() {
		while(pos<data.length() && Character.isWhitespace(data.charAt(pos))) ++pos;
		return pos<data.length();
	}
	
	/**
	 * Increment position until a character that is not whitespace or a comma is encountered
	 */
	private void skip_wsc() {
		while(Character.isWhitespace(data.charAt(pos)) || data.charAt(pos)==',') ++pos;
	}
	
	/**
	 * Increment position until a character that is whitespace or a comma is encountered
	 */
	private void skipto_wsc() {
		while(!Character.isWhitespace(data.charAt(pos)) && data.charAt(pos)!=',') ++pos;
	}
	
	/**
	 * Coordinate pair
	 *
	 */
	static class CP {
		CP(double x, double y) { this.x = x; this.y = y; }
		final double x, y;
		
		CP add(CP cp) {
			return new CP(x+cp.x, y+cp.y);
		}
		
		CP sub(CP cp) {
			return new CP(x-cp.x, y-cp.y);
		}
		
		CP mult(double val) {
			return new CP(x*val, y*val);
		}
		
		CP abs() {
			if(x>0 && y>0)
				return this;
			return new CP(Math.abs(x), Math.abs(y));
		}
	}
}
