// Copyright (c) 2005 Dustin Sallings <dustin@spy.net>
// arch-tag: 466DB5A1-811D-428E-AC3E-4B6B9326DF52

package net.spy.db;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import net.spy.SpyObject;

public class FileDriver extends SpyObject implements Driver {

	public static final String URL_PREFIX = "jdbc:spy:";

	// Register myself
	static {
		try {
			DriverManager.registerDriver(new FileDriver());
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}
	
	private Map queryMap=null;
	private Map updateMap=null;

	public FileDriver() {
		super();
		queryMap=new HashMap();
		updateMap=new HashMap();
	}
	
	private Map getMap(String key, Map m) {
		Map rv=(Map)m.get(key);
		if(rv == null) {
			rv=new HashMap();
			m.put(key, rv);
		}
		return(rv);
	}
	
	/**
	 * Register a query to file mapping.
	 * @param url JDBC URL to which this query applies
	 * @param s the query string
	 * @param f the file that will provide the results
	 */
	public void registerQuery(String url, String s, File f) {
		registerQuery(url, s, new Object[0], f);
	}

	/** 
	 * Register a ResultSet File to a parameterized query.
	 * @param url JDBC URL to which this query applies
	 * @param s the query string
	 * @param args the args
	 * @param f the file with the result set
	 */
	public void registerQuery(String url, String s, Object args[], File f) {
		ParameterizedQuery pq=new ParameterizedQuery(s, args);
		getMap(url, queryMap).put(pq, f);
	}

	/** 
	 * Register the query(-ies) from the given DBSQL and args to a file.
	 * @param url JDBC URL to which this query applies
	 * @param db the DBSQL instance.
	 * @param args arguments to this query
	 * @param path the path of the results
	 */
	public void registerQuery(String url, DBSQL db, Object args[], File path) {
		for(Iterator i=db.getRegisteredQueries().values().iterator();
			i.hasNext();) {
			String query=(String)i.next();
			registerQuery(url, query, args, path);
		}
	}

	/** 
	 * Register an update action.
	 * @param url JDBC URL to which this query applies
	 * @param s the query
	 * @param args the args 
	 * @param action the action to be performed upon this update
	 */
	public void registerUpdate(String url, String s, Object args[], Updater action) {
		ParameterizedQuery pq=new ParameterizedQuery(s, args);
		getMap(url, updateMap).put(pq, action);
	}

	/** 
	 * Register the update(s) from the given DBSQL and args to an updater.
	 * @param url JDBC URL to which this query applies
	 * @param db the DBSQL instance
	 * @param args the arguments to the update
	 * @param u the updater
	 */
	public void registerUpdate(String url, DBSQL db, Object args[], Updater u) {
		for(Iterator i=db.getRegisteredQueries().values().iterator();
			i.hasNext();) {
			String query=(String)i.next();
			registerUpdate(url, query, args, u);
		}
	}

	/** 
	 * Register the update(s) from the given DBSQL and args to return an int.
	 * @param url JDBC URL to which this query applies
	 * @param db the DBSQL instance
	 * @param args the arguments to the update
	 * @param rv the return value
	 */
	public void registerUpdate(String url, DBSQL db, Object args[], int rv) {
		registerUpdate(url, db, args, new IntUpdater(rv));
	}

	/** 
	 * Register a simple update that returns an int.
	 * @param url JDBC URL to which this query applies
	 * @param s the query
	 * @param args the args 
	 * @param rv the value that should be returned from this update
	 */
	public void registerUpdate(String url, String s, Object args[], int rv) {
		ParameterizedQuery pq=new ParameterizedQuery(s, args);
		getMap(url, updateMap).put(pq, new IntUpdater(rv));
	}

	/**
	 * Get the File for the specified query.
	 * @param url JDBC URL to which this query applies
	 * @param pq the query
	 * @return the File
	 * @throws SQLException if there isn't a query managed for this query
	 */
	File getQuery(String url, ParameterizedQuery pq) throws SQLException {
		File rv=(File)getMap(url, queryMap).get(pq);
		if(rv == null) {
			throw new SQLException("No mapping registered for query " + pq
				+ " in DB specified as " + url);
		}
		return(rv);
	}

	/** 
	 * Get the Updater for the specified query
	 * @param url JDBC URL to which this query applies
	 * @param pq the query
	 * 
	 * @return the Updater
	 * @throws SQLException if there isn't an update managed for this query
	 */
	Updater getUpdate(String url, ParameterizedQuery pq) throws SQLException {
		Updater rv=(Updater)getMap(url, updateMap).get(pq);
		if(rv == null) {
			throw new SQLException("No mapping registered for update " + pq
				+ " in DB specified as " + url);
		}
		return(rv);
	}

	/**
	 * Remove all mapped queries.
	 */
	public void clearQueries() {
		queryMap.clear();
		updateMap.clear();
	}

	public Connection connect(String url, Properties prop) throws SQLException {
		return (new FileConnection(url));
	}

	public boolean acceptsURL(String arg0) throws SQLException {
		return (arg0.startsWith(URL_PREFIX));
	}

	public DriverPropertyInfo[] getPropertyInfo(String arg0, Properties arg1)
		throws SQLException {
		return new DriverPropertyInfo[0];
	}

	public int getMajorVersion() {
		return 0;
	}

	public int getMinorVersion() {
		return 1;
	}

	public boolean jdbcCompliant() {
		return false;
	}

	/** 
	 * Interface for registering updates.
	 */
	public static interface Updater {
		/** 
		 * Perform the update and return a value.
		 */
		int doUpdate() throws SQLException;
	}

	// An updater that returns an int
	private static class IntUpdater implements Updater {
		private int rv=0;
		public IntUpdater(int v) {
			super();
			this.rv=v;
		}
		public int doUpdate() {
			return(rv);
		}
	}

	private static final class ParameterizedQuery extends SpyObject {
		private String query=null;
		private Object args[]=null;

		public ParameterizedQuery(String q, Object a[]) {
			super();
			if(q == null) {
				throw new NullPointerException("Invalid null query");
			}
			if(a == null) {
				throw new NullPointerException("Invalid null arguments");
			}
			this.query=q;
			this.args=a;
		}

		public ParameterizedQuery(String q) {
			this(q, new Object[0]);
		}

		public int hashCode() {
			int rv=query.hashCode();
			for(int i=0; i<args.length; i++) {
				if(args[i] != null) {
					rv ^= args[i].hashCode();
				}
			}
			return(rv);
		}

		public boolean equals(Object o) {
			boolean rv=false;
			if(o instanceof ParameterizedQuery) {
				ParameterizedQuery pq=(ParameterizedQuery)o;
				if(query.equals(pq.query)) {
					rv=Arrays.equals(args, pq.args);
				}
			}
			return(rv);
		}

		public String toString() {
			return("{ParameterizedQuery ``" + query + "'' with "
				+ Arrays.asList(args) + "}");
		}
	}

	private static final class FileConnection extends SpyObject
		implements Connection {
		
		private String url=null;

		private boolean closed = false;

		private boolean autoCommit = false;

		private String catalog = "testCatalog";

		private int transactionIsolation = TRANSACTION_SERIALIZABLE;

		private boolean readOnly = false;

		private int holdAbility = 0;

		private Map<String, Class<?>> typeMap = null;

		public FileConnection(String u) {
			super();
			url=u;
		}

		public Statement createStatement() throws SQLException {
			throw new SQLException("Not implemented");
		}

		public PreparedStatement prepareStatement(String query)
			throws SQLException {
			return(new FilePreparedStatement(url, query));
		}

		public CallableStatement prepareCall(String arg0) throws SQLException {
			throw new SQLException("Not implemented");
		}

		public String nativeSQL(String arg0) throws SQLException {
			return arg0;
		}

		public void setAutoCommit(boolean arg0) throws SQLException {
			autoCommit = arg0;
		}

		public boolean getAutoCommit() throws SQLException {
			return autoCommit;
		}

		public void commit() throws SQLException {
			// NOOP
		}

		public void rollback() throws SQLException {
			// NOOP
		}

		public void close() throws SQLException {
			closed = true;
		}

		public boolean isClosed() throws SQLException {
			return closed;
		}

		public DatabaseMetaData getMetaData() throws SQLException {
			// TODO DatabaseMetaData is a particularly difficult thing to
			// implement
			return null;
		}

		public void setReadOnly(boolean arg0) throws SQLException {
			readOnly = arg0;
		}

		public boolean isReadOnly() throws SQLException {
			return readOnly;
		}

		public void setCatalog(String arg0) throws SQLException {
			catalog = arg0;
		}

		public String getCatalog() throws SQLException {
			return (catalog);
		}

		public void setTransactionIsolation(int arg0) throws SQLException {
			transactionIsolation = arg0;
		}

		public int getTransactionIsolation() throws SQLException {
			return transactionIsolation;
		}

		public SQLWarning getWarnings() throws SQLException {
			return null;
		}

		public void clearWarnings() throws SQLException {
			// NOOP
		}

		public Statement createStatement(int arg0, int arg1)
			throws SQLException {
			throw new SQLException("Not implemented");
		}

		public PreparedStatement prepareStatement(
			String arg0, int arg1, int arg2) throws SQLException {
			throw new SQLException("Not implemented");
		}

		public CallableStatement prepareCall(String arg0, int arg1, int arg2)
			throws SQLException {
			throw new SQLException("Not implemented");
		}

		public Map<String, Class<?>> getTypeMap() throws SQLException {
			return (typeMap);
		}

		public void setTypeMap(Map<String, Class<?>> arg0) throws SQLException {
			this.typeMap = arg0;
		}

		public void setHoldability(int arg0) throws SQLException {
			this.holdAbility = arg0;
		}

		public int getHoldability() throws SQLException {
			return holdAbility;
		}

		public Savepoint setSavepoint() throws SQLException {
			throw new SQLException("Not implemented");
		}

		public Savepoint setSavepoint(String arg0) throws SQLException {
			throw new SQLException("Not implemented");
		}

		public void rollback(Savepoint arg0) throws SQLException {
			// NOOP
		}

		public void releaseSavepoint(Savepoint arg0) throws SQLException {
			throw new SQLException("Not implemented");
		}

		public Statement createStatement(int arg0, int arg1, int arg2)
			throws SQLException {
			throw new SQLException("Not implemented");
		}

		public PreparedStatement prepareStatement(
			String arg0, int arg1, int arg2, int arg3) throws SQLException {
			throw new SQLException("Not implemented");
		}

		public CallableStatement prepareCall(
			String arg0, int arg1, int arg2, int arg3) throws SQLException {
			throw new SQLException("Not supported");
		}

		public PreparedStatement prepareStatement(String arg0, int arg1)
			throws SQLException {
			throw new SQLException("Not implemented");
		}

		public PreparedStatement prepareStatement(String arg0, int[] arg1)
			throws SQLException {
			throw new SQLException("Not implemented");
		}

		public PreparedStatement prepareStatement(String arg0, String[] arg1)
			throws SQLException {
			throw new SQLException("Not implemented");
		}
	}

	private static final class FilePreparedStatement
		extends GenericPreparedStatementStub
		implements PreparedStatement {
		
		private String url=null;

		public FilePreparedStatement(String u, String q) {
			super(q);
			this.url=u;
		}

		public ResultSet executeQuery() throws SQLException {
			FileDriver fd=(FileDriver)DriverManager.getDriver(
				URL_PREFIX + "blah");
			File f=fd.getQuery(url, new ParameterizedQuery(getQuery(),
				getApplicableArgs()));
			ResultSet rs=new FileResultSet(f);
			return(rs);
		}	
		
		public int executeUpdate() throws SQLException {
			FileDriver fd=(FileDriver)DriverManager.getDriver(
				URL_PREFIX + "blah");
			Updater u=fd.getUpdate(
				url, new ParameterizedQuery(getQuery(), getApplicableArgs()));
			return(u.doUpdate());
		}
		
		public void setByte(int arg0, byte arg1) throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setBytes(int arg0, byte[] arg1) throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setTimestamp(int arg0, Timestamp arg1) throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setAsciiStream(int arg0, InputStream arg1, int arg2)
			throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setUnicodeStream(int arg0, InputStream arg1, int arg2)
			throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setBinaryStream(int arg0, InputStream arg1, int arg2)
			throws SQLException {
			// TODO Auto-generated method stub

		}

		public void clearParameters() throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setObject(int arg0, Object arg1, int arg2, int arg3)
			throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setObject(int arg0, Object arg1, int arg2)
			throws SQLException {
			// TODO Auto-generated method stub

		}

		public boolean execute() throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}

		public void addBatch() throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setCharacterStream(int arg0, Reader arg1, int arg2)
			throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setRef(int arg0, Ref arg1) throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setBlob(int arg0, Blob arg1) throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setClob(int arg0, Clob arg1) throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setArray(int arg0, Array arg1) throws SQLException {
			// TODO Auto-generated method stub

		}

		public ResultSetMetaData getMetaData() throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public void setDate(int arg0, Date arg1, Calendar arg2)
			throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setTime(int arg0, Time arg1, Calendar arg2)
			throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setTimestamp(int arg0, Timestamp arg1, Calendar arg2)
			throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setNull(int arg0, int arg1, String arg2)
			throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setURL(int arg0, URL arg1) throws SQLException {
			// TODO Auto-generated method stub

		}

		public ParameterMetaData getParameterMetaData() throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet executeQuery(String arg0) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public int executeUpdate(String arg0) throws SQLException {
			// TODO Auto-generated method stub
			return 0;
		}

		public int getMaxFieldSize() throws SQLException {
			// TODO Auto-generated method stub
			return 0;
		}

		public void setMaxFieldSize(int arg0) throws SQLException {
			// TODO Auto-generated method stub

		}

		public int getMaxRows() throws SQLException {
			// TODO Auto-generated method stub
			return 0;
		}

		public void setMaxRows(int arg0) throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setEscapeProcessing(boolean arg0) throws SQLException {
			// TODO Auto-generated method stub

		}

		public int getQueryTimeout() throws SQLException {
			// TODO Auto-generated method stub
			return 0;
		}

		public void setQueryTimeout(int arg0) throws SQLException {
			// TODO Auto-generated method stub

		}

		public void cancel() throws SQLException {
			// TODO Auto-generated method stub

		}

		public SQLWarning getWarnings() throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public void clearWarnings() throws SQLException {
			// TODO Auto-generated method stub

		}

		public void setCursorName(String arg0) throws SQLException {
			// TODO Auto-generated method stub

		}

		public boolean execute(String arg0) throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}

		public ResultSet getResultSet() throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public int getUpdateCount() throws SQLException {
			// TODO Auto-generated method stub
			return 0;
		}

		public boolean getMoreResults() throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}

		public void setFetchDirection(int arg0) throws SQLException {
			// TODO Auto-generated method stub

		}

		public int getFetchDirection() throws SQLException {
			// TODO Auto-generated method stub
			return 0;
		}

		public void setFetchSize(int arg0) throws SQLException {
			// TODO Auto-generated method stub

		}

		public int getFetchSize() throws SQLException {
			// TODO Auto-generated method stub
			return 0;
		}

		public int getResultSetConcurrency() throws SQLException {
			// TODO Auto-generated method stub
			return 0;
		}

		public int getResultSetType() throws SQLException {
			// TODO Auto-generated method stub
			return 0;
		}

		public void addBatch(String arg0) throws SQLException {
			// TODO Auto-generated method stub

		}

		public void clearBatch() throws SQLException {
			// TODO Auto-generated method stub

		}

		public int[] executeBatch() throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public Connection getConnection() throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean getMoreResults(int arg0) throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}

		public ResultSet getGeneratedKeys() throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public int executeUpdate(String arg0, int arg1) throws SQLException {
			throw new SQLException("Not implemented");
		}

		public int executeUpdate(String arg0, int[] arg1) throws SQLException {
			throw new SQLException("Not implemented");
		}

		public int executeUpdate(String arg0, String[] arg1)
			throws SQLException {
			throw new SQLException("Not implemented");
		}

		public boolean execute(String arg0, int arg1) throws SQLException {
			throw new SQLException("Not implemented");
		}

		public boolean execute(String arg0, int[] arg1) throws SQLException {
			throw new SQLException("Not implemented");
		}

		public boolean execute(String arg0, String[] arg1) throws SQLException {
			throw new SQLException("Not implemented");
		}

		public int getResultSetHoldability() throws SQLException {
			// TODO Auto-generated method stub
			return 0;
		}
	}

}
