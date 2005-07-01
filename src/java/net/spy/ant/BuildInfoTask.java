// Copyright (c) 2004  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 5778DEDA-1110-11D9-8E09-000A957659CC

package net.spy.ant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

import net.spy.util.SpyToker;
import net.spy.util.SpyUtil;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Task to build a build info class for a package.
 *
 * An example ant recipe for building a build info class is as follows:
 * <pre>
 *  &lt;propertyfile file="${BUILDDIR}/com/me/build.properties"&gt;
 *    &lt;entry key="java.vendor" value="${java.vendor}"/&gt;
 *    &lt;entry key="java.version" value="${java.version}"/&gt;
 *    &lt;entry key="os.name" value="${os.name}"/&gt;
 *    &lt;entry key="os.version" value="${os.version}"/&gt;
 *    &lt;entry key="build.date" type="date" value="now"/&gt;
 *    &lt;entry key="tree.version" value="${tree.version}"/&gt;
 *  &lt;/propertyfile&gt;
 *  &lt;buildinfo package="com.me" buildprops="com/me/build.properties"
 *    changelog="com/me/changelog.txt"
 *    destdir="${GENDIR}"/&gt;
 * </pre>
 *
 * In your jar task, you can now do the following:
 *
 * <pre>
 *    &lt;manifest&gt;
 *      &lt;attribute name="Main-Class" value="com.me.BuildInfo"/&gt;
 *    &lt;/manifest&gt;
 * </pre>
 *
 * The changelog is optional, and may be displayed from the jar main with the
 * -c option if it is present.
 */
public class BuildInfoTask extends Task {

	private static final String BUILDINFO="net/spy/ant/BuildInfo.txt";

	private String pkg=null;
	private String buildProps=null;
	private String changelog=null;
	private String destdir=".";

	/**
	 * Get an instance of BuildInfoTask.
	 */
	public BuildInfoTask() {
		super();
	}

	/** 
	 * Set the name of the package that will have the buildinfo file.
	 */
	public void setPackage(String to) {
		this.pkg=to;
	}

	/** 
	 * Set the path to the build properties.
	 */
	public void setBuildprops(String to) {
		this.buildProps=to;
	}

	/** 
	 * Set the path to the change log.
	 */
	public void setChangelog(String to) {
		this.changelog=to;
	}

	/** 
	 * Set the destination directory (top level generated code directory).
	 */
	public void setDestdir(String to) {
		this.destdir=to;
	}

	/** 
	 * Create the BuildInfo class.
	 */
	public void execute() throws BuildException {
		if(pkg == null) {
			throw new BuildException("package name required");
		}
		if(buildProps == null) {
			throw new BuildException("buildprops required");
		}

		ClassLoader cl=getClass().getClassLoader();
		URL u=cl.getResource(BUILDINFO);
		if(u == null) {
			throw new BuildException("Can't find " + BUILDINFO);
		}
		try {
			InputStreamReader ir=new InputStreamReader(u.openStream());
			String s=SpyUtil.getReaderAsString(ir);
			ir.close();

			HashMap<String, String> tokens=new HashMap<String, String>();
			tokens.put("PACKAGE", pkg);
			tokens.put("CHANGELOG", changelog);
			tokens.put("BUILDPROPS", buildProps);
			String output=new SpyToker().tokenizeString(s, tokens);

			String outFileName=destdir + File.separatorChar
				+ pkg.replace('.', File.separatorChar);

			// Make sure the directories exist for the package
			new File(outFileName).mkdirs();
			outFileName += File.separatorChar + "BuildInfo.java";

			// Get the output file and write it
			File outFile=new File(outFileName);
			FileWriter fw=new FileWriter(outFile);
			fw.write(output);
			fw.close();

			System.out.println("Wrote " + pkg + ".BuildInfo");
		} catch(IOException e) {
			e.printStackTrace();
			throw new BuildException("Could not process buildinfo", e);
		}
	}

}
