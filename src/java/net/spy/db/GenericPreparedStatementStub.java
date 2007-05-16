// Copyright (c) 2005 Dustin Sallings <dustin@spy.net>
// arch-tag: 2A85D729-9719-48CF-82D3-2DCC72EDADC8

package net.spy.db;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

import net.spy.SpyObject;

public abstract class GenericPreparedStatementStub extends SpyObject {

	private final String query;
	private final Object[] args;
	private final int[] types;

	public GenericPreparedStatementStub(String q) {
		super();
		query=q;
		// Figure out how many arguments may be used.
		int ntokens=DBUtil.countQs(query);
		args=new Object[ntokens];
		types=new int[ntokens];
	}
	
	protected String getQuery() {
		return(query);
	}
	
	protected Object[] getArgs() {
		return(args);
	}
	
	/**
	 * Get the args as can be applied to a database.
	 * @return the arguments, with DBNulls replaced with null
	 */
	protected Object[] getApplicableArgs() {
		Object[] rv=new Object[args.length];
		for(int i=0; i<args.length; i++) {
			if(args[i] instanceof DBNull) {
				rv[i]=null;
			} else {
				rv[i]=args[i];
			}
		}
		return(rv);
	}
	
	protected int[] getTypes() {
		return(types);
	}
	
	private void setArg(int index, Object what, int type) throws SQLException {
		// Our base is 0, JDBC base is 1
		index--;
	
		if(index<0) {
			throw new SQLException("Illegal index, they start at 1, G");
		}
		if(index>=args.length) {
			throw new SQLException("Illegal index, this statement takes a "
				+ "maximum of " + args.length + " arguments.");
		}
	
		// Set the vars
		args[index]=what;
		types[index]=type;
	}

	public void setBoolean(int a0, boolean a1) throws SQLException {
		setArg(a0, Boolean.valueOf(a1), Types.BIT);
	}

	public void setDate(int a0, Date a1) throws SQLException {
		setArg(a0, a1, Types.DATE);
	}

	public void setDouble(int a0, double a1) throws SQLException {
		setArg(a0, new Double(a1), Types.DOUBLE);
	}

	public void setFloat(int a0, float a1) throws SQLException {
		setArg(a0, new Float(a1), Types.FLOAT);
	}

	public void setInt(int a0, int a1) throws SQLException {
		setArg(a0, new Integer(a1), Types.INTEGER);
	}

	public void setLong(int a0, long a1) throws SQLException {
		setArg(a0, new Long(a1), Types.BIGINT);
	}

	public void setNull(int a0, int a1) throws SQLException {
		// This one works a bit different because we have to store the
		// original type
		setArg(a0, new DBNull(a1), Types.NULL);
	}

	public void setBigDecimal(int a0, BigDecimal a1) throws SQLException {
		setArg(a0, a1, Types.DECIMAL);
	}

	public void setObject(int a0, java.lang.Object a1) throws SQLException {
		setArg(a0, a1, Types.OTHER);
	}

	public void setShort(int a0, short a1) throws SQLException {
		setArg(a0, new Integer(a1), Types.TINYINT);
	}

	public void setString(int a0, java.lang.String a1) throws SQLException {
		setArg(a0, a1, Types.VARCHAR);
	}

	public void setTime(int a0, Time a1) throws SQLException {
		setArg(a0, a1, Types.TIME);
	}
	
	/**
	 * Set a timetamp value.
	 */
	public void setTimestamp(int a0, Timestamp a1) throws SQLException {
		setArg(a0, a1, Types.TIMESTAMP);
	}

	public void close() throws SQLException {
		// nothing
	}

}
