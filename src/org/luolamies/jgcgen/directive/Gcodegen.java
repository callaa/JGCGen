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
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.PathGenerator;
import org.luolamies.jgcgen.routers.Router;

/**
 * Generate G-code from a path
 * <p>Usage:
 * <code>#g ($router, $path, [$offset[)</code> or
 * <code>#g ($path, [$offset])</code>
 * <p>If $router is not defined, the default router $router will be used.
 */
public class Gcodegen extends Directive {

	@Override
	public String getName() {
		return "g";
	}

	@Override
	public int getType() {
		return LINE;
	}

	@Override
	public boolean render(InternalContextAdapter ctx, Writer out, Node node)
			throws IOException, ResourceNotFoundException, ParseErrorException,
			MethodInvocationException {
		if(node.jjtGetNumChildren()!=2 && node.jjtGetNumChildren()!=3)
			throw new RenderException("#g takes only two or three parameters!");
		
		Object par1 = node.jjtGetChild(0).value(ctx);
		Object par2 = node.jjtGetChild(1).value(ctx);
		Object par3 = node.jjtGetNumChildren()==3 ? node.jjtGetChild(2).value(ctx) : null;
		
		Router r;
		PathGenerator pathg;
		String offset;
		if(par1 instanceof Router) {
			r = (Router)par1;
			pathg = (PathGenerator)par2;
			offset = (String)par3;
		} else if(par1 instanceof PathGenerator) {
			r = (Router)ctx.get("router");
			if(r==null)
				throw new RenderException("Default router $router not set!");
			pathg = (PathGenerator)par1;
			offset = (String)par2;
		} else {
			throw new RenderException("First parameter must be either a Router or a PathGenerator!");
		}
		
		Path path = pathg.toPath();
		if(path.getSize()>0)
			r.toGcode(out, path, offset);
		return true;
	}

}
