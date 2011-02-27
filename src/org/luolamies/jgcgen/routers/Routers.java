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

import org.apache.velocity.VelocityContext;

/**
 * Get various router implementations.
 *
 */
public final class Routers {
	private final VelocityContext ctx;
	
	public Routers(VelocityContext ctx) {
		this.ctx = ctx;
	}
	
	/**
	 * Get a path code generator optimized for the specified machine type.
	 * @param name
	 * @return path router
	 */
	@SuppressWarnings("unchecked")
	public Router get(String name) {
		Class<? extends Router> clazz;
		try {
			clazz = (Class<? extends Router>) Class.forName(Routers.class.getPackage().getName() + ".R" + name);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("No implementation found for \"" + name + '"');
		}
		try {
			return clazz.getConstructor(VelocityContext.class).newInstance(ctx);
		} catch(Exception e) {
			throw new IllegalArgumentException("Implementation \"" + name + "\" is not available: " + e.getMessage());
		}
	}
}
