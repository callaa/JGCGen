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
import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.Subroutines;

/**
 * A directive to make using O codes easier.
 * Supported O codes are:
 * <ul>
 * <li>O- sub [name]</li>
 * <li>O- until [condition]</li>
 * <li>O- while [condition]</li>
 * <li>O- if [condition]</li>
 * <li>O- repeat [count]</li>
 * </ul>
 * @see Ocall, Oelse, Obreak and Ocontinue
 */
public final class Oblock extends Directive {
	static protected Map<Node, String> nodes = new IdentityHashMap<Node, String>();
	
	@Override
	public String getName() {
		return "o";
	}

	@Override
	public int getType() {
		return Directive.BLOCK;
	}
	
	@Override
	public boolean render(InternalContextAdapter ctx, Writer out, Node node)
			throws IOException, ResourceNotFoundException, ParseErrorException,
			MethodInvocationException
	{
		String type = node.jjtGetChild(0).value(ctx).toString();
		String param = node.jjtGetChild(1).value(ctx).toString();
		
		String num, pre, post;
		// Subroutines cannot be nested
		if("sub".equals(type)) {
			Subroutines.Sub s = Subroutines.enterSubroutine(param);
			
			num = Integer.toString(s.number);
			pre = "sub (" + param + ')';
			post = "endsub";
		} else {
			// All other blocks can be nested
			num = Subroutines.getNextOnumber();
			
			if("repeat".equals(type)) {
				pre = "repeat [" + param + ']';
				post = "endrepeat";
			} else if("until".equals(type)) {
				pre = "do";
				post = "until [" + param + ']';
			} else if("while".equals(type)) {
				pre = "while [" + param + ']';
				post = "endwhile";
			} else if("if".equals(type)) {
				pre = "if [" + param + ']';
				post = "endif";
			} else
				throw new RenderException("Unsupported O code type \"" + type + '"');
		}

		nodes.put(node, num);
		
		String op = "o" + num + ' ';
		out.write(op);
		out.write(pre);
		out.write('\n');
		node.jjtGetChild(node.jjtGetNumChildren()-1).render(ctx, out);
		out.write(op);
		out.write(post);
		out.write('\n');
		if("sub".equals(type))
			Subroutines.endSubroutine();
		return true;
	}

}
