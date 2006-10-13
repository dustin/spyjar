// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 9C8BC02C-7D2D-4948-9A0D-556DFF753392

package net.spy.db;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import net.spy.SpyObject;
import net.spy.util.SpyConfig;

/**
 * Get a connection source from a config.
 */
public class ConnectionSourceFactory extends SpyObject {

	private static AtomicReference<ConnectionSourceFactory> instanceRef=
		new AtomicReference<ConnectionSourceFactory>(null);

	private ConcurrentMap<String, ConnectionSource> sources=null;

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
	public static ConnectionSourceFactory getInstance() {
		ConnectionSourceFactory rv=instanceRef.get();
		if(rv == null) {
			rv=new ConnectionSourceFactory();
			if(! instanceRef.compareAndSet(null, rv)) {
				rv=instanceRef.get();
				assert rv != null;
			}
		}
		return(rv);
	}

	/**
	 * Set the singleton instance.
	 */
	public static void setInstance(ConnectionSourceFactory to) {
		instanceRef.set(to);
	}

	/** 
	 * Get a connection source by name.
	 * @param conf configuration specifying how to get the source
	 * @return the connection source
	 * @exception RuntimeException if the specified connection source
	 *   cannot be instantiated
	 */
	@SuppressWarnings("unchecked")
	public ConnectionSource getConnectionSource(SpyConfig conf) {
		String connectionClassName=conf.get("dbConnectionSource",
			"net.spy.db.ObjectPoolConnectionSource");

		ConnectionSource source=sources.get(connectionClassName);

		// OK, we now know *how* we're going to get connections, let's get
		// the source object.
		if(source == null) {
			try {
				getLogger().debug("Instantiating %s", connectionClassName);

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
