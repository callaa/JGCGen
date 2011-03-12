package org.luolamies.jgcgen.directive;

import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.nfunk.jep.JEP;
import org.nfunk.jep.SymbolTable;
import org.nfunk.jep.Variable;

/**
 * Parse and evaluate mathematical expressions.
 * <p>
 * Usage: <code#e("<i>expression</i>")</code> or
 * <code>#e($var, "<i>expression</i>")</code>
 *
 */
public class Expression extends Directive {

	public String getName() {
		return "e";
	}

	@Override
	public int getType() {
		return LINE;
	}

	@Override
	public boolean render(InternalContextAdapter ctx, Writer out, Node node)
			throws IOException, ResourceNotFoundException, ParseErrorException,
			MethodInvocationException {
		
		String store = null;
		if(node.jjtGetNumChildren()==2)
			store = node.jjtGetChild(0).literal().substring(1);
		String expr = (String)node.jjtGetChild(store!=null ? 1 : 0).value(ctx);

		parser.parseExpression(expr);
		if(parser.hasError())
			throw new ParseErrorException(parser.getErrorInfo());
		
		// Load variables
		SymbolTable symbols = parser.getSymbolTable();
		for(Object key : symbols.keySet()) {
			Variable var  = symbols.getVar((String)key);
			if(!var.isConstant()) {
				Object val = ctx.get(var.getName());
				if(val==null || !(val instanceof Number))
					throw new ParseErrorException("Variable \"" + var.getName() + "\" is not a number!");
				var.setValue(val);
			}
		}
		
		// Print out or save the result
		if(store!=null)
			ctx.put(store, parser.getValue());
		else
			out.write(String.format("%.3f", parser.getValue()));
		
		return true;
	}

	static private final JEP parser = new JEP();
	
	static {
		parser.addStandardFunctions();
		parser.addStandardConstants();
		parser.setAllowUndeclared(true);
	}
}
