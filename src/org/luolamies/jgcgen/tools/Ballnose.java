package org.luolamies.jgcgen.tools;

public class Ballnose extends Tool {

	protected Ballnose(double dia) {
		super(dia);
	}

	public double getProfile(double r) {
		return 0.0;
	}
	
	@Override
	public String toString() {
		return String.format(".3f ball", getDiameter());
	}
}
