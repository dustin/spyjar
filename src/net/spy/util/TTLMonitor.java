// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: TTLMonitor.java,v 1.6 2003/08/05 09:01:05 dustin Exp $

package net.spy.util;

import java.util.Iterator;
import java.util.ArrayList;

import net.spy.SpyThread;

/**
 * Monitor TTLs.
 *
 * @see TTL
 */
public final class TTLMonitor extends SpyThread {

	private ArrayList ttls=null;
	private int expiredTTLs=0;

	private static final long NAPTIME=5000;
	private long lastAddition=0;
	private static final long MAX_QUIESCENCE=300000;

	private static TTLMonitor instance=null;

	/**
	 * Get an instance of TTLMonitor.
	 */
	private TTLMonitor() {
		super();
		ttls=new ArrayList();
		lastAddition=System.currentTimeMillis();
		setName("TTL Monitor");
		setDaemon(true);
		start();
	}

	/** 
	 * Get the singleton instance of the TTLMonitor.
	 * 
	 * @return the TTLMonitor instance.
	 */
	public static synchronized TTLMonitor getTTLMonitor() {
		if(instance==null || (!instance.isAlive())) {
			instance=new TTLMonitor();
		}
		return(instance);
	}

	/**
	 * Add a new TTL to the list we're monitoring.
	 */
	public void add(TTL ttl) {
		lastAddition=System.currentTimeMillis();
		synchronized(ttls) {
			ttls.add(ttl);
		}
	}

	/**
	 * String me.
	 */
	public String toString() {
		StringBuffer sb=new StringBuffer(64);
		sb.append(super.toString());
		sb.append(" - Outstanding TTLs:  ");
		sb.append(ttls.size());
		sb.append(", expired=");
		sb.append(expiredTTLs);
		return(sb.toString());
	}

	private boolean shouldIKeepRunning() {
		long now=System.currentTimeMillis();
		return((lastAddition+MAX_QUIESCENCE) > now);
	}

	/**
	 * Monitor the TTLs.
	 */
	public void run() {
		while(shouldIKeepRunning()) {
			synchronized(ttls) {
				// Reset the expired count
				expiredTTLs=0;
				// Flip through the TTLs
				for(Iterator i=ttls.iterator(); i.hasNext();) {
					TTL ttl=(TTL)i.next();
					// Update the expired count if it's expired
					if(ttl.isExpired()) {
						expiredTTLs++;
					}
					// Have it report itself
					ttl.report();
					// If it's done, remove it
					if(ttl.isClosed()) {
						i.remove();
					} // closed
				} // Iterator
			} // Lock

			try {
				sleep(NAPTIME);
			} catch(InterruptedException e) {
				getLogger().warn("Hey!  Someone interrupted my sleep!", e);
			}
		}
	}

}
