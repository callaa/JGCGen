package org.luolamies.jgcgen.tools;

public abstract class Tool {
	protected double diameter, radius;
	
	/**
	 * Parse a tool definition and return a tool.
	 * Format is: <code><var>dia</var> <var>type</var> [<var>options</var>].
	 * <p>
	 * Currently supported types are <i>flat</i>, <i>ball</i> and <i>v</i>.
	 * V takes an angle as a parameter.
	 * @param def
	 * @return tool
	 */
	static public Tool get(String def) {
		String[] d = def.split(" ");
		double dia = Double.parseDouble(d[0]);
		if("flat".equalsIgnoreCase(d[1]))
			return new Endmill(dia);
		else if("ball".equalsIgnoreCase(d[1]))
			return new Ballnose(dia);
		else if("v".equalsIgnoreCase(d[1]))
				return new Vbit(dia, Double.parseDouble(d[2]));
		else
			throw new IllegalArgumentException("Unsupported tool type \"" + d[0] + '"');
	}
	
	protected Tool(double dia) {
		this.diameter = dia;
		this.radius = dia/2.0;
	}
	
	/**
	 * Get the tool diameter
	 * @return diameter
	 */
	public final double getDiameter() {
		return diameter;
	}
	
	/**
	 * Get the tool radius
	 * @return radius
	 */
	public final double getRadius() {
		return radius;
	}
	
	/**
	 * Get the Z offset relative to tool tip at distance r from tool center.
	 * @param r distance from center squared
	 * @return Z offset
	 */
	public abstract double getProfile(double rr);
}
