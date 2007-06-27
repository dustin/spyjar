package net.spy.pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.Properties;

import net.spy.util.SpyConfig;

/**
 * PoolFiller object to fill a pool with JDBC PoolAbles
 */
public class JDBCPoolFiller extends PoolFiller {

	private String source=null;
	private Properties dbProps=null;
	private long maxAge=0;

	/** 
	 * Instantiate the JDBCPoolFiller.
	 */
	public JDBCPoolFiller(String name, SpyConfig conf) throws PoolException {
		super(name, conf);

		initialize();
	}

	// Extract db options from various properties.
	private void setDBOptions(Properties from, Properties tmpconf,
		String base) {
		for(Enumeration<?> e=from.propertyNames(); e.hasMoreElements(); ) {
			String pname=(String)e.nextElement();
			if(pname.startsWith(base)) {
				String oname=pname.substring(base.length());
				String ovalue=from.getProperty(pname);
				tmpconf.put(oname, ovalue);
			}
		}
	}

	private void initialize() throws PoolException {
		try {
			// Load the JDBC driver
			String classname=getProperty("dbDriverName");
			if(classname==null) {
				throw new Exception("No dbDriverName property given");
			}
			Class.forName(classname);

			source=getProperty("dbSource");
			if(source==null) {
				throw new Exception("No dbSource property given");
			}

			dbProps=new Properties();
			dbProps.put("user", getProperty("dbUser", ""));
			dbProps.put("password", getProperty("dbPass", ""));

			// Set the system-wide and local DB properties here.
			Properties sysprop=System.getProperties();
			setDBOptions(sysprop, dbProps, "dboption.");
			setDBOptions(getConfig(), dbProps, getName()+".dboption.");

			maxAge=getPropertyInt("max_age", 0);
		} catch(Exception e) {
			throw new PoolException("Problem initializing pool filler", e);
		}
	}

	/**
	 * get a new object for the pool.
	 *
	 * The following config entries are required:
	 * <ul>
	 *  <li>dbDriverName - Name of the JDBC driver to use</li>
	 *  <li>dbSource - JDBC url for the database</li>
	 *  <li>dbUser - Database username</li>
	 *  <li>dbPass - Database password</li>
	 * </ul>
	 *
	 * The following config entries are optional:
	 * <ul>
	 *  <li>maxAge - The maximum amount of time (in milliseconds) that the
	 *      connection can live.  Default is forever</li>
	 *  <li>dboptions.* - Any JDBC driver specific options you want to
	 *      pass.</li>
	 * </ul>
	 *
	 * @exception PoolException if a new connection could not be made.
	 */
	@Override
	public PoolAble getObject() throws PoolException {
		JDBCPoolAble p = null;
		try {
			// Grab a connection.
			Connection db = DriverManager.getConnection(source, dbProps);
			// Create the PoolAble object
			p=new JDBCPoolAble(db, maxAge, getPoolHash());
		} catch(Exception e) {
			throw new PoolException(
				"Error getting new DB object for the "
					+ debugName() + " pool:  " + e
				);
		}

		return(p);
	}
}
