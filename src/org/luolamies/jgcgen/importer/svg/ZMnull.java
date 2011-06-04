package org.luolamies.jgcgen.importer.svg;

import org.luolamies.jgcgen.path.NumericCoordinate;

/**
 * A null Z-mapper for producing flat paths
 *
 */
public class ZMnull implements ZMap {
	static public final ZMnull INSTANCE = new ZMnull();
	
	private ZMnull() {
		
	}

	@Override
	public NumericCoordinate mapZ(NumericCoordinate c, Style elstyle, Transform matrix) {
		return c;
	}
}
