package org.luolamies.jgcgen.tools;

/**
 * A flat endmill
 *
 */
public class Endmill extends Tool {

	protected Endmill(double dia) {
		super(dia);
	}

	public double getProfile(double r) {
		return radius-Math.sin(Math.acos(r/radius))*radius;
	}
	
	@Override
	public String toString() {
		return String.format(".3f flat", getDiameter());
	}

}
