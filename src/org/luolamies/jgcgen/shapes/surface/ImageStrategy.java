package org.luolamies.jgcgen.shapes.surface;

import org.luolamies.jgcgen.path.Path;

interface ImageStrategy {
	Path toPath(Surface img);
}
