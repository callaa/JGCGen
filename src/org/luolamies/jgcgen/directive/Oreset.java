package org.luolamies.jgcgen.directive;

import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.Subroutines;

/**
 * Reset O word numbering.
 * Usage:
 * #oreset("sub|main", new number)<br>
 *
 */
public class Oreset extends Directive {

	@Override
	public String getName() {
		return "oreset";
	}

	@Override
	public int getType() {
		return LINE;
	}

	@Override
	public boolean render(InternalContextAdapter ctx, Writer out, Node node) {
		String type = node.jjtGetChild(0).value(ctx).toString();
		Integer number = (Integer)node.jjtGetChild(1).value(ctx);
		
		if("sub".equals(type))
			Subroutines.resetSub(number);
		else if("main".equals(type))
			Subroutines.resetMain(number);
		else
			throw new RenderException("Unknown type \"" + type + "\". Must be either \"sub\" or \"main\"");
		return true;
	}

}
