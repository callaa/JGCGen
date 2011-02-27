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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.luolamies.jgcgen.RenderException;

public class Fonts {
	private final File workdir;
	
	public Fonts(File workdir) {
		this.workdir = workdir;
	}
	
	public Font get(String name) {
		String type;
		if(name.endsWith(".jhf"))
			type = "Hershey";
		else
			throw new RenderException("Can't figure out font type from filename! Use $fonts.get(\"file\", \"type\")");
		
		return get(name, type);
	}
	
	@SuppressWarnings("unchecked")
	public Font get(String name, String type) {
		Class<? extends Font> fclass;
		try {
			fclass = (Class<? extends Font>) Class.forName(getClass().getPackage().getName() + "." + type + "Font");
		} catch (ClassNotFoundException e1) {
			throw new RenderException("Font type \"" + type + "\" not supported!");
		}
		
		InputStream in;
		File file = new File(workdir, name);
		if(file.isFile()) {
			try {
				in = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				in = null;
			}
		} else
			in = getClass().getResourceAsStream("/fonts/" + name);
		
		if(in==null)
			throw new RenderException("Can't find font: " + name);
		
		try {
			return fclass.getConstructor(InputStream.class).newInstance(in);
		} catch(Exception e) {
			throw new RenderException("Error while trying to construct handler for font \"" + type + "\": " + e.getMessage(), e);
		} finally {
			try { in.close(); } catch (IOException e) { }
		}
	}
}
