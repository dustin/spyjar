// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: TTLMonitor.java,v 1.3 2002/11/20 04:52:34 dustin Exp $

package net.spy.util;

import java.util.Iterator;
import java.util.ArrayList;

import net.spy.SpyThread;

/**
 * Monitor TTLs.
 *
 * @see TTL
 */
public class TTLMonitor extends SpyThread {

	private ArrayList ttls=null;

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
		setName("DB TTL Monitor");
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
				for(Iterator i=ttls.iterator(); i.hasNext(); ) {
					TTL ttl=(TTL)i.next();
					ttl.report();
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
