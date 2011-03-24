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

	/**
	 * Is the given string a number?
	 * @param str
	 * @return true if string is a valid number
	 */
	static public boolean isNumber(String str) {
		try {
			Double.parseDouble(str);
		} catch(NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Convert the string to a number
	 * @param str
	 * @return number
	 */
	static public double number(String str) {
		return Double.parseDouble(str);
	}
}
