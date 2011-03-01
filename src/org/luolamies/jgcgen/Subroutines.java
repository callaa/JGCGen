/*
 * This file is part of JGCGen.
 *
 * JGCGen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JGCGen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGCGen.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.luolamies.jgcgen;

import java.util.HashMap;
import java.util.Map;

/**
 * A class to keep track of subroutine and other
 * flow control structures.
 *
 */
public class Subroutines {
	static public class Sub {
		Sub(int num) {
			number = num;
			subblocks = 0;
		}
		final public int number;
		private int subblocks;		
	}

	static private Map<String, Sub> subs = new HashMap<String, Sub>();
	static private Sub current; 
	static private int nextsub = 100;
	static private int flows = 0;
	
	/**
	 * Enter a new subroutine definition
	 * @param name
	 * @return
	 */
	static public Sub enterSubroutine(String name) {
		if(current!=null)
			throw new IllegalStateException("Subroutines cannot be nested!");
		Sub sub = new Sub(nextsub++);
		subs.put(name, sub);
		current = sub;
		return sub;
	}
	
	/**
	 * End the subroutine definition
	 */
	static public void endSubroutine() {
		if(current==null)
			throw new IllegalStateException("Subroutine not open!");
		current = null;
	}
	
	/**
	 * Get the named subroutine
	 * @param name
	 * @return
	 */
	static public Sub getSubroutine(String name) {
		return subs.get(name);
	}
	
	/**
	 * Get the currently open subroutine
	 * @return subroutine or null
	 */
	static public Sub getCurrent() {
		return current;
	}
	
	static public void resetSub(int next) {
		nextsub = next;
	}
	
	static public void resetMain(int next) {
		flows = next;
	}
	
	/**
	 * Get the next available number for an O flow control block.
	 * <p>The value returned depends on if we are currently in a subroutine.
	 * @return
	 */
	static public String getNextOnumber() {
		if(current!=null) {
			return String.format("%d%03d", current.number, ++current.subblocks);
		} else {
			return String.format("%03d", ++flows);
		}
	}
}
