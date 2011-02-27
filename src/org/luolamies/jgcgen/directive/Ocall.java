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

import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.parser.node.Node;
import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.Subroutines;

/**
 * Call a subroutine.
 * <p>Usage: #ocall "name"
 *
 */
public class Ocall extends OlineBase {

	@Override
	public String getName() {
		return "ocall";
	}

	@Override
	public boolean render(InternalContextAdapter ctx, Writer out, Node node)
			throws IOException {
		
		String name = node.jjtGetChild(0).value(ctx).toString();
		
		Subroutines.Sub sub = Subroutines.getSubroutine(name);
		
		if(sub==null)
			throw new RenderException("Subroutine \"" + name + "\" not yet defined!"); 
		
		out.write("o" + sub.number + " call");
		for(int i=1;i<node.jjtGetNumChildren();++i) {
			out.write(" [");
			out.write(node.jjtGetChild(i).value(ctx).toString());
			out.write(']');
		}
		out.write('\n');
		return true;
	}

}
