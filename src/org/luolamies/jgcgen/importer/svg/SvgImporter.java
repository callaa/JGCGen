package org.luolamies.jgcgen.importer.svg;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.luolamies.jgcgen.Files;
import org.luolamies.jgcgen.RenderException;
import org.luolamies.jgcgen.importer.Importer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SvgImporter extends Importer {
	final Element root;
	Transform rootmatrix;
	
	public SvgImporter(String file) throws SAXException, IOException {
		this(new InputSource(new FileInputStream(Files.get(file))));
	}
	
	public SvgImporter(InputSource input) throws SAXException, IOException {
		DocumentBuilder builder;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			builder = factory.newDocumentBuilder();
			// Entity resolver that does nothing. Resolving external references can be really slow
			// and gains us nothing.
			builder.setEntityResolver(new EntityResolver() {
		        @Override
		        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		               return new InputSource(new StringReader(""));
		        }
		    });
			
		} catch (ParserConfigurationException e) {
			throw new RenderException("Couldn't construct document builder", e);
		}
		
		Document doc = builder.parse(input);
		
		root = doc.getDocumentElement();
		
		// The default inkscape export resolution is 90 DPI
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
	
	/**
	 * Dump out the XML. This is mostly for debugging purposes.
	 * @param stream output stream
	 * @throws TransformerConfigurationException 
	 */
	public void dump(PrintStream stream) {
		try {
			TransformerFactory tff = TransformerFactory.newInstance();
			Transformer tf = tff.newTransformer();
			
			DOMSource source = new DOMSource(root);
			StreamResult result = new StreamResult(stream);
			tf.transform(source, result); 

		} catch(TransformerConfigurationException e) {
			stream.println("Error dumping XML: " + e.getMessage());
		} catch (TransformerException e) {
			stream.println("Error dumping XML: " + e.getMessage());
		}
	}
}
