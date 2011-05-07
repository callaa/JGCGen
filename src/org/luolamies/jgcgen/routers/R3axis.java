/*
 * This file is part of JGCGen.
 *
 * JGCGen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JGCGen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGCGen.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.luolamies.jgcgen.routers;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.velocity.VelocityContext;
import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.Subroutines;
import org.luolamies.jgcgen.path.Axis;
import org.luolamies.jgcgen.path.Coordinate;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.SymbolicCoordinate;
import org.luolamies.jgcgen.path.Path.SType;

/**
 * Path code generator for 3 axis machines.
 * (Velocity) variables used:
 * <ul>
 * <li>$safe_z
 * <li>$near_z
 * <li>$plunge_f
 * <li>$normal_f
 * <li>$passdepth
 * <li>$r3singlepass</li>
 * </ul>
 * <p>{@link #toGcode(Path, String)} will cut the path in multiple passes (ceil(z/passdepth))
 * if z is not null. This is useful for cutting out 2D shapes.
 * <p>Note that the passes are calculated only for the z offset! For
 * complex geometries, generate the tool path &mdash; passes included &mdash; yourself and use a null offset.
 * <p>
 * Pass generation can be disabled by setting the variable $r3singlepass to <code>true</code>.
 */
public class R3axis extends Router {
	
	public R3axis(VelocityContext ctx) {
		super(ctx);
	}
	
	@Override
	public void toGcode(Writer out, Path path, String zoffset) throws IOException {		
		// Safe height for rapids
		final Coordinate safez = Coordinate.parse("z" + var("safe_z"));
		final Coordinate nearz;
		if(!var("safe_z").equals("near_z"))
			nearz = Coordinate.parse("z" + var("near_z"));
		else
			nearz = null;
		
		// Do intrapath rapids at near_z instead of safe_z?
		final boolean rapidnear = nearz!=null && Boolean.parseBoolean(var("rapidnear"));
		
		// If a Z offset is given, we obviously offset the path but
		// also enable multiple pass looping. Loops are calculated for the offset only!
		// This is useful for cutting out 2D shapes. For 3D toolpaths, you should
		// use a null offset and calculate the passes yourself.
		Coordinate zoffc=null;
		String zoff=null;
		if(zoffset!=null) {
			zoffc = Coordinate.parse("z" + zoffset);
			zoff = zoffc.get(Axis.Z);
		}

		// Set pass depths if not explicitly disabled
		Coordinate zpassc = null;
		String zpass = null;
		if(zoffc!=null && !Boolean.valueOf(var("r3singlepass"))) {
			zpassc = Coordinate.parse("z" + var("passdepth"));
			zpass = zpassc.get(Axis.Z);
		}
		
		// Have we set the plunge rate? We must remember to set the normal feed
		// rate afterwards.
		boolean fplungeset=false;
		
		List<Path.Segment> segments = path.getSegments();
		Coordinate firstpoint = segments.get(0).point;
		Coordinate lastpoint = null;
		for(int i=segments.size()-1;i>=0;--i) {
			if(segments.get(i).point!=null) {
				lastpoint = segments.get(i).point;
				break;
			}
		}
		if(lastpoint==null) {
			// A path that consist only of markers?
			return;
		}
		
		// Preparation for multi-pass cut
		boolean skipfirstrapid = false;
		String loopn=null;	// the number for the loop O word
		String zvar=null;	// The variable for the Z position
		Coordinate zvarc = null;
		if(zpassc!=null) {
			// If the first move is a rapid and has the same X and Y coordinates
			// as the last point, move it outside the loop.
			skipfirstrapid =
				segments.get(0).type == Path.SType.MOVE &&
				firstpoint.get(Axis.X).equals(lastpoint.get(Axis.X)) &&
				firstpoint.get(Axis.Y).equals(lastpoint.get(Axis.Y));
			
			if(skipfirstrapid) {
				out.write("G00 ");
				out.write(safez.toGcode());
				out.write("\n\t");
				out.write(firstpoint.toGcode());
				if(segments.get(0).label!=null) {
					out.write(" (");
					out.write(segments.get(0).label);
					out.write(')');
				}
					
				if(nearz!=null) {
					out.write("\n\t");
					out.write(nearz.toGcode());
				}
				out.write('\n');
			}
			
			// Get o numbers for the z WHILE and IF
			loopn = Subroutines.getNextOnumber();
			String ifn = Subroutines.getNextOnumber();
			zvar = "#<o" + loopn + '>';
			zvarc = new SymbolicCoordinate(null, null, zvar);
			out.write(zvar + " = 0\n");
			out.write('o' + loopn + " while [" + zvar + " gt " + zoff + "]\n");
			out.write(zvar + " = [" + zvar + " - " + zpass + "]\n");
			out.write('o' + ifn + " if [ " + zvar + " lt " + zoff + "]\n");
			out.write('\t' + zvar + " = " + zoff + '\n');
			out.write('o' + ifn + " endif\n");
		}
		
		// Convert path segments to G codes
		Path.Segment prev = null;
		boolean firstrapid = segments.get(0).type==SType.MOVE;
		for(ListIterator<Path.Segment> i=segments.listIterator();i.hasNext();) {
			Path.Segment s = i.next();
			switch(s.type) {
			// Seam are just markers.
			case SEAM: continue;
			// Rapid move
			// Go to safe Z height, move over the target point and plunge down
			case MOVE:
				// The rapid over target point is skipped when we do multiple passes
				// and the end point is the same as the starting point.
				if(!skipfirstrapid || !firstrapid) {
					out.write("G00 ");
					out.write((rapidnear && !firstrapid ? nearz : safez).toGcode());
					out.write("\n\t");
					out.write(s.point.undefined(Axis.Z).toGcode());
					if(s.label!=null) {
						out.write(" (");
						out.write(s.label);
						out.write(')');
					}
					out.write('\n');
				}
				
				Path.Segment targ;
				// If a Z value is defined for the move, use it.
				// Otherwise we use the Z value of the next non rapid move.
				if(s.point.isDefined(Axis.Z))
					targ = s;
				else
					targ = findNext(segments, i.nextIndex());
				if(targ!=null) {
					// Plunge down to target depth
					// If near_z is not the same as safe_z, rapid there first
					if(!skipfirstrapid && nearz!=null) {
						out.write("G00 ");
						out.write(nearz.toGcode());
						out.write('\n');
					}
					out.write("G01");
					out.write(" F");
					out.write(var("plunge_f"));
					out.write(" Z");
					// If no offset is given, plunge down to target depth
					if(zoffc==null) {
						if(!s.point.isDefined(Axis.Z))
							throw new RenderException("No Z coordinate defined! This may be a 2D path. Make sure the initial move has a Z value or use an offset.");
						out.write(targ.point.get(Axis.Z));
					} else {
						// Otherwise plunge to Z offset + target depth
						out.write(Coordinate.parse("z" + (zvar!=null ? zvar : zoff)).offset(targ.point).get(Axis.Z));
					}
					out.write('\n');
					fplungeset=true;
				}
				firstrapid = false;
				break;
			// Dab down
			case POINT:
				// Move over the target point
				out.write("G00 ");
				out.write((rapidnear ? nearz : safez).toGcode());
				out.write("\n\t");
				out.write(s.point.undefined(Axis.Z).toGcode());
				// Plunge down to target depth
				if(nearz!=null) {
					out.write("\n\t ");
					out.write(nearz.toGcode());
				}
				out.write("\nG01");
				out.write(" F");
				out.write(var("plunge_f"));
				out.write(" Z");
				// If no offset is given, plunge down to the given point depth 
				if(zoffc==null) {
					if(!s.point.isDefined(Axis.Z))
						throw new RenderException("No Z coordinate defined! Use an offset.");
					out.write(s.point.get(Axis.Z));
				} else // Otherwise plunge to Z offset + depth of point
					out.write(Coordinate.parse("z" + (zvar!=null ? zvar : zoff)).offset(s.point).get(Axis.Z));
				
				if(s.label!=null) {
					out.write(" (");
					out.write(s.label);
					out.write(')');
				}
				
				out.write('\n');
				// Retract back to safety (if this is not the last entry)
				if(i.hasNext()) {
					out.write("G00 ");
					out.write((rapidnear ? nearz : safez).toGcode());
					out.write('\n');
				}
				break;
			// Motion at feed rate
			case LINE:
			case CWARC:
			case CCWARC:
				if(prev.type==s.type)
					out.write('\t');
				else {
					out.write(s.type.gcode);
					out.write(' ');
				}
				if(fplungeset) {
					out.write("F");
					out.write(var("default_f"));
					out.write(' ');
					fplungeset = false;
				}
				Coordinate point = s.point;
				if(zoffc!=null)
					point = point.offset(zvarc!=null ? zvarc : zoffc);
				out.write(point.toGcode());
				if(s.label!=null) {
					out.write(" (");
					out.write(s.label);
					out.write(')');
				}
				out.write('\n');
				break;
			default:
				throw new RuntimeException("BUG! Unhandled segment type " + s.type);
			}
			prev = s;
		}
		
		if(loopn!=null) {
			out.write('o');
			out.write(loopn);
			out.write(" endwhile\n");
		}
		
		out.write("G00 ");
		out.write(safez.toGcode());
		out.write('\n');
	}
	
	/**
	 * Find the next non-rapid motion command.
	 * @param list
	 * @param from
	 * @return
	 */
	static private Path.Segment findNext(List<Path.Segment> list, int from) {
		Iterator<Path.Segment> ii = list.listIterator(from);
		while(ii.hasNext()) {
			Path.Segment s = ii.next();
			if(s.type != Path.SType.MOVE && s.type != Path.SType.SEAM)
				return s;
		}
		return null;
	}

}
