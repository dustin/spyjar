// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: SPGen.java,v 1.18 2002/11/10 04:01:49 dustin Exp $

package net.spy.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;

import java.lang.reflect.Field;

import java.text.NumberFormat;

import net.spy.SpyUtil;
import net.spy.db.QuerySelector;

/**
 * Generator for .spt-&gt;.java.
 */
public class SPGen extends Object {

	private BufferedReader in=null;
	private PrintWriter out=null;
	private String classname=null;

	private String section="";
	private String description="";
	private String procname="";
	private String pkg="";
	private String superclass="net.spy.db.DBSP";
	private String version="$Revision: 1.18 $";
	private long cachetime=0;
	private Map queries=null;
	private String currentQuery=QuerySelector.DEFAULT_QUERY;
	private List required=null;
	private List optional=null;
	private List output=null;
	private List results=null;
	private List args=null;
	private boolean debug=false;
	private int timeout=0;

	private static Set types=null;

	private boolean looseTypes=false;

	/**
	 * Get a new SPGen from the given BufferedReader.
	 */
	public SPGen(String classname, BufferedReader in, PrintWriter out) {
		super();
		this.in=in;
		this.out=out;
		this.classname=classname;
		queries=new TreeMap();
		required=new ArrayList();
		optional=new ArrayList();
		output=new ArrayList();
		results=new ArrayList();
		args=new ArrayList();

		if(types==null) {
			initTypes();
		}
	}

	private static synchronized void initTypes() {
		if(types==null) {
			Field fields[]=java.sql.Types.class.getDeclaredFields();
			types=new HashSet();

			for(int i=0; i<fields.length; i++) {
				types.add(fields[i].getName());
			}
		}
	}

	/** 
	 * Return true if this is a valid JDBC type.
	 * 
	 * @param name the name of the field to test
	 * @return true if the field is valid
	 */
	public static boolean isValidJDBCType(String name) {
		return(types.contains(name));
	}

	/** 
	 * Perform the actual generation.
	 * 
	 * @throws Exception if there's a problem parsing or writing
	 */
	public void generate() throws Exception {
		parse();
		write();
	}

	// Make a pretty string out of the cache time for the documentation.
	private String formatCacheTime() {
		long nowT=System.currentTimeMillis();
		long thenT=nowT-(cachetime * 1000);
		Date now=new Date(nowT);
		Date then=new Date(thenT);
		TimeSpan ts=new TimeSpan(now, then);
		return(ts.toString());
	}

	// Create a methodable name (i.e. blah returns Blah so you can make
	// getBlah.
	private String methodify(String word) {
		StringBuffer sb=new StringBuffer(word.length());

		StringTokenizer st=new StringTokenizer(word, "_");
		while(st.hasMoreTokens()) {
			String part=st.nextToken();
			StringBuffer mntmp=new StringBuffer(part);
			char c=Character.toUpperCase(mntmp.charAt(0));
			mntmp.setCharAt(0, c);

			sb.append(mntmp.toString());
		}

		return(sb.toString());
	}

	// Create a specific set method for a given parameter.
	private String createSetMethod(Parameter p) throws Exception {
		String rv=null;
		String types[]=null;

		// TODO:  Replace this with an extensible Map.
		if(p.getType().equals("java.sql.Types.BIT")) {
			String typesTmp[]={"boolean", "java.lang.Boolean"};
			types=typesTmp;
		} else if(p.getType().equals("java.sql.Types.DATE")) {
			String typesTmp[]={"java.sql.Date"};
			types=typesTmp;
		} else if(p.getType().equals("java.sql.Types.DOUBLE")) {
			String typesTmp[]={"double", "java.lang.Double"};
			types=typesTmp;
		} else if(p.getType().equals("java.sql.Types.FLOAT")) {
			String typesTmp[]={"float", "java.lang.Float"};
			types=typesTmp;
		} else if(p.getType().equals("java.sql.Types.INTEGER")) {
			String typesTmp[]={"int", "java.lang.Integer", "short",
								"java.lang.Short"};
			types=typesTmp;
		} else if(p.getType().equals("java.sql.Types.BIGINT")) {
			String typesTmp[]={"long", "java.lang.Long"};
			types=typesTmp;
		} else if(p.getType().equals("java.sql.Types.NUMERIC")) {
			String typesTmp[]={"java.math.BigDecimal"};
			types=typesTmp;
		} else if(p.getType().equals("java.sql.Types.DECIMAL")) {
			String typesTmp[]={"java.math.BigDecimal"};
			types=typesTmp;
		} else if(p.getType().equals("java.sql.Types.SMALLINT")) {
			String typesTmp[]={"int", "java.lang.Integer",
								"short", "java.lang.Short"};
			types=typesTmp;
		} else if(p.getType().equals("java.sql.Types.TINYINT")) {
			String typesTmp[]={"short", "java.lang.Short"};
			types=typesTmp;
		} else if(p.getType().equals("java.sql.Types.OTHER")) {
			String typesTmp[]={"java.lang.Object"};
			types=typesTmp;
		} else if(p.getType().equals("java.sql.Types.VARCHAR")) {
			String typesTmp[]={"java.lang.String"};
			types=typesTmp;
		} else if(p.getType().equals("java.sql.Types.TIME")) {
			String typesTmp[]={"java.sql.Time"};
			types=typesTmp;
		} else if(p.getType().equals("java.sql.Types.TIMESTAMP")) {
			String typesTmp[]={"java.sql.Timestamp"};
			types=typesTmp;
		} else {
			throw new Exception("Whoops, type " + p.getType() 
				+ " seems to have been overlooked.");
		}

		String methodName=methodify(p.getName());

		rv="";
		for(int i=0; i<types.length; i++) {
			String type=types[i];
			System.out.println("Generating " + p + " for " + type);
			rv+="\t/**\n"
				+ "\t * Set the ``" + p.getName() + "'' parameter.\n"
				+ "\t * " + p.getDescription() + "\n"
				+ "\t *\n"
				+ "\t * @param to the value to which to set the parameter\n"
				+ "\t */\n"
				+ "\tpublic void set" + methodName + "(" + type + " to)\n"
				+ "\t\tthrows SQLException {\n\n"
				+ "\t\tset(\"" + p.getName() + "\", to);\n"
				+ "\t}\n";
		}
	
		return(rv);
	}

	private void write() throws Exception {
		System.out.println("Writing out " + pkg + "." + classname);
		// Copyright info
		out.println(
			"// Copyright (c) 2001  SPY internetworking <dustin@spy.net>\n"
		    + "// Written by Dustin's SQL generator version " + version +"\n"
			+ "//\n"
			+ "// $" + "Id" + "$\n");
		out.flush();

		// Package info
		out.println("package " + pkg + ";\n");

		// Imports
		out.println("import java.sql.Types;\n"
			+ "import java.sql.Connection;\n"
			+ "import java.sql.SQLException;\n"
			+ "import java.util.Map;\n"
			+ "import java.util.HashMap;\n"
			+ "import " + superclass + ";\n"
			+ "import net.spy.SpyConfig;\n");

		// Generate the documentation.
		out.println("/**\n"
			+ " * \n"
			+ " * " + description + "\n"
			+ " *\n"
			+ " * <p>\n"
			+ " *\n"
			+ " * Generated by SPGen " + version + " on "
				+ new java.util.Date() + "\n"
			+ " *\n"
			+ " * </p>\n"
			+ " * <p>\n"
			+ " *");

		// Debug mode.
		if(debug) {
			out.println(" * <font color=\"red\" size=\"+2\"><b>"
				+ "Debug is on!</b></font>\n"
				+ " *\n"
				+ " * </p>\n"
				+ " *\n"
				+ " * <p>\n"
				+ " *");
		}

		// Different stuff for different classes
		if(superclass.equals("net.spy.db.DBSP")) {
			out.println(" * <b>Procedure Name</b>\n"
				+ " *\n"
				+ " * <ul>\n"
				+ " *  <li>" + procname + "</li>\n"
				+ " * </ul>");
		} else if (superclass.equals("net.spy.db.DBCP")) {
			out.println(" * <b>Callable Name</b>\n"
				+ " *\n"
				+ " * <ul>\n"
				+ " *  <li>" + procname + "</li>\n"
				+ " * </ul>");
		} else {
			out.println(" * <b>SQL Query</b>\n"
				+ " *\n"
				+ " * <ul>\n"
				+ " " + getDocQuery() + "\n"
				+ " * </ul>");
		}

		// Required parameters
		out.println(" *\n"
			+ " * <b>Required Parameters</b>\n"
			+ " * <ul>");
		if(required.size()==0) {
			out.println(" *  <li><i>none</i></li>");
		} else {
			for(Iterator i=required.iterator(); i.hasNext(); ) {
				Parameter p=(Parameter)i.next();
				out.println(" * <li>" + p.getName() + " - "
					+ "{@link java.sql.Types#" + p.getType() + " "
						+ p.getType() + "}\n * "
					+ " - " + p.getDescription() + "</li>");
			}
		}
		out.println(" * </ul>\n"
			+ " *\n"
			+ " * </p>\n"
			+ " * <p>\n"
			+ " *");

		// Optional parameters
		out.println(" *\n"
			+ " * <b>Optional Parameters</b>\n"
			+ " * <ul>");
		if(optional.size()==0) {
			out.println(" *  <li><i>none</i></li>");
		} else {
			for(Iterator i=optional.iterator(); i.hasNext(); ) {
				Parameter p=(Parameter)i.next();
				out.println(" * <li>" + p.getName() + " - "
					+ "{@link java.sql.Types#" + p.getType() + " "
						+ p.getType() + "}\n * "
					+ " - " + p.getDescription() + "</li>");
			}
		}
		out.println(" * </ul>\n"
			+ " *\n"
			+ " * </p>\n"
			+ " * <p>\n"
			+ " *");

		if (superclass.equals("net.spy.db.DBCP")) {
			// Output parameters
			out.println(" *\n"
				+ " * <b>Output Parameters</b>\n"
				+ " * <ul>");
			if(output.size()==0) {
				out.println(" *  <li><i>none</i></li>");
			} else {
				for(Iterator i=output.iterator(); i.hasNext(); ) {
					Parameter p=(Parameter)i.next();
					out.println(" * <li>" + p.getName() + " - "
						+ "{@link java.sql.Types#" + p.getType() + " "
							+ p.getType() + "}\n * "
						+ " - " + p.getDescription() + "</li>");
				}
			}
			out.println(" * </ul>\n"
				+ " *\n"
				+ " * </p>\n"
				+ " * <p>\n"
				+ " *");
		}

		// Results
		if(results.size() > 0) {
			out.println(" * <b>Results</b>\n"
				+ " * <ul>");

			for(Iterator i=results.iterator(); i.hasNext(); ) {
				Result r=(Result)i.next();

				out.print(" *  <li>"
					+ r.getName() + " - ");
				if(isValidJDBCType(r.getType())) {
					out.print("{@link java.sql.Types#" + r.getType() + " "
						+ r.getType() + "}\n *   ");
				} else {
					out.print(r.getType());
				}
				out.println(" - " + r.getDescription() + "</li>");
			}

			out.println(" * </ul>\n"
				+ " *");
		}

		// Document the cache time
		out.println(" * <b>Cache Time</b>\n"
			+ " * <ul>");
		if(cachetime > 0) {
			NumberFormat nf=NumberFormat.getNumberInstance();
			out.println(" *  <li>The results of this call will be cached for "
				+ formatCacheTime() 
				+ " (" + nf.format(cachetime) + " seconds) by default.</li>");
		} else {
			out.println(" *  <li>The results of this call will not "
				+ "be cached by default.</li>");
		}
		out.println(" * </ul>");

		// end the class documentation comment
		out.println(" * </p>\n"
			+ " */");

		// Actual code generation
		out.println("public class " + classname + " extends "
		+ superclass + " {\n");

		// The map (staticially initialized)

		out.println("\tprivate static final Map queries=getQueries();\n");

		// Constructor documentation
		out.println("\t/**\n"
			+ "\t * Construct a DBSP which will get its connections from\n"
			+ "\t *   SpyDB using the given config.\n"
			+ "\t * @param conf the configuration to use\n"
			+ "\t * @exception SQLException if there's a failure to "
			+ "construct\n"
			+ "\t */");

		// SpyConfig constructor
		out.println("\tpublic " + classname + "(SpyConfig conf) "
			+ "throws SQLException {\n"
			+ "\t\t// Super constructor\n"
			+ "\t\tsuper(conf);\n"
			+ "\t\tspinit();\n"
			+ "\t}\n");

		// Constructor documentation
		out.println("\t/**\n"
			+ "\t * Construct a DBSP which use the existing Connection\n"
			+ "\t *   for database operations.\n"
			+ "\t * @param conn the connection to use\n"
			+ "\t * @exception SQLException if there's a failure to "
			+ "construct\n"
			+ "\t */");

		// Connection constructor
		out.println("\tpublic " + classname + "(Connection conn) "
			+ "throws SQLException {\n"
			+ "\t\t// Super constructor\n"
			+ "\t\tsuper(conn);\n"
			+ "\t\tspinit();\n"
			+ "\t}\n");

		// Initializer
		out.println("\tprivate void spinit() throws SQLException {");

		// Set the debug mode if we're supposed to
		if(debug) {
			out.println("\t\t// Debug is on for this DBSP\n"
				+ "\t\tsetDebug(true);\n");
		}

		// set the timeout variable
		out.println("\t\tsetQueryTimeout("+timeout+");\n");

		// Figure out whether we're a DBSP or a DBSQL
		if(superclass.equals("net.spy.db.DBSP") ||
				superclass.equals("net.spy.db.DBCP")) {
			out.println("\t\t// Set the stored procedure name\n"
				+ "\t\tsetSPName(\"" + procname + "\");");
		} else {
			out.println("\t\t// Register the SQL queries");
			out.println("\t\tsetRegisteredQueryMap(queries);");
		}

		if (args.size()>0) {
			out.println("\n\t\t// Set the parameters.");
			for (Iterator i=args.iterator(); i.hasNext(); ) {
				Parameter p=(Parameter)i.next();
				if (p.isRequired()) {
					if (!p.isOutput()) {
						out.println("\t\tsetRequired(\"" + p.getName() + "\", "
							+ p.getType() + ");");
					} else {
						out.println("\t\tsetOutput(\"" + p.getName() + "\", "
							+ p.getType() + ");");
					}
				} else {
					out.println("\t\tsetOptional(\"" + p.getName() + "\", "
						+ p.getType() + ");");
				}
			}
		}

		// Set the cachetime, if there is one
		if(cachetime>0) {
			out.println("\n\t\t// Set the default cache time.");
			out.println("\t\tsetCacheTime(" + cachetime + ");");
		}

		// End of spinit
		out.println("\t}\n");

		// Create the static initializers
		out.println("\t// Static initializer for query map.");
		out.println("\tprivate static Map getQueries() {");
		out.println("\t\t" + getJavaQueries());
		out.println("\n\t\treturn(rv);");
		out.println("\t}\n");

		// Create set methods for all the individual parameters
		for(Iterator i=args.iterator(); i.hasNext(); ) {
			Parameter p=(Parameter)i.next();

			out.println(createSetMethod(p));
		}

		out.println("}");

	}

	// Fix > and < characters, and & characters if there are any
	private String docifySQL(String sql) {
		StringBuffer sb=new StringBuffer(sql.length());

		char acters[]=sql.toCharArray();
		for(int i=0; i<acters.length; i++) {
			switch(acters[i]) {
				case '>':
					sb.append("&gt;");
					break;
				case '<':
					sb.append("&lt;");
					break;
				case '&':
					sb.append("&amp;");
					break;
				default:
					sb.append(acters[i]);
			}
		}

		return (sb.toString());
	}

	private String getDocQuery() {
		StringBuffer sb=new StringBuffer(1024);

		for(Iterator i=queries.entrySet().iterator(); i.hasNext();) {

			Map.Entry me=(Map.Entry)i.next();
			List sqlquery=(List)me.getValue();

			sb.append(" * <li>\n");
			sb.append(" *  <b>\n");
			sb.append(" *   ");
			sb.append(me.getKey());
			sb.append("\n");
			sb.append(" *  </b>\n");
			sb.append(" *  <pre>\n");
			for(Iterator i2=sqlquery.iterator(); i2.hasNext(); ) {
				String part=(String)i2.next();
				sb.append(" * ");
				sb.append(docifySQL(part));
				sb.append("\n");
			}
			sb.append(" *  </pre>\n * </li>\n");

		}

		return(sb.toString().trim());
	}

	private String getJavaQueries() {
		StringBuffer sb=new StringBuffer(1024);

		sb.append("StringBuffer query=null;\n");
		sb.append("\t\tMap rv=new HashMap();\n");

		for(Iterator i=queries.entrySet().iterator(); i.hasNext();) {

			Map.Entry me=(Map.Entry)i.next();
			List sqlquery=(List)me.getValue();

			sb.append("\n\t\tquery=new StringBuffer(1024);");

			for(Iterator i2=sqlquery.iterator(); i2.hasNext(); ) {
				String part=(String)i2.next();
				sb.append("\n\t\tquery.append(\"");

				for(StringTokenizer st=new StringTokenizer(part, "\"", true);
					st.hasMoreTokens(); ) {

					String tmp=st.nextToken();
					if(tmp.equals("\"")) {
						tmp="\\\"";
					}
					sb.append(tmp);
				}

				sb.append("\\n\");");
			}

			sb.append("\n\t\trv.put(\"");
			sb.append(me.getKey());
			sb.append("\", ");
			sb.append("query.toString());\n");

		}

		return(sb.toString().trim());
	}

	private void parse() throws Exception {

		// this is for when a user overrides the superclass
		StringBuffer user_superclass=null;

		System.out.println("Parsing " + classname + ".spt");

		String tmp=in.readLine();
		while(tmp!=null) {

			// Don't do anything if the line is empty
			if(tmp.length() > 0) {
				if(tmp.charAt(0) == '@') {
					// lower case and trim before we begin...RAP WITH ME!!
					section=tmp.substring(1).trim().toLowerCase();
					// System.out.println("Working on section " + section);

					// Handlers for things that occur when a section is begun
					if(section.equals("debug")) {
						debug=true;
					} else if (section.startsWith("loosetyp")) {
						looseTypes=true;
					} else if (section.startsWith("sql.")) {
						currentQuery=section.substring(4);
						section="sql";
					} else if (section.equals("sql")) {
						currentQuery=QuerySelector.DEFAULT_QUERY;
					}
				} else if(tmp.charAt(0) == '#') {
					// Comment, ignore
				} else {

					if(section.equals("description")) {
						description+=tmp;
					} else if(section.equals("debug")) {
						System.err.println("Warning, stuff in debug section:  "
							+ tmp);
					} else if(section.equals("sql")) {
						superclass="net.spy.db.DBSQL";

						List sqlquery=(List)queries.get(currentQuery);
						if(sqlquery == null) {
							sqlquery=new ArrayList();
							queries.put(currentQuery, sqlquery);
						}
						sqlquery.add(tmp);
					} else if(section.equals("procname")) {
						procname+=tmp;
						superclass="net.spy.db.DBSP";
					} else if(section.equals("callable")) {
						procname+=tmp;
						superclass="net.spy.db.DBCP";
					} else if(section.equals("params")) {
						Parameter param=new Parameter(tmp);
						args.add(param);
						if(param.isRequired()) {
							if (!param.isOutput()) {
								required.add(param);
							} else {
								output.add(param);
							}
						} else {
							optional.add(param);
						}
					} else if(section.equals("results")) {
						try {
							results.add(new Result(tmp));
						} catch(IllegalArgumentException e) {
							System.err.println("Warning in " + classname
								+ ":  " + e.getMessage());
						}
					} else if(section.equals("package")) {
						pkg+=tmp;
					} else if(section.equals("cachetime")) {
						cachetime=Long.parseLong(tmp);
					} else if(section.equals("timeout")) {
						timeout=Integer.parseInt(tmp);
					} else if(section.equals("superclass")) {
						user_superclass=new StringBuffer(96);
						user_superclass.append(tmp);
					} else {
						throw new Exception("Unknown section: ``"+section+"''");
					}

				}
			}
			
			tmp=in.readLine();
		}
		
		// if the user over-rode (like your mom) the superclass, use it!!
		if (user_superclass!=null) {
			superclass=user_superclass.toString();
		}

	}

	// Private class for results

	private class Result extends Object {
		private String name=null;
		private String type=null;
		private String description=null;

		public Result(String line) {
			super();

			StringTokenizer st=new StringTokenizer(line, " \t");
			try {
				name=st.nextToken();
			} catch(NoSuchElementException e) {
				throw new IllegalArgumentException("No name given for result");
			}

			try {
				type=st.nextToken();

				if(!isValidJDBCType(type)) {
					System.err.println("Warning! Invalid JDBC type found:  "
						+ type);
				}
			} catch(NoSuchElementException e) {
				throw new IllegalArgumentException(
					"No type given for result ``" + name + "''");
			}

			try {
				description=st.nextToken("\n");
			} catch(NoSuchElementException e) {
				throw new IllegalArgumentException(
					"No description given for result ``" + name + "''");
			}
		}

		public String getName() {
			return(name);
		}

		public String getType() {
			return(type);
		}

		public String getDescription() {
			return(description);
		}

	}

	// Private class for parameters

	private class Parameter extends Object {
		private String name=null;
		private boolean required=false;
		private String type=null;
		private String description=null;
		private boolean output=false;

		public Parameter(String line) {
			super();

			StringTokenizer st=new StringTokenizer(line, " \t");
			try {
				name=st.nextToken();
			} catch (NoSuchElementException ex) {
				// ASSERT: this theoretically should never happen,
				// otherwise how did we end up here in the first place?
				throw new IllegalArgumentException("Missing parameter name! "
					+ex.toString());
			}

			String tmp=null;
			try {
				tmp=st.nextToken();
			} catch (NoSuchElementException ex) {
				// at this point you have forgotten to add in the parameter
				// of whether or not the parameter is required/optional.  I
				// guess a default could be applied here, but I'm just
				// gonna throw an Exception.
				throw new IllegalArgumentException(
					"Missing parameter requirement! " +ex.toString());
			}

			if(tmp.equals("required")) {
				required=true;
				output=false;
			} else if(tmp.equals("optional")) {
				required=false;
				output=false;
			} else if(tmp.equals("output")) {
				required=true;
				output=true;
			} else {
				throw new IllegalArgumentException(
					"Parameter must be required or optional, not "
					+ tmp + " like in " + line);
			}

			try {
				type=st.nextToken();

				if(isValidJDBCType(type)) {
					type="java.sql.Types."+type;
				} else {
					if (!looseTypes) {
						throw new IllegalArgumentException("Invalid JDBC type: "
							+ type);
					}
				}
			} catch (NoSuchElementException ex) {
				// now the variable type is missing  That's no good, you
				// need a speficic type ya know.
				throw new IllegalArgumentException("Missing parameter type! "
					+ex.toString());
			}

			try {
				// This character pretty much can't be in the line.
				description=st.nextToken("\n");
			} catch (NoSuchElementException ex) {
				// I don't think we cre if it's documented or not!  But
				// honestly I don't think this should ever happen cause you
				// need a newline.  Well, I guess if you ended the file odd
				// enough, and without a EOL before the EOF...very odd case
				description="";
			}
		}

		public String toString() {
			return("{Parameter " + name + "}");
		}

		public String getName() {
			return(name);
		}

		public String getType() {
			return(type);
		}

		public String getDescription() {
			return(description);
		}

		public boolean isRequired() {
			return(required);
		}

		public boolean isOutput() {
			return(output);
		}
	}

	/**
	 * Usage:  SPGen filename
	 */
	public static void main(String args[]) throws Exception {

		String infile=args[0];
		// Get rid of the .spt
		int lastslash=infile.lastIndexOf(File.separatorChar);
		String basename=infile.substring(0, infile.indexOf(".spt"));
		// If it matches, start at the next character, if it didn't, it's
		// -1 and start at 0
		String classname=basename.substring(lastslash+1);
		String outfile=basename + ".java";

		BufferedReader in=new BufferedReader(new FileReader(infile));
		PrintWriter out=new PrintWriter(new FileWriter(outfile));
		SPGen spg=new SPGen(classname, in, out);

		spg.generate();

		in.close();
		out.close();
	}

}
