// Copyright (c) 2001  SPY internetworking <dustin@spy.net>
//
// arch-tag: 6765FB00-1110-11D9-BFE9-000A957659CC

package net.spy.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;

import net.spy.util.SpyConfig;
import net.spy.util.SpyUtil;

/**
 * Super class for all stored procedure calls.
 */
public abstract class DBSP extends SpyCacheDB implements DBSPLike {

	// Size for buffer for to stringing
	private static final int TOSTRING_SB_SIZE=128;

	// BigDecimal scale for doing data conversions
	private static final int BD_SCALE=4;

	// The set of parameters available to this DBSP (defined in the subclass)
	private LinkedHashMap<String, Parameter>  parameters=null;

	// The set of arguments provided to this DBSP at runtime
	private LinkedHashMap<String, Argument> arguments=null;

	// SP name
	private String spname=null;

	// Caching info
	private long cachetime=0;

	// timeout
	private int timeout=0;
	// Max rows
	private int maxRows=0;

	private boolean debug=false;

	// The query
	private String query=null;

	// My prepared statement
	private PreparedStatement pst=null;

	// cursor name if cursors are enabled
	private String cursorName=null;

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
		this.parameters=new LinkedHashMap<String, Parameter>();
		this.arguments=new LinkedHashMap<String, Argument>();
		// Inherit debug flag from the logger.
		debug=getLogger().isDebugEnabled();
	}

	/**
	 * Execute the query.
	 */
	public ResultSet executeQuery() throws SQLException {
		prepare();

		ResultSet rs=null;

		rs=pst.executeQuery();

		if (debug && !(this instanceof DBCP)) {
			getLogger().debug("Returned: ");
			ResultSetMetaData rsmd=rs.getMetaData();
			String cols="";
			for (int x=1; x<=rsmd.getColumnCount(); x++) {
				if (x>1) {
					cols+=", ";
				}
				cols+=rsmd.getColumnName(x)+"="+rsmd.getColumnTypeName(x);
			}
			getLogger().debug(cols);
		}

		return(rs);
	}

	/**
	 * Execute a query for update only.
	 */
	public int executeUpdate() throws SQLException  {
		int rv=0;
		prepare();

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
	 * Set the cursor name to be handed to the underlying Statement.
	 *
	 * @param name cursor name (or null to disable cursor handling)
	 */
	public void setCursorName(String name) throws SQLException {
		cursorName=name;
		if(pst != null) {
			getLogger().debug("Setting the pst cursor name to %s", cursorName);
			pst.setCursorName(cursorName);
		}
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
	 * @param to the prepared statement
	 */
	protected void setPreparedStatement(PreparedStatement to)
		throws SQLException {

		if(this.pst != null) {
			getLogger().warn("Discarding old prepared statement %s", this.pst);
		}
		this.pst=to;

		if(debug) {
			getLogger().debug("Setting timeout to: %s", timeout);
		}
		pst.setQueryTimeout(timeout);
		pst.setMaxRows(maxRows);

		// Set the cursor name if there is one.
		if(cursorName != null) {
			getLogger().debug("Setting the pst cursor name to %s", cursorName);
		}
	}

	/**
	 * Get the prepared statement on which this DBSP will operate.
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
		if(parameters.containsKey(p.getName())) {
			throw new SQLException(
				"parameter ``" + p + "'' already provided.");
		}

		// Save it.
		parameters.put(p.getName(), p);
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

		// I'm not sure how best to do this, but this has to be something
		// other than a null.  If it's null, than the Argument will make
		// the javaType into a Types.NULL instead of what I'm trying to set
		// it to.  Then I just get pissed and life sucks.  I guess we could
		// make the Argument have a "getSetJavaType()" method?  Anyhow, for
		// now I'm just going to make an object.
		setArg(name, "", type);
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

		arguments.put(which, new Argument(type, which, what));
	}

	/**
	 * Get the arguments in parameter order.
	 *
	 * @return an unmodifiable list of {@link Argument} objects
	 */
	public Collection<Argument> getArguments() {
		ArrayList<Argument> al=new ArrayList<Argument>(arguments.size());
		for(Parameter p : getParameters()) {
			Argument arg=arguments.get(p.getName());
			if(arg==null && p.getParamType()==Parameter.REQUIRED) {
				throw new NullPointerException("Missing argument for " + p);
			}

			if(arg!=null) {
				al.add(arg);
			}
		}
		return(Collections.unmodifiableList(al));
	}

	/**
	 * Get a Collection of all of the parameters in the order in which they were
	 * defined.
	 *
	 * @return a Collection of {@link Parameter} objects.
	 */
	protected Collection<Parameter> getParams() {
		return(parameters.values());
	}

	/**
	 * Reset all arguments.
	 */
	protected void resetArgs() {
		arguments.clear();
	}

	/**
	 * Set the timeout for this query.  See JDCB Statement documentation.
	 */
	public void setQueryTimeout(int to) {
		this.timeout=to;
	}

	/**
	 * Set the maximum number of rows that should be returned from this
	 * query.  See JDBC Statement documentation.
	 */
	public void setMaxRows(int to) {
		this.maxRows=to;
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
			getLogger().debug("Checking");
			getLogger().debug("Parameters:  %s", parameters);
			getLogger().debug("Args:  %s", arguments);
		}

		// Now, verify all of the arguments we have are correctly typed.
		for(Argument arg : arguments.values()) {
			// Find the matching parameter.
			Parameter p=parameters.get(arg.getName());

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
		for(Parameter p : parameters.values()) {
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

		if(pst == null) {

			// Make sure all the arguments are there.
			checkArgs();

			// Get ready to build our query.
			StringBuilder querySb=new StringBuilder(TOSTRING_SB_SIZE);
			querySb.append("exec ");
			querySb.append(spname);
			querySb.append(" ");

			int nargs=0;
			for(Argument arg : getArguments()) {
				querySb.append("\t@");
				querySb.append(arg.getName());
				querySb.append("=?,\n");
				nargs++;
			}

			// Remove the last comma if we had params
			if(nargs>0) {
				querySb.delete(querySb.length()-2, querySb.length());
			}
			String tmpQuery=querySb.toString().trim();

			if (debug) {
				getLogger().debug("Query: %s", tmpQuery);
			}

			// Fill in the arguments.
			setQuery(tmpQuery);
		}
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
	protected void setQuery(String to) throws SQLException {
		this.query=to;

		// Get a prepared statement, varies whether it's cachable or not.
		PreparedStatement tmpPst=null;
		if(getCacheTime()>0) {
			tmpPst=prepareStatement(to, getCacheTime());
		} else {
			tmpPst=prepareStatement(to);
		}
		setPreparedStatement(tmpPst);
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
	 * @param v the list of Parameters we need to add, in order
	 */
	protected void applyArgs(Collection<Argument> v) throws SQLException {

		// Use this iterator for the now positional arguments
		int i=1;
		for(Argument arg : v) {
			Object o=arg.getValue();
			int type=arg.getJavaType();

			if(isDebugEnabled()) {
				getLogger().debug("arg[%d] = %s", i, arg);
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
						pst.setDouble(i, ((Number)o).doubleValue());
						break;
					case Types.FLOAT:
						pst.setFloat(i, ((Number)o).floatValue());
						break;
					case Types.INTEGER:
						pst.setInt(i, ((Number)o).intValue());
						break;
					case Types.BIGINT:
						pst.setLong(i, ((Number)o).longValue());
						break;
					case Types.NUMERIC:
					case Types.DECIMAL:
						BigDecimal bd=((BigDecimal)o).setScale(BD_SCALE,
													BigDecimal.ROUND_HALF_UP);
						pst.setBigDecimal(i, bd);
						break;
					case Types.SMALLINT:
					case Types.TINYINT:
						pst.setShort(i, (short)((Number)o).intValue());
						break;
					case Types.NULL:
						pst.setNull(i, ((DBNull)o).getType());
						break;
					case Types.OTHER:
						pst.setObject(i, o);
						break;
					case Types.CLOB:
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
				getLogger().warn("SQLException while applying %s"
					+ " in prepared statement for type %s %s", arg,
					TypeNames.getTypeName(type), o, se);
				throw se;
			} catch (Exception applyException) {
				String msg="Problem setting " + arg
					+ " in prepared statement for type "
					+ TypeNames.getTypeName(type) + " "
					+ String.valueOf(o) + " : " + applyException;
				getLogger().warn(msg, applyException);
				SQLException se=new SQLException(msg);
				se.initCause(applyException);
				throw se;
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
		setArg(which, SpyUtil.getBoolean(a1), Types.BIT);

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
	 * Set field <i>which</i> to the value a1 of the type float
	 *
	 * @param which which field to set
	 * @param a1 the value to set
	 *
	 * @exception SQLException if there's an error setting this argument.
	 */
	public void set(String which, Float a1)
		throws SQLException {
		setArg(which, a1, Types.FLOAT);
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
			getLogger().debug("Adding Integer->INTEGER for "+which);
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
		getLogger().debug("Setting OTHER for "+which);
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
	public void setTinyInt(String which,short a1)
		throws SQLException {
		setArg(which, new Integer(a1), Types.SMALLINT);
	}
	
	/**
	 * Set a parameter to the tinyint value b of the type byte.
	 */
	public void set(String which, byte b) throws SQLException {
		setArg(which, new Integer(b), Types.TINYINT);
	}
	
	/**
	 * Set a parameter to a tinyint value.
	 */
	public void set(String which, Byte b) throws SQLException {
		setArg(which, new Integer(b.intValue()), Types.TINYINT);
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
		setArg(which, new Integer(a1), Types.SMALLINT);
	}

	/**
	 * Set the field <i>which</i> to the value s of the type short.
	 *
	 * @param which the field to set (by name)
	 * @param s the new value
	 * @throws SQLException if there's a problem setting this value
	 */
	public void set(String which, Short s) throws SQLException {
		setArg(which, new Integer(s.intValue()), Types.SMALLINT);
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
			getLogger().debug("Adding String->VARCHAR for "+which);
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

	public void set(String which,Object obj)
		throws SQLException {
		setArg(which, obj, Types.OTHER);
	}

	/**
	 * Get a Collection containing all parameters.
	 *
	 * @return an umodifiable Collection of {@link Parameter} objects.
	 */
	public Collection<Parameter> getParameters() {
		return(parameters.values());
	}

	/**
	 * Get a Collection of parameters of a specific type.
	 *
	 * @param type the Parameter type.
	 * @return a list of {@link Parameter}s of the specified type.
	 */
	public Collection<Parameter> getParameters(int type) {
		ArrayList<Parameter> al=new ArrayList<Parameter>();

		for(Parameter p : getParameters()) {
			if(p.getJavaType() == type) {
				al.add(p);
			}
		}

		return(al);
	}

	/**
	 * Get the {@link java.sql.Types} type of the given parameter.
	 *
	 * @return the type, or -1 if there's no such variable
	 */
	public int getType(String var) {
		int rv=-1;

		Parameter p=parameters.get(var);
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

		Parameter p=parameters.get(var);
		if(p!=null) {
			rv=p.getParamType();
		}

		return(rv);
	}

	/**
	 * Close this statement and whatever the superclass wants to do.
	 */
	public void close() {
		if(pst!=null) {
			try {
				pst.close();
			} catch(SQLException e) {
				getLogger().warn("Problem closing prepared statement", e);
			}
			pst=null;
		}
		super.close();
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
			set(var, n);
		} else {
			// Value is not null, parse it and call the proper set method
			switch(type) {
				case Types.BIT:
					set(var, SpyUtil.getBoolean(value).booleanValue());
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
				case Types.SMALLINT:
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
	 * Objects with names.
	 */
	public abstract class NamedObject extends Object {

		private String name=null;

		/**
		 * Get an instance of a named object with the given name.
		 *
		 * @param nm the name
		 */
		protected NamedObject(String nm) {
			super();

			if(nm==null) {
				throw new NullPointerException("name not given");
			}

			this.name=nm;
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
			return("{" + getClass().getName() + " " + getName() + "}");
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
		 * @param pType the parameter type (REQUIRED, etc...)
		 * @param jType the {@link java.sql.Types} type
		 * @param name name of the parametr
		 */
		public Parameter(int pType, int jType, String name) {

			super(name);

			this.paramType=pType;
			this.javaType=jType;

			validateParamType();
		}
		private void validateParamType() {
			if((paramType != REQUIRED)
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
			return "{Param: type="
				+ TypeNames.getTypeName(javaType)
				+ "(" + javaType + "), name="
				+ getName() + ", required=" + paramType + "}";
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
		 * @param jType the {@link java.sql.Types} type
		 * @param name name of the parametr
		 */
		public Argument(int jType, String name, Object v) {

			super(name);

			this.javaType=jType;
			this.value=v;
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
			String valName=null;
			if (value==null) {
				valName="NULL";
			} else {
				valName="'" + value + "'";
			}
			return "{Arg: type=" + javaType
				+ "(" + TypeNames.getTypeName(javaType)
				+ "), name=" + getName() + ", value=" + valName + "}";
		}

	}
}
