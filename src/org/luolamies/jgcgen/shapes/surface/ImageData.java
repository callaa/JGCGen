package org.luolamies.jgcgen.shapes.surface;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.luolamies.jgcgen.tools.Tool;

final class ImageData {
	private final float[] data;
	private final int width, height;
	
	private float xyscale, zscale;
	private int stepover;
	
	ImageData(String filename, boolean normalize, boolean invert, boolean flip, boolean mirror, boolean rotate) throws IOException {
		BufferedImage img = ImageIO.read(new File(filename));
		if(rotate) {
			width = img.getHeight();
			height = img.getWidth();
		} else {
			width = img.getWidth();
			height = img.getHeight();
		}
		data = new float[width*height];
		
		// Make sure the image is grayscale		
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);  
		ColorConvertOp op = new ColorConvertOp(cs, null);  
		img = op.filter(img, null);
		
		Raster raster = img.getData();
		
		// Calculate normalization coefficient if enabled.
		int offset = 0;
		float c = 1.0f/255;
		if(normalize) {
			int min=255, max=0;
			for(int x=0;x<img.getWidth();++x) {
				for(int y=0;y<img.getHeight();++y) {
					int z = raster.getSample(x, y, 0);
					if(z<min)
						min=z;
					if(z>max)
						max=z;
				}
			}
			offset = -min;
			if(min==max) {
				c = 0.0f;
			} else {
				c = 1.0f / (float)(max-min);
			}
		}
		
		// Read the image
		int i=0;
		if(rotate) {
			for(int x=0;x<img.getWidth();++x)
				for(int y=0;y<img.getHeight();++y,++i)
					data[i] = -(raster.getSampleFloat(mirror ? img.getWidth()-1-x : x, flip ? img.getHeight()-1-y : y, 0) - offset) * c;
		} else {
			for(int y=0;y<img.getHeight();++y)
				for(int x=0;x<img.getWidth();++x,++i)
					data[i] = -(raster.getSampleFloat(mirror ? img.getWidth()-1-x : x, flip ? img.getHeight()-1-y : y, 0) - offset) * c;
		}
		
		// Invert if enabled
		// Note! This looks like its reversed and here's why:
		// We want a black pixel (0) to represent the deepest point and a white pixel (1)
		// the highest. The coordinate system is set up so that Z0 is at the top of the
		// workpiece, therefore a white pixel should be at Z0 and a black pixel at Z-zscale. 
		if(!invert)
			for(i=0;i<data.length;++i)
				data[i] = -1.0f - data[i];
	}
	
	public void setScale(float xy, float z) {
		xyscale = xy;
		zscale = z;
	}

	/**
	 * Set the stepover size in pixels
	 * @param pixels
	 */
	public void setStepover(int pixels) {
		this.stepover = pixels;
	}
	
	/**
	 * Get the number of rows/columns to skip between lines
	 * @return stepover pixels
	 */
	public int getStepover() {
		return stepover;
	}
	
	/**
	 * Get the width of the image
	 * @return width in pixels
	 */
	public int getWidth() {
		return width;
	}
	
	/**
	 * Get the height of the image
	 * @return height in pixels
	 */
	public int getHeight() {
		return height;
	}
	
	/**
	 * Get the XY scale factor
	 * @return
	 */
	public float getXYscale() {
		return xyscale;
	}

	/**
	 * Get Z scale factor
	 * @return
	 */
	public float getZscale() {
		return zscale;
	}
	
	/**
	 * Get the scaled Z value at the given coordinates. Remember! The top of the image is at 0 and the bottom at -zscale.
	 * @param x x coordinate in pixels
	 * @param y y coordinate in pixels
	 * @return scaled depth
	 */
	public float getDepthAt(int x, int y) {
		if(x<0 || x>= width || y<0 || y>=height)
			throw new ArrayIndexOutOfBoundsException("Coordinate out of range (" + x + "," + y + ") [" + width + "," + height + "]");
		return data[y*width + x] * zscale;
	}
	
	/**
	 * Find the maximum depth (Z scaled value) at the given coordinates
	 * the tool can be plunged to.
	 * @param cx center X
	 * @param cy center Y
	 * @param tool
	 * @return scaled max. depth
	 */
	public float getDepthAt(int cx, int cy, Tool tool) {
		if(cx<0 || cx>= width || cy<0 || cy>=height)
			throw new ArrayIndexOutOfBoundsException("Coordinate out of range (" + cx + "," + cy + ") [" + width + "," + height + "]");
		
		int rad = (int)Math.max(1, Math.round(tool.getRadius() * xyscale));
		int minx = Math.max(0, cx-rad), miny = Math.max(0, cy-rad);
		int maxx = Math.min(width-1, cx+rad), maxy = Math.min(height-1, cy+rad);
		
		float maxz = -zscale;
		for(int y=miny;y<maxy;++y) {
			for(int x=minx;x<maxx;++x) {
				double r = Math.hypot(x-cx, y-cy); 
				if(r <= tool.getRadius()) {
					// Maximum allowed depth for the tool at this pixel when centered
					// at cx, cy and taking in account the tool shape
					float v = data[y*width + x] * zscale - (float)tool.getProfile(r);
					if(v>maxz)
						maxz = v;
				}
			}
		}
		return maxz;
	}
}
