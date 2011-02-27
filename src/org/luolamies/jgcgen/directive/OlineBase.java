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

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.ASTDirective;
import org.apache.velocity.runtime.parser.node.Node;
import org.luolamies.jgcgen.RenderException;

/** Base class for single-line O code helpers
 *
 */
public abstract class OlineBase extends Directive {

	/**
	 * Find the nearest parent O block of the given type
	 * @param n
	 * @return the o- number
	 */
	protected String findParent(InternalContextAdapter ctx, Node n, String... types) {
		if(n==null)
			throw new RenderException("#" + getName() + " must be nested in a #" + types[0] + " block!");
		
		if(n instanceof ASTDirective) {
			ASTDirective d = (ASTDirective)n;
			if(d.getDirectiveName().equals("o")) {
				for(int i=0;i<types.length;++i)
					if(d.jjtGetChild(0).value(ctx).equals(types[i]))
						return Oblock.nodes.get(n);
			}
		}
		
		return findParent(ctx, n.jjtGetParent(), types);
	}
	
	@Override
	public final int getType() {
		return Directive.LINE;
	}
}
