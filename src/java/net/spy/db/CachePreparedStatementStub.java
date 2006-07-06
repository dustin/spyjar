/*
 * Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
 *
 * arch-tag: 64B2B521-1110-11D9-BFD3-000A957659CC
 */

package net.spy.db;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;

import net.spy.cache.SpyCache;

/**
 * Prepared statement for executing cached queries
 */
public class CachePreparedStatementStub extends GenericPreparedStatementStub {

	// Stored DB handle
	SpyDB db=null;

	// How long the results of this statement should be cached
	private long cacheTime=60*60*1000;

	// Query timeout
	private int timeout=0;
	// Max rows
	private int maxRows=0;

	/**
	 * Create a CachePreparedStatement object for the given query (you
	 * probably don't want to do this directly).
	 */
	public CachePreparedStatementStub(SpyDB d, String query, long cTime) {
		super(query);
		this.db=d;
		this.cacheTime=cTime;
	}


	/**
	 * Get an integer hash code to uniquely identify this object.
	 */
	public int hashCode() {
		int hc=0;

		hc+=getQuery().hashCode();
		StringBuilder sb=new StringBuilder(256);
		for(int i=0; i<getArgs().length; i++) {
			sb.append(getArgs()[i]);
			sb.append((char)0x00);
		}
		// Hashcode of all of the args run together.
		hc+=sb.toString().hashCode();

		return(hc);
	}

	/** 
	 * Equal if two objects have the same hash code, same query, and same
	 * parameters.
	 */
	public boolean equals(Object o) {
		boolean rv=false;

		int otherHc=o.hashCode();
		if(otherHc == hashCode()) {
			if(o instanceof CachePreparedStatementStub) {
				CachePreparedStatementStub cpss=(CachePreparedStatementStub)o;
				String oQuery=cpss.getQuery();
				Object[] otherArgs=cpss.getArgs();

				if(oQuery != null && oQuery.equals(getQuery())) {
					// Finally, true if the args are equal.
					rv=Arrays.equals(getArgs(), otherArgs);
				} // query check
			} // instance check
		} // hash code check

		return(rv);
	}

	// Implemented
	public ResultSet executeQuery() 
		throws SQLException {

		int hc=hashCode();
		String key="dbcache_prepared_" + hc;
		SpyCache cache=SpyCache.getInstance();
		CachedResultSet crs=(CachedResultSet)cache.get(key);
		if(crs==null) {
			crs=realExecuteQuery();
			cache.store(key, crs, cacheTime*1000);
		}
		ResultSet crsret=(ResultSet)crs.newCopy();
		return(crsret);
	}

	/** 
	 * Set the query timeout.
	 * 
	 * @param to the maximum number of seconds
	 * @throws SQLException if the number is not greater than or equal to 0
	 */
	public void setQueryTimeout(int to) throws SQLException {
		if(to < 0) {
			throw new SQLException("Invalid value for query timeout:  " + to);
		}
		timeout=to;
	}

	/** 
	 * Set the max rows for the query return.
	 * 
	 * @param to the maximum number of rows to return
	 * @throws SQLException if the value is not greater than or equal to 0
	 */
	public void setMaxRows(int to) throws SQLException {
		if(to < 0) {
			throw new SQLException("Invalid value for max rows:  " + to);
		}
		maxRows=to;
	}

	// OK, here's what happens when we determine that we really don't have
	// the data and need to come up with it.
	private CachedResultSet realExecuteQuery() throws SQLException {
		PreparedStatement pst=db.prepareStatement(getQuery());
		pst.setQueryTimeout(timeout);
		pst.setMaxRows(maxRows);

		// Set allllllll the types
		Object[] args=getArgs();
		for(int i=0; i<args.length; i++) {
			try {
				switch(getTypes()[i]) {
					case Types.BIT:
						pst.setBoolean(i+1, ((Boolean)args[i]).booleanValue());
						break;
					case Types.DATE:
						pst.setDate(i+1, (Date)args[i]);
						break;
					case Types.DOUBLE:
						pst.setDouble(i+1, ((Double)args[i]).doubleValue());
						break;
					case Types.FLOAT:
						pst.setFloat(i+1, ((Float)args[i]).floatValue());
						break;
					case Types.INTEGER:
						pst.setInt(i+1, ((Integer)args[i]).intValue());
						break;
					case Types.BIGINT:
						pst.setLong(i+1, ((Long)args[i]).longValue());
						break;
					case Types.TINYINT:
						pst.setShort(i+1, (short)((Integer)args[i]).intValue());
						break;
					case Types.NULL:
						pst.setNull(i+1, ((DBNull)args[i]).getType());
						break;
					case Types.OTHER:
						pst.setObject(i+1, args[i]);
						break;
					case Types.VARCHAR:
						pst.setString(i+1, (String)args[i]);
						break;
					case Types.TIME:
						pst.setTime(i+1, (Time)args[i]);
						break;
					case Types.TIMESTAMP:
						pst.setTimestamp(i+1, (Timestamp)args[i]);
						break;
					case Types.DECIMAL:
						pst.setBigDecimal(i+1, (BigDecimal)args[i]);
						break;
					default:
						throw new SQLException("Whoops, type "
							+ getTypes()[i] + " ("
							+ TypeNames.getTypeName(getTypes()[i])
							+ ") seems to have been overlooked.");
				}
			} catch (NullPointerException ex) {
				getLogger().error(
						"error with %s in type %s at param position %d",
						args[i], getTypes()[i], i);
				throw ex;
			}
		}

		// OK, at this point, all the arguments should be set, proceed to
		// execute the query.
		return(new CachedResultSet(pst.executeQuery()));

	}

	// Implemented (sorta)
	public int executeUpdate() 
		throws SQLException {
		throw new SQLException("Illegal?  This operation makes no sense!");
	}
	
	public void close() throws SQLException {
		super.close();
		db=null;
	}
}
