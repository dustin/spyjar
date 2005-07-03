// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: C9780BAD-26B0-4C7F-A9B0-036E16487F7D

package net.spy.db;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.lang.reflect.Field;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Stub class for building a ResultSet from a file.
 */
public class FileResultSetStub extends GenericResultSetStub {

	/**
	 * Get an instance of FileResultSetStub.
	 */
	public FileResultSetStub(File f) throws SQLException {
		super();
		try {
			initFromFile(f);
		} catch(IOException e) {
			SQLException toThrow=new SQLException(
				"Could not initialize results from " + f);
			toThrow.initCause(e);
			throw toThrow;
		}
	}

	private void initFromFile(File f) throws SQLException, IOException {
		FileReader fr=null;
		try {
			fr=new FileReader(f);
			LineNumberReader lnr=new LineNumberReader(fr);

			MyMetaData mmd=new MyMetaData(lnr.readLine());
			setMetaData(mmd);

			List results=new ArrayList();
			String tmp=lnr.readLine();
			while(tmp != null) {
				Object result[]=mmd.parseLine(tmp);
				results.add(result);
				tmp=lnr.readLine();
			}
			setResults(results);
		} finally {
			if(fr != null) {
				fr.close();
			}
		}
	}

	private static interface Parser {
		Object parseString(String s) throws Exception;
	}

	private static final class ParserFactory extends Object {
		private static ParserFactory instance=null;
		private Map parsers=null;
		private ParserFactory() {
			super();
			parsers=new HashMap();
			parsers.put(new Integer(Types.VARCHAR), new Parser() {
					public Object parseString(String s) {
						return(s);
					}
				});
			parsers.put(new Integer(Types.INTEGER), new Parser() {
					public Object parseString(String s) {
						return(new Integer(s));
					}
				});
			parsers.put(new Integer(Types.TIMESTAMP), new Parser() {
					private SimpleDateFormat sdf=new SimpleDateFormat(
						"yyyyMMdd'T'HH:mm:ss");
					public Object parseString(String s) throws Exception {
						Date d=sdf.parse(s);
						return(new java.sql.Timestamp(d.getTime()));
					}
				});
			parsers.put(new Integer(Types.DATE), new Parser() {
					private SimpleDateFormat sdf=new SimpleDateFormat(
						"yyyyMMdd");
					public Object parseString(String s) throws Exception {
						Date d=sdf.parse(s);
						return(new java.sql.Date(d.getTime()));
					}
				});
			parsers.put(new Integer(Types.TIME), new Parser() {
					private SimpleDateFormat sdf=new SimpleDateFormat(
						"HH:mm:ss");
					public Object parseString(String s) throws Exception {
						Date d=sdf.parse(s);
						return(new java.sql.Time(d.getTime()));
					}
				});
		}

		public static synchronized ParserFactory getInstance() {
			if(instance == null) {
				instance=new ParserFactory();
			}
			return(instance);
		}
		
		public Parser getParser(int type) throws SQLException {
			Parser rv=(Parser)parsers.get(new Integer(type));
			if(rv == null) {
				throw new SQLException("Don't have a parser for "
					+ TypeNames.getTypeName(type));
			}
			return(rv);
		}
	}

	private static final class MyMetaData implements ResultSetMetaData {

		private String names[]=null;
		private int types[]=null;

		public MyMetaData(String line) throws SQLException {
			super();
			StringTokenizer st=new StringTokenizer(line, "\t");
			names=new String[st.countTokens()];
			types=new int[st.countTokens()];

			int i=0;
			while(st.hasMoreTokens()) {
				String desc=st.nextToken();
				StringTokenizer parts=new StringTokenizer(desc, ":");
				names[i]=parts.nextToken();
				types[i]=lookupType(parts.nextToken());
				i++;
			}
		}

		private int lookupType(String typeName) throws SQLException {
			int rv=0;
			try {
				Field f=Types.class.getDeclaredField(typeName);
				rv=((Integer)f.get(null)).intValue();
			} catch(Exception e) {
				SQLException toThrow=new SQLException(
					"Cannot look up type " + typeName);
				toThrow.initCause(e);
				throw toThrow;
			}
			return(rv);
		}

		public Object[] parseLine(String line) throws SQLException {
			// Parse the line
			ParserFactory pf=ParserFactory.getInstance();
			StringTokenizer st=new StringTokenizer(line, "\t");
			Object rv[]=new Object[names.length];
			int i=0;
			while(st.hasMoreTokens()) {
				String toParse=st.nextToken();
				try {
					rv[i]=pf.getParser(types[i]).parseString(toParse);
				} catch(SQLException e) {
					throw e;
				} catch(Exception e) {
					SQLException toThrow=new SQLException("Couldn't parse "
						+ toParse + " as " + TypeNames.getTypeName(types[i]));
					toThrow.initCause(e);
					throw toThrow;
				}
				i++;
			}
			
			return(rv);
		}

    	public int getColumnCount() throws SQLException {
			return(names.length);
		}

    	public boolean isAutoIncrement(int col) throws SQLException {
			return(false);
		}

    	public boolean isCaseSensitive(int col) throws SQLException {
			return(true);
		}

    	public boolean isSearchable(int col) throws SQLException {
			return(false);
		}

    	public boolean isCurrency(int col) throws SQLException {
			return(false);
		}

    	public int isNullable(int col) throws SQLException {
			return(columnNoNulls);
		}

    	public boolean isSigned(int col) throws SQLException {
			return(false);
		}

    	public int getColumnDisplaySize(int col) throws SQLException {
			return(20);
		}

    	public String getColumnLabel(int col) throws SQLException {
			return(names[col-1]);
		}

    	public String getColumnName(int col) throws SQLException {
			return(names[col-1]);
		}

    	public String getSchemaName(int col) throws SQLException {
			return("testSchema");
		}

    	public int getPrecision(int col) throws SQLException {
			return(0);
		}

    	public int getScale(int col) throws SQLException {
			return(0);
		}

    	public String getTableName(int col) throws SQLException {
			return("testTable");
		}

    	public String getCatalogName(int col) throws SQLException {
			return("testCatalog");
		}

    	public int getColumnType(int col) throws SQLException {
			return(types[col-1]);
		}

    	public String getColumnTypeName(int col) throws SQLException {
			return(TypeNames.getTypeName(types[col-1]));
		}

    	public boolean isReadOnly(int col) throws SQLException {
			return(true);
		}

    	public boolean isWritable(int col) throws SQLException {
			return(false);
		}

    	public boolean isDefinitelyWritable(int col) throws SQLException {
			return(false);
		}

    	public String getColumnClassName(int col) throws SQLException {
			return("java.lang.Object");
		}


	}

}
