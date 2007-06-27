// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>

package net.spy.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.spy.util.CloseUtil;

/**
 * Stub class for building a ResultSet from a URL.
 */
public class FileResultSetStub extends GenericResultSetStub {

	/**
	 * Get an instance of FileResultSetStub.
	 */
	public FileResultSetStub(URL f, int maxResults) throws SQLException {
		super();
		try {
			initFromURL(f, maxResults);
		} catch(IOException e) {
			SQLException toThrow=new SQLException(
				"Could not initialize results from " + f);
			toThrow.initCause(e);
			throw toThrow;
		}
	}

	private void initFromURL(URL u, int maxResults)
		throws SQLException, IOException {
		InputStream is=null;
		try {
			is=u.openStream();
			LineNumberReader lnr=new LineNumberReader(new InputStreamReader(is));

			MyMetaData mmd=new MyMetaData(lnr.readLine());
			setMetaData(mmd);

			List<Object[]> results=new ArrayList<Object[]>();
			String tmp=lnr.readLine();
			while(tmp != null && results.size() < maxResults) {
				Object[] result=mmd.parseLine(tmp);
				results.add(result);
				tmp=lnr.readLine();
			}
			setResults(results);
		} finally {
			CloseUtil.close(is);
		}
	}

	private static interface Parser {
		Object parseString(String s) throws Exception;
	}
	
	static abstract class PreParser implements Parser {
		public final Object parseString(String s) throws Exception {
			Object rv=null;
			String cleanedUp=cleanString(s);
			if(cleanedUp != null) {
				rv=subParse(cleanedUp);
			}
			return(rv);
		}
		
		private String cleanString(String s) {
			String rv=null;
			if(!s.equals("\\N")) {
				StringBuilder sb=new StringBuilder(s.length());
				for(int i=0; i<s.length(); i++) {
					char c=s.charAt(i);
					switch(c) {
						case '\\':
							i++;
							char escaped=s.charAt(i);
							switch(escaped) {
								case 't':
									sb.append('\t');
									break;
								case 'n':
									sb.append('\n');
									break;
								default:
									sb.append('\\');
							}
							break;
						default:
							sb.append(c);
					}
				}
				rv=sb.toString();
			}
			return(rv);
		}
		
		protected abstract Object subParse(String s) throws Exception;
	}

	static final class NumberParser extends PreParser {
		@Override
		public Object subParse(String s) throws Exception {
			return(new BigDecimal(s));
		}
	}
	
	static final class StringParser extends PreParser {
		@Override
		public Object subParse(String s) {
			return(s);
		}
	}

	abstract static class MultiDateParser extends PreParser {
		private SimpleDateFormat[] formats=null;

		public MultiDateParser(String[] formatStrings) {
			super();
			formats=new SimpleDateFormat[formatStrings.length];
			for(int i=0; i<formatStrings.length; i++) {
				formats[i]=new SimpleDateFormat(formatStrings[i]);
				formats[i].setLenient(false);
			}
		}

		protected long parseDate(String s) {
			long rv=0;
			for(int i=0; rv==0 && i<formats.length; i++) {
				try {
					rv=formats[i].parse(s).getTime();
				} catch(ParseException e) {
					// skip
				}
			}
			return(rv);
		}
	}

	private static final class TimeParser extends MultiDateParser {
		public TimeParser() {
			super(new String[]{"HH:mm:ss.SSS", "HH:mm:ss"});
		}

		@Override
		public Object subParse(String s) throws Exception {
			return(new java.sql.Time(parseDate(s)));
		}
	}

	private static final class DateParser extends MultiDateParser {
		public DateParser() {
			super(new String[]{
				"yyyyMMdd", "yyyy-MM-dd", "yyyy/MM/dd", "dd/MM/yyyy"});
		}

		@Override
		public Object subParse(String s) throws Exception {
			return(new java.sql.Date(parseDate(s)));
		}
	}

	private static final class TimestampParser extends MultiDateParser {
		public TimestampParser() {
			super(new String[]{
				"yyyyMMdd'T'HH:mm:ss.SSS",
				"yyyyMMdd'T'HH:mm:ss",
				"yyyyMMdd'T'HHmmss.SSS",
				"yyyyMMdd'T'HHmmss",
				"yyyy-MM-dd HH:mm:ss.SSS",
				"yyyy-MM-dd HH:mm:ss",
				"yyyy/MM/dd HH:mm:ss.SSS",
				"yyyy/MM/dd HH:mm:ss",
				"dd/MM/yyyy HH:mm:ss.SSS",
				"dd/MM/yyyy HH:mm:ss",
				});
		}

		@Override
		public Object subParse(String s) throws Exception {
			return(new java.sql.Timestamp(parseDate(s)));
		}
	}

	static final class ParserFactory extends Object {
		private static ParserFactory instance=null;
		private Map<Integer, PreParser> parsers=null;
		private ParserFactory() {
			super();
			parsers=new HashMap<Integer, PreParser>();
			parsers.put(new Integer(Types.VARCHAR), new StringParser());
			parsers.put(new Integer(Types.LONGVARCHAR), new StringParser());
			parsers.put(new Integer(Types.INTEGER), new NumberParser());
			parsers.put(new Integer(Types.BIGINT), new NumberParser());
			parsers.put(new Integer(Types.DECIMAL), new NumberParser());
			parsers.put(new Integer(Types.DOUBLE), new NumberParser());
			parsers.put(new Integer(Types.FLOAT), new NumberParser());
			parsers.put(new Integer(Types.NUMERIC), new NumberParser());
			parsers.put(new Integer(Types.REAL), new NumberParser());
			parsers.put(new Integer(Types.SMALLINT), new NumberParser());
			parsers.put(new Integer(Types.TINYINT), new NumberParser());
			parsers.put(new Integer(Types.TIMESTAMP), new TimestampParser());
			parsers.put(new Integer(Types.DATE), new DateParser());
			parsers.put(new Integer(Types.TIME), new TimeParser());
			parsers.put(new Integer(Types.BIT), new PreParser() {
				@Override
				public Object subParse(String s) throws Exception {
					Boolean rv=Boolean.FALSE;
					try {
						if(Integer.parseInt(s) == 0) {
							rv=Boolean.FALSE;
						} else {
							rv=Boolean.TRUE;
						}
					} catch(NumberFormatException e) {
						rv=Boolean.valueOf(s);
					}
					return(rv);
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
			Parser rv=parsers.get(new Integer(type));
			if(rv == null) {
				throw new SQLException("Don't have a parser for "
					+ TypeNames.getTypeName(type));
			}
			return(rv);
		}
	}

	static final class MyMetaData implements ResultSetMetaData {

		private String[] names=null;
		private int[] types=null;

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
			Object[] rv=new Object[names.length];
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
