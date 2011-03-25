package org.luolamies.jgcgen.tools;

/**
 * A conical engraving cutter.
 *
 */
public class Vbit extends Tool {
	private double a;
	
	/**
	 * 
	 * @param dia cutter diameter
	 * @param ia included angle
	 */
	public Vbit(double dia, double ia) {
		super(dia);
		
		a = Math.tan(ia / 2.0 / 180.0 * Math.PI);
	}
	
	@Override
	public double getProfile(double rr) {
		return Math.sqrt(rr) * a;
	}

}
