package org.luolamies.jgcgen.shapes.surface;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.Raster;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.luolamies.jgcgen.Files;
import org.luolamies.jgcgen.JGCGenerator;

/**
 * A heightmap image
 *
 */
final class ImageData extends Surface {
	private final float[] data;
	private final int width, height;
	
	private double xyscale, zscale;
	
	ImageData(String filename, boolean normalize, boolean invert, boolean flip, boolean mirror, boolean rotate) throws IOException {
		BufferedImage img = ImageIO.read(Files.get(filename));
		if(rotate) {
			width = img.getHeight();
			height = img.getWidth();
		} else {
			width = img.getWidth();
			height = img.getHeight();
		}
		
		JGCGenerator.getLogger().status("ImageData(" + filename + "): " + img.getWidth() + "x" + img.getHeight() + " px.");
		
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
		xyscale = Math.min(w / (width-1), h / (height-1));
		zscale = d;
		JGCGenerator.getLogger().status("ImageData " + w + "x" + h + ", xyscale=" + xyscale + ", zscale=" + zscale);
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
		double sx = x / xyscale;
		double sy = -y / xyscale;
		
		// Linear interpolation
		int ix = (int)Math.floor(sx);
		int iy = (int)Math.floor(sy);
		double fx = sx-ix;
		double fy = sy-iy;

		//if(ix<0 || iy<0 || ix>=width || iy>=height)
		//	throw new ArrayIndexOutOfBoundsException("Coordinate out of range (" + x + "," + y + ") = (" + ix + ", " + iy+ ") [" + width + "," + height + "]");			

		if(ix<0)
			ix = 0;
		else if(ix>width-2)
			ix = width-2;
		if(iy<0)
			iy = 0;
		else if(iy>height-2)
			iy = height-2;
		
		int yy = width * iy;
		
		double fx1 = 1.0-fx;
		double fy1 = 1.0-fy;
		return zscale * (
				data[yy + ix] * (fx1 * fy1) +
			    data[yy + ix + 1] * (fx * fy1) +
			    data[yy + width + ix] * (fx1 * fy) +
			    data[yy + width + ix + 1] * (fx*fy)
			    )
		    ;

		// Nearest neighbor interpolation (this is only good for testing really)
		//return zscale * data[((int)Math.round(sy) * width + (int)Math.round(sx))];
	}
}
