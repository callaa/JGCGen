package org.luolamies.jgcgen.math;

/**
 * A collection of static functions.
 *
 */
public class MathTools {
	/**
	 * An inclusive range from <var>from</var> to <var>to</var>
	 * @param from starting value
	 * @param to ending value
	 * @param step step size
	 * @return
	 */
	static public Iterable<Double> range(double from, double to, double step) {
		return new Range(from, to, step);
	}
	
}
