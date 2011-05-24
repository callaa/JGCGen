package org.luolamies.jgcgen.importer.svg;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.luolamies.jgcgen.Files;
import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.importer.Importer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class SvgImporter extends Importer {
	final Element root;
	Transform rootmatrix;
	
	public SvgImporter(String file) throws SAXException, IOException {
		this(new FileInputStream(file));
	}
	
	public SvgImporter(InputStream input) throws SAXException, IOException {
		DocumentBuilder builder;
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RenderException("Couldn't construct document builder", e);
		}
		
		Document doc = builder.parse(input);
		
		root = doc.getDocumentElement();
		scale(1.0 / (90 / 25.5));
	}
	
	/**
	 * Set document scale.
	 * @param scale
	 * @return
	 */
	public SvgImporter scale(double scale) {
		if(scale<=0)
			throw new IllegalArgumentException("Scale must be greater than zero!");
		this.rootmatrix = Transform.scale(scale, -scale);
		return this;
	}
	
	public PathExtractor getPath() {
		return new PathExtractor(this);
	}
}
