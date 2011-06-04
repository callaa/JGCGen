package org.luolamies.jgcgen.importer.svg;

import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.tools.Tool;

public class ZMstrokewidth implements ZMap {
	private final Tool tool;
	
	public ZMstrokewidth() {
		tool = null;
	}
	
	public ZMstrokewidth(String tool) {
		this.tool = Tool.get(tool);
	}
	
	@Override
	public NumericCoordinate mapZ(NumericCoordinate c, Style elstyle, Transform matrix) {
		try {
			double d = matrix.applyScale(elstyle.getStrokewidth());
			if(tool!=null) {
				if(d < tool.getDiameter())
					d = tool.getProfile((d*d)/4.0);
				else
					d = tool.getProfile(tool.getRadius() * tool.getRadius());
			}
			c.set(Axis.Z, -d);
		} catch(NullPointerException e) {
			throw new RenderException("Stroke width not set in SVG path!");
		}
		return c;
	}

}
