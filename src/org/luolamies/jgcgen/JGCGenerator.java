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
package org.luolamies.jgcgen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.apache.velocity.runtime.resource.util.StringResourceRepositoryImpl;
import org.luolamies.jgcgen.directive.Split;
import org.luolamies.jgcgen.math.MathTools;
import org.luolamies.jgcgen.importer.Importer;
import org.luolamies.jgcgen.routers.Routers;
import org.luolamies.jgcgen.shapes.Shapes;
import org.luolamies.jgcgen.text.Fonts;

/**
 * The main class for the Java GCode Generator command line tool.
 *
 */
public class JGCGenerator {

	static private Logger logger;
	
	static public Logger getLogger() {
		return logger;
	}
	
	public static void main(String[] args) {

		Map<String,String> vars = new HashMap<String,String>();
		
		// Default flags
		boolean split = false;
		
		// Output file name
		String outputfile=null;
		
		// Parse command line arguments
		Options opts = new Options();
	
		opts.addOption("h", false, "Show this help text");
		opts.addOption("s", false, "Split output");
		opts.addOption("o", true, "Output filename");
		opts.addOption("v", false, "Verbose error messages");
		opts.addOption("D", true, "Define variable (var=value)");
		
		CommandLineParser parser = new GnuParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(opts, args);
		} catch (ParseException e) {
			System.err.println("Error parsing command line: " + e.getMessage());
			return;
		}
		
		if(cmd.hasOption('h') || cmd.getArgs().length==0) {
			HelpFormatter fmt = new HelpFormatter();
			fmt.printHelp("jcgen [options] [input file]", opts);
			return;
		}
		
		if(cmd.hasOption('o')) {
			outputfile = cmd.getOptionValue('o');
		}
		
		String[] vardefs = cmd.getOptionValues('D');
		if(vardefs!=null) {
			for(String var : vardefs) {
				int eq = var.indexOf('=');
				if(eq<0) {
					System.err.println(var + ": Variable value missing!");
					System.exit(1);
				}
				vars.put(var.substring(0, eq).trim(), var.substring(eq+1).trim());
			}
		}
		
		if(cmd.hasOption('s'))
			split = true;
				
		logger = new Logger(cmd.hasOption('v'));
		
		// Select input source
		String inputfile;
		
		if(cmd.getArgs()[0].equals("-")) {
			// The default output for STDIN is STDOUT
			if(outputfile==null) {
				if(split) {
					logger.fatal("Cannot split output when printing to stdout! Use -o to define an output file.", null);
				}
				outputfile = "-";
			}
						
			inputfile = "STDIN";
			
			StringWriter str = new StringWriter();
			
			char[] buffer = new char[2048];
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			int len;
			try {
				while((len=reader.read(buffer))>0) {
					str.write(buffer, 0, len);
				}
			} catch(IOException e) {
				logger.fatal("Error while reading from STDIN: " + e.getMessage(), e);
			}
			
			StringResourceRepository repo = new StringResourceRepositoryImpl();
			repo.putStringResource("STDIN", str.toString());
			StringResourceLoader.setRepository(StringResourceLoader.REPOSITORY_NAME_DEFAULT, repo);			
		} else {
			// Read from file
			File in = new File(cmd.getArgs()[0]);
			if(in.canRead()==false) {
				logger.fatal(in.getAbsolutePath() + ": Cannot read!", null);
			}
			Files.setWorkdir(in.getAbsoluteFile().getParentFile());
			
			inputfile = in.getName();
			
			// If no output file is specified, the default is the input file with the extension
			// replaced with "ngc"
			// If the file has no extension or the extension is already ngc, output must be
			// specified manually.
			if(outputfile==null) {
				int dot = inputfile.lastIndexOf('.');
				if(dot<0 || !inputfile.substring(dot+1).equals("ngc"))
					outputfile = inputfile.substring(0,dot+1) + "ngc";
				else {
					logger.fatal("File already has the suffix \".ngc\". Select output file with -o\n", null);
				}
			} else if(outputfile.equals("-") && split) {
				logger.fatal("-o - and -s cannot be used at the same time!", null);
			}
		}
		
		// Initialize Velocity
		Velocity.setProperty("input.encoding", System.getProperty("file.encoding"));
		Velocity.setProperty("output.encoding", System.getProperty("file.encoding"));
		Velocity.setProperty("file.resource.loader.path", Files.getWorkdir().getAbsolutePath());
		Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, logger);
		
		Properties props = new Properties();
		try {
			props.load(props.getClass().getResourceAsStream("/config/velocity.properties"));
		} catch (IOException e) {
			logger.fatal("Unable to load internal properties file! Error: " + e.getMessage(), e);
			return;
		}

		Velocity.init(props);
		
		Template template;
		
		try {
			template = Velocity.getTemplate(inputfile);
		} catch(ParseErrorException e) {
			logger.fatal("Parse error: " + e.getMessage(), e);
			return;
		}  catch(ResourceNotFoundException e) {
			logger.fatal("Resource not found: " + e.getMessage(), null);
			return;
		}
		
		if(renderTemplate(inputfile, outputfile, template, split, vars)==false)
			System.exit(1);
	}
	
	/**
	 * Render the template.
	 * @param input name of the input file
	 * @param out output
	 * @param template template to render
	 * @param split enable split mode?
	 */
	static public boolean renderTemplate(String input, String outfile, Template template, boolean split, Map<String, String> vars) {
		int i = split ? 1 : 0;
		do {
			i = renderTemplate2(input, outfile, template, i, vars);
		} while(i>0);
		return i==0;
	}
	
	/**
	 * Render the template. (Internal)
	 * 
	 * @param input
	 * @param outfile
	 * @param template
	 * @param vars
	 * @param split
	 * @return -1 on error, 0 when done, positive integer for the next split block
	 */
	@SuppressWarnings("unchecked")
	static private int renderTemplate2(String input, String outfile, Template template, int split, Map<String, String> vars) {
		
		// Decide output file name
		OutputStream out;
		if("-".equals(outfile))
			out = System.out;
		else {
			String fname = outfile;
			if(split>0) {
				int i = fname.lastIndexOf('.');
				fname = fname.substring(0, i) + "_" + split + fname.substring(i);
			}
			try {
				out = new FileOutputStream(fname);
			} catch(IOException e) {
				System.err.println("Couldn't open " + fname + " for writing: " + e.getMessage());
				return -1;
			}
			System.out.println("Generating " + fname + "...");
		}
		
		// Initialize velocity context and set initial
		// context variables.
		VelocityContext ctx = new VelocityContext();

		Configuration.getInstance().setVariables(ctx);

		for(Map.Entry<String, String> e : vars.entrySet())
			ctx.put(e.getKey(), e.getValue());

		ctx.put("inputfile", input);
		
		if(split>0) {
			ctx.put(Split.CURSPLIT, split);
			ctx.put(Split.SPLITS, new TreeSet<Integer>());
		}
		
		ctx.put("Routers", new Routers(ctx));
		ctx.put("Shapes", new Shapes(ctx));
		ctx.put("Fonts", new Fonts(new File(".")));
		ctx.put("Math", MathTools.class);
		ctx.put("Import", Importer.class);
		
		// Render the template
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		try {
			template.merge(ctx, writer);
		} catch(Exception e) {
			logger.fatal(e.getMessage(), e);
		}
		
		// If splitting was enabled, check that current split block was found.
		Set<Integer> splitblocks = (Set<Integer>) ctx.get(Split.SPLITS);
		if(splitblocks!=null) {
			if(!splitblocks.contains(split)) {
				System.err.println("Error: Split block " + split + " not found!");
				System.exit(1);
			}
		}
		
		// Clean up
		try {
			writer.close();
		} catch (IOException e) { }
				
		// Find the next split block number if in split mode
		if(split>0) {
			// We use a treeset, so the set can be iterated in order.
			Iterator<Integer> i = splitblocks.iterator();
			while(i.hasNext()) {
				Integer ii = i.next();
				if(ii.equals(split)) {
					if(i.hasNext())
						return i.next();
					break;
				}
			}
		}
		
		return 0;
	}
}
