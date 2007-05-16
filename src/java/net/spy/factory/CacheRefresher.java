// Copyright (c) 2006 Dustin Sallings <dustin@spy.net>
// arch-tag: 1A5869F6-2526-4B67-9FC9-6F461EFD77DA

package net.spy.factory;

import java.util.Timer;
import java.util.TimerTask;

import net.spy.SpyObject;

/**
 * Cache refresh singleton for scheduling and performing cache clears.
 */
public class CacheRefresher extends SpyObject {

	private static CacheRefresher instance=null;
	private final Timer timer;

	/**
	 * Constructor for subclasses of CacheRefresher.
	 */
	protected CacheRefresher() {
		super();
		timer=new Timer("CacheRefresh timer", true);
	}

	/**
	 * Get the singleton cache refresher instance.
	 */
	public static synchronized CacheRefresher getInstance() {
		if(instance == null) {
			setInstance(new CacheRefresher());
		}
		return(instance);
	}

	/**
	 * Set a CacheRefresher instance.
	 */
	public static synchronized void setInstance(CacheRefresher to) {
		if(instance != null) {
			throw new IllegalStateException(
					"Attempting to overwrite cache refresher instance");
		}
		instance=to;
	}

	/**
	 * Shut down this cache refresher instance.
	 */
	public void shutdown() {
		timer.cancel();
		instance=null;
	}

	synchronized void performRecache(GenFactoryImpl<?> gf, long when) {
		if(when > gf.getLastRefresh()) {
			gf.recache();
		} else {
			getLogger().info("Avoiding unnecessary recache of %s.", gf);
		}
		gf.setNextRefresh(null);
	}

	/** 
	 * Request a recache of the given factory to get data as of the given date
	 * after the given delay.
	 *
	 * @param when the date we requested the data
	 * @param delay how long to wait before refreshing
	 */
	public synchronized void recache(final GenFactoryImpl<?> gf, final long when,
			long delay) {

		TimerTask nextRefresh=gf.getNextRefresh();
		if(nextRefresh != null) {
			boolean canceled=nextRefresh.cancel();
			getLogger().debug("%s next refresh of %s, scheduling a future one.",
					canceled?"Cancelled":"Did not cancel", gf);
			nextRefresh=null;
		}
		nextRefresh=new TimerTask() {
			@Override
			public void run() { performRecache(gf, when); }
		};
		timer.schedule(nextRefresh, delay);
		gf.setNextRefresh(nextRefresh);
	}

}
