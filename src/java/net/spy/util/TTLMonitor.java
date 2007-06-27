// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.util;

import java.util.Timer;

import net.spy.SpyObject;

/**
 * Monitor TTLs.
 *
 * @see TTL
 */
public final class TTLMonitor extends SpyObject {

	private static TTLMonitor instance=null;

	private final Timer timer;

	/**
	 * Get an instance of TTLMonitor.
	 */
	private TTLMonitor() {
		super();
		// Get a new daemon timer
		timer=new Timer(true);
	}

	/** 
	 * Shutdown the monitor.
	 */
	public synchronized void shutdown() {
		timer.cancel();
		instance=null;
	}

	/** 
	 * Get the singleton instance of the TTLMonitor.
	 * 
	 * @return the TTLMonitor instance.
	 */
	public static synchronized TTLMonitor getTTLMonitor() {
		if(instance==null) {
			instance=new TTLMonitor();
		}
		return(instance);
	}

	/**
	 * Add a new TTL to the list we're monitoring.
	 */
	public void add(TTL ttl) {
		timer.scheduleAtFixedRate(ttl, ttl.getTTL(), ttl.getReportInterval());
	}

}
