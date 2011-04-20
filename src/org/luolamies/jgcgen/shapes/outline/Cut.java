package org.luolamies.jgcgen.shapes.outline;

import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;

import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.NumericCoordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.PathGenerator;

/**
 * Take any 2D path and generate a 3D cutting path from it.
 */
public class Cut implements PathGenerator {
	private PathGenerator src;
	
	private double z0, depth, passdepth;
	private double ramplen;
	private boolean finish = true;
	
	public Cut src(PathGenerator path) {
		src = path;
		return this;
	}
	
	public Cut z0(double z0) {
		this.z0 = z0;
		return this;
	}
	
	public Cut depth(double depth) {
		this.depth = depth;
		return this;
	}
	
	public Cut pass(double depth) {
		this.passdepth = depth;
		return this;
	}
	
	public Cut ramplen(double len) {
		ramplen = len;
		return this;
	}
	
	public Cut noFinish() {
		finish = false;
		return this;
	}
	
	@Override
	public Path toPath() {
		if(this.src==null)
			throw new RenderException("Source path (src) not set!");
		if(depth<=0)
			throw new RenderException("Depth not set!");
		if(passdepth<=0)
			throw new RenderException("Pass depth not set!");

		final List<Path> sources = src.toPath().splitAtSubpaths();
		// Sort the paths from smallest to largest, so the small (possibly inner) parts
		// get cut first.
		if(sources.size()>1) {
			Collections.sort(sources, new Comparator<Path>() {
				IdentityHashMap<Path, Double> areacache = new IdentityHashMap<Path, Double>(sources.size());
				
				@Override
				public int compare(Path p1, Path p2) {
					double a1 = calcArea(p1);
					double a2 = calcArea(p2);
					return Double.compare(a1, a2);
				}
				
				/** Calculate the area of a path bounding rectangle */
				private double calcArea(Path p) {
					if(areacache.containsKey(p))
						return areacache.get(p);
					
					double minx=Double.MAX_VALUE, maxx=-Double.MAX_VALUE;
					double miny=Double.MAX_VALUE, maxy=-Double.MAX_VALUE;
					for(Path.Segment s : p.getSegments()) {
						if(s.point!=null) {
							Double x = ((NumericCoordinate)s.point).getValue(Axis.X);
							Double y = ((NumericCoordinate)s.point).getValue(Axis.Y);
							if(x!=null) {
								if(x<minx)
									minx = x;
								if(x>maxx)
									maxx = x;
							}
							if(y!=null) {
								if(y<miny)
									miny = y;
								if(y>maxy)
									maxy = y;
							}
						}
					}
					
					double area = (maxx-minx) * (maxy-miny);
					areacache.put(p, area);
					return area;
				}
			});
		}
		
		Path cut = new Path();
		for(Path p : sources)
			makePath(cut, p);
		
		return cut;
	}

	private void makePath(Path cut, Path src) {
		// Precalculate Z distances
		double[] zd = new double[src.getSize()];
		int j=1;
		Iterator<Path.Segment> i = src.getSegments().iterator();
		Path.Segment prev = i.next();
		while(i.hasNext()) {
			Path.Segment seg = i.next();
			zd[j++] = ((NumericCoordinate)seg.point).distance((NumericCoordinate)prev.point);
			prev = seg;
		}
		
		double ramplen = this.ramplen;
		if(ramplen<=0)
			ramplen = depth;
		
		double dist=0;
		zd[0]=0;
		for(j=1;j<zd.length;++j) {
			dist += zd[j];
			if(dist>=ramplen)
				zd[j] = passdepth;
			else
				zd[j] = dist/ramplen * passdepth;
		}
		
		if(dist<ramplen)
			throw new RenderException(String.format("Path is shorter (%.2f) than ramp length (%.2f)!", dist,ramplen));
		
		boolean closedpath = src.isClosed();
		
		final double z1 = z0 - depth + passdepth;
		double z = z0 + passdepth;
		boolean first=true;
		while(z>z1) {
			z -= passdepth;
			if(z<z1)
				z = z1;
			
			boolean omitfirst = false;
			if(first)
				first=false;
			else if(closedpath)
				omitfirst = true;
			
			i = src.getSegments().iterator();
			j=0;
			if(omitfirst) {
				i.next();
				++j;
			}
			
			while(i.hasNext()) {
				Path.Segment s = i.next();
				cut.addSegment(s.type, s.point.offset(new NumericCoordinate(null, null, z-zd[j++]), false, true));
			}
		}
		
		// Cut the last unfinished part
		if(finish) {
			z = z0 - depth;
			j=0;
			i = src.getSegments().iterator();
			
			if(closedpath) {
				i.next();
				++j;
			}
			
			while(i.hasNext()) {
				Path.Segment s = i.next();
				cut.addSegment(s.type, s.point.offset(new NumericCoordinate(null, null, z), false, true));
				if(zd[j]==passdepth)
					break;
			}
		}
	}
}
