package org.luolamies.jgcgen.shapes.outline;

import java.util.Iterator;

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
	
	@Override
	public Path toPath() {
		if(this.src==null)
			throw new RenderException("Source path (src) not set!");
		if(depth<=0)
			throw new RenderException("Depth not set!");
		if(passdepth<=0)
			throw new RenderException("Pass depth not set!");
		
		Path src = this.src.toPath();
		
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
		
		Path cut = new Path();
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
		
		// Finish
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
		return cut;
	}

}
