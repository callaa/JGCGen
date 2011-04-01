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
package org.luolamies.jgcgen.shapes;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.path.Path;

/**
 * Get various path generators.
 * <p>
 * The shapes are sorted into packages. To get one, a velocity expression like this is used: <code>$Shapes.<var>package</var>.<var>shape</var></code>. 
 * A shape is a class which implements PathGenerator. Chainable methods are used to configure the shape before converting it to a path.
 * A shape class should have either a default constructor or a constructor that takes a <code>Shapes</code> reference as its sole parameter.
 */
public class Shapes {
	public class ShapePackage {
		private final String pkg;
		
		ShapePackage(String pkg) {
			this.pkg = pkg;
		}
		
		public Object get(String generator) throws SecurityException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
			Class<?> c;
			generator = Character.toUpperCase(generator.charAt(0)) + generator.substring(1).toLowerCase();
			try {
				c = (Class<?>) Class.forName(getClass().getPackage().getName() + "." + pkg + "." + generator);
			} catch (ClassNotFoundException e) {
				throw new RenderException("Shape \"" + generator + "\" not found in package " + pkg);
			}
			
			try {
				return c.getConstructor(Shapes.class).newInstance(Shapes.this);
			} catch(NoSuchMethodException e) {
				return c.newInstance();
			}
		}
	}
	
	static private Map<String,ShapePackage> pkgcache = new HashMap<String, ShapePackage>();
	
	public final VelocityContext ctx;
	
	public Shapes(VelocityContext ctx) {
		this.ctx = ctx;
	}
	
	/**
	 * Get a shape generator package
	 * @param generator name of package which contains the generator
	 * @return
	 */
	public ShapePackage get(String pkg) {
		if(pkgcache.containsKey(pkg))
			return pkgcache.get(pkg);

		ShapePackage sp = new ShapePackage(pkg.toLowerCase());
		pkgcache.put(pkg, sp);
		
		return sp;
	}
	
	/**
	 * Get an empty path
	 * @return path
	 */
	public Path getNewPath() {
		return new Path();
	}
}
