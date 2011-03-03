package org.luolamies.jgcgen.shapes.surface;

import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.tools.Tool;

interface ImageStrategy {
	Path toPath(NumericCoordinate origin, ImageData image, Tool tool);
}
