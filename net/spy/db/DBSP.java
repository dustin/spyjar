// Copyright (c) 2001  SPY internetworking <dustin@spy.net>
//
// $Id: DBSP.java,v 1.12 2002/11/11 21:38:36 knitterb Exp $

package net.spy.db;

import java.lang.reflect.Constructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

import java.math.BigDecimal;

import java.util.Collections;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.spy.SpyConfig;

/**
 * Super class for all stored procedure calls.
 */
public abstract class DBSP extends SpyCacheDB {

	// The set of parameters available to this DBSP (defined in the subclass)
	private NamedObjectStorage parameters=null;

	// The set of arguments provided to this DBSP at runtime
	private NamedObjectStorage arguments=null;

	// SP name
	private String spname=null;

	// Caching info
	private long cachetime=0;

	// timeout
	private int timeout=0;

	private boolean debug=false;

	// The query
	private String query=null;

	// My prepared statement
	private PreparedStatement pst=null;

	/**
	 * Get a new DBSP object with a given config.
	 */
	public DBSP(SpyConfig conf) throws SQLException {
		super(conf);
		initsp();
	}

	/**
	 * Get a new DBSP object using the given Connection
	 */
	public DBSP(Connection conn) throws SQLException {
		super(conn);
		initsp();
	}

	// Initialize hashtables
	private void initsp() {
		this.parameters=new NamedObjectStorage();
		this.arguments=new NamedObjectStorage();
	}

	/**
	 * Execute the query.
	 */
	public ResultSet executeQuery() throws SQLException {
		prepare();

		ResultSet rs=null;

		if (debug) {
			System.out.println("Setting timeout to: "+timeout);
		}
		pst.setQueryTimeout(timeout);

		rs=pst.executeQuery();

		if (debug) {
			System.err.print("Returned: ");
			ResultSetMetaData rsmd=rs.getMetaData();
			String cols="";
			for (int x=1; x<=rsmd.getColumnCount(); x++) {
				if (x>1) {
					cols+=", ";
				}
				cols+=rsmd.getColumnName(x)+"="+rsmd.getColumnTypeName(x);
			}
			System.err.println(cols);
		}

		return(rs);
	}

	/**
	 * Execute a query for update only.
	 */
	public int executeUpdate() throws SQLException  {
		int rv=0;
		prepare();

		if (debug) {
			System.out.println("Setting timeout to: "+timeout);
		}
		pst.setQueryTimeout(timeout);

		rv=pst.executeUpdate();
		return(rv);
	}

	/**
	 * Get the next result set.
	 *
	 * @return null if there are no more result sets.
	 */
	public ResultSet nextResults() throws SQLException  {
		ResultSet rs=null;

		// Skip over the updates
		pst.getUpdateCount();

		if(pst.getMoreResults()) {
			rs=pst.getResultSet();
		}

		return(rs);
	}

	/**
	 * Get the warnings.
	 */
	public SQLWarning getWarnings() throws SQLException {
		return(pst.getWarnings());
	}

	/**
	 * Set the number of seconds the results of this SP will be valid.
	 * This option uses {@link SpyCacheDB#prepareStatement(String,long)}
	 * instead of the native driver's version.
	 *
	 * @param time time (in seconds) to keep the results around
	 */
	public void setCacheTime(long time) {
		this.cachetime=time;
	}

	/** 
	 * Get the cache time configured for this SP.
	 * 
	 * @return the time (in seconds) the results will be valid
	 */
	public long getCacheTime() {
		return(cachetime);
	}

	/** 
	 * Set the prepared statement on which this DBSP will operate.
	 * 
	 * @param pst the prepared statement
	 */
	protected void setPreparedStatement(PreparedStatement pst) {
		this.pst=pst;
	}

	/** 
	 * Get the prepared statement on which this DBSP will operate.
	 * 
	 * @param pst the prepared statement
	 */
	protected PreparedStatement getPreparedStatement() {
		return(pst);
	}

	/** 
	 * Define a parameter.
	 * 
	 * @param p the parameter
	 * @throws SQLException if the parameter has already been added
	 */
	protected void addParameter(Parameter p) throws SQLException {
		if(parameters.contains(p.getName())) {
			throw new SQLException(
				"parameter ``" + p + "'' already provided.");
		}

		// Save it.
		parameters.add(p);
	}

	/**
	 * Define a field to be required.
	 *
	 * @param name the name of the field
	 * @param type the type
	 * @throws SQLException if the type has already been added
	 * @see java.sql.Types
	 */
	protected void setRequired(String name, int type) throws SQLException {
		addParameter(new Parameter(Parameter.REQUIRED, type, name));
	}

	/**
	 * Define a field to be optional.
	 *
	 * @param name the name of the field
	 * @param type the type
	 * @throws SQLException if the type has already been added
	 * @see java.sql.Types
	 */
	protected void setOptional(String name, int type) throws SQLException {
		addParameter(new Parameter(Parameter.OPTIONAL, type, name));
	}

	/**
	 * Define a field to be output.
	 *
	 * @param name the name of the field
	 * @param type the type
	 * @throws SQLException if the type has already been added
	 * @see java.sql.Types
	 */
	protected void setOutput(String name, int type) throws SQLException {
		addParameter(new Parameter(Parameter.OUTPUT, type, name));
	}

	/**
	 * Set the named argument to the value contained in the given object
	 * for the given type.
	 *
	 * @param which which variable to set
	 * @param what what value to set it to
	 * @param type type of variable this is
	 */
	protected void setArg(String which, Object what, int type)
			throws SQLException {
		arguments.add(new Argument(type, which, what));
	}

	/** 
	 * Get the arguments in parameter order.
	 * 
	 * @return an unmodifiable list of {@link Argument} objects
	 */
	protected List getArguments() {
		ArrayList al=new ArrayList(arguments.size());
		for(Iterator i=getParameters().iterator(); i.hasNext();) {
			Parameter p=(Parameter)i.next();

			Argument arg=(Argument)arguments.get(p.getName());
			if(arg==null) {
				throw new NullPointerException("Missing argument for " + p);
			}
			al.add(arg);
		}
		return(al);
	}

	/** 
	 * Get a List of all of the parameters in the order in which they were
	 * defined.
	 * 
	 * @return a List of {@link Parameter} objects.
	 */
	protected List getParams() {
		return(parameters.getObjectList());
	}

	/** 
	 * Reset all arguments.
	 */
	protected void resetArgs() {
		arguments.clear();
	}

	/**
	 * Set the timeout for this query
	 */
	public void setQueryTimeout(int timeout) {
		this.timeout=timeout;
	}

	/**
	 * Get the timeout for this query
	 */
	public int getQueryTimeout() {
		return(this.timeout);
	}
		

	/**
	 * Set the stored procedure to the given value.
	 */
	protected void setSPName(String to) {
		this.spname=to;
	}

	/** 
	 * Get the SP name.
	 *
	 * @return the name of the SP to call, or null if not applicable
	 */
	protected String getSPName() {
		return(spname);
	}

	/**
	 * Verify that the arguments, as given, are acceptable.
	 */
	protected void checkArgs() throws SQLException {

		if(debug) {
			System.err.println("Checking");
			System.err.println("Parameters:  "+ parameters);
			System.err.println("Args:  "+ arguments);
		}

		// Now, verify all of the arguments we have are correctly typed.
		for(Iterator i=arguments.getObjectList().iterator(); i.hasNext(); ) {
			Argument arg=(Argument)i.next();

			// Find the matching parameter.
			Parameter p=(Parameter)parameters.get(arg.getName());

			// Verify the argument exists
			if(p==null) {
				throw new SQLException("Invalid argument " + arg);
			}

			int ptype=p.getJavaType();
			int atype=arg.getJavaType();
			if (atype==Types.NULL) {
				atype=((DBNull)arg.getValue()).getType();
			}
			// Check the type
			if(ptype!=atype) {
				throw new SQLException("Invalid type for arg " + arg
					+ " type was "
					+ atype + " ("
						+ TypeNames.getTypeName(atype) + ")"
					+ " should be "
					+ ptype
						+ " (" + TypeNames.getTypeName(ptype)
						+ ")"
				);
			}
		}

		// Next, verify all of the required arguments are there.
		for(Iterator i=parameters.getObjectList().iterator(); i.hasNext(); ) {
			Parameter p=(Parameter)i.next();

			if(p.getParamType() == Parameter.REQUIRED) {
				if(arguments.get(p.getName()) == null) {
					throw new SQLException("Required argument "
						+ p + " missing.");
				}
			}
		}

		// Check complete.  :)
	}

	/**
	 * Prepare the statement for execution.
	 */
	protected void prepare() throws SQLException {

		// Make sure all the arguments are there.
		checkArgs();

		// Get ready to build our query.
		StringBuffer querySb=new StringBuffer(256);
		querySb.append("exec ");
		querySb.append(spname);
		querySb.append(" ");

		int nargs=0;
		for(Iterator e=getArguments().iterator(); e.hasNext(); ) {
			Argument arg=(Argument)e.next();

			querySb.append("\t@");
			querySb.append(arg.getName());
			querySb.append("=?,\n");
			nargs++;
		}

		// Remove the last comma if we had params
		if(nargs>0) {
			querySb=new StringBuffer(querySb.toString().substring(0,
										querySb.length()-2));
		}
		String query=querySb.toString().trim();

		// Get a prepared statement, varies whether it's cachable or not.
		// XXX: This duplicates what goes on in applyArgs()
		if(getCacheTime()>0) {
			pst=prepareStatement(query, getCacheTime());
		} else {
			pst=prepareStatement(query);
		}

		if (debug) {
			System.out.println("Query: "+query);
		}

		// Fill in the arguments.
		setQuery(query);
		applyArgs(getArguments());
	}

	/**
	 * Set the value of debug for this instance.
	 *
	 * @param db the debug value to set
	 */
	protected void setDebug(boolean db) {
		this.debug=db;
	}

	/** 
	 * Find out if debug is enabled.
	 * 
	 * @return true if debug is enabled
	 */
	protected boolean isDebugEnabled() {
		return(debug);
	}

	/**
	 * Set the SQL query to call
	 */
	protected void setQuery(String query) {
		this.query=query;
	}

	/** 
	 * Get the query this DBSP will be calling.
	 */
	protected String getQuery() {
		return(query);
	}

	/**
	 * Fill in the arguments (with types) for the given list of parameters.
	 *
	 * @param query the query we'll be calling
	 * @param v the list of Parameters we need to add, in order
	 */
	protected void applyArgs(Collection v) throws SQLException {

		// Get the statement
		if(getCacheTime()>0) {
			pst=prepareStatement(query, getCacheTime());
		} else {
			pst=prepareStatement(query);
		}

		// Use this iterator for the now positional arguments
		int i=1;
		for(Iterator e=v.iterator(); e.hasNext(); ) {
			Argument arg=(Argument)e.next();
			Object o=arg.getValue();
			int type=arg.getJavaType();

			if(isDebugEnabled()) {
				System.err.println("arg[" + i + "] = " + arg);
			}

			try {
				switch(type) {
					case Types.BIT:
						pst.setBoolean(i, ((Boolean)o).booleanValue());
						break;
					case Types.DATE:
						pst.setDate(i, (java.sql.Date)o);
						break;
					case Types.DOUBLE:
						pst.setDouble(i, ((Double)o).doubleValue());
						break;
					case Types.FLOAT:
						pst.setFloat(i, ((Float)o).floatValue());
						break;
					case Types.INTEGER:
						pst.setInt(i, ((Integer)o).intValue());
						break;
					case Types.BIGINT:
						pst.setLong(i, ((Long)o).longValue());
						break;
					case Types.NUMERIC:
					case Types.DECIMAL:
						BigDecimal bd=((BigDecimal)o).setScale(4,
													BigDecimal.ROUND_HALF_UP);
						pst.setBigDecimal(i, bd);
						break;
					case Types.SMALLINT:
					case Types.TINYINT:
						pst.setShort(i, (short)((Integer)o).intValue());
						break;
					case Types.NULL:
						pst.setNull(i, ((DBNull)o).getType());
						break;
					case Types.OTHER:
						pst.setObject(i, o);
						break;
					case Types.VARCHAR:
						pst.setString(i, (String)o);
						break;
					case Types.TIME:
						pst.setTime(i, (Time)o);
						break;
					case Types.TIMESTAMP:
						pst.setTimestamp(i, (Timestamp)o);
						break;
					default:
						throw new SQLException("Whoops, type "
							+ TypeNames.getTypeName(type) + "(" + type + ")"
							+ " seems to have been overlooked.");
				}
			} catch(SQLException se) {
				throw se;
			} catch (Exception applyException) {
				applyException.printStackTrace();
				String msg="Problem setting " + arg
					+ " in prepared statement for type "
					+ TypeNames.getTypeName(type) + " "
					+ o.toString() + " : " + applyException;
				throw new SQLException (msg);
			}

			i++;

		}
	}

	/**
	 * Set field <i>which</i> to the value a1 of the type java.math.BigDecimal 
	 *
	 * @param which which field to set
	 * @param a1 the value to set
	 *
	 * @exception SQLException if there's an error setting this argument.
	 */
	public void set(String which,java.math.BigDecimal a1) 
		throws SQLException {
		setArg(which, a1, Types.DECIMAL);
	}

	/**
	 * Set field <i>which</i> to the value a1 of the type boolean 
	 *
	 * @param which which field to set
	 * @param a1 the value to set
	 *
	 * @exception SQLException if there's an error setting this argument.
	 */
	public void set(String which,boolean a1) 
		throws SQLException {
		setArg(which, new Boolean(a1), Types.BIT);
	}

	/** 
	 * Set field <i>which</i> to the value of b.
	 * 
	 * @param which the field to set
	 * @param b the value to set
	 * @throws SQLException if there's an error setting this argument
	 */
	public void set(String which, Boolean b) throws SQLException {
		setArg(which, b, Types.BIT);
	}

	/**
	 * Set field <i>which</i> to the value a1 of the type java.sql.Date 
	 *
	 * @param which which field to set
	 * @param a1 the value to set
	 *
	 * @exception SQLException if there's an error setting this argument.
	 */
	public void set(String which,java.sql.Date a1) 
		throws SQLException {
		setArg(which, a1, Types.DATE);
	}

	/**
	 * Set field <i>which</i> to the value a1 of the type double 
	 *
	 * @param which which field to set
	 * @param a1 the value to set
	 *
	 * @exception SQLException if there's an error setting this argument.
	 */
	public void set(String which,double a1) 
		throws SQLException {
		setArg(which, new Double(a1), Types.DOUBLE);
	}

	/**
	 * Set field <i>which</i> to the value a1 of the type float 
	 *
	 * @param which which field to set
	 * @param a1 the value to set
	 *
	 * @exception SQLException if there's an error setting this argument.
	 */
	public void set(String which,float a1) 
		throws SQLException {
		setArg(which, new Float(a1), Types.FLOAT);
	}

	/**
	 * Set field <i>which</i> to the value a1 of the type java.lang.Integer 
	 *
	 * @param which which field to set
	 * @param a1 the value to set
	 *
	 * @exception SQLException if there's an error setting this argument.
	 */
	public void set(String which,Integer a1) 
		throws SQLException {
		if (debug) {
			System.err.println("Adding Integer->INTEGER for "+which);
		}
		setArg(which, a1, Types.INTEGER);
	}

	/**
	 * Set field <i>which</i> to the value a1 of the type int 
	 *
	 * @param which which field to set
	 * @param a1 the value to set
	 *
	 * @exception SQLException if there's an error setting this argument.
	 */
	public void set(String which,int a1) 
		throws SQLException {
		setArg(which, new Integer(a1), Types.INTEGER);
	}

	/**
	 * Set field <i>which</i> to the value a1 of the type long 
	 *
	 * @param which which field to set
	 * @param a1 the value to set
	 *
	 * @exception SQLException if there's an error setting this argument.
	 */
	public void set(String which,long a1) 
		throws SQLException {
		setArg(which, new Long(a1), Types.BIGINT);
	}

	/**
	 * Set field <i>which</i> to a null of the given type.
	 * Do not call this directly, instead, if you have an empty String,
	 * for example, consider:
	 * <pre><code>
	 *  set("mycol", (String)null);
	 * </code></pre>
	 *
	 * @param which which field to set
	 * @param a1 the value to set
	 *
	 * @exception SQLException if there's an error setting this argument.
	 */
	protected void setNull(String which,int a1) 
		throws SQLException {
		// This one works a bit different because we have to store the
		// original type
		setArg(which, new Integer(a1), Types.NULL);
	}

	/**
	 * Set field <i>which</i> to the value a1 of the type java.lang.Object 
	 *
	 * @param which which field to set
	 * @param a1 the value to set
	 *
	 * @exception SQLException if there's an error setting this argument.
	 */
	/*
	public void set(String which,java.lang.Object a1) 
		throws SQLException {
		System.err.println("Setting OTHER for "+which);
		setArg(which, a1, Types.OTHER);
	}
	*/

	/**
	 * Set field <i>which</i> to the value a1 of the type short 
	 *
	 * @param which which field to set
	 * @param a1 the value to set
	 *
	 * @exception SQLException if there's an error setting this argument.
	 */
	public void setSmallInt(String which,short a1) 
		throws SQLException {
		setArg(which, new Integer(a1), Types.SMALLINT);
	}

	/**
	 * Set field <i>which</i> to the value a1 of the type short 
	 *
	 * @param which which field to set
	 * @param a1 the value to set
	 *
	 * @exception SQLException if there's an error setting this argument.
	 */
	public void set(String which,short a1) 
		throws SQLException {
		setArg(which, new Integer(a1), Types.TINYINT);
	}

	/** 
	 * Set the field <i>which</i> to the value s of the type short.
	 * 
	 * @param which the field to set (by name)
	 * @param s the new value
	 * @throws SQLException if there's a problem setting this value
	 */
	public void set(String which, Short s) throws SQLException {
		setArg(which, new Integer(s.intValue()), Types.TINYINT);
	}

	/**
	 * Set field <i>which</i> to the value a1 of the type java.lang.String 
	 *
	 * @param which which field to set
	 * @param a1 the value to set
	 *
	 * @exception SQLException if there's an error setting this argument.
	 */
	public void set(String which,java.lang.String a1) 
		throws SQLException {
		if (debug) {
			System.err.println("Adding String->VARCHAR for "+which);
		}
		setArg(which, a1, Types.VARCHAR);
	}

	/**
	 * Set field <i>which</i> to the value a1 of the type Time 
	 *
	 * @param which which field to set
	 * @param a1 the value to set
	 *
	 * @exception SQLException if there's an error setting this argument.
	 */
	public void set(String which,Time a1) 
		throws SQLException {
		setArg(which, a1, Types.TIME);
	}

	/**
	 * Set field <i>which</i> to the value a1 of the type Timestamp 
	 *
	 * @param which which field to set
	 * @param a1 the value to set
	 *
	 * @exception SQLException if there's an error setting this argument.
	 */
	public void set(String which,Timestamp a1) 
		throws SQLException {
		setArg(which, a1, Types.TIMESTAMP);
	}

	/** 
	 * Get a List containing all parameters.
	 * 
	 * @return an umodifiable List of {@link Parameter} objects.
	 */
	public List getParameters() {
		return(parameters.getObjectList());
	}

	/** 
	 * Get a List of parameters of a specific type.
	 * 
	 * @param type the Parameter type.
	 * @return a list of {@link Parameter}s of the specified type.
	 */
	public List getParameters(int type) {
		ArrayList al=new ArrayList();

		for(Iterator i=getParameters().iterator(); i.hasNext(); ) {
			Parameter p=(Parameter)i.next();
			if(p.getJavaType() == type) {
				al.add(p);
			}
		}

		return(al);
	}

	/** 
	 * Get the required arguments.
	 *
	 * @deprecated use getParameters() or getParameters(int) instead
	 * 
	 * @return a List of {@link Parameter}s representing the required
	 * 	arguments in the order in which they will be passed into the query
	 */
	public List getRequiredArgs() {
		return(getParameters(Parameter.REQUIRED));
	}

	/** 
	 * Get the optional arguments.
	 *
	 * @deprecated use getParameters() or getParameters(int) instead
	 * 
	 * @return a List of {@link Parameter}s representing the optional
	 * 	arguments in the order in which they will be passed into the query
	 */
	public List getOptionalArgs() {
		return(getParameters(Parameter.OPTIONAL));
	}

	/** 
	 * Get the output arguments.
	 *
	 * @deprecated use getParameters() or getParameters(int) instead
	 * 
	 * @return a List of {@link Parameter}s representing the output
	 * 	arguments in the order in which they will be passed into the query
	 */
	public List getOutputArgs() {
		return(getParameters(Parameter.OUTPUT));
	}

	/**
	 * Get the {@link java.sql.Types} type of the given parameter.
	 *
	 * @return the type, or -1 if there's no such variable
	 */
	public int getType(String var) {
		int rv=-1;

		Parameter p=(Parameter)parameters.get(var);
		if(p!=null) {
			rv=p.getJavaType();
		}

		return(rv);
	}

	/** 
	 * Get the {@link Parameter} type.
	 * 
	 * @param var the name of the parameter
	 * @return the {@link Parameter} type, or -1 if there's no such
	 * 			parameter
	 */
	public int getParameterType(String var) {
		int rv=-1;

		Parameter p=(Parameter)parameters.get(var);
		if(p!=null) {
			rv=p.getParamType();
		}

		return(rv);
	}

	/** 
	 * Set a field in the DBSP after coercing the String value to the
	 * required value for the given field.  This should <i>rarely</i> be
	 * used, but is useful for accessing DBSPs through web forms.
	 * 
	 * @param var the field to set
	 * @param value the String representation of the value to set
	 * @throws SQLException if there's a problem with coersion
	 */
	public void setCoerced(String var, String value) throws SQLException {

		int type=getType(var);
		if(value == null) {
			// if the value is null, send in a null
			DBNull n=new DBNull(type);
		} else {
			// Value is not null, parse it and call the proper set method
			switch(type) {
				case Types.BIT:
					set(var, Boolean.valueOf(value).booleanValue());
					break;
				case Types.DOUBLE:
					set(var, new Double(value).doubleValue());
					break;
				case Types.FLOAT:
					set(var, new Float(value).floatValue());
					break;
				case Types.INTEGER:
					set(var, Integer.parseInt(value));
					break;
				case Types.BIGINT:
					set(var, Long.parseLong(value));
					break;
				case Types.NUMERIC:
				case Types.DECIMAL:
					set(var, new BigDecimal(value));
					break;
				case Types.TINYINT:
					set(var, (short)Integer.parseInt(value));
					break;
				case Types.OTHER:
					set(var, value);
					break;
				case Types.VARCHAR:
					set(var, value);
					break;
				case Types.DATE:
				case Types.TIME:
				case Types.TIMESTAMP:
					throw new SQLException("Date types not currently handled");
				default:
					throw new SQLException(
						"No known type for " + var + ", you sure it's valid?.");
			}
		}
	}

	/**
	 * Commandline test for SPs that return result sets.  Invoked thusly:
	 *
	 * <p/>
	 *
	 * java net.spy.db.DBSP net.spy.db.sp.SPClassName configpath
	 * key value [...]
	 */
	public static void main(String args[]) throws Exception {
		// Get a Config
		SpyConfig dbconfig=new SpyConfig(new java.io.File(args[1]));

		// Now, we have sit back and reflect on a way to instantiate this
		// thing.
		// Figure out what the argument types are
		Class argtypes[]={dbconfig.getClass()};
		// Make the actual argument list
		Object dargs[]={dbconfig};
		Class c=Class.forName(args[0]);
		Constructor cons=c.getConstructor(argtypes);
		DBSP dbsp=(DBSP)cons.newInstance(dargs);

		// Set the args
		for(int i=2; i<args.length; i+=2) {
			dbsp.setCoerced(args[i], args[i+1]);
		}

		ResultSet rs=dbsp.executeQuery();
		System.out.println("ResultSet type is " + rs.getClass().getName());

		int rsi=1;
		while(rs!=null) {
			int rowi=1;
			System.out.println("Result set " + rsi + ":");
			ResultSetMetaData rsmd=rs.getMetaData();
			int ncolumns=rsmd.getColumnCount();
			while(rs.next()) {
				System.out.println("\tRow " + rowi + ":");

				for(int i=1; i<=ncolumns; i++) {
					int type=rsmd.getColumnType(i);
					String extra=TypeNames.getTypeName(type);
					String data=rs.getString(i);
					if(type==Types.VARCHAR) {
						extra+="," + data.length();
					}
					System.out.println("\t\t" + rsmd.getColumnName(i)
						+ "("
						+ extra
						+ ")=" + data);
				}
				rowi++;
			}
			rs=dbsp.nextResults();
			rsi++;
		}

		// Print out the warnings.
		SQLWarning warn=dbsp.getWarnings();
		while(warn!=null) {
			System.out.println("Warning:  " + warn);
			warn=warn.getNextWarning();
		}
	}

	// Generic container for named objects.  Ideally, I'd like to use a
	// LinkedHashMap, but I don't have them before java 1.4, so here we go
	private class NamedObjectStorage extends Object {

		private Map byName=null;
		private List byPosition=null;

		/** 
		 * Get a NamedObjectStorage instance.
		 */
		public NamedObjectStorage() {
			super();
			byName=new HashMap();
			byPosition=new LinkedList();
		}

		/** 
		 * String me.
		 */
		public String toString() {
			return(byPosition.toString());
		}

		/** 
		 * Get the number of objects stored in this object.
		 * 
		 * @return number of objects
		 */
		public int size() {
			return(byPosition.size());
		}

		/** 
		 * Add the object to the list.
		 * 
		 * @param no the NamedObject to add
		 */
		public void add(NamedObject no) {
			// First, get rid of an existing one if there is one.
			if(byName.containsKey(no.getName())) {
				byName.remove(no.getName());

				for(Iterator i=byPosition.iterator(); i.hasNext(); ) {
					NamedObject sno=(NamedObject)i.next();
					if(no.getName().equals(sno.getName())) {
						i.remove();
					}
				}
			}
			// Now add the new one
			byName.put(no.getName(), no);
			byPosition.add(no);
		}

		/** 
		 * Get the named object.
		 * 
		 * @param name the name of the object to get
		 * @return the NamedObject with that name, or null if there isn't one
		 */
		public NamedObject get(String name) {
			return((NamedObject)byName.get(name));
		}

		/** 
		 * Determine whether the storage contains an object with the given
		 * name.
		 * 
		 * @param name the name of the object we're looking for
		 * @return true if the object is here
		 */
		public boolean contains(String name) {
			return(byName.containsKey(name));
		}

		/** 
		 * Get a Map of object names to objects.
		 * 
		 * @return an unmodifable map mapping object names to objects
		 */
		public Map getObjectMap() {
			return(Collections.unmodifiableMap(byName));
		}

		/** 
		 * Get a List of objects in insert order.
		 * 
		 * @return an unmodifiable list of objects in insert order
		 */
		public List getObjectList() {
			return(Collections.unmodifiableList(byPosition));
		}

		/** 
		 * Get rid of all objects.
		 */
		public void clear() {
			byName.clear();
			byPosition.clear();
		}

	}

	/** 
	 * Objects with names.
	 */
	public abstract class NamedObject extends Object {

		private String name=null;

		/** 
		 * Get an instance of a named object with the given name.
		 * 
		 * @param name 
		 */
		protected NamedObject(String name) {
			super();

			if(name==null) {
				throw new NullPointerException("name not given");
			}

			this.name=name;
		}

		/** 
		 * Get the name of this object.
		 */
		public String getName() {
			return(name);
		}

		/** 
		 * String me.
		 */
		public String toString() {
			StringBuffer sb=new StringBuffer(32);

			sb.append("{");
			sb.append(getClass().getName());
			sb.append(" ");
			sb.append(getName());
			sb.append("}");

			return(sb.toString());
		}

		/** 
		 * Get the hash code of the name of this object.
		 * 
		 * @return getName().hashCode()
		 */
		public int hashCode() {
			return(name.hashCode());
		}

		/** 
		 * Test for equality.
		 * 
		 * @param o object to test against
		 * @return true if <code>o</code> is a NamedObject object with the
		 * 			same name
		 */
		public boolean equals(Object o) {
			boolean rv=false;

			if(o instanceof NamedObject) {
				NamedObject no=(NamedObject)o;

				rv=name.equals(no.name);
			}

			return(rv);
		}


	}

	/** 
	 * Parameters for DBSPs.
	 */
	public class Parameter extends NamedObject {

		/** 
		 * Parameter type indicating this is a required parameter.
		 */
		public static final int REQUIRED=1;
		/** 
		 * Parameter type indicating this is an optional parameter.
		 */
		public static final int OPTIONAL=2;
		/** 
		 * Parameter type indicating this is an output parameter.
		 */
		public static final int OUTPUT=2;

		private int paramType=0;
		private int javaType=0;

		/** 
		 * Construct a new Parameter.
		 * 
		 * @param paramType the parameter type (REQUIRED, etc...)
		 * @param javaType the {@link java.sql.Types} type
		 * @param name name of the parametr
		 */
		public Parameter(int paramType, int javaType, String name) {

			super(name);

			this.paramType=paramType;
			this.javaType=javaType;

			validateParamType();
		}
		private void validateParamType() {
			if( (paramType != REQUIRED)
				&& (paramType != OPTIONAL)
				&& (paramType != OUTPUT)) {

				throw new IllegalArgumentException(paramType
					+ " is not a valid " + getClass().getName()
					+ " type.");
			}
		} // validateParamType

		/** 
		 * Get the type of this parameter.
		 */
		public int getParamType() {
			return(paramType);
		}

		/** 
		 * Get the {@link java.sql.Types} type of this parameter.
		 */
		public int getJavaType() {
			return(javaType);
		}

		public String toString() {
			StringBuffer rc=new StringBuffer(100);
			rc.append("{ Param: type=");
			rc.append(TypeNames.getTypeName(javaType));
			rc.append("(");
			rc.append(javaType);
			rc.append(")");
			rc.append(", name=");
			rc.append(getName().toString());
			rc.append(", required=");
			rc.append(paramType);
			rc.append("}");
			return(rc.toString());
		}
	}
	
	/** 
	 * Arguments for DBSPs.
	 */
	public class Argument extends NamedObject {

		private int javaType=0;
		private Object value=null;

		/** 
		 * Construct a new Argument.
		 * 
		 * @param javaType the {@link java.sql.Types} type
		 * @param name name of the parametr
		 */
		public Argument(int javaType, String name, Object value) {

			super(name);

			this.javaType=javaType;
			this.value=value;
		}

		/** 
		 * Get the {@link java.sql.Types} type of this parameter.
		 */
		public int getJavaType() {
			int rv=0;
			if(value==null) {
				rv=Types.NULL;
			} else {
				rv=javaType;
			}
			return(rv);
		}

		/** 
		 * Get the value of this argument.
		 * @return the object passed in at construct time unless it was
		 * 			null, in which case it will be an appropriate
		 *			{@link DBNull} object 
		 */
		public Object getValue() {
			Object rv=value;
			if(rv==null) {
				rv=new DBNull(javaType);
			}
			return(rv);
		}

		public String toString() {
			StringBuffer rc=new StringBuffer(100);
			rc.append("{ Arg: type=");
			rc.append(javaType);
			rc.append("(");
			rc.append(TypeNames.getTypeName(javaType));
			rc.append(")");
			rc.append(", name=");
			rc.append(getName().toString());
			rc.append(", value=");
			if (value==null) {
				rc.append("NULL");
			} else {
				rc.append("'");
				rc.append(value.toString());
				rc.append("'");
			}
			rc.append("}");
			return(rc.toString());
		}

	}
}
