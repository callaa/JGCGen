package org.luolamies.jgcgen.shapes.surface;

import java.io.IOException;

import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.Coordinate;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.PathGenerator;
import org.luolamies.jgcgen.tools.Tool;

public class Image implements PathGenerator {
	private NumericCoordinate topleft = new NumericCoordinate(0.0, 0.0, 0.0);
	private String filename;
	private String strategy="simple";
	private boolean invert, normalize, flip, mirror, rotate;
	private float xsize=-1, ysize=-1, zscale=1.0f;
	private String stepover="";
	private Tool tool;
	
	private ImageData imgcache;
	
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
		imgcache = null;
		invert = !invert;
		return this;
	}
	
	/**
	 * Normalize the input image. This stretches the values so the smallest value will become 0 and the biggest 1.
	 * @return
	 */
	public Image normalize() {
		imgcache = null;
		normalize = true;
		return this;
	}
	
	/**
	 * Flip the image upside down
	 * @return
	 */
	public Image flip() {
		imgcache = null;
		flip = !flip;
		return this;
	}
	
	/**
	 * Mirror the image
	 * @return
	 */
	public Image mirror() {
		imgcache = null;
		mirror = !mirror;
		return this;
	}
	
	/**
	 * Rotate the image 90 degrees
	 * @return
	 */
	public Image rotate() {
		imgcache = null;
		rotate = !rotate;
		return this;
	}
	/**
	 * Set the height of the image. Image pixel zero value will correspond to origin Z - height and 1.0 to origin Z.
	 * @param scale
	 * @return
	 */
	public Image height(float scale) {
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
	public Image size(float x,float y) {
		if(x<=0 || y<=0)
			throw new IllegalArgumentException("Dimensions must be greater than zero!");
		this.xsize = x;
		this.ysize = y;
		return this;
	}
	
	/**
	 * Set the size of a pixel. This is an alternative to @{link #size(float, float)}.
	 * @param size pixel size
	 * @return
	 */
	public Image pixelsize(float size) {
		if(size<=0)
			throw new IllegalArgumentException("Pixel size must be greater than zero!");
		this.xsize = -size;
		this.ysize = -size;
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
	
	public Path toPath() {
		if(tool==null)
			throw new RenderException("Tool not set!");
		
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
		else
			throw new RenderException("Unknown strategy: " + strategy);

		// Load image
		try {
			if(imgcache==null)
				imgcache = new ImageData(filename, normalize, invert, flip, mirror, rotate);
		} catch(IOException e) {
			throw new RenderException("Couldn't load image \"" + filename + "\": " + e.getMessage(), e);
		}
		
		// Set scale
		if(xsize<0) {
			// Pixel size set manually
			imgcache.setScale(-xsize, zscale);
		} else {
			// Calculate pixel size
			float scale = Math.min((float) xsize / imgcache.getWidth(), (float) ysize / imgcache.getHeight());
			
			imgcache.setScale(scale, zscale);
		}
		
		// Calculate stepover
		if(stepover.length()==0) {
			imgcache.setStepover(0);
		} else {
			double so;
			if(stepover.endsWith("%"))
				so = Double.parseDouble(stepover.substring(0,stepover.length()-1)) / 100.0 * tool.getDiameter();
			else
				so = Double.parseDouble(stepover);
			imgcache.setStepover((int)Math.round(so / imgcache.getXYscale()));
			if(imgcache.getStepover()==0)
				imgcache.setStepover(1);
		}
		
		return is.toPath(imgcache).reduce();
	}
}
