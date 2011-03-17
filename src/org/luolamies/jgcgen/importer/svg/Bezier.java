package org.luolamies.jgcgen.importer.svg;

import java.util.ArrayList;
import java.util.List;

import org.luolamies.jgcgen.importer.svg.SvgPath.CP;

/**
 * Functions for manipulating bezier curves
 * TODO biarc approximation
 */
class Bezier {
	/**
	 * Approximate a bezier curve with line segments using De Casteljau algorithm.
	 * @param p1
	 * @param p2
	 * @param p3
	 * @param p4
	 * @param depth
	 * @return
	 */
	static public List<CP> linearapprox(CP p1, CP p2, CP p3, CP p4) {
		// Stop recursion once sufficient accuracy has been attained
		double d0 = dist(p1, p2, p3, p4);
		double d = dist(p1, p4);
		if(d0/d < 1.001) {
			ArrayList<CP> cp = new ArrayList<CP>();
			cp.add(p1);
			cp.add(p4);
			return cp;
		}
		
		// Calculate new control points
		CP r1 = p1;
		CP r2 = p1.add(p2).mult(0.5);
		CP r3 = r2.mult(0.5).add(p2.add(p3).mult(0.25));
		
		CP s3 = p3.add(p4).mult(0.5);
		CP s2 = p2.add(p3).mult(0.25).add(s3.mult(0.5));
		CP r4 = r3.add(s2).mult(0.5);
		CP s4 = p4;
		CP s1 = r4;
		
		List<CP> cp = linearapprox(r1, r2, r3, r4);
		cp.addAll(linearapprox(s1, s2, s3, s4));
		return cp;		
	}
	
	private static double dist(CP... p) {
		double dist=0;
		for(int i=1;i<p.length;++i) {
			dist += Math.hypot(p[i-1].x-p[i].x, p[i-1].y-p[i].y);
		}
		return dist;
	}
}
