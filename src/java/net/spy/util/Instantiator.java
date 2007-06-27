// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

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
	 * Create an instantiator for the given class name.
	 * @param className the name of the class to instantiate
	 * 
	 * @throws Exception if the instance can't be instantiated
	 */
	public Instantiator(String className) throws Exception {
		super();
		setInstance(createInstance(className));
	}

	/**
	 * Create an instantiator for the given class name in the given class
	 * loader.
	 * @param className the name of the class to load
	 * @param cl the class loader to use to instantiate the class
	 * 
	 * @throws Exception if the instance can't be instantiated
	 */
	public Instantiator(String className, ClassLoader cl) throws Exception {
		super();
		setInstance(createInstance(className, cl));
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
	protected C createInstance(String className) throws Exception {
		getLogger().info("Initializing %s", className);
		@SuppressWarnings("unchecked")
		Class<C> c=(Class<C>) Class.forName(className);
		C rv=c.newInstance();
		getLogger().info("Initialization complete.");
		return rv;
	}

	/**
	 * Create an instance of the given class (expected to be a C).
	 * 
	 * @param className the name of the class
	 * @return the new instance
	 * @throws Exception if the class can't be instantiated
	 */
	protected C createInstance(String className, ClassLoader cl)
		throws Exception {
		getLogger().info("Initializing %s in %s", className, cl);
		@SuppressWarnings("unchecked")
		Class<C> c=(Class<C>) Class.forName(className, true, cl);
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
