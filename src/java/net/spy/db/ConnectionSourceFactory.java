// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 9C8BC02C-7D2D-4948-9A0D-556DFF753392

package net.spy.db;

import java.util.HashMap;
import java.util.Map;

import net.spy.SpyObject;
import net.spy.util.SpyConfig;

/**
 * Get a connection source from a config.
 */
public class ConnectionSourceFactory extends SpyObject {

	private static ConnectionSourceFactory instance=null;

	private Map<String, ConnectionSource> sources=null;

	/**
	 * Get an instance of ConnectionSourceFactory.
	 */
	private ConnectionSourceFactory() {
		super();
		sources=new HashMap<String, ConnectionSource>();
	}

	/** 
	 * Get the singleton ConnectionSourceFactory instance.
	 */
	public static synchronized ConnectionSourceFactory getInstance() {
		if(instance == null) {
			instance=new ConnectionSourceFactory();
		}
		return(instance);
	}

	/** 
	 * Get a connection source by name.
	 * @param conf configuration specifying how to get the source
	 * @return the connection source
	 * @exception RuntimeException if the specified connection source
	 *   cannot be instantiated
	 */
	public ConnectionSource getConnectionSource(SpyConfig conf) {
		String connectionClassName=conf.get("dbConnectionSource",
			"net.spy.db.ObjectPoolConnectionSource");

		ConnectionSource source=sources.get(connectionClassName);

		// OK, we now know *how* we're going to get connections, let's get
		// the source object.
		if(source == null) {
			try {
				getLogger().debug("Instantiating " + connectionClassName);

				Class connectionSourceClass=Class.forName(connectionClassName);
				source=(ConnectionSource)connectionSourceClass.newInstance();

				sources.put(connectionClassName, source);
			} catch(Exception e) {
				RuntimeException re=new RuntimeException(
					"Cannot initialize connection source: "
						+ connectionClassName);
				re.initCause(e);
				throw re;
			}
		}

		return(source);
	}

}
