// Copyright (c) 2001  SPY internetworking <dustin@spy.net>
//
// arch-tag: 66057B6F-1110-11D9-8133-000A957659CC

package net.spy.db;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;

import net.spy.util.SpyConfig;

/**
 * Superclass for dynamic SQL calls.
 */
public abstract class DBCP extends DBSP {

	private int argumentIndex=1;

	/**
	 * Get a DBCP object with the given DBConfig.
	 */
	public DBCP(SpyConfig conf) throws SQLException {
		super(conf);
	}

	/**
	 * Get a DBCP object with the given Connection.
	 */
	public DBCP(Connection conn) throws SQLException {
		super(conn);
	}

	/**
	 * Execute a query for update only.
	 */
	public boolean execute() throws SQLException  {
		boolean rv=true;
		prepare();
		rv=getPreparedStatement().execute();
		return(rv);
	}

	/** 
	 * Optional parameters are not currently supported for DBCP objects.
	 * This method will always throw a SQLException when called.
	 * 
	 * @param name name of the parameter
	 * @param type type of the parameter
	 * @throws SQLException Optional Parameters are not supported
	 */
	protected void setOptional(String name, int type) throws SQLException {
		throw new SQLException("Optional Parameters Not Supported on DBCP");
	}

	/** 
	 * Get the CallableStatement for fetching output args.
	 * TODO get the get* methods implemented
	 * 
	 * @return The CallableStatement for getting the RC's from.
	 */
	public CallableStatement getCallableStatement() {
		CallableStatement rc=(CallableStatement)getPreparedStatement();
		return(rc);
	}

	/**
	 * Fill in the arguments (with types) for the given list of parameters.
	 *
	 * @param v the list of Argument objects we need to add, in order
	 */
	protected void applyArgs(Collection<Argument> v) throws SQLException {

		PreparedStatement pst=getPreparedStatement();
		// Get the statement
		if (pst==null) {
			if(getCacheTime()>0) {
				throw new Error ("Not Implemented!!");
				//pst=prepareStatement(query, getCacheTime());
			} else {
				pst=prepareCall(getQuery());
			}
		}

		// Use this iterator for the now positional arguments
		for(Argument arg : getArguments()) {
			int i=argumentIndex;
			Object o=arg.getValue();

			// Get it as an int so we can switch it
			int type=arg.getJavaType();

			try {
				if(getParameterType(arg.getName()) == Parameter.OUTPUT) {
					if (getLogger().isDebugEnabled()) {
						getLogger().debug("OUT -> Setting column "
							+arg+"("+i+") type "+type);
					}
					CallableStatement cst=(CallableStatement)pst;
					cst.registerOutParameter(i, type);
				} else {
					if (getLogger().isDebugEnabled()) {
						getLogger().debug("IN -> Setting column "
							+arg+"("+i+") type "+type);
					}
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
				} // end output_arg if statement
			} catch(SQLException se) {
				throw se;
			} catch (Exception applyException) {
				getLogger().warn("Exception while applying", applyException);
				String msg="Problem setting " + arg
					+ " in prepared statement for type "
					+ TypeNames.getTypeName(type) + " "
					+ o.toString() + " : " + applyException;
				throw new SQLException (msg);
			}

			argumentIndex++;

		}
	}

	/**
	 * Prepare the statement for execution.
	 */
	protected void prepare() throws SQLException {

		// Make sure all the arguments are there.
		checkArgs();

		// Get ready to build our query.
		StringBuffer querySb=new StringBuffer(256);
		querySb.append("{call ");
		querySb.append(getSPName());
		querySb.append(" (");

		int nargs=getArguments().size();
		for(int i=0; i<nargs; i++) {
			querySb.append("?,");
		}

		// Remove the last comma if we had params
		if(nargs>0) {
			querySb=new StringBuffer(querySb.toString().substring(0,
										querySb.length()-1));
		}

		// finish out
		querySb.append(")}");

		String query=querySb.toString().trim();

		if (getLogger().isDebugEnabled()) {
			getLogger().debug("Query: "+query);
		}

		PreparedStatement pst=getPreparedStatement();

		// Get a prepared statement, varies whether it's cachable or not.
		if (pst==null) {
			if(getCacheTime()>0) {
				throw new Error("Not Implemented");
				//pst=prepareCall(query, getCacheTime());
			} else {
				pst=prepareCall(query);
			}
			setPreparedStatement(pst);
		}

		// Fill in the arguments.
		setQuery(query);
		applyArgs(getArguments());
	}

}
