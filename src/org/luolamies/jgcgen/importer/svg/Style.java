package org.luolamies.jgcgen.importer.svg;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

/**
 * Keep track of element styles.
 * Only a small subset of styles is supported.
 *
 */
public class Style {
	private Double strokewidth;
	
	public Style(Element el) {
		// Parse inline CSS
		String style = el.getAttribute("style");
		if(style.length()>0)
			parseCss(style);
		
		// Override CSS with attributes 
		String sw = el.getAttribute("stroke-width");
		if(sw.length()>0)
			strokewidth = Double.parseDouble(sw);
	}
	
	public Double getStrokewidth() {
		return strokewidth;
	}
	
	private void parseCss(String style) {
		Matcher m = EXPRESSION.matcher(style);
		int i=0;
		while(m.find(i)) {
			String prop = m.group(1);
			String val = m.group(2);
			if("stroke-width".equals(prop)) {
				strokewidth = Double.valueOf(val);
			}
			i = m.end();
		}
	}
	
	static private final Pattern EXPRESSION = Pattern.compile("([\\w-]+):\\s*([^;]+)(?:;|$)");
	
}
