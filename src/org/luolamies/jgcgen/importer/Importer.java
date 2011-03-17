package org.luolamies.jgcgen.importer;

import java.io.IOException;

import org.luolamies.jgcgen.importer.svg.SvgImporter;
import org.xml.sax.SAXException;

public class Importer {
	/**
	 * Import the named file. File type is determined from the extension. 
	 * @param filename
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public static Importer file(String filename) throws SAXException, IOException {
		int ext = filename.lastIndexOf('.');
		if(ext<0)
			throw new IllegalArgumentException("Can't determine file type! Use file(\"name\", \"type\")");
		return file(filename, filename.substring(ext+1).toLowerCase());
	}
	
	/**
	 * Import the named file. File type is determined from the extension. 
	 * @param filename
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public static Importer file(String filename, String type) throws SAXException, IOException {
		if("svg".equals(type)) {
			return new SvgImporter(filename);
		} else
			throw new IllegalArgumentException("Unsupported type \"" + type + '"');
	}
}
