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
package org.luolamies.jgcgen.routers;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.velocity.VelocityContext;
import org.luolamies.jgcgen.path.Path;

public abstract class Router {
	protected final VelocityContext ctx;
	
	protected Router(VelocityContext ctx) {
		this.ctx = ctx;
	}

	// Overrides
	private Map<String, String> overrides;
	private Stack<Map<String,String>> stack;
	
	/**
	 * Override a variable
	 * @param var
	 * @param value
	 */
	public void setOverride(String var, String value) {
		if(overrides==null)
			overrides = new HashMap<String, String>();
		overrides.put(var, value);
	}
	
	/**
	 * Push current set of overrides to the stack
	 */
	public void push() {
		if(stack==null)
			stack = new Stack<Map<String,String>>();
		
		if(overrides==null)
			stack.push(null);
		else {
			Map<String, String> copy = new HashMap<String, String>();
			copy.putAll(overrides);
			stack.push(copy);
		}
	}
	
	/**
	 * Restore overrides with the set popped from the stack
	 */
	public void pop() {
		if(stack!=null)
			overrides = stack.pop();
	}
	
	/**
	 * Get a variable. If not overridden, it is got from
	 * the context.
	 * @param var
	 * @return
	 */
	protected String var(String var) {
		String val=null;
		if(overrides!=null)
			val = overrides.get(var);
		if(val==null) {
			Object v = ctx.get(var);
			if(v!=null)
				val = v.toString();
		}
		return val;
	}
	
	/**
	 * Convert the path to G-code.
	 * <p>The cut may be done in multiple passes, depending on the
	 * router implementation and variables.
	 * @param out the output
	 * @param path path to convert
	 * @param offset cutting depth offset
	 */
	public abstract void toGcode(Writer out, Path path, String offset) throws IOException;
}
