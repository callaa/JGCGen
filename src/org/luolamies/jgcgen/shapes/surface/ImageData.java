package org.luolamies.jgcgen.shapes.surface;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * A heightmap image
 *
 */
final class ImageData extends Surface {
	private final float[] data;
	private final int width, height;
	
	private double xyscale, zscale;
	
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
	
	public void setTargetSize(double w, double h, double d) {
		xyscale = Math.min(w / width, h / height);
		zscale = d;
	}
	
	public double getResolution() {
		return xyscale;
	}
	
	public double getAspectRatio() {
		return (double)width / height;
	}
	
	public double getMaxZ() {
		return zscale;
	}
	
	public double getDepthAt(double x, double y) {
		// Scale X and Y
		double sx = x / xyscale - 0.5;
		double sy = y / xyscale + 0.5;
		
		double xd = sx - Math.floor(sx);
		double yd = sy - Math.floor(sy);
		int xpix = (int)Math.round(sx);
		int ypix = -(int)Math.round(sy);
		
		/*
		if(xpix<0 || ypix<0 || xpix>=width || ypix>=height)
			throw new ArrayIndexOutOfBoundsException("Coordinate out of range (" + x + "," + y + ") = (" + xpix + ", " + ypix + ") [" + width + "," + height + "]");	
		*/
		
		double val = zscale * (
			valueAt(xpix, ypix) * (xd) * (yd) +
			valueAt(xpix, ypix+1) * (xd) * (1-yd) +
			valueAt(xpix+1, ypix) * (1-xd) * (yd) +
			valueAt(xpix+1, ypix+1) * (1-xd) * (1-yd)
			)
			;
		
		return val;
	}
	
	private float valueAt(int x, int y) {
		if(x<0)
			x = 0;
		else if(x>=width)
			x = width-1;
		if(y<0)
			y = 0;
		else if(y>=height)
			y = height-1;
		return data[y*width+x];
	}
}
