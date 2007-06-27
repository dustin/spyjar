// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>

package net.spy.db;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.spy.SpyObject;
import net.spy.util.SpyConfig;

/**
 * Get a connection source from a config.
 */
public class ConnectionSourceFactory extends SpyObject {

	private static ConnectionSourceFactory instance=null;

	private final ConcurrentMap<String, ConnectionSource> sources;

	/**
	 * Get an instance of ConnectionSourceFactory.
	 */
	private ConnectionSourceFactory() {
		super();
		sources=new ConcurrentHashMap<String, ConnectionSource>();
	}

	/** 
	 * Get the singleton ConnectionSourceFactory instance.
	 */
	public static synchronized ConnectionSourceFactory getInstance() {
		if(instance == null) {
			instance=new ConnectionSourceFactory();
		}
		return instance;
	}

	/**
	 * Set the singleton instance.
	 */
	public static synchronized void setInstance(ConnectionSourceFactory to) {
		instance=to;
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
				getLogger().debug("Instantiating %s", connectionClassName);

				@SuppressWarnings("unchecked")
				Class<? extends ConnectionSource> connectionSourceClass
					=(Class<? extends ConnectionSource>)Class.forName(
							connectionClassName);
				ConnectionSource newSource=connectionSourceClass.newInstance();

				ConnectionSource oldSource=sources.putIfAbsent(
						connectionClassName, newSource);
				source = oldSource == null ? newSource : oldSource;
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
