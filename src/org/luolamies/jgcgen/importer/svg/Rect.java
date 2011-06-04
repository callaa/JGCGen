package org.luolamies.jgcgen.importer.svg;

import org.luolamies.jgcgen.path.Path;
import org.w3c.dom.Element;

/**
 * Functions for converting &lt;rect&gt;s to g-code
 *
 */
class Rect {
	static public void toPath(Path path, Element rect, Transform matrix, ZMap zmap) {
		double width = Double.parseDouble(rect.getAttribute("width"));
		double height = Double.parseDouble(rect.getAttribute("height"));
		
		double x = getDouble(rect,"x");
		double y = getDouble(rect, "y");
		
		// Rounding TODO
		double ry = getDouble(rect, "ry");
		double rx = getDouble(rect, "rx");
		
		System.err.println("RECT " + x + " " + y + " " + width + " " + height);
		path.addSegment(Path.SType.MOVE, matrix.apply(x,y));
		path.addSegment(Path.SType.LINE, matrix.apply(x+width,y));
		path.addSegment(Path.SType.LINE, matrix.apply(x+width,y+height));
		path.addSegment(Path.SType.LINE, matrix.apply(x,y+height));
		path.addSegment(Path.SType.LINE, matrix.apply(x,y));
		path.addSegment(Path.SType.SEAM, null);
	}
	
	static private double getDouble(Element e, String attr) {
		String a = e.getAttribute(attr);
		double d = 0.0;
		if(a!=null && a.length()>0)
			d = Double.parseDouble(a);
		return d;
	}
}
