package org.luolamies.jgcgen.importer.svg;

import java.util.ArrayList;
import java.util.List;

import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.path.Path;
import org.luolamies.jgcgen.path.PathGenerator;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PathExtractor implements PathGenerator {
	private final SvgImporter svg;
	private List<Extr> extract; 
	
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
		if(extract.isEmpty())
			throw new RenderException("No includes set!");
		
		Path path = new Path();
		
		Transform matrix = svg.rootmatrix;
		
		// Apply document level transformation if present
		String roottra = svg.root.getAttribute("transform");
		if(roottra.length()>0)
			matrix = matrix.multiply(Transform.parse(roottra));
		
		for(int i=0;i<svg.root.getChildNodes().getLength();++i) {
			Node node = svg.root.getChildNodes().item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				Node id = node.getAttributes().getNamedItem("id");				
				
				Extr extr = id!=null ? extract(id.getNodeValue()) : null;
				if(extr!=null && extr.include) {
					include(path, (Element)node, svg.rootmatrix);
				}
			}
		}
		
		return path;
	}
	
	private void include(Path path, Element el, Transform matrix) {
		// First, check if this element has been explicitly excluded
		if(isExcluded(el.getAttribute("id")))
			return;
		
		// Apply transformation matrix if this element has one
		String transform = el.getAttribute("transform");
		if(transform.length()>0)
			matrix = matrix.multiply(Transform.parse(transform));
				
		// Identify element type
		String type = el.getNodeName();
		if("g".equals(type)) {
			// Group. Just include all subnodes unless explicitly excluded
		} else if("path".equals(type)) {
			// Path
			SvgPath.toPath(path, el, matrix);
		} else if("rect".equals(type)) {
			// A rectangle
			Rect.toPath(path, el, matrix);
		} else {
			System.err.println("Warning: Unhandled SVG element " + el.getNodeName());
			return;
		}
		
		// Recurse into child elements
		NodeList nodes = el.getChildNodes();
		for(int i=0;i<nodes.getLength();++i) {
			if(nodes.item(i).getNodeType() == Node.ELEMENT_NODE)
				include(path, (Element)nodes.item(i), matrix);
		}
	}

}
