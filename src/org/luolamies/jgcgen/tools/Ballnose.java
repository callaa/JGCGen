package org.luolamies.jgcgen.tools;

public class Ballnose extends Tool {
	private final double[] profile;
	private final static int RESOLUTION = 512;
	private final double ir;
	
	public Ballnose(double dia) {
		super(dia);
		profile = new double[RESOLUTION+1];
		ir = 1.0 / (dia/2.0) * RESOLUTION;
		for(int i=0;i<=RESOLUTION;++i) {
			profile[i] = radius-Math.sin(Math.acos(i/(double)RESOLUTION))*radius;
		}
		
	}

	public double getProfile(double rr) {
		// Uncached:
		//return radius-Math.sin(Math.acos(Math.sqrt(rr)/radius))*radius;
		
		// Cached
		return profile[(int)Math.round(Math.sqrt(rr) * ir)];
	}
	
	@Override
	public String toString() {
		return String.format("%.3f ball", getDiameter());
	}
}
