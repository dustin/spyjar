/*
 * Copyright (c) 2002 Scott Lamb <slamb@slamb.org>
 * This code is released under the MIT license; see the file LICENSE.
 *
 * arch-tag: 5DFF88CC-1110-11D9-92B5-000A957659CC
 */

package net.spy.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;

import org.apache.tools.ant.taskdefs.MatchingTask;

import net.spy.util.SPGen;

/**
 * Generates Java code from SPT files.
 *
 * This is an Ant task that recurses a directory and invokes
 * net.spy.util.SPGen on .spt files to generate Java code. It does the usual
 * stuff expected of a Make-style tool: leaving up-to-date .java files intact,
 * creating output with a temporary file until it is finished so partial
 * builds don't create problems, etc.
 *
 * <p>Example ant build entry:
 * <pre><code>
 *	&lt;target name="spfiles"&gt;
 *		&lt;taskdef name="spgen" classname="net.spy.util.SPGenTask"&gt;
 *			&lt;classpath refid="compile.classpath"/&gt;
 *		&lt;/taskdef&gt;
 *		&lt;spgen srcdir="${spt.dir}" destdir="${spt.dir}"
 *			superclass="com.foo.MyDbsp"/&gt;
 *	&lt;/target&gt;
 * </pre></code>
 * </p>
 *
 * @author Scott Lamb
 * @version $Revision: 1.7 $ $Date: 2003/08/05 09:01:05 $
 **/
public class SPGenTask extends MatchingTask {

	private File srcDir;
	private File destDir;
	private String superclass=null;
	private String dbcpSuperclass=null;
	private String dbspSuperclass=null;
	private boolean verbose=false;

	/**
	 * Sets the source directory to search for .spt files.
	 **/
	public void setSrcdir(File srcDir) {
		this.srcDir = srcDir;
	}

	/**
	 * Sets the destination directory to place .java files.
	 * If not specified, assumed to be the same as the source directory.
	 **/
	public void setDestdir(File destDir) {
		this.destDir = destDir;
	}

	/** 
	 * Set the superclass for the generated class.
	 */
	public void setSuperclass(String sc) {
		this.superclass=sc;
	}

	/** 
	 * Set the DBCP superclass for the generated class.
	 */
	public void setDbcpSuperclass(String sc) {
		this.dbcpSuperclass=sc;
	}

	/** 
	 * Set the DBSP superclass for the generated class.
	 */
	public void setDbspSuperclass(String sc) {
		this.dbspSuperclass=sc;
	}

	/** 
	 * True if we want verbose transformations.
	 */
	public void setVerbose(boolean to) {
		this.verbose=to;
	}

	/**
	 * Performs the operation.
	 **/
	public void execute() throws BuildException {
		if (srcDir == null) {
			throw new BuildException("srcdir attribute must be set!", location);
		}
		if (!srcDir.isDirectory()) {
			throw new BuildException("source directory \"" + srcDir
					+ "\" is not valid.");
		}
		if (destDir == null) {
			destDir = srcDir;
		}

		if (!destDir.isDirectory()) {
			throw new BuildException("destination directory \"" + destDir
				+ "\" is not valid.", location);
		}

		DirectoryScanner ds = getDirectoryScanner(srcDir);
		String[] includes = {"**\\*.spt"};
		ds.setIncludes(includes);
		ds.setBasedir(srcDir);
		ds.scan();
		String files[] = ds.getIncludedFiles();
		String toDoFiles[] = trimSPTList(files);

		if(toDoFiles.length > 0) {
			log("Compiling " + toDoFiles.length + " of " + files.length
				+ " spt file" + (files.length == 1 ? "" : "s")
				+ " to " + destDir);

			// Do the work
			for (int i = 0; i < toDoFiles.length; i++) {
				processFile(toDoFiles[i]);
			}
		}
	}

	private String[] trimSPTList(String input[]) {
		ArrayList a=new ArrayList();

		for(int i = 0; i < input.length; i++) {
			File srcFile = new File(srcDir, input[i]);
			File destFile = getDestFile(input[i]);

			if(destFile.exists()
				&& destFile.lastModified() > srcFile.lastModified()) {
				// Already have this file.
			} else {
				a.add(input[i]);
			}
		}

		String rv[]=new String[a.size()];
		for(int i=0; i<a.size(); i++) {
			rv[i]=(String)a.get(i);
		}

		return(rv);
	}

	// Translate a source filename (.spt) to a dest File (.java)
	private File getDestFile(String filename) {
		File destFile = new File(destDir,
				filename.substring(0, filename.length()-4)+".java");
		return(destFile);
	}

	/**
	 * Processes an individual file.
	 * @param filename The filename, relative to the source directory.
	 **/
	protected void processFile(String filename) {
		File srcFile = new File(srcDir, filename);
		File tmpFile = null;
		File destFile = getDestFile(filename);
		BufferedReader in;
		PrintWriter out;

		try {

			// Open input file
			try {
				FileReader reader = new FileReader(srcFile);
				in = new BufferedReader(reader);
			} catch (IOException e) {
				throw new BuildException(e);
			}

			if (destFile.exists()
					&& destFile.lastModified() > srcFile.lastModified()) {
				return;
			}

			// Open output file
			// Temporary so partial builds don't impact future ones
			try {
				tmpFile = File.createTempFile("sptgen", "java", destDir);
				FileWriter writer = new FileWriter(tmpFile);
				out = new PrintWriter(writer);
			} catch (IOException e) {
				throw new BuildException(e);
			}

			String name = srcFile.getName();
			// assert name.endsWith(".spt");
			SPGen spg = new SPGen(name.substring(0, name.length()-4), in, out);
			if (this.superclass!=null) {
				spg.setSuperclass(this.superclass);
			}
			if (this.dbcpSuperclass!=null) {
				spg.setDbcpSuperclass(this.dbcpSuperclass);
			}
			if (this.dbspSuperclass!=null) {
				spg.setDbspSuperclass(this.dbspSuperclass);
			}
			spg.setVerbose(verbose);
			try {
				spg.generate();
				in.close();
				out.close();
			} catch (Exception e) {
				throw new BuildException(e);
			}

			if (destFile.exists()) {
				if (!destFile.delete()) {
					throw new BuildException("Unable to delete "
							+ destFile);
				}
			} else {
				checkParent(destFile);
			}
			if (!tmpFile.renameTo(destFile)) {
				throw new BuildException("Unable to rename "
						+ tmpFile + " to " + destFile);
			}
			tmpFile = null;
		} finally {
			if (tmpFile != null) {
				tmpFile.delete();
			}
		}
	}

	/**
	 * Checks if a parent directory exists and, if not, creates it.
	 * Stops at the destination directory level.
	 **/
	protected void checkParent(File f) throws BuildException {
		File parent = f.getParentFile();
		if (parent.equals(destDir) && !parent.exists()) {
			throw new BuildException("Destination dir no longer exists");
		}
		if (!parent.exists()) {
			checkParent(parent);
			if (!parent.mkdir()) {
				throw new BuildException("Unable to create directory "
						+ parent);
			}
		} else if (!parent.isDirectory()) {
			throw new BuildException("Not a directory: " + parent);
		}
	}
}
