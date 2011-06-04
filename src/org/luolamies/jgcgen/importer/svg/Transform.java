package org.luolamies.jgcgen.importer.svg;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.luolamies.jgcgen.path.NumericCoordinate;

/**
 * A transformation matrix
 *
 */
public class Transform {
	private final double a,b,c,d,e,f;
	
	/**
	 * The unit transformation matrix.
	 */
	static public final Transform UNIT = new Transform(1,0,0,1,0,0);
	
	/**
	 * Construct the transformation matrix
	 * <pre>
	 * ┏       ┓
	 * ┃ a c e ┃
	 * ┃ b d f ┃
	 * ┃ 0 0 1 ┃
	 * ┗       ┛
	 * </code>
	 */
	public Transform(double a, double b, double c, double d, double e, double f) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;
	}
	
	/**
	 * Get a matrix for translation
	 * @param tx
	 * @param ty
	 * @return
	 */
	static public Transform translate(double tx, double ty) {
		return new Transform(1,0,0,1,tx,ty);
	}
	
	/**
	 * Get a matrix for scaling
	 * @param sx
	 * @param sy
	 * @return
	 */
	static public Transform scale(double sx, double sy) {
		return new Transform(sx,0, 0, sy, 0, 0);
	}
	
	/**
	 * Get a matrix for rotation around the origin
	 * @param a
	 * @return
	 */
	static public Transform rotate(double a) {
		return new Transform(Math.cos(a),Math.sin(a),-Math.sin(a),Math.cos(a),0,0);
	}
	
	/**
	 * Get a horizontal skewing matrix
	 * @param a
	 * @return
	 */
	static public Transform skewx(double a) {
		return new Transform(1, 0, Math.tan(a), 1, 0, 0);
	}
	
	/**
	 * Get a vertical skewing matrix
	 * @param a
	 * @return
	 */
	static public Transform skewy(double a) {
		return new Transform(1, Math.tan(a), 0, 1, 0, 0);
	}
	
	/**
	 * Get a copy of this matrix multiplied with <code>t</code>
	 * @param t
	 * @return <code>this</code> x <code>t</code>
	 */
	public Transform multiply(Transform t) {
		return new Transform(
				a*t.a + c*t.b,
				b*t.a + d*t.b,
				a*t.c + c*t.d,
				b*t.c + d*t.d,
				a*t.e + c*t.f + e,
				b*t.e + d*t.f + f
				);
	}
	
	/**
	 * Apply the transformation to a point
	 * @param src
	 * @return scaled coordinate
	 */
	public NumericCoordinate apply(double x, double y) {
		return new NumericCoordinate(
				a*x + c*y + e,
				b*x + d*y + f,
				null
				);
	}
	
	/**
	 * Scale a scalar value using the matrix.
	 * @param value
	 * @return
	 */
	public double applyScale(double value) {
		return a * value;
	}
	
	@Override
	public String toString() {
		return "[" + a + ", " + b + ", " + c + ", " + d + ", " + e + ", " + f + "]";
	}
		
	/**
	 * Parse an SVG transform attribute.
	 * @param transform
	 * @return transformation matrix
	 * @see http://www.w3.org/TR/SVG/coords.html#TransformAttribute
	 */
	public static Transform parse(String transform) {
		Transform matrix = UNIT;
		Matcher m = parsepattern.matcher(transform);
		int start=0;
		while(m.find(start)) {
			start = m.end();
			String def = m.group(1).toLowerCase();
			
			Transform tr;
			if("matrix".equals(def))
				tr = parseMatrix(m.group(2));
			else if("translate".equals(def))
				tr = parseTranslate(m.group(2));
			else if("scale".equals(def))
				tr = parseScale(m.group(2));
			else if("rotate".equals(def))
				tr = parseRotate(m.group(2));
			else if("skewx".equals(def))
				tr = parseSkew(true,m.group(2));
			else if("skewy".equals(def))
				tr = parseSkew(false,m.group(2));
			else
				throw new IllegalArgumentException("Unsupported definition: " + def);
			
			matrix = matrix.multiply(tr);
		}
		if(start==0)
			throw new IllegalArgumentException("Can't find definitions in \"" + transform + '"');
		return matrix;
	}
	static private final Pattern parsepattern = Pattern.compile("(\\w+)\\(([^)]+)\\)");
	
	private static Transform parseMatrix(String param) {
		String[] params = param.split(",");
		if(params.length!=6)
			throw new IllegalArgumentException("Matrix takes 6 parameters!");
		return new Transform(
				Double.parseDouble(params[0]),
				Double.parseDouble(params[1]),
				Double.parseDouble(params[2]),
				Double.parseDouble(params[3]),
				Double.parseDouble(params[4]),
				Double.parseDouble(params[5])
				);
	}
	
	private static Transform parseTranslate(String param) {
		String[] params = param.split(",");
		if(params.length!=1 & params.length!=2)
			throw new IllegalArgumentException("Translate takes 1 or 2 parameters!");
		
		double tx = Double.parseDouble(params[0]);
		double ty = params.length>1 ? Double.parseDouble(params[1]) : 0;
			
		return translate(tx, ty);
	}
	
	private static Transform parseScale(String param) {
		String[] params = param.split(",");
		if(params.length!=1 && params.length!=2)
			throw new IllegalArgumentException("Scale takes 1 or 2 parameters!");
		
		double sx = Double.parseDouble(params[0]);
		double sy = params.length>1 ? Double.parseDouble(params[1]) : sx;
		
		return scale(sx, sy);
	}
	
	private static Transform parseRotate(String param) {
		String[] params = param.split(",");
		if(params.length!=1 && params.length!=3)
			throw new IllegalArgumentException("Rotate takes 1 or 3 parameters!");
		
		Double a = Double.parseDouble(params[0]) / 180.0 * Math.PI;
		if(params.length>1) {
			double tx = Double.parseDouble(params[1]);
			double ty = Double.parseDouble(params[2]);
			return translate(tx,ty).multiply(rotate(a)).multiply(translate(-tx,-ty));
		} else
			return rotate(a);
	}
	
	private static Transform parseSkew(boolean x, String param) {
		Double a = Double.parseDouble(param) / 180.0 * Math.PI;
		if(x)
			return skewx(a);
		else
			return skewy(a);
	}
}
