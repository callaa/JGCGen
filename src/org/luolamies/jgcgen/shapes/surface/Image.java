package org.luolamies.jgcgen.shapes.surface;

import java.io.IOException;

import org.luolamies.jgcgen.JGCGenerator;
import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.Coordinate;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.PathGenerator;
import org.luolamies.jgcgen.tools.Tool;

public class Image implements PathGenerator {
	// Configuration
	private NumericCoordinate topleft = new NumericCoordinate(0.0, 0.0, 0.0);
	private String filename;
	private String strategy="simple";
	private boolean invert, normalize, flip, mirror, rotate;
	private double xsize=-1, ysize=-1, zscale=1.0;
	private String stepover="";
	private Tool tool;
	
	// Computed values
	private double width, height;
	private double dstepover;
	
	private Surface imgcache;

	/**
	 * Get the configured tool
	 * @return tool
	 */
	protected final Tool getTool() {
		return tool;
	}
	
	/**
	 * Get the origin (topleft) point
	 * @return origin
	 */
	protected final NumericCoordinate getOrigin() {
		return topleft;
	}
	
	/**
	 * Set the name of the input image
	 * @param name
	 * @return
	 */
	public Image file(String name) {
		if(!name.equals(filename)) {
			filename = name;
			imgcache = null;
		}
		return this;
	}
	
	/**
	 * Set image from a surface
	 * @param surface
	 * @return
	 */
	public Image src(Surface surface) {
		filename = null;
		imgcache = surface;
		return this;
	}
	
	/**
	 * Set the image origin (topleft coordinate)
	 * @param origin
	 * @return this
	 */
	public Image origin(String origin) {
		NumericCoordinate nc;
		try {
			nc = (NumericCoordinate) Coordinate.parse(origin);
		} catch(ClassCastException e) {
			throw new IllegalArgumentException("Only numeric coordinates are supported!");
		}
		if(!nc.isDefined(Axis.X) || !nc.isDefined(Axis.Y))
			throw new IllegalArgumentException("X and Y axes must be defined!");
		if(!nc.isDefined(Axis.Z))
			nc.set(Axis.Z, 0.0);
		topleft = nc;
		
		return this;
	}
	
	/**
	 * Invert the input image
	 * @return
	 */
	public Image invert() {
		if(filename!=null)
			imgcache = null;
		invert = !invert;
		return this;
	}
	
	/**
	 * Normalize the input image. This stretches the values so the smallest value will become 0 and the biggest 1.
	 * @return
	 */
	public Image normalize() {
		if(filename!=null)
			imgcache = null;
		normalize = true;
		return this;
	}
	
	/**
	 * Flip the image upside down
	 * @return
	 */
	public Image flip() {
		if(filename!=null)
			imgcache = null;
		flip = !flip;
		return this;
	}
	
	/**
	 * Mirror the image
	 * @return
	 */
	public Image mirror() {
		if(filename!=null)
			imgcache = null;
		mirror = !mirror;
		return this;
	}
	
	/**
	 * Rotate the image 90 degrees
	 * @return
	 */
	public Image rotate() {
		if(filename!=null)
			imgcache = null;
		rotate = !rotate;
		return this;
	}
	/**
	 * Set the height of the image. Image pixel zero value will correspond to origin Z - height and 1.0 to origin Z.
	 * @param scale
	 * @return
	 */
	public Image height(double scale) {
		this.zscale = scale;
		return this;
	}
	
	/**
	 * Set the size of the carving area. The pixel size will be set so the image will
	 * fit in the requested area. Aspect ratio is maintained.  
	 * @param x width in millimeters or inches
	 * @param y height in millimeters or inches
	 * @return
	 */
	public Image size(double x,double y) {
		if(x<=0 || y<=0)
			throw new IllegalArgumentException("Dimensions must be greater than zero!");
		this.xsize = x;
		this.ysize = y;
		if(filename!=null)
			imgcache = null;
		return this;
	}
	
	/**
	 * Set the maximum distance between adjacent rows or columns.
	 * The stepover size determines how many pixel rows/columns are skipped between lines.
	 * If zero is given, a stepover value is calculated automatically.
	 * @param size stepover size in mm/inches
	 * @return
	 */
	public Image stepover(double size) {
		if(size<0)
			throw new IllegalArgumentException("Stepover must be greater zero or greater!");
		if(size==0)
			this.stepover = "";
		else
			this.stepover = Double.toString(size);
		return this;
	}
	
	/**
	 * Set the maximum distance between adjacent rows or columns.
	 * The size should either be a number or a percentage. If a percentage is
	 * used, the stepover size is calculated from the tool diameter.
	 * <p>If a zero or empty string is given, the stepover will be calculated
	 * automatically.
	 * @param size
	 * @return
	 */
	public Image stepover(String size) {
		if("0".equals(size))
			stepover = "";
		else
			stepover = size;
		return this;
	}
	
	/**
	 * Select tool
	 * <p>
	 * Tool definition format is defined in {@link Tool#get(String)}.
	 * @param tooldef
	 * @return
	 */
	public Image tool(String tooldef) {
		this.tool = Tool.get(tooldef);
		return this;
	}
	
	/**
	 * Set carving strategy
	 * @param strategy
	 * @return
	 */
	public Image strategy(String strategy) {
		this.strategy = strategy.toLowerCase();
		return this;
	}
	
	/**
	 * Get the width of the image
	 * @return
	 */
	protected final double getWidth() {
		return width;
	}
	
	/**
	 * Get the height of the image
	 * @return
	 */
	protected final double getHeight() {
		return height;
	}
	
	/**
	 * Get the distance between rows or columns
	 * @return row/column gap
	 */
	protected final double getStepover() {
		return dstepover;
	}

	/**
	 * Get the image surface
	 * @return image surface
	 */
	public Surface getSurface() {
		if(xsize<0)
			throw new RenderException("Target size not set!");
		
		if(imgcache==null) {
			// Load image
			try {
				imgcache = new ImageData(filename, normalize, invert, flip, mirror, rotate);
			} catch(IOException e) {
				throw new RenderException("Couldn't load image \"" + filename + "\": " + e.getMessage(), e);
			}
			
			// Set target size
			imgcache.setTargetSize(xsize, ysize, zscale);
			
			// Get true size
			// TODO maintain aspect ratio
			width = xsize;
			height = ysize;
		} else if(filename==null) {
			// Initialize external surface
			imgcache.setTargetSize(xsize, ysize, zscale);
			width = xsize;
			height = ysize;
		}
		
		return imgcache;
	}
	
	public Path toPath() {
		if(tool==null)
			throw new RenderException("Tool not set!");
		
		if(filename==null && imgcache==null)
			throw new RenderException("Input file or source surface not set!");
		
		// Select carving strategy
		ImageStrategy is;
		if("simple".equals(strategy))
			is = new SimpleStrategy(this);
		else if(strategy.startsWith("simple "))
			is = new SimpleStrategy(this, strategy.substring(7));
		else if("rough".equals(strategy))
			is = new RoughStrategy(this);
		else if(strategy.startsWith("rough "))
			is = new RoughStrategy(this, strategy.substring(6));
		else if(strategy.equals("outline"))
			is = new OutlineStrategy(this);
		else if(strategy.startsWith("outline "))
			is = new OutlineStrategy(this, strategy.substring(8));
		else
			throw new RenderException("Unknown strategy: " + strategy);

		// Make sure the image is loaded
		getSurface();
		
		// Calculate the gap between rows or columns.
		if(stepover.length()==0) {
			// Zero stepover means the stategy will use some default value
			dstepover = 0;
		} else {
			// Stepover can be expressed in absolute units or as a percentage of tool diamater
			if(stepover.endsWith("%"))
				dstepover = Double.parseDouble(stepover.substring(0,stepover.length()-1)) / 100.0 * tool.getDiameter();
			else
				dstepover = Double.parseDouble(stepover);
		}
		
		// Generate toolpath
		long time = System.currentTimeMillis();
		Path path = is.toPath(imgcache);
		time = System.currentTimeMillis() - time;
		JGCGenerator.getLogger().status(is.getClass().getSimpleName() + " finished. Took " + String.format("%.2f", time/1000.0) + " seconds.");
		
		return path;//.reduce();
	}
}
