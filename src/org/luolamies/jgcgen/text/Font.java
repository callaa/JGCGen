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
package org.luolamies.jgcgen.text;

import java.util.HashMap;
import java.util.Map;

import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;

/**
 * Font loading and rendering.
 * <p>Supported options:
 * <ul>
 * <li>lspace: Letter spacing
 * </ul>
 *
 */
public abstract class Font {

	/** Option key for letter spacing */
	static public final String OPT_LSPACE = "lspace";
	
	private Map<String, Object> opts = new HashMap<String, Object>();
	
	public void setOption(String key, Object option) {
		this.opts.put(key, option);
	}
	
	/**
	 * Get a configuration option
	 * @param <O>
	 * @param key
	 * @param def
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected Object getOption(String key, Object def) {
		Object opt = opts.get(key);
		if(opt==null)
			opt = def;
		return opt;
	}
	
	protected Double getDouble(String key, Double def) {
		Object o = getOption(key, null);
		if(o!=null) {
			if(o instanceof Number)
				return ((Number)o).doubleValue();
			else
				return Double.valueOf(o.toString());
		}
		return def;
	}
	
	/**
	 * Get a path for the given character.
	 * @param c
	 * @return numeric path
	 */
	abstract public Path getChar(char c);
	
	abstract protected int getSpaceWidth();
	
	/**
	 * Get a path for a string.
	 * @param str
	 * @return numeric path
	 */
	public Path getString(String str) {
		Path path = new Path();
		
		NumericCoordinate offset = new NumericCoordinate(0.0, null, null);
		for(int i=0;i<str.length();++i) {
			char chr = str.charAt(i);
			if(chr==' ') {
				offset.set(Axis.X, offset.getValue(Axis.X) + getSpaceWidth());
			} else {
				Path chrpath = getChar(chr);
				
				path.addPath(chrpath.offset(offset));
				
				offset.set(Axis.X, offset.getValue(Axis.X) +
						chrpath.getDimension(Axis.X) + getDouble(OPT_LSPACE, 0.0)
						);
			}
		}
		
		return path;
	}
}
