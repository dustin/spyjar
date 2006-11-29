// Copyright (c) 2005 Dustin Sallings <dustin@spy.net>
// arch-tag: 466DB5A1-811D-428E-AC3E-4B6B9326DF52

package net.spy.db;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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
	
	private Map<String, Map<ParameterizedQuery, Object>> queryMap=null;
	private Map<String, Map<ParameterizedQuery, Object>> updateMap=null;

	public FileDriver() {
		super();
		queryMap=new HashMap<String, Map<ParameterizedQuery, Object>>();
		updateMap=new HashMap<String, Map<ParameterizedQuery, Object>>();
	}
	
	private Map<ParameterizedQuery, Object> getMap(String key,
			Map<String, Map<ParameterizedQuery, Object>> m) {
		Map<ParameterizedQuery, Object> rv=m.get(key);
		if(rv == null) {
			rv=new HashMap<ParameterizedQuery, Object>();
			m.put(key, rv);
		}
		return(rv);
	}
	
	/**
	 * Register a query to URL mapping.
	 * @param url JDBC URL to which this query applies
	 * @param s the query string
	 * @param f the URL that will provide the results
	 */
	public void registerQuery(String url, String s, URL f) {
		registerQuery(url, s, new Object[0], f);
	}

	/** 
	 * Register a ResultSet URL to a parameterized query.
	 * @param url JDBC URL to which this query applies
	 * @param s the query string
	 * @param args the args
	 * @param f the URL with the result set
	 */
	public void registerQuery(String url, String s, Object args[], URL f) {
		ParameterizedQuery pq=new ParameterizedQuery(s, args);
		getMap(url, queryMap).put(pq, f);
	}

	/** 
	 * Register the query(-ies) from the given DBSQL and args to a URL.
	 * @param url JDBC URL to which this query applies
	 * @param db the DBSQL instance.
	 * @param args arguments to this query
	 * @param path the path of the results
	 */
	public void registerQuery(String url, DBSQL db, Object args[], URL path) {
		for(String query : db.getRegisteredQueries().values()) {
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
		for(String query : db.getRegisteredQueries().values()) {
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
	 * Get the URL for the specified query.
	 * @param url JDBC URL to which this query applies
	 * @param pq the query
	 * @return the URL
	 * @throws SQLException if there isn't a query managed for this query
	 */
	URL getQuery(String url, ParameterizedQuery pq) throws SQLException {
		URL rv=(URL)getMap(url, queryMap).get(pq);
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
		private Object[] args=null;

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

		@Override
		public int hashCode() {
			int rv=query.hashCode() ^ args.length;
			return(rv);
		}

		@Override
		public boolean equals(Object o) {
			boolean rv=false;
			if(o instanceof ParameterizedQuery) {
				ParameterizedQuery pq=(ParameterizedQuery)o;
				if(query.equals(pq.query) && args.length == pq.args.length) {
					// Default to true, try to disprove
					rv=true;
					// Look at each argument and validate that it's either
					// the same, or the pq has a matcher that likes what it
					// sees
					for(int i=0; i<pq.args.length; i++) {
						if(pq.args[i] instanceof ParamMatcher) {
							ParamMatcher pm=(ParamMatcher)pq.args[i];
							rv &= pm.matches(args[i]);
						} else {
							if(pq.args[i] == null) {
								rv &= (args[i] == null);
							} else {
								rv &= pq.args[i].equals(args[i]);
							}
						}
					}
				}
			}
			return(rv);
		}

		@Override
		public String toString() {
			ArrayList<String> paramStrings=new ArrayList<String>(args.length);
			for(int i=0; i<args.length; i++) {
				String type="";
				if(args[i] != null) {
					type="(" + args[i].getClass().getName() + ")";
				}
				paramStrings.add(type + args[i]);
			}
			return("{ParameterizedQuery ``" + query + "'' with "
				+ paramStrings + "}");
		}
	}

	/** 
	 * Parameter matching interface for fuzzy matches on query parameters.
	 */
	public static interface ParamMatcher {
		/** 
		 * Return true if this parameter matches the given object.
		 */
		boolean matches(Object o);
	}

	/** 
	 * Parameter matching instance that matches any parameter.
	 */
	public static class AnyParamMatcher implements ParamMatcher {
		public boolean matches(Object o) {
			return(true);
		}
	}

	/** 
	 * Parameter matching instance that matches objects by class.
	 */
	public static class ClassParamMatcher implements ParamMatcher {
		private Class<?> theClass=null;
		public ClassParamMatcher(Class<?> c) {
			super();
			theClass=c;
		}
		public boolean matches(Object o) {
			return(theClass.equals(o.getClass()));
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
			return new FileDriverMetaData(this, url);
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
		private int maxRows = Integer.MAX_VALUE;

		public FilePreparedStatement(String u, String q) {
			super(q);
			this.url=u;
		}

		public ResultSet executeQuery() throws SQLException {
			FileDriver fd=(FileDriver)DriverManager.getDriver(
				URL_PREFIX + "blah");
			URL f=fd.getQuery(url, new ParameterizedQuery(getQuery(),
				getApplicableArgs()));
			ResultSet rs=new FileResultSet(f, maxRows);
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
			throw new SQLException("Not supported.");

		}

		public void setBytes(int arg0, byte[] arg1) throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setAsciiStream(int arg0, InputStream arg1, int arg2)
			throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setUnicodeStream(int arg0, InputStream arg1, int arg2)
			throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setBinaryStream(int arg0, InputStream arg1, int arg2)
			throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void clearParameters() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setObject(int arg0, Object arg1, int arg2, int arg3)
			throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setObject(int arg0, Object arg1, int arg2)
			throws SQLException {
			throw new SQLException("Not supported.");
		}

		public boolean execute() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void addBatch() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setCharacterStream(int arg0, Reader arg1, int arg2)
			throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setRef(int arg0, Ref arg1) throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setBlob(int arg0, Blob arg1) throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setClob(int arg0, Clob arg1) throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setArray(int arg0, Array arg1) throws SQLException {
			throw new SQLException("Not supported.");
		}

		public ResultSetMetaData getMetaData() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setDate(int arg0, Date arg1, Calendar arg2)
			throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setTime(int arg0, Time arg1, Calendar arg2)
			throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setTimestamp(int arg0, Timestamp arg1, Calendar arg2)
			throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setNull(int arg0, int arg1, String arg2)
			throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setURL(int arg0, URL arg1) throws SQLException {
			throw new SQLException("Not supported.");
		}

		public ParameterMetaData getParameterMetaData() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public ResultSet executeQuery(String arg0) throws SQLException {
			throw new SQLException("Not supported.");
		}

		public int executeUpdate(String arg0) throws SQLException {
			throw new SQLException("Not supported.");
		}

		public int getMaxFieldSize() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setMaxFieldSize(int arg0) throws SQLException {
			throw new SQLException("Not supported.");
		}

		public int getMaxRows() throws SQLException {
			return maxRows;
		}

		public void setMaxRows(int to) throws SQLException {
			assert to >= 0;
			maxRows=(to == 0 ? Integer.MAX_VALUE : to);
		}

		public void setEscapeProcessing(boolean arg0) throws SQLException {
			throw new SQLException("Not supported.");
		}

		public int getQueryTimeout() throws SQLException {
			return(0);
		}

		public void setQueryTimeout(int arg0) throws SQLException {
			// Do nothing
		}

		public void cancel() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public SQLWarning getWarnings() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void clearWarnings() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setCursorName(String arg0) throws SQLException {
			throw new SQLException("Not supported.");
		}

		public boolean execute(String arg0) throws SQLException {
			throw new SQLException("Not supported.");
		}

		public ResultSet getResultSet() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public int getUpdateCount() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public boolean getMoreResults() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setFetchDirection(int arg0) throws SQLException {
			throw new SQLException("Not supported.");
		}

		public int getFetchDirection() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void setFetchSize(int arg0) throws SQLException {
			throw new SQLException("Not supported.");
		}

		public int getFetchSize() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public int getResultSetConcurrency() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public int getResultSetType() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void addBatch(String arg0) throws SQLException {
			throw new SQLException("Not supported.");
		}

		public void clearBatch() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public int[] executeBatch() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public Connection getConnection() throws SQLException {
			throw new SQLException("Not supported.");
		}

		public boolean getMoreResults(int arg0) throws SQLException {
			throw new SQLException("Not supported.");
		}

		public ResultSet getGeneratedKeys() throws SQLException {
			throw new SQLException("Not supported.");
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
			throw new SQLException("Not supported.");
		}
	}

	private static final class FileDriverMetaData extends SpyObject
		implements DatabaseMetaData {
		
		private Connection conn=null;
		private String url=null;
		
		public FileDriverMetaData(Connection c, String u) {
			super();
			conn=c;
			url=u;
		}

		public boolean allProceduresAreCallable() throws SQLException {
			return false;
		}

		public boolean allTablesAreSelectable() throws SQLException {
			return false;
		}

		public String getURL() throws SQLException {
			return url;
		}

		public String getUserName() throws SQLException {
			return "user";
		}

		public boolean isReadOnly() throws SQLException {
			return false;
		}

		public boolean nullsAreSortedHigh() throws SQLException {
			return false;
		}

		public boolean nullsAreSortedLow() throws SQLException {
			return true;
		}

		public boolean nullsAreSortedAtStart() throws SQLException {
			return false;
		}

		public boolean nullsAreSortedAtEnd() throws SQLException {
			return true;
		}

		public String getDatabaseProductName() throws SQLException {
			return "SpyDB FileDriver";
		}

		public String getDatabaseProductVersion() throws SQLException {
			return "2.1";
		}

		public String getDriverName() throws SQLException {
			return "SpyDB FileDriver";
		}

		public String getDriverVersion() throws SQLException {
			return "2.1";
		}

		public int getDriverMajorVersion() {
			return 2;
		}

		public int getDriverMinorVersion() {
			return 1;
		}

		public boolean usesLocalFiles() throws SQLException {
			return true;
		}

		public boolean usesLocalFilePerTable() throws SQLException {
			return false;
		}

		public boolean supportsMixedCaseIdentifiers() throws SQLException {
			return true;
		}

		public boolean storesUpperCaseIdentifiers() throws SQLException {
			return true;
		}

		public boolean storesLowerCaseIdentifiers() throws SQLException {
			return true;
		}

		public boolean storesMixedCaseIdentifiers() throws SQLException {
			return true;
		}

		public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
			return true;
		}

		public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
			return false;
		}

		public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
			return true;
		}

		public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
			return true;
		}

		public String getIdentifierQuoteString() throws SQLException {
			return "\"";
		}

		public String getSQLKeywords() throws SQLException {
			return "";
		}

		public String getNumericFunctions() throws SQLException {
			return "";
		}

		public String getStringFunctions() throws SQLException {
			return "";
		}

		public String getSystemFunctions() throws SQLException {
			return "";
		}

		public String getTimeDateFunctions() throws SQLException {
			return "";
		}

		public String getSearchStringEscape() throws SQLException {
			return "";
		}

		public String getExtraNameCharacters() throws SQLException {
			return "";
		}

		public boolean supportsAlterTableWithAddColumn() throws SQLException {
			return true;
		}

		public boolean supportsAlterTableWithDropColumn() throws SQLException {
			return true;
		}

		public boolean supportsColumnAliasing() throws SQLException {
			return true;
		}

		public boolean nullPlusNonNullIsNull() throws SQLException {
			return true;
		}

		public boolean supportsConvert() throws SQLException {
			return true;
		}

		public boolean supportsConvert(int fromType, int toType) throws SQLException {
			return true;
		}

		public boolean supportsTableCorrelationNames() throws SQLException {
			return true;
		}

		public boolean supportsDifferentTableCorrelationNames() throws SQLException {
			return true;
		}

		public boolean supportsExpressionsInOrderBy() throws SQLException {
			return true;
		}

		public boolean supportsOrderByUnrelated() throws SQLException {
			return false;
		}

		public boolean supportsGroupBy() throws SQLException {
			return true;
		}

		public boolean supportsGroupByUnrelated() throws SQLException {
			return true;
		}

		public boolean supportsGroupByBeyondSelect() throws SQLException {
			return false;
		}

		public boolean supportsLikeEscapeClause() throws SQLException {
			return true;
		}

		public boolean supportsMultipleResultSets() throws SQLException {
			return false;
		}

		public boolean supportsMultipleTransactions() throws SQLException {
			return false;
		}

		public boolean supportsNonNullableColumns() throws SQLException {
			return false;
		}

		public boolean supportsMinimumSQLGrammar() throws SQLException {
			return true;
		}

		public boolean supportsCoreSQLGrammar() throws SQLException {
			return true;
		}

		public boolean supportsExtendedSQLGrammar() throws SQLException {
			return true;
		}

		public boolean supportsANSI92EntryLevelSQL() throws SQLException {
			return true;
		}

		public boolean supportsANSI92IntermediateSQL() throws SQLException {
			return true;
		}

		public boolean supportsANSI92FullSQL() throws SQLException {
			return true;
		}

		public boolean supportsIntegrityEnhancementFacility() throws SQLException {
			return false;
		}

		public boolean supportsOuterJoins() throws SQLException {
			return true;
		}

		public boolean supportsFullOuterJoins() throws SQLException {
			return true;
		}

		public boolean supportsLimitedOuterJoins() throws SQLException {
			return true;
		}

		public String getSchemaTerm() throws SQLException {
			return "schema";
		}

		public String getProcedureTerm() throws SQLException {
			return "procedure";
		}

		public String getCatalogTerm() throws SQLException {
			return "catalog";
		}

		public boolean isCatalogAtStart() throws SQLException {
			return false;
		}

		public String getCatalogSeparator() throws SQLException {
			return ".";
		}

		public boolean supportsSchemasInDataManipulation() throws SQLException {
			return true;
		}

		public boolean supportsSchemasInProcedureCalls() throws SQLException {
			return true;
		}

		public boolean supportsSchemasInTableDefinitions() throws SQLException {
			return true;
		}

		public boolean supportsSchemasInIndexDefinitions() throws SQLException {
			return true;
		}

		public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
			return true;
		}

		public boolean supportsCatalogsInDataManipulation() throws SQLException {
			return false;
		}

		public boolean supportsCatalogsInProcedureCalls() throws SQLException {
			return false;
		}

		public boolean supportsCatalogsInTableDefinitions() throws SQLException {
			return false;
		}

		public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
			return false;
		}

		public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
			return false;
		}

		public boolean supportsPositionedDelete() throws SQLException {
			return true;
		}

		public boolean supportsPositionedUpdate() throws SQLException {
			return true;
		}

		public boolean supportsSelectForUpdate() throws SQLException {
			return true;
		}

		public boolean supportsStoredProcedures() throws SQLException {
			return true;
		}

		public boolean supportsSubqueriesInComparisons() throws SQLException {
			return true;
		}

		public boolean supportsSubqueriesInExists() throws SQLException {
			return true;
		}

		public boolean supportsSubqueriesInIns() throws SQLException {
			return true;
		}

		public boolean supportsSubqueriesInQuantifieds() throws SQLException {
			return true;
		}

		public boolean supportsCorrelatedSubqueries() throws SQLException {
			return true;
		}

		public boolean supportsUnion() throws SQLException {
			return true;
		}

		public boolean supportsUnionAll() throws SQLException {
			return true;
		}

		public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
			return true;
		}

		public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
			return true;
		}

		public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
			return true;
		}

		public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
			return true;
		}

		public int getMaxBinaryLiteralLength() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxCharLiteralLength() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxColumnNameLength() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxColumnsInGroupBy() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxColumnsInIndex() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxColumnsInOrderBy() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxColumnsInSelect() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxColumnsInTable() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxConnections() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxCursorNameLength() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxIndexLength() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxSchemaNameLength() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxProcedureNameLength() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxCatalogNameLength() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxRowSize() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
			return false;
		}

		public int getMaxStatementLength() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxStatements() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxTableNameLength() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxTablesInSelect() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getMaxUserNameLength() throws SQLException {
			return Integer.MAX_VALUE;
		}

		public int getDefaultTransactionIsolation() throws SQLException {
			return Connection.TRANSACTION_SERIALIZABLE;
		}

		public boolean supportsTransactions() throws SQLException {
			return true;
		}

		public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
			return true;
		}

		public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
			return true;
		}

		public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
			return false;
		}

		public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
			return false;
		}

		public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
			return false;
		}

		public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getSchemas() throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getCatalogs() throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getTableTypes() throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getCrossReference(String primaryCatalog, String primarySchema, String primaryTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getTypeInfo() throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean supportsResultSetType(int type) throws SQLException {
			return true;
		}

		public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
			return true;
		}

		public boolean ownUpdatesAreVisible(int type) throws SQLException {
			return true;
		}

		public boolean ownDeletesAreVisible(int type) throws SQLException {
			return true;
		}

		public boolean ownInsertsAreVisible(int type) throws SQLException {
			return true;
		}

		public boolean othersUpdatesAreVisible(int type) throws SQLException {
			return false;
		}

		public boolean othersDeletesAreVisible(int type) throws SQLException {
			return false;
		}

		public boolean othersInsertsAreVisible(int type) throws SQLException {
			return false;
		}

		public boolean updatesAreDetected(int type) throws SQLException {
			return false;
		}

		public boolean deletesAreDetected(int type) throws SQLException {
			return false;
		}

		public boolean insertsAreDetected(int type) throws SQLException {
			return false;
		}

		public boolean supportsBatchUpdates() throws SQLException {
			return true;
		}

		public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public Connection getConnection() throws SQLException {
			return conn;
		}

		public boolean supportsSavepoints() throws SQLException {
			return true;
		}

		public boolean supportsNamedParameters() throws SQLException {
			return false;
		}

		public boolean supportsMultipleOpenResults() throws SQLException {
			return true;
		}

		public boolean supportsGetGeneratedKeys() throws SQLException {
			return false;
		}

		public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean supportsResultSetHoldability(int holdability) throws SQLException {
			return false;
		}

		public int getResultSetHoldability() throws SQLException {
			return ResultSet.CLOSE_CURSORS_AT_COMMIT;
		}

		public int getDatabaseMajorVersion() throws SQLException {
			return 2;
		}

		public int getDatabaseMinorVersion() throws SQLException {
			// TODO Auto-generated method stub
			return 1;
		}

		public int getJDBCMajorVersion() throws SQLException {
			return 3;
		}

		public int getJDBCMinorVersion() throws SQLException {
			return 0;
		}

		public int getSQLStateType() throws SQLException {
			// XXX:  I don't know where this is defined off hand
			return 0;
		}

		public boolean locatorsUpdateCopy() throws SQLException {
			return false;
		}

		public boolean supportsStatementPooling() throws SQLException {
			return false;
		}
		
	}
}
