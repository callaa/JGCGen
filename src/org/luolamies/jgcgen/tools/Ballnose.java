package org.luolamies.jgcgen.tools;

public class Ballnose extends Tool {

	protected Ballnose(double dia) {
		super(dia);
	}

	public double getProfile(double r) {
		return radius-Math.sin(Math.acos(r/radius))*radius;
	}
	
	@Override
	public String toString() {
		return String.format("%.3f ball", getDiameter());
	}
}
