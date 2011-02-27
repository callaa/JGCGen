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
import java.util.Set;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

/**
 * When a split directive is used, the template is rendered multiple times
 * and outputted to different files.
 * This makes it possible to split the work into multiple parts that can be executed independantly.
 * <p>Usage: #split(i), where i is the number of the split block.
 * <p>
 * There must be at least one block with number 1. Number 0 and negative numbers are skipped
 * when splitting is enabled.
 */
public class Split extends Directive {
	/** Context variable holding the number of the current split block */
	static public final String CURSPLIT = "__cursplit";
	/** Context variable holding a Set of all encountered split blocks.
	 * If splitting is not enabled, this will not be set.
	 */
	static public final String SPLITS = "__splits";
	
	public String getName() {
		return "split";
	}

	public int getType() {
		return BLOCK;
	}

	@SuppressWarnings("unchecked")
	public boolean render(InternalContextAdapter ctx, Writer out, Node node) throws IOException {
		Set<Integer> splits = (Set<Integer>)ctx.get(SPLITS);		

		if(splits==null) {
			// Splitting not enabled, render everything. (Zero and negative blocks included)
			node.jjtGetChild(node.jjtGetNumChildren()-1).render(ctx, out);
		} else {
			Integer i = Integer.valueOf(node.jjtGetChild(0).literal(), 10);
			
			if(i>0) {
				// Only positive split numbers are rendered.
				splits.add(i);
				if(i.equals(ctx.get(CURSPLIT))) {
					// Render current split block only
					node.jjtGetChild(node.jjtGetNumChildren()-1).render(ctx, out);
				}
			}
		}
		return true;
	}

}
