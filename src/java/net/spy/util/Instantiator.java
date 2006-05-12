// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: F39611D0-301F-49AA-8128-AE7114BD7F34

package net.spy.util;

import net.spy.SpyObject;

/**
 * Generic object instantiator.  The factory factory.
 *
 * @param <C> the type of object to be instantiated.
 */
public class Instantiator<C> extends SpyObject {

	private C inst=null;
	private boolean hasSet=false;

	/**
	 * Default constructor to allow subclasses to figure out how they want to
	 * build their instance.  It's expected that the instance will be set after
	 * this invocation.
	 * 
	 * @see #setInstance(Object)
	 */
	protected Instantiator() throws Exception {
		super();
	}

	/**
	 * Create an instantiator for the given configuration name and default.
	 * @param className the name of the default implementation
	 * 
	 * @throws Exception if the instance can't be instantiated
	 */
	public Instantiator(String className) throws Exception {
		super();
		setInstance(createInstance(className));
	}

	/**
	 * Set the instance.  This may only be invoked once.
	 * 
	 * @param i the instance
	 */
	protected void setInstance(C i) {
		assert !hasSet : "Instance has already been set.";
		inst=i;
		hasSet=true;
	}

	/**
	 * Create an instance of the given class (expected to be a C).
	 * 
	 * @param className the name of the class
	 * @return the new instance
	 * @throws Exception if the class can't be instantiated
	 */
	@SuppressWarnings("unchecked")
	protected C createInstance(String className) throws Exception {
		getLogger().info("Initializing %s", className);
		Class<C> c=(Class<C>) Class.forName(className);
		C rv=c.newInstance();
		getLogger().info("Initialization complete.");
		return rv;
	}

	/**
	 * Get the instantiated instance.
	 */
	public C getInstance() throws Exception {
		assert hasSet : "Instance has not been set.";
		return inst;
	}

}
