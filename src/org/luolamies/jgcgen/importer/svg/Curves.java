package org.luolamies.jgcgen.importer.svg;

import java.util.ArrayList;
import java.util.List;

import org.luolamies.jgcgen.importer.svg.SvgPath.CP;

/**
 * Functions for calculating curves
 * TODO biarc approximation
 */
class Curves {
	/**
	 * Approximate a cubic bezier curve with line segments using De Casteljau algorithm.
	 * @param p1
	 * @param p2
	 * @param p3
	 * @param p4
	 * @param depth
	 * @return
	 */
	static public List<CP> linearapproxCubicBezier(CP p1, CP p2, CP p3, CP p4) {
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
		
		List<CP> cp = linearapproxCubicBezier(r1, r2, r3, r4);
		cp.addAll(linearapproxCubicBezier(s1, s2, s3, s4));
		return cp;		
	}
	
	/**
	 * Calculate the length of the path formed by the given array of points.
	 * @param p
	 * @return
	 */
	private static double dist(CP... p) {
		double dist=0;
		for(int i=1;i<p.length;++i) {
			dist += Math.hypot(p[i-1].x-p[i].x, p[i-1].y-p[i].y);
		}
		return dist;
	}
	
	/**
	 * Generate an elliptical arc.
	 * @param start arc starting point
	 * @param radius arc radius
	 * @param angle arc rotation around X axis
	 * @param largearc
	 * @param sweep
	 * @param end ending point
	 * @return
	 */
	static public List<CP> ellipseArc(CP start, CP radius, double angle, boolean largearc, boolean sweep, CP end) {
		// Based on ExtendedGeneralPath.computeArc() from Apache Batik
		
		// Compute the half distance between the current and the final point
		double dx2 = (start.x - end.x) / 2.0;
		double dy2 = (start.y - end.y) / 2.0;
		
		double cosAngle = Math.cos(angle);
		double sinAngle = Math.sin(angle);
		
		// Step 1: Compute (x1, y1)
		double x1 = (cosAngle * dx2 + sinAngle * dy2);
		double y1 = (-sinAngle * dx2 + cosAngle * dy2);
		
		// Ensure radii are large enough
		double rx = Math.abs(radius.x);
		double ry = Math.abs(radius.y);
		
		double Prx = rx * rx;
		double Pry = ry * ry;
		double Px1 = x1 * x1;
		double Py1 = y1 * y1;
		
		// check that radii are large enough
		double radiiCheck = Px1/Prx + Py1/Pry;
		if (radiiCheck > 1) {
			rx = Math.sqrt(radiiCheck) * rx;
			ry = Math.sqrt(radiiCheck) * ry;
			Prx = rx * rx;
			Pry = ry * ry;
		}
		
		// Step 2: Compute (cx1, cx2)
		double sign = (largearc == sweep) ? -1 : 1;
		double sq = ((Prx*Pry)-(Prx*Py1)-(Pry*Px1)) / ((Prx*Py1)+(Pry*Px1));
		sq = (sq < 0) ? 0 : sq;
		double coef = (sign * Math.sqrt(sq));
		double cx1 = coef * ((rx * y1) / ry);
		double cy1 = coef * -((ry * x1) / rx);
		
		// Step 3: Compute (cx, cy) from (cx1, cy1)
		double sx2 = (start.x + end.x) / 2.0;
		double sy2 = (start.y + end.y) / 2.0;
		double cx = sx2 + (cosAngle * cx1 - sinAngle * cy1);
		double cy = sy2 + (sinAngle * cx1 + cosAngle * cy1);
		
		// Step 4: Compute the angleStart (angle1) and angleExtent (dangle)
		double ux = (x1 - cx1) / rx;
		double uy = (y1 - cy1) / ry;
		double vx = (-x1 - cx1) / rx;
		double vy = (-y1 - cy1) / ry;
		double p, n;
		// Compute the angle start
		n = Math.sqrt((ux * ux) + (uy * uy));
		p = ux; // (1 * ux) + (0 * uy)
		sign = (uy < 0) ? -1.0 : 1.0;
		double angleStart = Math.toDegrees(sign * Math.acos(p / n));
		
		// Compute the angle extent
		n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
		p = ux * vx + uy * vy;
		sign = (ux * vy - uy * vx < 0) ? -1.0 : 1.0;
		double angleExtent = Math.toDegrees(sign * Math.acos(p / n));
		if(!sweep && angleExtent > 0) {
			angleExtent -= 360f;
		} else if (sweep && angleExtent < 0) {
			angleExtent += 360f;
		}
		angleExtent %= 360f;
		angleStart %= 360f;
		
		// Build the arc
		/*
		System.err.println("arc.x = " + (cx - rx));
		System.err.println("arc.y = " + (cy - ry));
		System.err.println("arc.width = " + (rx * 2));
		System.err.println("arc.height = " + (ry * 2));
		System.err.println("arc.start = " + angleStart);
		System.err.println("arc.extent = " + angleExtent);
		*/
		
		List<CP> path = new ArrayList<CP>();
		for(double t=angleStart;t<angleStart+angleExtent;t+=2) {
			double cost = Math.cos(Math.toRadians(t));
			double sint = Math.sin(Math.toRadians(t));
			path.add(new CP(
					cx + rx * cost * cosAngle - ry * sint * sinAngle,
					cy + rx * cost * sinAngle + ry * sint * cosAngle
					));
		}
		
		return path;
	}
}
