package org.luolamies.jgcgen.shapes.surface;

import org.apache.velocity.exception.ParseErrorException;
import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.shapes.Shapes;
import org.nfunk.jep.JEP;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.SymbolTable;
import org.nfunk.jep.Variable;

/**
 * A parametric surface
 *
 */
public class Parametric extends Surface {
	private final JEP jep;
	private final Shapes shapes;
	private double resolution = 0.1;
	private double zmin=0, zscale=1.0, maxz=1.0;
	private String z0, z1;
	private double xoff, yoff;
	private Node jepnode;
	
	public Parametric(Shapes shapes) {
		this.shapes = shapes;
		jep = new JEP();
		jep.addStandardFunctions();
		jep.addStandardConstants();
		jep.setAllowUndeclared(true);
	}
	
	/**
	 * Define the function describing the surface.
	 * The function should return values in range [z0,z1]. ZScale should be used to scale to the desired height.
	 * @param func
	 * @return this
	 */
	public Parametric f(String func, String z0, String z1) {
		this.z0 = z0;
		this.z1 = z1;
		
		jepnode = jep.parseExpression(func);
		if(jep.hasError())
			throw new ParseErrorException(jep.getErrorInfo());

		return this;
	}
	
	
	/**
	 * Define the function describing the surface.
	 * The function should return values in range [0,1]. ZScale should be used to scale to the desired height.
	 * @param func
	 * @return this
	 */
	public Parametric f(String func) {
		return f(func, "0", "1");
	}
	
	/**
	 * Set the surface resolution. This affects path generation.
	 * @param res
	 * @return
	 */
	public Parametric resolution(double res) {
		if(res<=0)
			throw new IllegalArgumentException("Resolution must be greater than zero!");
		this.resolution = res;
		return this;
	}

	public double getAspectRatio() {
		return 1;
	}

	public double getDepthAt(double x, double y) {
		jep.addVariable("x", x+xoff);
		jep.addVariable("y", y+yoff);
		try {
			return -((Double)jep.evaluate(jepnode) - zmin) * zscale;
		} catch (ParseException e) {
			throw new ParseErrorException(e.getMessage());
		}
	}

	public double getMaxZ() {
		return maxz;
	}

	public double getResolution() {
		return resolution;
	}

	public void setTargetSize(double width, double height, double depth) {
		xoff = -width / 2;
		yoff = height / 2;

		// Set dimension constants
		jep.removeVariable("w"); jep.addConstant("w", width);
		jep.removeVariable("h"); jep.addConstant("h", height);
		jep.removeVariable("d"); jep.addConstant("d", depth);
		
		// Evaluate Z scaling expressions
		Node znode0 = jep.parseExpression(z0);
		Node znode1 = jep.parseExpression(z1);
		
		// Set velocity variables
		SymbolTable symbols = jep.getSymbolTable();
		for(Object key : symbols.keySet()) {
			if(!"x".equals(key) && !"y".equals(key)) {
				Variable var  = symbols.getVar((String)key);
				if(!var.isConstant()) {
					Object val = shapes.ctx.get(var.getName());
					if(val==null || !(val instanceof Number))
						throw new ParseErrorException("Variable \"" + var.getName() + "\" is not a number!");
					var.setValue(val);
				}
			}
		}
		
		// Calculate Z scaling		
		double z1;
		try {
			this.zmin = (Double) jep.evaluate(znode0);
			z1 = (Double) jep.evaluate(znode1);
		} catch(ParseException e) {
			throw new RenderException("Couldn't parse function limits!", e);
		}
		
		if(z1==this.zmin)
			throw new RenderException("Range cannot be zero!");
		
		this.maxz = depth;
		this.zscale = depth / (z1 - zmin);
	}

}
