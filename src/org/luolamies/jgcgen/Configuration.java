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

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;

public class Configuration {
	static private Configuration singleton;
	private Properties props;
	
	private Configuration() {
		// Load built-in defaults
		props = new Properties(); 
		try {
			props.load(props.getClass().getResourceAsStream("/config/defaults.properties"));
		} catch (IOException e) {
			System.err.println("Uh oh! Couldn't load built-in properties! " + e.getMessage());
		}		
	}
	
	/**
	 * Get the configuration singleton instance
	 * @return
	 */
	static public Configuration getInstance() {
		if(singleton==null)
			singleton = new Configuration();
		return singleton;
	}
	
	void setVariables(VelocityContext ctx) {
		for(Map.Entry<Object, Object> e : props.entrySet()) {
			String key = e.getKey().toString();
			if(key.startsWith("var.")) {
				key = key.substring(4);
				ctx.put(key, e.getValue());
			}
		}
	}
}
