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
package org.luolamies.jgcgen.directive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.path.Coordinate;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;

/**
 * Capture a path as a variable.
 * <p>Usage:
 * <pre><code> #capture($var, ["relative"])
 * G00 X0 Y0
 * G01 X10 Y0...
 * 		Y-10...
 * ...
 * #end
 * </code></pre>
 */
public class Capture extends Directive {

	@Override
	public String getName() {
		return "capture";
	}

	@Override
	public int getType() {
		return BLOCK;
	}

	@Override
	public boolean render(InternalContextAdapter ctx, Writer out, Node node) throws IOException {
		
		// Get the name of the variable in which to store the pat
		String var = node.jjtGetChild(0).literal().substring(1);
		
		// Get options
		String opts="";
		if(node.jjtGetNumChildren()==3)
			opts = (String)node.jjtGetChild(1).value(ctx);
		
		boolean relative = false;
		if("relative".equals(opts))
			relative = true;
		
		// Render block contents into a buffer
		StringWriter buffer = new StringWriter();
		node.jjtGetChild(node.jjtGetNumChildren()-1).render(ctx, buffer);
		
		// Parse buffer and extract path
		Path path = new Path();
		BufferedReader codes = new BufferedReader(new StringReader(buffer.toString()));
		String line;
		Path.SType lasttype=null;
		
		NumericCoordinate prev = new NumericCoordinate(0.0,0.0,0.0);
		
		while((line=codes.readLine())!=null) {
			line = line.trim().toUpperCase();
			if(line.length()==0)
				continue;
			
			Path.SType type;
			int skip=3;
			if(line.startsWith("G00"))
				type = Path.SType.MOVE;
			else if(line.startsWith("G01"))
				type = Path.SType.LINE;
			else if(line.startsWith("G02"))
				type = Path.SType.CWARC;
			else if(line.startsWith("G03"))
				type = Path.SType.CCWARC;
			else if(line.equals("---") || line.equals("(---)"))
				type = Path.SType.SEAM;
			else {
				if(lasttype==null)
					throw new RenderException("No type set! Use G00, G01, G02 or G03");
				type = lasttype;
				skip=0;
			}
			Coordinate c = type!=Path.SType.SEAM ? Coordinate.parse(line.substring(skip)) : null;
			if(relative) {
				if(!(c instanceof NumericCoordinate))
					throw new RenderException("Only numeric coordinates supported in relative mode!");
				c = prev.offset(c);
				prev = (NumericCoordinate)c;
			}
			path.addSegment(type, c);
			lasttype = type;
		}
		
		ctx.put(var, path);
		
		return true;
	}

}
