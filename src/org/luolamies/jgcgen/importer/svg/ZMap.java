package org.luolamies.jgcgen.importer.svg;

import org.luolamies.jgcgen.path.NumericCoordinate;

/**
 * A method of mapping some input variable to cut depth
 *
 */
public interface ZMap {
	
	/**
	 * Set the Z coordinate
	 * @param c
	 * @return
	 */
	public NumericCoordinate mapZ(NumericCoordinate c, Style elstyle, Transform matrix);

}
