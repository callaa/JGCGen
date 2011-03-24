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

import org.apache.velocity.app.event.IncludeEventHandler;

/**
 * This event handler does the following transformations to
 * parse/include resource names:
 * <ul>
 * <li>Automatically add "/velocity/" to the beginning of resources
 * that start with "jgc_".
 * <li>If the resource has no suffix, append ".vm"
 * </ul>
 *
 */
public class IncludeHandler implements IncludeEventHandler {

	@Override
	public String includeEvent(String resource, String path, String directive) {
		int dot = resource.lastIndexOf('.');
		if(dot<0)
			resource = resource + ".jgc";
		
		if(resource.startsWith("jgc_"))
			resource = "/velocity/" + resource;
		
		return resource;
	}

}
