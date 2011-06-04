package org.luolamies.jgcgen.importer.svg;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.management.RuntimeErrorException;

import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.PathGenerator;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PathExtractor implements PathGenerator {
	private final SvgImporter svg;
	private List<Extr> extract; 
	private boolean extractAll;
	private ZMap zmap = ZMnull.INSTANCE;
	
	private static class Extr {
		Extr(boolean include, String id) {
			this.include = include;
			this.id = id;
		}
		final boolean include;
		final String id;
	}
	
	PathExtractor(SvgImporter svg) {
		this.svg = svg;
		this.extract = new ArrayList<Extr>();
	}
	
	/**
	 * Include everything
	 * @return this
	 */
	public PathExtractor all() {
		extractAll = true;
		return this;
	}
	
	public PathExtractor zmap(String mapping) {
		String mapper, params;
		mapping = mapping.trim();
		int space = mapping.indexOf(' ');
		if(space<0) {
			mapper = mapping;
			params = "";
		} else {
			mapper = mapping.substring(0, space);
			params = mapping.substring(space+1);
		}
		
		try {
			if(mapper.length()==0) {
				this.zmap = ZMnull.INSTANCE;
			} else {
				Class<? extends ZMap> zmap = (Class<? extends ZMap>) Class.forName(getClass().getPackage().getName() + ".ZM" + mapper);
				if(params.length()==0)
					this.zmap = zmap.newInstance();
				else
					this.zmap = zmap.getConstructor(String.class).newInstance(params);
			}
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Z mapping type \"" + mapper + "\" not found!");
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("Z mapping type \"" + mapper + "\" does not take any parameters!");
		}
		return this;
	}
	
	/**
	 * Include paths, groups and other elements with the given IDs
	 * @param id list of IDs, separated by spaces
	 */
	public PathExtractor id(String id) {
		String[] ids = id.split(" ");
		for(String i : ids) {
			if(!" ".equals(i))
				extract.add(new Extr(true, i));
		}
		return this;
	}
	
	/**
	 * Exclude paths, groups and other elements with the given IDs
	 * @param id list of IDs, separated by spaces
	 */
	public PathExtractor notId(String id) {
		String[] ids = id.split(" ");
		for(String i : ids) {
			if(!" ".equals(i))
				extract.add(new Extr(false, i));
		}
		return this;
	}
	
	private Extr extract(String id) {
		for(Extr e : extract)
			if(id.equals(e.id))
				return e;
		return null;
	}
	
	private boolean isExcluded(String id) {		
		Extr e = extract(id);
		return e!=null && e.include==false;
	}
	
	public Path toPath() {
		if(!extractAll && extract.isEmpty())
			throw new RenderException("No includes set!");
		
		Path path = new Path();
		
		Transform matrix = svg.rootmatrix;
		
		// Apply document level transformation if present
		String roottra = svg.root.getAttribute("transform");
		if(roottra.length()>0)
			matrix = matrix.multiply(Transform.parse(roottra));

		// Convert selected elements
		// Convert selected elements
		// Convert selected elements
		render(path, svg.root, svg.rootmatrix, extractAll);
		
		return path;
	}
	
	private void render(Path path, Element el, Transform matrix, boolean include) {
		String id = el.getAttribute("id");
		
		// Defs node is skipped
		if("defs".equals(el.getNodeName()) || isExcluded(id))
			return;

		// Apply transformation matrix if this element has one
		String transform = el.getAttribute("transform");
		if(transform.length()>0)
			matrix = matrix.multiply(Transform.parse(transform));
		
		// Check if this node has been explicitly excluded or included
		Extr extr = id!=null ? extract(id) : null;
		if(extr!=null) {
			if(extr.include) {
				include = true;
				// add a seam before an explicitly included node
				path.addSegment(Path.SType.SEAM, null, id);
			} else
				return;
		}
		
		if(include)
			include(path, el, matrix);
	
		// Render child elements too
		NodeList nodes = el.getChildNodes();
		for(int i=0;i<nodes.getLength();++i) {
			if(nodes.item(i).getNodeType() == Node.ELEMENT_NODE)
				render(path, (Element)nodes.item(i), matrix, include);
		}
	}
	
	private void include(Path path, Element el, Transform matrix) {				
		// Identify element type
		String type = el.getNodeName();
		if("g".equals(type)) {
			// Group. Just include all subnodes unless explicitly excluded
		} else if("path".equals(type)) {
			// Path
			SvgPath.toPath(path, el, matrix, zmap);
		} else if("rect".equals(type)) {
			// A rectangle
			Rect.toPath(path, el, matrix, zmap);
		} else if("metadata".equals(type)) {
			// Ignore
		} else {
			System.err.println("Warning: Unhandled SVG element " + svgpath(el));
		}		
	}
	
	private static String svgpath(Node node) {
		if(node.getParentNode()!=null)
			return svgpath(node.getParentNode()) + '/' + node.getNodeName();
		else
			return node.getNodeName();
	}

}
