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

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;

public class Logger implements LogChute {
	private final boolean verbose;
	
	public Logger(boolean verbose) {
		this.verbose = verbose;
	}
	
	@Override
	public void init(RuntimeServices rs) throws Exception {
	}

	@Override
	public boolean isLevelEnabled(int level) {
		return level>0;
	}

	/**
	 * Print a fatal error message and exit with an error code.
	 * @param message message to print
	 * @param exception possible exception
	 */
	public void fatal(String message, Throwable exception) {
		if(exception!=null)
			log(1, message, exception);
		else
			log(1, message);
		System.exit(1);
	}
	
	/**
	 * Print a status message if in verbose mode.
	 * @param message
	 */
	public void status(String message) {
		if(verbose)
			System.err.println(message);
	}
	
	/**
	 * Print completion messages
	 * @param message
	 * @param value
	 * @param max
	 */
	public void progress(String message, double current, double max) {
		if(verbose) {
			System.err.println(String.format("%s: %.2f%%", message, current/max*100));
		}
	}
	
	@Override
	public void log(int level, String message) {
		if(level>0)
			System.err.println(message);
	}

	@Override
	public void log(int level, String message, Throwable exception) {
		System.err.println(exception.getClass().getSimpleName() + ": " + message);
		if(verbose && exception!=null)
			exception.printStackTrace(System.err);
	}

}
