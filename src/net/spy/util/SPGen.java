// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: SPGen.java,v 1.39 2004/02/17 19:22:08 dustin Exp $

package net.spy.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import java.util.Collection;
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
import java.util.ResourceBundle;
import java.util.MissingResourceException;

import java.math.BigDecimal;

import java.lang.reflect.Field;

import java.text.NumberFormat;

import net.spy.SpyObject;
import net.spy.SpyUtil;
import net.spy.db.QuerySelector;

/**
 * Generator for .spt-&gt;.java.
 */
public class SPGen extends SpyObject {

	private BufferedReader in=null;
	private PrintWriter out=null;
	private String classname=null;
	private boolean isInterface=true;
	private boolean wantsResultSet=false;
	private boolean wantsCursor=false;

	private String section="";
	private String description="";
	private String procname="";
	private String pkg="";

	private String superclass=null;
	private String dbcpSuperclass=null;
	private String dbspSuperclass=null;

	private String superinterface=null;
	private String version="$Revision: 1.39 $";
	private long cachetime=0;
	private Map queries=null;
	private String currentQuery=QuerySelector.DEFAULT_QUERY;
	private List results=null;
	private List args=null;
	private Set interfaces=null;
	private Set imports=null;
	private int timeout=0;

	private static Set types=null;
	private static Map javaTypes=null;
	private static Map javaResultTypes=null;

	private boolean looseTypes=false;

	private boolean typeDbsp=false;
	private boolean typeDbcp=false;

	/**
	 * Get a new SPGen from the given BufferedReader.
	 *
	 * @param cn the name of the class to generate
	 * @param in the stream containing the spt source
	 * @param out the stream to which the java code will be written
	 */
	public SPGen(String cn, BufferedReader i, PrintWriter o) {
		super();
		this.in=i;
		this.out=o;
		this.classname=cn;
		queries=new TreeMap();
		results=new ArrayList();
		args=new ArrayList();
		interfaces=new HashSet();
		imports=new HashSet();

		if(types==null) {
			initTypes();
		}
	}

	/** 
	 * Set the superclass of the generated java class.
	 */
	public void setSuperclass(String sc) {
		if (sc!=null) {
			this.superclass=sc;
		}
	}

	/** 
	 * Set the DBCP superclass of the generated java class.
	 */
	public void setDbcpSuperclass(String sc) {
		if (sc!=null) {
			this.dbcpSuperclass=sc;
		}
	}

	/** 
	 * Set the DBSP superclass of the generated java class.
	 */
	public void setDbspSuperclass(String sc) {
		if (sc!=null) {
			this.dbspSuperclass=sc;
		}
	}

	private static synchronized void initTypes() {
		if(types==null) {
			javaTypes=new HashMap();
			javaResultTypes=new HashMap();

			// Map the jdbc types to useful java types
			String t="java.sql.Types.BIT";
			javaTypes.put(t, "Boolean");
			javaResultTypes.put(t, "boolean");
			t="java.sql.Types.DATE";
			javaTypes.put(t, "Date");
			javaResultTypes.put(t, "java.sql.Date");
			t="java.sql.Types.DOUBLE";
			javaTypes.put(t, "Double");
			javaResultTypes.put(t, "double");
			t="java.sql.Types.FLOAT";
			javaTypes.put(t, "Float");
			javaResultTypes.put(t, "float");
			t="java.sql.Types.INTEGER";
			javaTypes.put(t, "Int");
			javaResultTypes.put(t, "int");
			t="java.sql.Types.BIGINT";
			javaTypes.put(t, "BigDecimal");
			javaResultTypes.put(t, "java.math.BigDecimal");
			t="java.sql.Types.NUMERIC";
			javaTypes.put(t, "BigDecimal");
			javaResultTypes.put(t, "java.math.BigDecimal");
			t="java.sql.Types.DECIMAL";
			javaTypes.put(t, "BigDecimal");
			javaResultTypes.put(t, "java.math.BigDecimal");
			t="java.sql.Types.SMALLINT";
			javaTypes.put(t, "Int");
			javaResultTypes.put(t, "int");
			t="java.sql.Types.TINYINT";
			javaTypes.put(t, "Int");
			javaResultTypes.put(t, "int");
			t="java.sql.Types.OTHER";
			javaTypes.put(t, "Object");
			javaResultTypes.put(t, "Object");
			t="java.sql.Types.VARCHAR";
			javaTypes.put(t, "String");
			javaResultTypes.put(t, "String");
			t="java.sql.Types.TIME";
			javaTypes.put(t, "Time");
			javaResultTypes.put(t, "java.sql.Time");
			t="java.sql.Types.TIMESTAMP";
			javaTypes.put(t, "Timestamp");
			javaResultTypes.put(t, "java.sql.Timestamp");

			// Same as above, without the java.sql. part
			Map tmp=new HashMap();
			for(Iterator i=javaTypes.entrySet().iterator(); i.hasNext();) {
				Map.Entry me=(Map.Entry)i.next();
				String k=(String)me.getKey();
				if(k.startsWith("java.sql.Types.")) {
					tmp.put(k.substring(15), me.getValue());
				}
			}
			javaTypes.putAll(tmp);
			tmp.clear();
			for(Iterator i=javaResultTypes.entrySet().iterator();
				i.hasNext();) {
				Map.Entry me=(Map.Entry)i.next();
				String k=(String)me.getKey();
				if(k.startsWith("java.sql.Types.")) {
					tmp.put(k.substring(15), me.getValue());
				}
			}
			javaResultTypes.putAll(tmp);

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
		boolean customtype=false;

		// Get the type map entry for this parameter
		try {
			ResourceBundle typeMap=
				ResourceBundle.getBundle("net.spy.db.typemap");
			String typeString=typeMap.getString(p.getType());
			types=SpyUtil.split(" ", typeString);
		} catch(MissingResourceException e) {
			getLogger().warn("Can't set all types for " + p, e);
			// XXX This is just a thing to get me over the hump
			//return("");
			String typesTmp[]={"java.lang.Object"};
			types=typesTmp;
			customtype=true;
		}

		String methodName=methodify(p.getName());

		rv="";
		for(int i=0; i<types.length; i++) {
			String type=types[i];
			// Too verbose, need some way to configure this kind of stuff
			getLogger().debug("Generating " + p + " for " + type);
			rv+="\t/**\n"
				+ "\t * Set the ``" + p.getName() + "'' parameter.\n"
				+ "\t * " + p.getDescription() + "\n"
				+ "\t *\n"
				+ "\t * @param to the value to which to set the parameter\n"
				+ "\t */\n"
				+ "\tpublic void set" + methodName + "(" + type + " to)\n"
				+ "\t\tthrows SQLException";
			if(isInterface) {
				rv+=";\n";
			} else {
				if (customtype) {
					rv+=" {\n\n"
						+ "\t\tsetArg(\"" + p.getName() + "\", to, "
						+ p.getType() +");\n\t}\n";
				} else {
					rv+=" {\n\n"
						+ "\t\tset(\"" + p.getName() + "\", to);\n\t}\n";
				}
			}
		}

		return(rv);
	}

	private void write() throws Exception {
		System.out.println("Writing out " + pkg + "." + classname);
		// Extract the version from the version var.
		String v=version.substring(11, version.length()-2);
		// Copyright info
		out.println(
			"// Copyright (c) 2001  SPY internetworking <dustin@spy.net>\n"
		    + "// Written by Dustin's SQL generator version " + v +"\n"
			+ "//\n"
			+ "// $" + "Id" + "$\n");
		out.flush();

		// Package info
		out.println("package " + pkg + ";\n");

		// Imports
		out.println("import java.sql.Types;\n"
			+ "import java.sql.Connection;\n"
			+ "import java.sql.SQLException;\n"
			+ "import java.sql.ResultSet;\n"
			+ "import java.util.Map;\n"
			+ "import java.util.HashMap;\n"
			+ "import net.spy.SpyConfig;\n");

		// custom imports
		for (Iterator it=imports.iterator(); it.hasNext();) {
			String tmpimp=(String)it.next();
			out.print("import ");
			out.print(tmpimp);
			out.println(";");
		}
		out.println("\n");

		// Generate the documentation.
		out.println("/**\n"
			+ " * \n"
			+ " * " + description + "\n"
			+ " *\n"
			+ " * <p>\n"
			+ " *\n"
			+ " * Generated by SPGen " + v + " on "
				+ new java.util.Date() + ".\n"
			+ " *\n"
			+ " * </p>\n"
			+ " * <p>\n"
			+ " *");

		// cursor requested mode.
		if(wantsCursor) {
			out.println(" * <b>This query requests a cursor.</b>\n"
				+ " *\n"
				+ " * </p>\n"
				+ " *\n"
				+ " * <p>\n"
				+ " *");
		}

		// Different stuff for different classes
		if(typeDbsp) {
			out.println(" * <b>Procedure Name</b>\n"
				+ " *\n"
				+ " * <ul>\n"
				+ " *  <li>" + procname + "</li>\n"
				+ " * </ul>");
		} else if (typeDbcp) {
			out.println(" * <b>Callable Name</b>\n"
				+ " *\n"
				+ " * <ul>\n"
				+ " *  <li>" + procname + "</li>\n"
				+ " * </ul>");
		} else {
			// If it's not a stored procedure or callable, and it's not an
			// interface, show the query.
			if(!isInterface) {
				out.println(" * <b>SQL Query</b>\n"
					+ " *\n"
					+ " * <ul>\n"
					+ " " + getDocQuery() + "\n"
					+ " * </ul>");
			}
		}

		// Required parameters
		out.println(" *\n"
			+ " * <b>Required Parameters</b>\n"
			+ " * <ul>");
		if(getRequiredArgs(false).size()==0) {
			out.println(" *  <li><i>none</i></li>");
		} else {
			for(Iterator i=getRequiredArgs(false).iterator(); i.hasNext();) {
				Parameter p=(Parameter)i.next();
				out.print(" * <li>" + p.getName() + " - "
					+ "{@link java.sql.Types#" + p.getShortType() + " "
						+ p.getType() + "}\n * "
					+ " - " + p.getDescription());
				Default d=p.getDefaultValue();
				if(d!=null) {
					out.print(" (default:  <i>" + d.getPrintValue() + "</i>)");
				}
				out.println("</li>");
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
		if(getOptionalArgs().size()==0) {
			out.println(" *  <li><i>none</i></li>");
		} else {
			for(Iterator i=getOptionalArgs().iterator(); i.hasNext();) {
				Parameter p=(Parameter)i.next();
				out.println(" * <li>" + p.getName() + " - "
					+ "{@link java.sql.Types#" + p.getShortType() + " "
						+ p.getType() + "}\n * "
					+ " - " + p.getDescription() + "</li>");
			}
		}
		out.println(" * </ul>\n"
			+ " *\n"
			+ " * </p>\n"
			+ " * <p>\n"
			+ " *");

		if (typeDbcp) {
			// Output parameters
			out.println(" *\n"
				+ " * <b>Output Parameters</b>\n"
				+ " * <ul>");
			if(getOutputParameters().size()==0) {
				out.println(" *  <li><i>none</i></li>");
			} else {
				for(Iterator i=getOutputParameters().iterator(); i.hasNext();) {
					Parameter p=(Parameter)i.next();
					out.println(" * <li>" + p.getName() + " - "
						+ "{@link java.sql.Types#" + p.getShortType() + " "
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

			for(Iterator i=results.iterator(); i.hasNext();) {
				Result r=(Result)i.next();

				out.print(" *  <li>"
					+ r.getName() + " - ");
				if(isValidJDBCType(r.getType())) {
					out.print("{@link java.sql.Types#" + r.getShortType() + " "
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
		out.print("public " + (isInterface?"interface ":"class ")
			+ classname + " extends "
				+ (isInterface?superinterface:superclass));
		if(interfaces.size() > 0) {
			out.print("\n\timplements " + SpyUtil.join(interfaces, ", "));
		}
		out.println(" {\n");

		// The map (staticially initialized)

		if(!isInterface) {
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

			// If a cursor was requested, build one
			if(wantsCursor) {
				out.println("\t\t// Generate a cursor for this query\n"
					+ "\t\tgenerateCursorName();\n");
			}

			// set the timeout variable
			out.println("\t\tsetQueryTimeout("+timeout+");\n");

			// Figure out whether we're a DBSP or a DBSQL
			if(typeDbsp || typeDbcp) {
				out.println("\t\t// Set the stored procedure name\n"
					+ "\t\tsetSPName(\"" + procname + "\");");
			} else {
				out.println("\t\t// Register the SQL queries");
				out.println("\t\tsetRegisteredQueryMap(queries);");
			}

			// parameters
			if (args.size()>0) {
				out.println("\n\t\t// Set the parameters.");
				for (Iterator i=args.iterator(); i.hasNext();) {
					Parameter p=(Parameter)i.next();
					if (p.isRequired()) {
						if (!p.isOutput()) {
							out.println("\t\tsetRequired(\"" + p.getName()
								+ "\", " + p.getType() + ");");
						} else {
							out.println("\t\tsetOutput(\"" + p.getName()
								+ "\", " + p.getType() + ");");
						}
					} else {
						out.println("\t\tsetOptional(\"" + p.getName()
							+ "\", " + p.getType() + ");");
					}

					Default d=p.getDefaultValue();
					if(d!=null) {
						out.println("\t\t// Default for " + d.getName());
						out.println("\t\tset(\"" + d.getName() + "\", "
							+ d.getPrintValue() + ");");
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
		}

		// Create set methods for all the individual parameters
		int count=1;
		for(Iterator i=args.iterator(); i.hasNext();) {
			Parameter p=(Parameter)i.next();

			if (!p.isOutput()) {
				out.println(createSetMethod(p));
			} else {
				// output param
				out.println(createGetOutputMethod(p, count));
			}
			count++;
		}

		// If we want result sets, add them.
		if(wantsResultSet) {
			if(results.size() > 0) {
				out.println(createExecuteMethods());
				out.println(createResultClass());
			}
		}

		out.println("}");

	}

	private String createExecuteMethods() {
		String rv="\t/**\n"
			+ "\t * Execute this query and get a Result object.\n"
			+ "\t */\n"
			+ "\tpublic Result getResult() throws SQLException {\n"
			+ "\t\treturn(new Result(executeQuery()));\n"
			+ "\t}\n";
		return(rv);
	}

	private String createGetMethod(Result r) {
		String rv="\t\t/**\n"
			+ "\t\t * Get the " + r.getName() + " value.\n"
			+ "\t\t */\n"
			+ "\t\tpublic " + r.getJavaResultType()
			+ " get" + methodify(r.getName()) + "() throws SQLException {\n"
			+ "\t\t\treturn(get" + r.getJavaType()
				+ "(\"" + r.getName() + "\"));\n"
			+ "\t\t}\n\n";

		return(rv);
	}

	private String createGetOutputMethod(Parameter p, int index) {
		String rv="\tpublic Object get"+methodify(p.getName())
			+"() throws SQLException {\n"
			+"\t\treturn(getCallableStatement().getObject("+index
				+"));\n"
			+"\t}\n\n";

		return(rv);
	}

	private String createResultClass() {
		String rv="";

		// Class header
		rv+="\t/**\n"
			+ "\t * ResultSet object representing the results of this query.\n"
			+ "\t */\n"
			+ "\tpublic class Result extends net.spy.db.DBSPResult {\n"
			+ "\n\t\tprivate Result(ResultSet rs) {\n"
			+ "\t\t\tsuper(rs);\n"
			+ "\t\t}\n\n";

		for(Iterator i=results.iterator(); i.hasNext();) {
			rv+=createGetMethod((Result)i.next());
		}

		// End of class
		rv+="\n\t}\n";

		return(rv);
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
			for(Iterator i2=sqlquery.iterator(); i2.hasNext();) {
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

			for(Iterator i2=sqlquery.iterator(); i2.hasNext();) {
				String part=(String)i2.next();
				sb.append("\n\t\tquery.append(\"");

				for(StringTokenizer st=new StringTokenizer(part, "\"", true);
					st.hasMoreTokens();) {

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
		StringBuffer userSuperclass=null;

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
						System.err.println("debug is deprecated");
					} else if (section.equals("genresults")) {
						wantsResultSet=true;
					} else if (section.equals("cursor")) {
						wantsCursor=true;
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
					} else if(section.equals("sql")) {
						isInterface=false;
						if (superclass==null) {
							superclass="net.spy.db.DBSQL";
						}

						List sqlquery=(List)queries.get(currentQuery);
						if(sqlquery == null) {
							sqlquery=new ArrayList();
							queries.put(currentQuery, sqlquery);
						}
						sqlquery.add(tmp);
					} else if(section.equals("procname")) {
						isInterface=false;
						procname+=tmp;
						if (dbspSuperclass==null) {
							superclass="net.spy.db.DBSP";
						} else {
							superclass=dbspSuperclass;
						}
						typeDbsp=true;
					} else if(section.equals("callable")) {
						isInterface=false;
						procname+=tmp;
						if (dbcpSuperclass==null) {
							superclass="net.spy.db.DBCP";
						} else {
							superclass=dbcpSuperclass;
						}
						typeDbcp=true;
					} else if(section.equals("defaults")) {
						Default d=new Default(tmp);
						registerDefault(d);
					} else if(section.equals("params")) {
						Parameter param=new Parameter(tmp);
						args.add(param);
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
						userSuperclass=new StringBuffer(96);
						userSuperclass.append(tmp);
					} else if(section.equals("import")) {
						imports.add(tmp);
					} else if(section.equals("implements")) {
						interfaces.add(tmp);
					} else {
						throw new Exception("Unknown section: ``"+section+"''");
					}

				}
			}
			
			tmp=in.readLine();
		}

		// Make sure a superinterface got defined
		if(superinterface == null) {
			superinterface="net.spy.db.DBSPLike";
		}
		
		// if the user over-rode (like your mom) the superclass, use it!!
		if (userSuperclass!=null) {
			if(isInterface) {
				superinterface=userSuperclass.toString();
			} else {
				superclass=userSuperclass.toString();
			}
		}

	}

	private void registerDefault(Default d) {
		boolean done=false;
		for(Iterator i=args.iterator(); done==false && i.hasNext();) {
			Parameter p=(Parameter)i.next();
			if(p.getName().equals(d.getName())) {
				p.setDefaultValue(d);
				done=true;
			}
		}
		if(done==false) {
			throw new IllegalArgumentException("Didn't find parameter "
				+ d.getName() + " when registering");
		}
	}

	// get all of the required arguments
	// If evenOutput is true, then we even get the output parameters
	private Collection getRequiredArgs(boolean evenOutput) {
		Collection rv=new ArrayList(args.size());
		for(Iterator i=args.iterator(); i.hasNext();) {
			Parameter p=(Parameter)i.next();
			if(p.isRequired()) {
				// Deal with output parameters
				if(p.isOutput()) {
					if(evenOutput) {
						rv.add(p);
					}
				} else {
					rv.add(p);
				}
			}
		}
		return(rv);
	}

	// get all of the required arguments
	private Collection getOptionalArgs() {
		Collection rv=new ArrayList(args.size());
		for(Iterator i=args.iterator(); i.hasNext();) {
			Parameter p=(Parameter)i.next();
			if(!p.isRequired()) {
				rv.add(p);
			}
		}
		return(rv);
	}

	// Get the output parameters
	private Collection getOutputParameters() {
		Collection rv=new ArrayList(args.size());
		for(Iterator i=args.iterator(); i.hasNext();) {
			Parameter p=(Parameter)i.next();
			if(p.isOutput()) {
				rv.add(p);
			}
		}
		return(rv);
	}

	// Private class for results

	private static class Result extends Object {
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
					throw new IllegalArgumentException("Invalid JDBC type: "
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

		public String getShortType() {
			String rv=type;
			int i=type.lastIndexOf('.');
			if(i>0) {
				rv=type.substring(i+1);
			}
			return(rv);
		}

		public String getJavaType() {
			String rv=(String)javaTypes.get(type);
			if(rv==null) {
				throw new RuntimeException("Whoops!  " + type
					+ " must have been overlooked");
			}
			return(rv);
		}

		public String getJavaResultType() {
			String rv=(String)javaResultTypes.get(type);
			if(rv==null) {
				throw new RuntimeException("Whoops!  " + type
					+ " must have been overlooked");
			}
			return(rv);
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
		private Default defaultValue=null;

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

		/** 
		 * Get the hash code of the name of this parameter.
		 */
		public int hashCode() {
			return(name.hashCode());
		}

		/** 
		 * True if the given objet is an instance of Parameter with the
		 * same name.
		 */
		public boolean equals(Object o) {
			boolean rv=false;
			if(o instanceof Parameter) {
				Parameter p=(Parameter)o;
				rv=name.equals(p.name);
			}

			return(rv);
		}

		public String getName() {
			return(name);
		}

		public String getType() {
			return(type);
		}

		public String getShortType() {
			String rv=type;
			int i=type.lastIndexOf('.');
			if(i>0) {
				rv=type.substring(i+1);
			}
			return(rv);
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

		public Default getDefaultValue() {
			return(defaultValue);
		}

		public void setDefaultValue(Default d) {
			defaultValue=d;
		}
	}

	// Default values for parameters

	private class Default extends Object {
		private String name=null;
		// Class of this parameter
		private String type=null;
		private Object value=null;

		public Default(String line) {
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

			// Figure out the type of this parameter
			type=findType();

			// Get the rest of the line
			String rest=st.nextToken("\n");

			parse(rest);
		}

		public String getName() {
			return(name);
		}

		// Get the value to be used for printing in the source
		public String getPrintValue() {
			String rv=null;
			boolean needsCast=false;

			// Figure out if we need a cast
			if(value == null) {
				// If the value was null, cast it
				needsCast=true;
			} else if(value instanceof Integer) {
				// Nothing
			} else if(value instanceof Float) {
				// Nothing
			} else if(value instanceof String) {
				// Nothing
			} else if(value instanceof Double) {
				// Nothing
			} else if(value instanceof Boolean) {
				// Nothing
			} else {
				needsCast=true;
			}

			if(needsCast) {
				rv="(" + javaResultTypes.get(type) + ")" + value;
			} else {
				rv=value.toString();
			}
			return(rv);
		}

		private void parse(String input) {
			if(input==null) {
				throw new NullPointerException("Can't parse a null string");
			}
			// Get rid of whitespace from the ends
			input=input.trim();

			// make sure there's something left
			if(input.length() == 0) {
				throw new IllegalArgumentException(
					"Can't parse nothin'");
			}

			if(input.equals("NULL")) {
				value=null;
			} else {

				if(type.equals("java.sql.Types.INTEGER")) {
					value=new Integer(input);
				} else if(type.equals("java.sql.Types.SMALLINT")) {
					value=new Short(input);
				} else if(type.equals("java.sql.Types.TINYINT")) {
					value=new Short(input);
				} else if(type.equals("java.sql.Types.BIGINT")) {
					value=new BigDecimal(input);
				} else if(type.equals("java.sql.Types.Decimal")) {
					value=new BigDecimal(input);
				} else if(type.equals("java.sql.Types.BIT")) {
					value=SpyUtil.getBoolean(input);
				} else if(type.equals("java.sql.Types.DOUBLE")) {
					value=new Double(input);
				} else if(type.equals("java.sql.Types.FLOAT")) {
					value=new Float(input);
				} else if(type.equals("java.sql.Types.VARCHAR")) {
					value=input;
				} else {
					throw new IllegalArgumentException(
						"I don't know how to parse a default for " + type);
				}
			}
		}

		// Lookup the type for this spt.
		private String findType() {
			String rv=null;

			// Look for the parameter
			for(Iterator i=args.iterator(); rv==null && i.hasNext();) {
				Parameter p=(Parameter)i.next();
				if(p.getName().equals(name)) {
					rv=p.getType();
				}
			}

			// Make sure we got some
			if(rv==null) {
				throw new IllegalArgumentException(
					"No parameter for this default:  " + name);
			}
			return(rv);
		}

		// String me
		public String toString() {
			String rv="{Default " + name + " (" + type + ") " + value + "}";
			return(rv);
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
