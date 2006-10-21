// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 22BCEBFB-1663-47CA-9287-B3B1D7F39C0E

package net.spy.stat;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.spy.SpyObject;

/**
 * Holder of the stats.
 */
public class Stats extends SpyObject {

	private static Stats instance=null;

	private ConcurrentMap<String, Stat> stats=null;

	protected Stats() {
		super();
		stats=new ConcurrentHashMap<String, Stat>();
	}

	/**
	 * Get the singleton Stats instance.
	 */
	public static synchronized Stats getInstance() {
		if(instance == null) {
			instance=new Stats();
		}
		return instance;
	}

	/**
	 * Set the singleton stats instance.
	 */
	public static synchronized void setInstance(Stats to) {
		instance=to;
	}

	/**
	 * Get a stat by name.
	 * 
	 * @param name the name of the stat
	 * @param kind the kind of stat object to get
	 * @return the stat instance
	 */
	public static Stat getStat(String name, Class<? extends Stat> kind) {
		Stats s=getInstance();
		Stat rv=s.stats.get(name);

		if(rv == null) {
			try {
				rv=kind.newInstance();
				rv.setName(name);
				Stat oldStat=s.stats.putIfAbsent(name, rv);
				if(oldStat != null) {
					rv=oldStat;
				}
			} catch(RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException("Couldn't create a " + kind, e);
			}
		}

		assert kind.isAssignableFrom(rv.getClass())
			: rv + " is not an instance of " + kind;
		return rv;
	}

	/**
	 * Get a counter stat by name (convenience).
	 * 
	 * @param name name of the stat
	 * @return the stat
	 */
	public static CounterStat getCounterStat(String name) {
		return (CounterStat)getStat(name, CounterStat.class);
	}

	/**
	 * Get a computing stat by name (convenience).
	 * 
	 * @param name name of the stat
	 * @return the stat
	 */
	public static ComputingStat getComputingStat(String name) {
		return (ComputingStat)getStat(name, ComputingStat.class);
	}

	/**
	 * Get the stat with the given name.
	 * 
	 * @param name the name of the stat
	 * @return the Stat, or null if there's no stat with that name
	 */
	public static Stat getStat(String name) {
		return getInstance().stats.get(name);
	}

	/**
	 * Get all known stats.
	 */
	public Map<String, Stat> getStats() {
		return Collections.unmodifiableMap(stats);
	}

}
