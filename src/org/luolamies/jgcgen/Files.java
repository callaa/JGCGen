package org.luolamies.jgcgen;

import java.io.File;

/**
 * Get files relative to source file.
 *
 */
public class Files {
	static private File workdir = new File(".");
	
	/**
	 * Set the working directory
	 * @param workdir
	 */
	static public void setWorkdir(File workdir) {
		Files.workdir = workdir;
	}
	
	/**
	 * Get the working directory
	 * @return
	 */
	static public File getWorkdir() {
		return workdir;
	}
	
	/**
	 * Get a file.
	 * @param name
	 * @return file
	 */
	static public File get(String name) {
		return new File(workdir, name);
	}
}
