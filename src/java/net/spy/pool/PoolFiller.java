//
// arch-tag: 7CFFFDDA-1110-11D9-8178-000A957659CC

package net.spy.pool;

import net.spy.util.SpyConfig;

/**
 * The PoolFiller class is used to populate entries in a pool.  It's an
 * abstract class because the getObject() method must be implemented to
 * build the PoolAbles for whatever types of objects you're pooling.
 */
public abstract class PoolFiller extends Object {

	private SpyConfig conf=null;
	private String name=null;
	private int poolHash=0;

	/**
	 * Get an unitialized PoolFiller object.  The name and config
	 * <i>must</i> be passed in later via setName() and setConfig()
	 * respectively.
	 */
	public PoolFiller() {
		super();
	}

	/**
	 * Get a PoolFiller object.
	 *
	 * @param nm the name to be used for config lookups
	 * @param cnf the config to use
	 */
	public PoolFiller(String nm, SpyConfig cnf) {
		this();
		this.conf=cnf;
		this.name=nm;
	}

	/**
	 * Set the hash to use for debug data.
	 */
	public void setPoolHash(int to) {
		this.poolHash=to;
	}

	/**
	 * Get the hash of the pool this filler is filling.
	 */
	protected int getPoolHash() {
		return(poolHash);
	}

	/**
	 * Get the debug name (including the pool's hash).
	 */
	protected String debugName() {
		return(name + " @" + Integer.toHexString(poolHash));
	}

	/**
	 * Set the name to be used for config lookups.
	 */
	public void setName(String nm) {
		this.name=nm;
	}

	/**
	 * Get the name of this filler.
	 */
	public String getName() {
		return(name);
	}

	/**
	 * Set the config file to use.
	 */
	public void setConfig(SpyConfig cnf) {
		this.conf=cnf;
	}

	/**
	 * Get the config this uses.
	 */
	public SpyConfig getConfig() {
		return(conf);
	}

	/**
	 * Get an object for the pool.
	 *
	 * @exception PoolException if it can't get a new object
	 */
	public abstract PoolAble getObject() throws PoolException;

	protected int getPropertyInt(String what, int def) {
		return(conf.getInt(name + "." + what, def));
	}

	protected String getProperty(String what, String def) {
		return(conf.get(name + "." + what, def));
	}

	protected String getProperty(String what) {
		return(conf.get(name + "." + what));
	}
}
