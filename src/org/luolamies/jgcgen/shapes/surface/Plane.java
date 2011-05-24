package org.luolamies.jgcgen.shapes.surface;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;

import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.importer.svg.SvgImporter;
import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.tools.Tool;
import org.xml.sax.SAXException;

/**
 * A 2D bitmap of possible tool positions on an image plane
 *
 */
class Plane {
	private final double[] depthmap;
	private final double resolution;
	private final double width, height;
	private final int bmw, bmh;
	private double curz, prevz;
	
	private boolean[] bitmap, prevbitmap;
	
	public static class Point {
		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
		int x, y;
	}
	
	public Plane(Surface surface, Tool tool, double width, double height, double resolution) {
		this.resolution = resolution;
		this.width = width;
		this.height = height;
		
		this.bmw = (int)Math.ceil(width / resolution);
		this.bmh = (int)Math.ceil(height / resolution);
		
		if(bmw==0 || bmh==0)
			throw new IllegalArgumentException("Image would be one dimensional!");
		this.depthmap = new double[bmw * bmh];
		
		int i=-1;
		for(double y=0;y<height;y+=resolution) {
			for(double x=0;x<width;x+=resolution) {
				depthmap[++i] = surface.getDepthAt(x, -y, tool);
			}
		}
	}
	
	/**
	 * Initialize the plane for the given level and tool
	 * @param surface
	 * @param level
	 * @param tool
	 * @return true is plane is all ones.
	 */
	public boolean init(double level) {
		boolean solid=true;
		prevbitmap = bitmap;
		prevz = curz;
		bitmap = new boolean[bmw * bmh];
		for(int i=0;i<depthmap.length;++i) {
			bitmap[i] = depthmap[i] > level;
			solid &= bitmap[i];
		}
		this.curz = level;
		return solid;
	}
	
	/**
	 * Is the newly initialized plane identical to the previous plane?
	 * This is used for optimization.
	 * @return true if last call to init() produced an identical bitmap as the previous one.
	 */
	public boolean isIdentical() {
		return Arrays.equals(bitmap, prevbitmap);
	}
	
	/**
	 * Restore the plane before the last init()
	 */
	public void restorePrevious() {
		bitmap = prevbitmap;
		curz = prevz;
	}
	
	public Path trace() {
		try {
			Process potrace = Runtime.getRuntime().exec("potrace --svg -W " + width + "mm -H " + height + "mm");
			
			System.err.println("Tracing layer...");
			printPbm(potrace.getOutputStream());
			System.err.println("Layer printed...");
			
			SvgImporter svg = new SvgImporter(new BufferedInputStream(potrace.getInputStream()));
			
			System.err.println("done.");
			System.err.println("exit: " + potrace.exitValue());
			
			Path path = svg.getPath().all().toPath();
			for(Path.Segment s : path.getSegments())
				((NumericCoordinate)s.point).set(Axis.Z, curz);
			
			return path;
		} catch (IOException e) {
			throw new RenderException("Error while writing output to potrace!", e);
		} catch(SAXException e) {
			throw new RenderException("Error while parsing potrace output!", e);
		}
	}
	
	private void printPbm(OutputStream output) throws IOException {
		OutputStreamWriter out = new OutputStreamWriter(output);
		
		out.write("P1\n");
		out.write(bmw + " " + bmh + "\n");
		for(int i=0,y=0;y<bmh;++y) {
			for(int x=0;x<bmw;++x,++i) {
				out.write(bitmap[i] ? '1' : '0');
				out.write(' ');
			}
			out.write('\n');
		}
		out.close();
	}
	
}
