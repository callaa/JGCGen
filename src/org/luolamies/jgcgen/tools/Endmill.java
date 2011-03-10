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
		return 0.0;
	}
	
	@Override
	public String toString() {
		return String.format("%.3f flat", getDiameter());
	}

}
